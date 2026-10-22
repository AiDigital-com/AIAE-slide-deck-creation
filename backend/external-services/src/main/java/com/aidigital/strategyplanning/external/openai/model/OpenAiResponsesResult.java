package com.aidigital.strategyplanning.external.openai.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * What a Responses API call produced.
 *
 * <p>The output text is what every caller needs; the raw {@code output} array is exposed as well
 * because a call made with the {@code web_search} tool interleaves {@code web_search_call} items
 * and citation annotations with the final message, and a feature may want to read those.
 *
 * @param outputText concatenated text of every {@code output_text} part of the model's message
 *                   items, never blank
 * @param output     the provider's raw {@code output} array, empty when the envelope carried none
 */
public record OpenAiResponsesResult(String outputText, JsonNode output) {

}
