package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyIcon;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftParser;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageIconLibraryService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link OnePageDraftParser}.
 */
@Service
@RequiredArgsConstructor
public class OnePageDraftParserImpl implements OnePageDraftParser {

	/** JSON member that carries the drafted field values. */
	public static final String FIELDS_MEMBER = "fields";

	/** JSON member that carries the icon pick per slot. */
	public static final String ICONS_MEMBER = "icons";

	private static final int UNMEASURED = 0;
	private static final String TRAILING_SENTENCE_MARKS = "[.!]+$";
	private static final String INVALID_JSON_MESSAGE = "AI draft was not valid JSON";

	/** Model replies that mean "no value" and must never land on a slide. */
	private static final Set<String> NON_ANSWERS = Set.of(
			"n/a", "na", "none", "null", "unknown", "not available", "not applicable", "tbd", "-", "—",
			"not specified", "not provided", "no data");

	private final ObjectMapper objectMapper;
	private final OnePageTemplateRegistry templateRegistry;
	private final OnePageIconLibraryService iconLibrary;

	@Override
	public OnePageCaseStudyDraft parseDraft(String content, String sourceFileNames) {
		JsonNode root = parseJson(content);
		JsonNode fieldsNode = root.path(FIELDS_MEMBER);
		List<OnePageCaseStudyField> fields = new ArrayList<>();
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			String raw = fieldsNode.path(spec.key()).asText("");
			String value = isNonAnswer(raw) ? "" : templateRegistry.formatValue(spec, raw);
			fields.add(new OnePageCaseStudyField(spec.key(), spec.label(), spec.section(), value, spec.source(),
					spec.maxChars(), UNMEASURED, UNMEASURED, UNMEASURED,
					value.isEmpty() ? OnePageFit.EMPTY : OnePageFit.FITS, null));
		}
		JsonNode iconsNode = root.path(ICONS_MEMBER);
		List<OnePageCaseStudyIcon> icons = new ArrayList<>();
		for (OnePageIconSlot slot : templateRegistry.iconSlots()) {
			boolean hasContent = slot.fieldKeys().stream().anyMatch(key -> fields.stream()
					.anyMatch(field -> field.key().equals(key) && StringUtils.hasText(field.value())));
			String pick = iconsNode.path(slot.slot()).asText("");
			String icon = hasContent && iconLibrary.contains(pick) ? pick : null;
			icons.add(new OnePageCaseStudyIcon(slot.slot(), slot.label(), icon));
		}
		return new OnePageCaseStudyDraft(fields, icons, iconLibrary.options(), sourceFileNames);
	}

	@Override
	public Map<String, String> parseRevisions(String content) {
		JsonNode fieldsNode = parseJson(content).path(FIELDS_MEMBER);
		Map<String, String> revisions = new LinkedHashMap<>();
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			String raw = fieldsNode.path(spec.key()).asText("");
			if (!isNonAnswer(raw)) {
				revisions.put(spec.key(), raw);
			}
		}
		return revisions;
	}

	@Override
	public boolean isNonAnswer(String value) {
		if (!StringUtils.hasText(value)) {
			return true;
		}
		return NON_ANSWERS.contains(value.trim().toLowerCase(Locale.ROOT).replaceAll(TRAILING_SENTENCE_MARKS, ""));
	}

	/**
	 * Parses a model reply as JSON.
	 *
	 * @param content JSON object string returned by the model
	 * @return the parsed tree
	 * @throws AppException with C003 reason when the reply is not valid JSON
	 */
	JsonNode parseJson(String content) {
		try {
			return objectMapper.readTree(content);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, INVALID_JSON_MESSAGE);
		}
	}
}
