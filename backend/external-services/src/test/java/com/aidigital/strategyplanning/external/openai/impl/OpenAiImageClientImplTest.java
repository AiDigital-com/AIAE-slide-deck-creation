package com.aidigital.strategyplanning.external.openai.impl;

import com.aidigital.strategyplanning.external.common.http.PooledRestClientFactory;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the response handling of {@link OpenAiImageClientImpl}.
 */
class OpenAiImageClientImplTest {

	private OpenAiImageClientImpl client() {
		return new OpenAiImageClientImpl(mock(PooledRestClientFactory.class), new ObjectMapper());
	}

	@Test
	void shouldDecodeTheBase64ImagePayloadTest() {
		// Given: an envelope carrying base64 PNG bytes
		byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
		String body = "{\"data\":[{\"b64_json\":\""
				+ Base64.getEncoder().encodeToString(png) + "\"}]}";

		// When-Then: the raw bytes come back decoded
		assertThat(client().decode(body)).isEqualTo(png);
	}

	@Test
	void shouldReportEmptyContentWhenNoImagePayloadIsPresentTest() {
		// Given: envelopes with no usable image
		// When-Then: each is reported as an empty payload
		for (String body : new String[]{"{}", "{\"data\":[]}", "{\"data\":[{\"b64_json\":\"\"}]}"}) {
			assertThatThrownBy(() -> client().decode(body))
					.isInstanceOf(OpenAiExternalException.class)
					.extracting(e -> ((OpenAiExternalException) e).getFailure())
					.isEqualTo(OpenAiFailure.EMPTY_CONTENT);
		}
	}

	@Test
	void shouldReportATransportFailureWhenTheBodyIsNotJsonTest() {
		// Given: a non-JSON body
		// When-Then: it is a transport failure
		assertThatThrownBy(() -> client().decode("upstream connect error"))
				.isInstanceOf(OpenAiExternalException.class)
				.extracting(e -> ((OpenAiExternalException) e).getFailure())
				.isEqualTo(OpenAiFailure.TRANSPORT);
	}
}
