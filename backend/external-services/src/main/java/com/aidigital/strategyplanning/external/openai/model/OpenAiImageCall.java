package com.aidigital.strategyplanning.external.openai.model;

import java.time.Duration;

/**
 * One image generation call, fully described by the calling feature.
 *
 * <p>As with chat, the request body is assembled by the caller and sent verbatim: size, quality,
 * background, and model are product decisions about how the artwork should look.
 *
 * @param baseUrl         provider base URL, without the {@code /images/generations} suffix
 * @param apiKey          bearer key for this feature
 * @param requestBody     serialized image generation request body, sent unchanged
 * @param responseTimeout maximum wait for the provider's first response byte
 */
public record OpenAiImageCall(String baseUrl, String apiKey, String requestBody,
							  Duration responseTimeout) {

}
