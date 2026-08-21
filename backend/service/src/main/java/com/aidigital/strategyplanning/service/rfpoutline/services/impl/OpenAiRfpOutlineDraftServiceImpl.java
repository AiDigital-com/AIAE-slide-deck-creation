package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftParser;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlinePromptComposer;
import com.aidigital.strategyplanning.service.rfpoutline.templates.RfpOutlineTemplateTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-backed implementation of {@link RfpOutlineDraftService}. Sends the extracted text of all
 * uploaded RFP documents to the chat completions API with a JSON-object response format so one
 * call drafts the POV fields and the response deck outline together.
 */
@Service
@RequiredArgsConstructor
public class OpenAiRfpOutlineDraftServiceImpl implements RfpOutlineDraftService {

	/**
	 * Long by design: a fully detailed deckOutline is a large completion (up to
	 * {@code max_tokens=8000}) and can take a while to generate.
	 */
	private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(240);

	private final RfpOutlineProperties properties;
	private final OpenAiChatClient openAiChatClient;
	private final RfpOutlinePromptComposer promptComposer;
	private final RfpOutlineDraftParser draftParser;

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public RfpOutlineDraft draft(List<SourceDocument> documents, String notes) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003,
					"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting");
		}
		String content = callChatCompletion(promptComposer.buildRequestBody(documents, notes));
		String sourceFileNames = documents.stream()
				.map(SourceDocument::fileName)
				.collect(Collectors.joining(", "));
		return draftParser.parseDraft(content, sourceFileNames);
	}

	@Override
	public Map<String, String> buildTemplateTokenValues(CreateRfpOutlineCommand command) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String key : RfpOutlineTemplateTokens.TOKEN_KEYS) {
			values.put(key, "");
		}
		values.put("client_name", nullToEmpty(command.clientName()));
		values.put("industry", nullToEmpty(command.industry()));
		values.put("challenge", nullToEmpty(command.challenge()));
		values.put("opportunity", nullToEmpty(command.opportunity()));
		values.put("solution", nullToEmpty(command.solution()));
		values.put("outcome", nullToEmpty(command.outcome()));
		values.put("deck_outline", nullToEmpty(command.deckOutline()));
		return values;
	}

	/**
	 * Converts null to an empty string.
	 *
	 * @param value possibly-null string
	 * @return the value, or empty string when null
	 */
	public String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Sends a prepared chat completions request body to OpenAI and returns the message content.
	 *
	 * @param requestBody serialized JSON chat completions request body
	 * @return the model's message content string
	 * @throws AppException with C003 reason when the call fails or returns no content
	 */
	public String callChatCompletion(String requestBody) {
		try {
			return openAiChatClient.complete(new OpenAiChatCall(
					properties.getOpenaiBaseUrl(),
					properties.getOpenaiApiKey(),
					requestBody,
					CHAT_TIMEOUT));
		} catch (OpenAiExternalException e) {
			throw draftCallFailure(e);
		}
	}

	/**
	 * Translates a provider failure into this feature's application error, preserving the wording
	 * the UI already surfaces for each case.
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
			case INTERRUPTED -> new AppException(ErrorReason.C003, failure,
					"AI drafting call interrupted");
			case TRANSPORT -> new AppException(ErrorReason.C003, failure,
					"AI drafting call failed: " + rootMessage(failure));
		};
	}

	/**
	 * Returns the message of the failure's underlying cause, which is what the previous direct
	 * HTTP call reported to the user.
	 *
	 * @param failure provider failure
	 * @return cause message, or the failure's own message when it has no cause
	 */
	String rootMessage(OpenAiExternalException failure) {
		return failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage();
	}
}
