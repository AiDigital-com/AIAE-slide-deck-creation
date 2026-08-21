package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Default implementation of {@link RfpOutlineDraftParser}.
 */
@Service
@RequiredArgsConstructor
public class RfpOutlineDraftParserImpl implements RfpOutlineDraftParser {

	private final ObjectMapper objectMapper;

	@Override
	public RfpOutlineDraft parseDraft(String content, String sourceFileNames) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, "AI draft was not valid JSON");
		}
		return new RfpOutlineDraft(
				textOrNull(root, "title"),
				textOrNull(root, "clientName"),
				textOrNull(root, "industry"),
				textOrNull(root, "challenge"),
				textOrNull(root, "opportunity"),
				textOrNull(root, "solution"),
				textOrNull(root, "outcome"),
				textOrNull(root, "deckOutline"),
				sourceFileNames);
	}

	/**
	 * Reads a string property from a JSON node, returning null when missing, null, or blank.
	 *
	 * @param root parsed JSON object
	 * @param key  property name to read
	 * @return trimmed string value, or null when absent
	 */
	public String textOrNull(JsonNode root, String key) {
		JsonNode node = root.path(key);
		if (node.isMissingNode() || node.isNull()) {
			return null;
		}
		String value = node.asText("").trim();
		return StringUtils.hasText(value) ? value : null;
	}
}
