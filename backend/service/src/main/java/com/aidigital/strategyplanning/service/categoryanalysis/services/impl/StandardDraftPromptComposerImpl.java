package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftPromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link StandardDraftPromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class StandardDraftPromptComposerImpl implements StandardDraftPromptComposer {

	private final CategoryAnalysisProperties properties;
	private final ObjectMapper objectMapper;
	private final StandardTemplateTokenRegistry tokenRegistry;

	@Override
	public String buildRequestBody(String category, String clientName, String guidanceNotes,
	                               String clientWebsite, String storyTheme, String websiteText) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior media strategist at a digital marketing agency. ")
				.append("Draft the full content of a 5-slide Category Analysis deck.\n\n")
				.append("Category: ").append(category).append('\n')
				.append("Client: ").append(clientName).append('\n')
				.append("Client website: ").append(clientWebsite).append('\n');
		if (StringUtils.hasText(websiteText)) {
			prompt.append("Website content excerpt (use it to understand the client's actual business focus ")
					.append("and to keep the deck relevant to what they sell):\n\"\"\"\n")
					.append(websiteText).append("\n\"\"\"\n");
		} else {
			prompt.append("(The client website could not be retrieved; infer their likely business focus ")
					.append("from the client name and category.)\n");
		}
		if (StringUtils.hasText(storyTheme)) {
			prompt.append("Story theme to guide the narrative arc of the deck: ").append(storyTheme).append('\n')
					.append("Build the slides around this theme so they tell one coherent story, while still ")
					.append("covering the broader market trends the template requires.\n");
		}
		if (StringUtils.hasText(guidanceNotes)) {
			prompt.append("Focus notes from the strategist: ").append(guidanceNotes).append('\n');
		}
		prompt.append("\nSlide structure: 1 title; 2 market trends (headline, intro, 4 trend bullets, ")
				.append("2 big stats with value+label, sources); 3 key drivers (slide title, ")
				.append("headline, intro, 4 driver title+text pairs, sources); 4 media/channel ")
				.append("opportunity (headline, intro, 2 stats with value+label, 2 insight paragraphs, sources); ")
				.append("5 highlights & implications (headline, 5 highlight title+text pairs, ")
				.append("3 implication title+text pairs).\n\n")
				.append("Rules:\n")
				.append("- HEADLINES and the category_name, drivers_title ")
				.append("values in ALL CAPS; titles of drivers/highlights/implications in Title Case.\n")
				.append("- Stats must be plausible, recent (2024-2026), with a value like \"54%\" or \"11.9M\" ")
				.append("and a label completing the sentence.\n")
				.append("- Sources fields start with \"Source: \" and cite plausible real research bodies.\n")
				.append("- Keep each text under 45 words; bullets and driver/highlight texts 20-35 words.\n")
				.append("- Content must be specific to the category and client, ready for an internal review.\n\n")
				.append("Return ONE JSON object with exactly these keys:\n")
				.append("\"fields\": an object with ALL of the following string keys:\n");
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			prompt.append("  - ").append(spec.key())
					.append(" (slide ").append(spec.slideNumber()).append(", ").append(spec.label())
					.append(", e.g. \"").append(abbreviate(spec.sampleText())).append("\")\n");
		}
		prompt.append("\"clientBusinessFocus\": one short sentence summarising the client's core business, ")
				.append("based on the website excerpt above.\n")
				.append("\"alignmentMatches\": boolean — true if the Category clearly fits that business focus, ")
				.append("false if the topic seems unrelated to what the client actually does.\n")
				.append("\"alignmentMessage\": one sentence confirming the deck aligns with the client's business ")
				.append("focus, or warning that the chosen category may not match what the client does.\n")
				.append("Return only the JSON object.");

		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt.toString());
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_object");
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put("temperature", 0.7);
		return payload.toString();
	}

	/**
	 * Builds the chat completions request body asking the model to redraft only one slide,
	 * with the current values of the whole deck supplied as context for consistency.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes original brief steering notes, may be null
	 * @param slideNumber   slide to redraft (1-5)
	 * @param slideNote     optional guidance for this redraft, may be null
	 * @param currentFields current values for every template field, used as context
	 * @return serialized JSON request body
	 */
	@Override
	public String buildSlideRequestBody(String category, String clientName, String guidanceNotes,
	                                    int slideNumber, String slideNote,
	                                    List<StandardFieldValue> currentFields) {
		Map<String, String> currentByKey = new LinkedHashMap<>();
		if (currentFields != null) {
			for (StandardFieldValue field : currentFields) {
				if (field != null && field.key() != null) {
					currentByKey.put(field.key(), field.value() == null ? "" : field.value());
				}
			}
		}
		List<StandardTemplateTokens.TokenSpec> slideTokens = slideTokens(slideNumber);

		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior media strategist at a digital marketing agency. ")
				.append("You are revising an existing 5-slide Category Analysis deck. ")
				.append("Redraft ONLY the fields of slide ").append(slideNumber)
				.append("; keep the deck coherent with the other slides, which are not changing.\n\n")
				.append("Category: ").append(category).append('\n')
				.append("Client: ").append(clientName).append('\n');
		if (StringUtils.hasText(guidanceNotes)) {
			prompt.append("Original brief focus notes: ").append(guidanceNotes).append('\n');
		}
		if (StringUtils.hasText(slideNote)) {
			prompt.append("Guidance for THIS redraft of slide ").append(slideNumber)
					.append(": ").append(slideNote).append('\n');
		}

		prompt.append("\nCurrent content of the whole deck (for context — do not change fields outside slide ")
				.append(slideNumber).append("):\n");
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			String value = currentByKey.getOrDefault(spec.key(), "");
			prompt.append("  - [slide ").append(spec.slideNumber()).append("] ")
					.append(spec.key()).append(": ").append(abbreviate(value)).append('\n');
		}

		prompt.append("\nRules:\n")
				.append("- HEADLINES and the category_name, drivers_title ")
				.append("values in ALL CAPS; titles of drivers/highlights/implications in Title Case.\n")
				.append("- Stats must be plausible, recent (2024-2026), with a value like \"54%\" or \"11.9M\" ")
				.append("and a label completing the sentence.\n")
				.append("- Sources fields start with \"Source: \" and cite plausible real research bodies.\n")
				.append("- Keep each text under 45 words; bullets and driver/highlight texts 20-35 words.\n")
				.append("- Content must be specific to the category and client, ")
				.append("and consistent with the other slides.\n")
				.append("- Produce genuinely fresh wording — do not simply repeat the current values.\n\n")
				.append("Return ONE JSON object with exactly these keys:\n")
				.append("\"fields\": an object with EXACTLY these string keys (slide ")
				.append(slideNumber).append(" only):\n");
		for (StandardTemplateTokens.TokenSpec spec : slideTokens) {
			prompt.append("  - ").append(spec.key())
					.append(" (").append(spec.label())
					.append(", e.g. \"").append(abbreviate(spec.sampleText())).append("\")\n");
		}
		prompt.append("Return only the JSON object.");

		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt.toString());
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_object");
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put("temperature", 0.7);
		return payload.toString();
	}

	/**
	 * Returns the token specs belonging to the given slide, preserving registry order.
	 *
	 * @param slideNumber slide to filter tokens for (1-5)
	 * @return list of token specs for the slide, empty when none match
	 */
	@Override
	public List<StandardTemplateTokens.TokenSpec> slideTokens(int slideNumber) {
		return tokenRegistry.tokensForSlide(slideNumber);
	}


	/**
	 * Shortens a sample text for inclusion in the prompt.
	 *
	 * @param text sample text from the template
	 * @return text truncated to 60 characters
	 */
	String abbreviate(String text) {
		return text.length() <= 60 ? text : text.substring(0, 57) + "...";
	}
}
