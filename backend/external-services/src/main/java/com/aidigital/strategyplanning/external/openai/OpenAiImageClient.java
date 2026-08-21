package com.aidigital.strategyplanning.external.openai;

import com.aidigital.strategyplanning.external.openai.model.OpenAiImageCall;

/**
 * Narrow outbound boundary for the OpenAI image generation API.
 *
 * <p>Returns decoded image bytes rather than a provider URL, because the current model answers
 * with base64 content and the application hosts the bytes itself.
 */
public interface OpenAiImageClient {

	/**
	 * Sends a prepared image generation request and returns the decoded image bytes.
	 *
	 * @param call fully described call, including the serialized request body
	 * @return decoded image bytes, never empty
	 * @throws OpenAiExternalException when the provider fails, answers non-200, or returns no
	 *                                image payload
	 */
	byte[] generate(OpenAiImageCall call);
}
