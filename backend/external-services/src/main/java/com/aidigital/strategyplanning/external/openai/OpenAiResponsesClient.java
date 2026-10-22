package com.aidigital.strategyplanning.external.openai;

import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;

/**
 * Narrow outbound boundary for the OpenAI Responses API.
 *
 * <p>This is the endpoint that accepts built-in tools such as {@code web_search}. As with chat
 * completions, the caller owns the request body and this interface owns only the transport —
 * pooling, authentication, status handling, and pulling the output out of the envelope. Vendor
 * types never cross the boundary.
 */
public interface OpenAiResponsesClient {

	/**
	 * Sends a prepared Responses API request and returns the model's output.
	 *
	 * @param call fully described call, including the serialized request body
	 * @return the model's output text plus the raw output items, text never blank
	 * @throws OpenAiExternalException when the provider fails, answers non-200, reports the output
	 *                                as incomplete, or returns no output text
	 */
	OpenAiResponsesResult respond(OpenAiResponsesCall call);
}
