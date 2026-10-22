package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthChartRequestBuilder;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckRequestAssembler;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Google Drive/Slides implementation of {@link InDepthDeckService}.
 * Uses the user's Google OAuth access token (obtained through Clerk SSO) to copy the In-Depth
 * master into the user's own Drive, keep only the checked slides, fill their tokens, and redraw
 * the charts to the drafted values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleInDepthDeckServiceImpl implements InDepthDeckService {

	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);
	private static final String PRESENTATION_URL_PREFIX = "https://docs.google.com/presentation/d/";
	private static final String PRESENTATION_URL_SUFFIX = "/edit";

	private final CategoryAnalysisProperties properties;
	private final GoogleGrantService googleGrantService;
	private final GoogleSlidesGateway slidesGateway;
	private final SlidesPresentationInspector inspector;
	private final InDepthDeckRequestAssembler requestAssembler;
	private final InDepthChartRequestBuilder chartRequestBuilder;
	private final InDepthValueFormatter valueFormatter;

	@Override
	public String createInDepthDeck(String userId, String deckTitle, List<Integer> selectedSlides,
	                                Map<String, String> tokenValues, String publicBaseUrl) {
		if (selectedSlides == null || selectedSlides.isEmpty()) {
			throw new AppException(ErrorReason.C003,
					"No slides selected — check at least one slide to include before creating the deck");
		}
		String accessToken = googleGrantService.requireAccessToken(
				properties.getClerkSecretKey(), userId, REQUIRED_SCOPES);
		String presentationId = slidesGateway.copyPresentation(accessToken,
				properties.getInDepthTemplatePresentationId(), deckTitle);
		JsonNode presentation = slidesGateway.getPresentation(accessToken, presentationId);
		List<String> slideIds = inspector.extractSlideIds(presentation);
		Map<String, String> values = valueFormatter.sortChartSeries(tokenValues);
		ArrayNode requests = requestAssembler.buildDeleteUnselectedSlidesRequests(slideIds, selectedSlides);
		requests.addAll(requestAssembler.buildReplaceTextRequests(values, slideIds, selectedSlides));
		if (!requests.isEmpty()) {
			slidesGateway.batchUpdate(accessToken, presentationId, requests);
		}
		applyCharts(accessToken, presentationId, presentation, selectedSlides, values, publicBaseUrl);
		return PRESENTATION_URL_PREFIX + presentationId + PRESENTATION_URL_SUFFIX;
	}

	/**
	 * Redraws the donut and resizes the bars of a freshly filled deck, one batch per chart. Fully
	 * best-effort: if drawing or applying one chart fails, the deck keeps the master's version of
	 * that chart and still has all its text and the other chart.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation being filled
	 * @param presentation   presentation JSON as copied from the master, before any edits
	 * @param selectedSlides checked slide numbers (1-based)
	 * @param values         final field values with chart series already sorted
	 * @param publicBaseUrl  externally reachable base URL the donut image is served from
	 */
	void applyCharts(String accessToken, String presentationId, JsonNode presentation,
			List<Integer> selectedSlides, Map<String, String> values, String publicBaseUrl) {
		List<ArrayNode> chartBatches;
		try {
			chartBatches = chartRequestBuilder.buildChartRequests(presentation, selectedSlides, values, publicBaseUrl);
		} catch (RuntimeException e) {
			log.warn("Could not build chart updates for deck {}: {}", presentationId, e.getMessage());
			return;
		}
		for (ArrayNode chartBatch : chartBatches) {
			try {
				slidesGateway.batchUpdate(accessToken, presentationId, chartBatch);
			} catch (RuntimeException e) {
				log.warn("Could not update a chart in deck {}: {}", presentationId, e.getMessage());
			}
		}
	}
}
