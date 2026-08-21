package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
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
 * Pooled implementation of {@link OpenAiChatClient}.
 *
 * <p>One pooled client is created per base URL and response timeout and then reused: creating a
 * pool per request would defeat pooling entirely and leak connections. Features differ in how
 * long they are willing to wait — a full deck outline is a far larger completion than a link
 * lookup — so the timeout is part of the pool key rather than a global ceiling.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Service
@RequiredArgsConstructor
public class OpenAiChatClientImpl implements OpenAiChatClient {

	private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
	private static final String CLIENT_NAME = "openai-chat";

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final Map<String, RestClient> clientsByPoolKey = new ConcurrentHashMap<>();

	@Override
	public String complete(OpenAiChatCall call) {
		String body = exchange(call);
		return contentOrFail(body);
	}

	/**
	 * Sends the request body and returns the raw response body.
	 *
	 * @param call fully described call
	 * @return raw response body
	 * @throws OpenAiExternalException on a non-200 status, transport failure, or interruption
	 */
	String exchange(OpenAiChatCall call) {
		try {
			return client(call.baseUrl(), call.responseTimeout())
					.post()
					.uri(CHAT_COMPLETIONS_PATH)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + call.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(call.requestBody())
					.exchange((request, response) -> {
						String responseBody = new String(response.getBody().readAllBytes());
						if (!response.getStatusCode().is2xxSuccessful()) {
							throw new OpenAiExternalException(OpenAiFailure.HTTP_STATUS,
									response.getStatusCode().value(),
									"OpenAI chat completions returned HTTP "
											+ response.getStatusCode().value());
						}
						return responseBody;
					}, true);
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw interruptedOrTransport(e);
		}
	}

	/**
	 * Extracts the model's message content from a chat completions response envelope.
	 *
	 * @param responseBody raw response body
	 * @return message content
	 * @throws OpenAiExternalException when the body is unreadable or carries no content
	 */
	String contentOrFail(String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (IOException e) {
			throw new OpenAiExternalException(OpenAiFailure.TRANSPORT,
					"OpenAI chat completions response was not valid JSON", e);
		}
		String content = root.path("choices").path(0).path("message").path("content").asText("");
		if (content.isBlank()) {
			throw new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200,
					"OpenAI chat completions returned no message content");
		}
		return content;
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
					"OpenAI chat completions call interrupted", cause);
		}
		return new OpenAiExternalException(OpenAiFailure.TRANSPORT,
				"OpenAI chat completions call failed: " + cause.getMessage(), cause);
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
