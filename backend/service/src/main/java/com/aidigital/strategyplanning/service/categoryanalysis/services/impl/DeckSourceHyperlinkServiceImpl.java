package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.DeckSourceHyperlinkService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SourceLinkService;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link DeckSourceHyperlinkService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckSourceHyperlinkServiceImpl implements DeckSourceHyperlinkService {

	private static final String SOURCES_KEY_SUFFIX = "_sources";

	private final SourceLinkService sourceLinkService;
	private final SlidesRequestFactory requestFactory;
	private final GoogleSlidesGateway slidesGateway;

	@Override
	public void applySourceHyperlinks(String accessToken, String presentationId,
	                                  Map<String, String> tokenValues) {
		Map<String, String> sourceValues = new LinkedHashMap<>();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			String value = tokenValues.get(spec.key());
			if (spec.key().endsWith(SOURCES_KEY_SUFFIX) && StringUtils.hasText(value)) {
				sourceValues.put(spec.key(), value);
			}
		}
		if (sourceValues.isEmpty()) {
			return;
		}
		Map<String, List<SourceLink>> linksByKey = sourceLinkService.resolveSourceLinks(sourceValues);
		if (linksByKey.isEmpty()) {
			return;
		}
		JsonNode presentation = slidesGateway.getPresentation(accessToken, presentationId);
		ArrayNode requests = requestFactory.buildSourceLinkRequests(presentation, sourceValues, linksByKey);
		if (requests.isEmpty()) {
			return;
		}
		try {
			slidesGateway.batchUpdate(accessToken, presentationId, requests);
		} catch (RuntimeException e) {
			log.warn("Could not apply source hyperlinks to deck {}: {}", presentationId, e.getMessage());
		}
	}
}
