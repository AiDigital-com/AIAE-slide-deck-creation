package com.aidigital.strategyplanning.external.openai.model;

import java.time.Duration;

/**
 * One Responses API call, fully described by the calling feature.
 *
 * <p>The request body is assembled by the caller and sent verbatim: model, input, tools such as
 * {@code web_search}, and output ceilings are product decisions that belong to the feature, not to
 * the transport layer.
 *
 * @param baseUrl         provider base URL, without the {@code /responses} suffix
 * @param apiKey          bearer key for this feature
 * @param requestBody     serialized Responses API request body, sent unchanged
 * @param responseTimeout maximum wait for the provider's first response byte
 */
public record OpenAiResponsesCall(String baseUrl, String apiKey, String requestBody,
								  Duration responseTimeout) {

}
