package com.aidigital.strategyplanning.service.casestudy.services;

/**
 * The Case Study drafting flows' view of OpenAI.
 *
 * <p>Wraps the outbound client so every provider failure arrives as this feature's application
 * error with the wording the UI already surfaces, and so the feature's key, base URL, and
 * timeout are applied in one place.
 */
public interface CaseStudyOpenAiGateway {

	/**
	 * Reports whether the AI drafting engine is configured.
	 *
	 * @return true when an OpenAI API key is present
	 */
	boolean isConnected();

	/**
	 * Sends a prepared chat completions request body and returns the message content.
	 *
	 * @param requestBody serialized JSON chat completions request body
	 * @return the model's message content string
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the engine is
	 * not connected, the call fails, is cut off, or returns no content
	 */
	String callChatCompletion(String requestBody);
}
