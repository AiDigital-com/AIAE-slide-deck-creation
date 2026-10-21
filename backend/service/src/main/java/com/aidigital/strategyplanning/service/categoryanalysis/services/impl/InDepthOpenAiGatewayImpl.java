package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiResponsesClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.external.openai.model.OpenAiResponsesCall;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthOpenAiGateway;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Default implementation of {@link InDepthOpenAiGateway}.
 */
@Service
@RequiredArgsConstructor
public class InDepthOpenAiGatewayImpl implements InDepthOpenAiGateway {

	/**
	 * Maximum wait for a provider answer. Search-grounded calls run several searches first.
	 */
	public static final Duration CALL_TIMEOUT = Duration.ofSeconds(180);

	private final CategoryAnalysisProperties properties;
	private final OpenAiChatClient openAiChatClient;
	private final OpenAiResponsesClient openAiResponsesClient;

	@Override
	public String callChatCompletion(String requestBody) {
		try {
			return openAiChatClient.complete(new OpenAiChatCall(
					properties.getOpenaiBaseUrl(), properties.getOpenaiApiKey(), requestBody, CALL_TIMEOUT));
		} catch (OpenAiExternalException e) {
			throw draftCallFailure(e);
		}
	}

	@Override
	public String callResponses(String requestBody) {
		try {
			return openAiResponsesClient.respond(new OpenAiResponsesCall(
					properties.getOpenaiBaseUrl(), properties.getOpenaiApiKey(), requestBody, CALL_TIMEOUT))
					.outputText();
		} catch (OpenAiExternalException e) {
			throw draftCallFailure(e);
		}
	}

	/**
	 * Translates a provider failure into this feature's application error.
	 *
	 * @param failure provider failure raised by an OpenAI client
	 * @return the application exception to throw
	 */
	AppException draftCallFailure(OpenAiExternalException failure) {
		return switch (failure.getFailure()) {
			case HTTP_STATUS -> new AppException(ErrorReason.C003,
					"AI drafting call failed with HTTP " + failure.getStatusCode());
			case EMPTY_CONTENT -> new AppException(ErrorReason.C003,
					"AI drafting call returned no content");
			case INCOMPLETE -> new AppException(ErrorReason.C003,
					"AI drafting response was cut off before finishing — try drafting fewer slides at once");
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
