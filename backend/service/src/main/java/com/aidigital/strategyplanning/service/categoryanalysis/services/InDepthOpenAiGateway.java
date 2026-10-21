package com.aidigital.strategyplanning.service.categoryanalysis.services;

/**
 * The In-Depth drafting flow's view of OpenAI.
 *
 * <p>Wraps both outbound clients so every provider failure arrives as this feature's application
 * error with the wording the UI already surfaces, and so the feature's key, base URL, and
 * timeout are applied in one place.
 */
public interface InDepthOpenAiGateway {

	/**
	 * Sends a prepared chat completions request body and returns the message content.
	 *
	 * @param requestBody serialized JSON chat completions request body
	 * @return the model's message content string
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the call fails,
	 * is cut off, or returns no content
	 */
	String callChatCompletion(String requestBody);

	/**
	 * Sends a prepared Responses API request body and returns the model's output text.
	 *
	 * @param requestBody serialized JSON Responses API request body
	 * @return the model's output text (expected to be a JSON object per the prompt's instructions)
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the call fails,
	 * is cut off, or returns no content
	 */
	String callResponses(String requestBody);
}
