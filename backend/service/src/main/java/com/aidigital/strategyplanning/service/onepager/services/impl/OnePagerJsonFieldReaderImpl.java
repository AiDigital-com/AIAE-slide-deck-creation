package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerJsonFieldReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Default implementation of {@link OnePagerJsonFieldReader}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnePagerJsonFieldReaderImpl implements OnePagerJsonFieldReader {

	/** Suffix recorded for a blank or placeholder entry inside a required array. */
	public static final String ARRAY_ENTRY_SUFFIX = "[]";

	private static final String MARKDOWN_FENCE = "```";
	private static final char OBJECT_START = '{';
	private static final char OBJECT_END = '}';
	private static final char SPACE = ' ';
	private static final String TRAILING_SENTENCE_MARKS = "[.!]+$";
	private static final String INVALID_JSON_MESSAGE = "AI draft response was not valid JSON";

	/** Model replies that mean "no value" and must never land on the page. */
	private static final Set<String> PLACEHOLDER_VALUES = Set.of(
			"-", "--", "—", "n/a", "na", "none", "unknown", "not available", "not applicable",
			"no data", "tbd", "pending", "missing", "not found", "null", "undefined");

	private final ObjectMapper objectMapper;

	@Override
	public JsonNode parseJson(String content) {
		try {
			return objectMapper.readTree(extractJsonObject(content));
		} catch (IOException e) {
			throw new AppException(ErrorReason.C003, e, INVALID_JSON_MESSAGE);
		}
	}

	/**
	 * Extracts the outermost JSON object from the model output, removing a surrounding markdown
	 * code fence if present.
	 *
	 * @param content raw message content, may be null
	 * @return the substring from the first brace to the last, or the trimmed content when none
	 */
	String extractJsonObject(String content) {
		String trimmed = content == null ? "" : content.trim();
		if (trimmed.startsWith(MARKDOWN_FENCE)) {
			int firstNewline = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf(MARKDOWN_FENCE);
			if (firstNewline > 0 && lastFence > firstNewline) {
				trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
			}
		}
		int start = trimmed.indexOf(OBJECT_START);
		int end = trimmed.lastIndexOf(OBJECT_END);
		return start >= 0 && end > start ? trimmed.substring(start, end + 1) : trimmed;
	}

	@Override
	public String requireText(JsonNode node, String field, OnePagerFieldBudget budget, List<String> missing,
			String prefix) {
		String value = node.path(field).asText("").trim();
		if (!StringUtils.hasText(value) || isPlaceholder(value)) {
			missing.add(prefix + field);
			return null;
		}
		return capToFit(value, budget);
	}

	@Override
	public List<String> requireStringArray(JsonNode node, String field, OnePagerFieldBudget budget,
			List<String> missing, String prefix) {
		JsonNode array = node.path(field);
		if (!array.isArray() || array.isEmpty()) {
			missing.add(prefix + field);
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (JsonNode entry : array) {
			String value = entry.asText("").trim();
			if (!StringUtils.hasText(value) || isPlaceholder(value)) {
				missing.add(prefix + field + ARRAY_ENTRY_SUFFIX);
				continue;
			}
			result.add(capToFit(value, budget));
		}
		return result;
	}

	@Override
	public List<String> optionalStringArray(JsonNode node, String field, OnePagerFieldBudget budget) {
		JsonNode array = node.path(field);
		if (!array.isArray()) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (JsonNode entry : array) {
			String value = entry.asText("").trim();
			if (StringUtils.hasText(value) && !isPlaceholder(value)) {
				result.add(capToFit(value, budget));
			}
		}
		return result;
	}

	@Override
	public boolean isPlaceholder(String value) {
		String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll(TRAILING_SENTENCE_MARKS, "");
		return PLACEHOLDER_VALUES.contains(normalized);
	}

	@Override
	public String capToFit(String value, OnePagerFieldBudget budget) {
		int maxChars = budget.getMaxChars();
		if (value == null || value.length() <= maxChars) {
			return value;
		}
		int cut = value.lastIndexOf(SPACE, maxChars);
		String truncated = (cut > 0 ? value.substring(0, cut) : value.substring(0, maxChars)).trim();
		log.warn("Drafted {} exceeded {} characters, truncated: \"{}\" -> \"{}\"", budget, maxChars, value,
				truncated);
		return truncated;
	}
}
