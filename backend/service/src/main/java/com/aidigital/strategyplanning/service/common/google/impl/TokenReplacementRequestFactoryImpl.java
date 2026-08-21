package com.aidigital.strategyplanning.service.common.google.impl;

import com.aidigital.strategyplanning.service.common.google.TokenReplacementRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Default implementation of {@link TokenReplacementRequestFactory}.
 *
 * <p>A null value becomes an empty string so no raw {@code {{token}}} is left visible in the
 * generated file.
 */
@Service
@RequiredArgsConstructor
public class TokenReplacementRequestFactoryImpl implements TokenReplacementRequestFactory {

	private final ObjectMapper objectMapper;

	@Override
	public ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues) {
		ArrayNode requests = objectMapper.createArrayNode();
		for (Map.Entry<String, String> entry : tokenValues.entrySet()) {
			ObjectNode request = objectMapper.createObjectNode();
			ObjectNode inner = request.putObject("replaceAllText");
			inner.putObject("containsText")
					.put("text", "{{" + entry.getKey() + "}}")
					.put("matchCase", true);
			inner.put("replaceText", entry.getValue() == null ? "" : entry.getValue());
			requests.add(request);
		}
		return requests;
	}
}
