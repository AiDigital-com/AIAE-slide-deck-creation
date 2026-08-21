package com.aidigital.strategyplanning.external.openai.model;

import java.time.Duration;

/**
 * One chat completions call, fully described by the calling feature.
 *
 * <p>The request body is assembled by the caller and sent verbatim: response format, model,
 * temperature, and token ceilings are product decisions that belong to the feature, not to the
 * transport layer.
 *
 * @param baseUrl         provider base URL, without the {@code /chat/completions} suffix
 * @param apiKey          bearer key for this feature
 * @param requestBody     serialized chat completions request body, sent unchanged
 * @param responseTimeout maximum wait for the provider's first response byte
 */
public record OpenAiChatCall(String baseUrl, String apiKey, String requestBody,
							 Duration responseTimeout) {

}
