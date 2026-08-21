package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftParser;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link StandardDraftParser}.
 */
@Service
@RequiredArgsConstructor
public class StandardDraftParserImpl implements StandardDraftParser {

	private final ObjectMapper objectMapper;
	private final StandardTemplateTokenRegistry tokenRegistry;

	@Override
	public StandardDraft parseDraft(String content) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, "AI draft was not valid JSON");
		}
		JsonNode fieldsNode = root.path("fields");
		List<StandardDraftField> fields = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			String value = fieldsNode.path(spec.key()).asText("");
			if (!StringUtils.hasText(value)) {
				missing.add(spec.key());
			} else {
				fields.add(new StandardDraftField(spec.key(), spec.label(), spec.slideNumber(), value.trim()));
			}
		}
		if (!missing.isEmpty()) {
			throw new AppException(ErrorReason.C003,
					"AI draft incomplete — missing fields: " + String.join(", ", missing));
		}
		return new StandardDraft(fields, parseAlignment(root));
	}

	/**
	 * Reads the website-based alignment fields from the model output, tolerating omissions. Used
	 * only when the website was successfully read, so the result is always a confirmed check.
	 *
	 * @param root parsed JSON root of the model response
	 * @return confirmed alignment result, defaulting to a matched state when the model omits it
	 */
	public DraftAlignment parseAlignment(JsonNode root) {
		String businessFocus = root.path("clientBusinessFocus").asText("").trim();
		boolean matches = root.path("alignmentMatches").asBoolean(true);
		String message = root.path("alignmentMessage").asText("").trim();
		if (!StringUtils.hasText(message)) {
			message = matches
					? "The deck aligns with the client's business focus."
					: "The chosen category may not match the client's business focus — review before sharing.";
		}
		return new DraftAlignment(StringUtils.hasText(businessFocus) ? businessFocus : null, true, matches, message);
	}

	/**
	 * Parses the model's JSON content into a {@link StandardDraft} for a single slide, validating
	 * that every field of that slide is present.
	 *
	 * @param content     JSON object string returned by the model
	 * @param slideNumber slide being redrafted (1-5)
	 * @return parsed draft holding only the requested slide's fields
	 * @throws AppException with C003 reason when the draft is malformed or incomplete
	 */
	@Override
	public StandardDraft parseSlideDraft(String content, int slideNumber) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, "AI draft was not valid JSON");
		}
		JsonNode fieldsNode = root.path("fields");
		List<StandardDraftField> fields = new ArrayList<>();
		List<String> missing = new ArrayList<>();
		for (StandardTemplateTokens.TokenSpec spec : tokenRegistry.tokensForSlide(slideNumber)) {
			String value = fieldsNode.path(spec.key()).asText("");
			if (!StringUtils.hasText(value)) {
				missing.add(spec.key());
			} else {
				fields.add(new StandardDraftField(spec.key(), spec.label(), spec.slideNumber(), value.trim()));
			}
		}
		if (!missing.isEmpty()) {
			throw new AppException(ErrorReason.C003,
					"AI redraft incomplete — missing fields: " + String.join(", ", missing));
		}
		return new StandardDraft(fields, null);
	}
}
