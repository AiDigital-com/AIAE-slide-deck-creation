package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiImageClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.aidigital.strategyplanning.external.openai.model.OpenAiImageCall;
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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pooled implementation of {@link OpenAiImageClient}.
 *
 * <p>Image generation is slower than a chat completion and gets its own pool key, so a long
 * render never consumes the connection budget or the timeout of ordinary calls.
 *
 * <p>Every {@code exchange} call passes {@code close = true}. Spring closes the response only in
 * that case, and each handler here reads the whole body into memory before returning, so there is
 * nothing left to stream. With {@code false} the pooled connection is never released and the pool
 * starves after {@code max-connections-per-route} calls.
 */
@Service
@RequiredArgsConstructor
public class OpenAiImageClientImpl implements OpenAiImageClient {

	private static final String IMAGE_GENERATIONS_PATH = "/images/generations";
	private static final String CLIENT_NAME = "openai-images";

	private final PooledRestClientFactory restClientFactory;
	private final ObjectMapper objectMapper;
	private final Map<String, RestClient> clientsByPoolKey = new ConcurrentHashMap<>();

	@Override
	public byte[] generate(OpenAiImageCall call) {
		return decode(exchange(call));
	}

	/**
	 * Sends the request body and returns the raw response body.
	 *
	 * @param call fully described call
	 * @return raw response body
	 * @throws OpenAiExternalException on a non-200 status or a transport failure
	 */
	String exchange(OpenAiImageCall call) {
		try {
			return client(call.baseUrl(), call.responseTimeout())
					.post()
					.uri(IMAGE_GENERATIONS_PATH)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + call.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(call.requestBody())
					.exchange((request, response) -> {
						String responseBody = new String(response.getBody().readAllBytes());
						if (!response.getStatusCode().is2xxSuccessful()) {
							throw new OpenAiExternalException(OpenAiFailure.HTTP_STATUS,
									response.getStatusCode().value(),
									"OpenAI image generation returned HTTP "
											+ response.getStatusCode().value(),
									responseBody);
						}
						return responseBody;
					}, true);
		} catch (ResourceAccessException | UncheckedIOException e) {
			throw new OpenAiExternalException(OpenAiFailure.TRANSPORT,
					"OpenAI image generation call failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Decodes the base64 image payload out of an image generation response.
	 *
	 * @param responseBody raw response body
	 * @return decoded image bytes
	 * @throws OpenAiExternalException when the body is unreadable or carries no image
	 */
	byte[] decode(String responseBody) {
		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (IOException e) {
			throw new OpenAiExternalException(OpenAiFailure.TRANSPORT,
					"OpenAI image generation response was not valid JSON", e);
		}
		String base64 = root.path("data").path(0).path("b64_json").asText("");
		if (base64.isBlank()) {
			throw new OpenAiExternalException(OpenAiFailure.EMPTY_CONTENT, 200,
					"OpenAI image generation returned no image payload");
		}
		return Base64.getDecoder().decode(base64);
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
