package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckRequestAssembler;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link InDepthDeckRequestAssembler}.
 */
@Service
@RequiredArgsConstructor
public class InDepthDeckRequestAssemblerImpl implements InDepthDeckRequestAssembler {

	private static final String TOKEN_OPEN = "{{";
	private static final String TOKEN_CLOSE = "}}";

	private final ObjectMapper objectMapper;
	private final InDepthTemplateTokenRegistry tokenRegistry;
	private final InDepthValueFormatter valueFormatter;
	private final SlidesRequestFactory requestFactory;

	@Override
	public ArrayNode buildDeleteUnselectedSlidesRequests(List<String> slideIds, List<Integer> selectedSlides) {
		Set<Integer> selected = new LinkedHashSet<>(selectedSlides);
		ArrayNode requests = objectMapper.createArrayNode();
		for (int i = 0; i < slideIds.size(); i++) {
			int slideNumber = i + 1;
			if (!selected.contains(slideNumber)) {
				ObjectNode delete = objectMapper.createObjectNode();
				delete.putObject("deleteObject").put("objectId", slideIds.get(i));
				requests.add(delete);
			}
		}
		return requests;
	}

	@Override
	public ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues, List<String> slideIds,
	                                          List<Integer> selectedSlides) {
		ArrayNode requests = objectMapper.createArrayNode();
		for (InDepthTokenSpec spec : tokenRegistry.tokensForSlides(selectedSlides)) {
			if (InDepthTemplateTokens.CHART_DATA_KEYS.contains(spec.key())) {
				continue;
			}
			String rawValue = tokenValues.get(spec.key());
			String value = StringUtils.hasText(rawValue) ? valueFormatter.formatForDeck(spec.key(), rawValue) : "";
			for (Integer slideNumber : spec.slideNumbers()) {
				if (!selectedSlides.contains(slideNumber)) {
					continue;
				}
				int slideIndex = slideNumber - 1;
				if (slideIndex < 0 || slideIndex >= slideIds.size()) {
					continue;
				}
				String pageId = slideIds.get(slideIndex);
				requests.add(requestFactory.replaceAllTextRequest(
						TOKEN_OPEN + spec.key() + TOKEN_CLOSE, value, pageId));
				if (!InDepthTemplateTokens.PLACEHOLDER_ONLY_KEYS.contains(spec.key())) {
					requests.add(requestFactory.replaceAllTextRequest(spec.sampleText(), value, pageId));
				}
			}
		}
		return requests;
	}
}
