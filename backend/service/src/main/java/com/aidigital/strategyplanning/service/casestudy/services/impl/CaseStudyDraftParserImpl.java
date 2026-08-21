package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftParser;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Default implementation of {@link CaseStudyDraftParser}.
 */
@Service
@RequiredArgsConstructor
public class CaseStudyDraftParserImpl implements CaseStudyDraftParser {

	private final ObjectMapper objectMapper;

	@Override
	public CaseStudyDraft parseDraft(String content, String sourceFileNames) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, "AI draft was not valid JSON");
		}
		return new CaseStudyDraft(
				textOrNull(root, "title"),
				textOrNull(root, "clientName"),
				textOrNull(root, "industry"),
				textOrNull(root, "challenge"),
				textOrNull(root, "solution"),
				textOrNull(root, "results"),
				textOrNull(root, "keyMetrics"),
				textOrNull(root, "timeline"),
				textOrNull(root, "testimonial"),
				sourceFileNames);
	}

	@Override
	public String textOrNull(JsonNode root, String key) {
		JsonNode node = root.path(key);
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText("").trim();
		return StringUtils.hasText(value) ? value : null;
	}
}
