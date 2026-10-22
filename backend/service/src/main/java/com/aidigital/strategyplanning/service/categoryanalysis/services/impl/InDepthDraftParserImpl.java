package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftParser;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link InDepthDraftParser}.
 */
@Service
@RequiredArgsConstructor
public class InDepthDraftParserImpl implements InDepthDraftParser {

	private static final String CODE_FENCE = "```";
	private static final String FIELDS_KEY = "fields";
	private static final String ALIGNED_MESSAGE = "The deck aligns with the client's business focus.";
	private static final String MISALIGNED_MESSAGE =
			"The chosen category may not match the client's business focus — review before sharing.";

	private final ObjectMapper objectMapper;
	private final InDepthValueFormatter valueFormatter;

	@Override
	public InDepthDraft parseDraft(String content, List<InDepthTokenSpec> tokens) {
		JsonNode root = parseJson(content);
		return new InDepthDraft(parseFields(root, tokens), parseAlignment(root));
	}

	@Override
	public InDepthDraft parseSlideDraft(String content, List<InDepthTokenSpec> slideTokens) {
		return new InDepthDraft(parseFields(parseJson(content), slideTokens), null);
	}

	@Override
	public JsonNode parseJson(String content) {
		try {
			return objectMapper.readTree(extractJsonObject(content));
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, "AI draft was not valid JSON");
		}
	}

	/**
	 * Isolates the JSON object in model output that is not guaranteed to be pure JSON: strips a
	 * leading/trailing fence (with or without a language tag), then takes the substring from the
	 * first {@code {} to the last {@code }}, which is a no-op on already-clean JSON.
	 *
	 * @param content raw model output text
	 * @return the isolated JSON object text, unchanged if no fence or extra text is found
	 */
	String extractJsonObject(String content) {
		String trimmed = content.trim();
		if (trimmed.startsWith(CODE_FENCE)) {
			int firstNewline = trimmed.indexOf('\n');
			int fenceEnd = trimmed.lastIndexOf(CODE_FENCE);
			if (firstNewline > 0 && fenceEnd > firstNewline) {
				trimmed = trimmed.substring(firstNewline + 1, fenceEnd).trim();
			}
		}
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return trimmed.substring(start, end + 1);
		}
		return trimmed;
	}

	@Override
	public List<InDepthDraftField> parseFields(JsonNode root, List<InDepthTokenSpec> tokens) {
		JsonNode fieldsNode = root.path(FIELDS_KEY);
		List<InDepthDraftField> fields = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (InDepthTokenSpec spec : tokens) {
			String value = fieldsNode.path(spec.key()).asText("");
			if (valueFormatter.isPlaceholder(value)) {
				missing.add(spec.key());
			} else {
				fields.add(new InDepthDraftField(spec.key(), spec.label(), spec.slideNumbers(),
						valueFormatter.formatForSlide(spec.key(), valueFormatter.capToFit(spec, value.trim()))));
			}
		}
		if (!missing.isEmpty()) {
			throw new AppException(ErrorReason.C003,
					"AI draft incomplete — missing fields: " + String.join(", ", missing));
		}
		return fields;
	}

	@Override
	public Map<String, String> parseCorrections(JsonNode root, List<InDepthTokenSpec> group) {
		Map<String, String> corrections = new LinkedHashMap<>();
		for (InDepthTokenSpec spec : group) {
			String value = root.path(spec.key()).asText("");
			if (!valueFormatter.isPlaceholder(value)) {
				corrections.put(spec.key(), value);
			}
		}
		return corrections;
	}

	@Override
	public DraftAlignment parseAlignment(JsonNode root) {
		String businessFocus = root.path("clientBusinessFocus").asText("").trim();
		boolean matches = root.path("alignmentMatches").asBoolean(true);
		String message = root.path("alignmentMessage").asText("").trim();
		if (!StringUtils.hasText(message)) {
			message = matches ? ALIGNED_MESSAGE : MISALIGNED_MESSAGE;
		}
		return new DraftAlignment(StringUtils.hasText(businessFocus) ? businessFocus : null, true, matches, message);
	}
}
