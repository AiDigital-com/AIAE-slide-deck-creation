package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerOpenAiGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Default implementation of {@link OnePagerOpenAiGateway}.
 */
@Service
@RequiredArgsConstructor
public class OnePagerOpenAiGatewayImpl implements OnePagerOpenAiGateway {

	/** Maximum wait for a provider answer; a full page with every module is a long JSON reply. */
	public static final Duration CALL_TIMEOUT = Duration.ofSeconds(120);

	private static final String NOT_CONNECTED_MESSAGE =
			"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting";

	private final OnePagerProperties properties;
	private final OpenAiChatClient openAiChatClient;

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public String callChatCompletion(String requestBody) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003, NOT_CONNECTED_MESSAGE);
		}
		try {
			return openAiChatClient.complete(new OpenAiChatCall(
					properties.getOpenaiBaseUrl(), properties.getOpenaiApiKey(), requestBody, CALL_TIMEOUT));
		} catch (OpenAiExternalException e) {
			throw draftCallFailure(e);
		}
	}

	/**
	 * Translates a provider failure into this feature's application error.
	 *
	 * @param failure provider failure raised by the OpenAI client
	 * @return the application exception to throw
	 */
	AppException draftCallFailure(OpenAiExternalException failure) {
		return switch (failure.getFailure()) {
			case HTTP_STATUS -> new AppException(ErrorReason.C003,
					"AI drafting call failed with HTTP " + failure.getStatusCode());
			case EMPTY_CONTENT -> new AppException(ErrorReason.C003,
					"AI drafting call returned no content");
			case INCOMPLETE -> new AppException(ErrorReason.C003,
					"AI draft was cut off before completing — try fewer modules or a shorter brief");
			case INTERRUPTED -> new AppException(ErrorReason.C003, failure,
					"AI drafting call interrupted");
			case TRANSPORT -> new AppException(ErrorReason.C003, failure,
					"AI drafting call failed: " + rootMessage(failure));
		};
	}

	/**
	 * Returns the message of the failure's underlying cause, which names the transport problem.
	 *
	 * @param failure provider failure
	 * @return cause message, or the failure's own message when it has no cause
	 */
	String rootMessage(OpenAiExternalException failure) {
		return failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage();
	}
}
