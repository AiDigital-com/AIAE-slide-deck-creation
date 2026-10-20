package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiResponsesClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pooled implementation of {@link OpenAiResponsesClient}.
 *
 * <p>A call that uses the {@code web_search} tool runs several searches before it answers, so it
 * is slower than a plain completion and gets its own pool name; as with chat, the response timeout
 * is part of the pool key so each feature waits only as long as it is willing to.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Service
@RequiredArgsConstructor
public class OpenAiResponsesClientImpl implements OpenAiResponsesClient {

	private static final String RESPONSES_PATH = "/responses";
	private static final String CLIENT_NAME = "openai-responses";
	private static final String STATUS_INCOMPLETE = "incomplete";
	private static final String ITEM_TYPE_MESSAGE = "message";
	private static final String CONTENT_TYPE_OUTPUT_TEXT = "output_text";
	private static final String UNKNOWN_REASON = "unknown";

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final Map<String, RestClient> clientsByPoolKey = new ConcurrentHashMap<>();

	@Override
	public OpenAiResponsesResult respond(OpenAiResponsesCall call) {
		return resultOrFail(exchange(call));
	}

	/**
	 * Sends the request body and returns the raw response body.
	 *
	 * @param call fully described call
	 * @return raw response body
	 * @throws OpenAiExternalException on a non-200 status, transport failure, or interruption
	 */
	String exchange(OpenAiResponsesCall call) {
		try {
			return client(call.baseUrl(), call.responseTimeout())
					.post()
					.uri(RESPONSES_PATH)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + call.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(call.requestBody())
					.exchange((request, response) -> {
						String responseBody = new String(response.getBody().readAllBytes());
						if (!response.getStatusCode().is2xxSuccessful()) {
							throw new OpenAiExternalException(OpenAiFailure.HTTP_STATUS,
									response.getStatusCode().value(),
									"OpenAI responses returned HTTP "
											+ response.getStatusCode().value(),
									responseBody);
						}
						return responseBody;
					}, true);
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw interruptedOrTransport(e);
		}
	}

	/**
	 * Extracts the model's output from a Responses API envelope.
	 *
	 * @param responseBody raw response body
	 * @return output text plus the raw output items
	 * @throws OpenAiExternalException when the body is unreadable, the response is reported as
	 *                                incomplete, or no output text is present
	 */
	OpenAiResponsesResult resultOrFail(String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (IOException e) {
			throw new OpenAiExternalException(OpenAiFailure.TRANSPORT,
					"OpenAI responses body was not valid JSON", e);
		}
		if (STATUS_INCOMPLETE.equals(root.path("status").asText(""))) {
			String reason = root.path("incomplete_details").path("reason").asText(UNKNOWN_REASON);
			throw new OpenAiExternalException(OpenAiFailure.INCOMPLETE, 200,
					"OpenAI responses output was cut off before completion (reason: " + reason + ")");
		}
		String outputText = outputText(root);
		if (outputText.isBlank()) {
			throw new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200,
					"OpenAI responses returned no output text");
		}
		JsonNode output = root.path("output");
		return new OpenAiResponsesResult(outputText,
				output.isArray() ? output : objectMapper.createArrayNode());
	}

	/**
	 * Concatenates the model's final text out of a Responses API envelope.
	 *
	 * <p>Prefers the top-level {@code output_text} convenience field; otherwise walks the
	 * {@code output} array and joins the {@code output_text} parts of every {@code message} item,
	 * skipping the {@code web_search_call} items a tool-enabled call interleaves before its answer.
	 *
	 * @param root parsed response envelope
	 * @return concatenated output text, empty when the envelope carries none
	 */
	String outputText(JsonNode root) {
		String direct = root.path(CONTENT_TYPE_OUTPUT_TEXT).asText("");
		if (!direct.isBlank()) {
			return direct;
		}
		StringBuilder text = new StringBuilder();
		for (JsonNode item : root.path("output")) {
			if (!ITEM_TYPE_MESSAGE.equals(item.path("type").asText(""))) {
				continue;
			}
			for (JsonNode part : item.path("content")) {
				if (CONTENT_TYPE_OUTPUT_TEXT.equals(part.path("type").asText(""))) {
					text.append(part.path("text").asText(""));
				}
			}
		}
		return text.toString();
	}

	/**
	 * Maps a Spring transport failure onto the interrupted or generic transport reason.
	 *
	 * @param cause transport failure raised by the REST client
	 * @return the typed provider exception to throw
	 */
	OpenAiExternalException interruptedOrTransport(RuntimeException cause) {
		if (Thread.currentThread().isInterrupted()) {
			return new OpenAiExternalException(OpenAiFailure.INTERRUPTED,
					"OpenAI responses call interrupted", cause);
		}
		return new OpenAiExternalException(OpenAiFailure.TRANSPORT,
				"OpenAI responses call failed: " + cause.getMessage(), cause);
	}

	/**
	 * Returns the pooled client for a base URL and timeout, creating it on first use.
	 *
	 * @param baseUrl         provider base URL
	 * @param responseTimeout maximum wait for the first response byte
	 * @return pooled REST client
	 */
	RestClient client(String baseUrl, Duration responseTimeout) {
		return clientsByPoolKey.computeIfAbsent(baseUrl + "|" + responseTimeout,
				key -> restClientFactory.createClient(CLIENT_NAME, baseUrl, responseTimeout));
	}
}
