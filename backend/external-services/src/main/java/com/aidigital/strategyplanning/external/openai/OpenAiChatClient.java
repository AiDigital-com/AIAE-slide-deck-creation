package com.aidigital.strategyplanning.external.openai;

import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;

/**
 * Narrow outbound boundary for the OpenAI chat completions API.
 *
 * <p>The caller owns the request body and therefore the whole prompt contract; this interface
 * owns only the transport — pooling, authentication, status handling, and pulling the message
 * content out of the envelope. Vendor types never cross the boundary.
 */
public interface OpenAiChatClient {

	/**
	 * Sends a prepared chat completions request and returns the model's message content.
	 *
	 * @param call fully described call, including the serialized request body
	 * @return the model's message content, never blank
	 * <p>The message content may be multimodal: an {@code image_url} part carrying a {@code data:}
	 * URL is just request-body JSON and passes through unchanged.
	 *
	 * @throws OpenAiExternalException when the provider fails, answers non-200, reports the
	 *                                completion as cut off at the token limit, or returns no
	 *                                message content
	 */
	String complete(OpenAiChatCall call);
}
