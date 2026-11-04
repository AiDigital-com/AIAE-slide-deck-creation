package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.service.rfpoutline.enums.RfpOutlineSection;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDocRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Default implementation of {@link RfpOutlineDocRequestFactory}. Inserts the whole text in one
 * request and then styles the heading paragraphs by index range, because the Docs API addresses
 * paragraphs by UTF-16 offset, which {@link String#length()} also counts.
 */
@Service
@RequiredArgsConstructor
public class RfpOutlineDocRequestFactoryImpl implements RfpOutlineDocRequestFactory {

	/** Index of the first body character in a blank document. */
	public static final int BODY_START_INDEX = 1;

	private static final String TITLE_STYLE = "TITLE";
	private static final String SECTION_HEADING_STYLE = "HEADING_2";
	private static final String BODY_STYLE = "NORMAL_TEXT";
	private static final String PARAGRAPH_BREAK = "\n";
	private static final String SECTION_GAP = "\n";

	private final ObjectMapper objectMapper;

	@Override
	public ArrayNode buildDocumentRequests(String title, Map<String, String> sectionValues) {
		ArrayNode requests = objectMapper.createArrayNode();
		StringBuilder text = new StringBuilder();
		ArrayNode styles = objectMapper.createArrayNode();
		appendParagraph(text, styles, title, TITLE_STYLE);
		for (RfpOutlineSection section : RfpOutlineSection.values()) {
			String value = sectionValues.get(section.getKey());
			if (!StringUtils.hasText(value)) {
				continue;
			}
			text.append(SECTION_GAP);
			appendParagraph(text, styles, section.getHeading(), SECTION_HEADING_STYLE);
			appendParagraph(text, styles, value.trim(), BODY_STYLE);
		}
		ObjectNode insert = requests.addObject().putObject("insertText");
		insert.putObject("location").put("index", BODY_START_INDEX);
		insert.put("text", text.toString());
		requests.addAll(styles);
		return requests;
	}

	/**
	 * Appends one paragraph to the document text and records the style request for its range.
	 *
	 * @param text   document text built so far, extended in place
	 * @param styles style requests built so far, extended in place
	 * @param value  paragraph text, without a trailing line break
	 * @param style  Docs named style to apply to the paragraph
	 */
	void appendParagraph(StringBuilder text, ArrayNode styles, String value, String style) {
		int start = BODY_START_INDEX + text.length();
		text.append(value).append(PARAGRAPH_BREAK);
		int end = BODY_START_INDEX + text.length();
		ObjectNode update = styles.addObject().putObject("updateParagraphStyle");
		update.putObject("range").put("startIndex", start).put("endIndex", end);
		update.putObject("paragraphStyle").put("namedStyleType", style);
		update.put("fields", "namedStyleType");
	}
}
