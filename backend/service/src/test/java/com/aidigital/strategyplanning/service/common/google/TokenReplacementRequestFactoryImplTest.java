package com.aidigital.strategyplanning.service.common.google;

import com.aidigital.strategyplanning.service.common.google.impl.TokenReplacementRequestFactoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the shared {@code replaceAllText} request factory used by the case study deck
 * and the RFP outline doc.
 */
class TokenReplacementRequestFactoryImplTest {

	@Test
	void shouldCoverAllTokensAndNullValuesTest() {
		// Given: a token map where one value was never filled in
		TokenReplacementRequestFactoryImpl factory =
				new TokenReplacementRequestFactoryImpl(new ObjectMapper());
		Map<String, String> values = new LinkedHashMap<>();
		values.put("client_vertical", "Retail");
		values.put("priority_1", null);

		// When: the replace requests are built
		ArrayNode requests = factory.buildReplaceTextRequests(values);

		// Then: every token is replaced, the null one with an empty string so no raw
		// {{token}} is left visible in the generated file
		assertThat(requests).hasSize(2);
		assertThat(requests.get(0).path("replaceAllText").path("containsText").path("text").asText())
				.isEqualTo("{{client_vertical}}");
		assertThat(requests.get(0).path("replaceAllText").path("replaceText").asText())
				.isEqualTo("Retail");
		assertThat(requests.get(1).path("replaceAllText").path("replaceText").asText())
				.isEmpty();
	}

	@Test
	void shouldMatchCaseSoTokensAreNeverPartiallyReplacedTest() {
		// Given: a single token
		TokenReplacementRequestFactoryImpl factory =
				new TokenReplacementRequestFactoryImpl(new ObjectMapper());

		// When: the request is built
		ArrayNode requests = factory.buildReplaceTextRequests(Map.of("client_name", "Acme"));

		// Then: matching is case-sensitive, so a differently-cased near-match is left alone
		assertThat(requests.get(0).path("replaceAllText").path("containsText").path("matchCase")
				.asBoolean()).isTrue();
	}

	@Test
	void shouldReturnAnEmptyArrayForNoTokensTest() {
		// Given: nothing to replace
		TokenReplacementRequestFactoryImpl factory =
				new TokenReplacementRequestFactoryImpl(new ObjectMapper());

		// When-Then: the caller gets an empty array it can skip the API call on
		assertThat(factory.buildReplaceTextRequests(Map.of())).isEmpty();
	}
}
