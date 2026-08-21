package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftParser;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyFallbackTokenResolver;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.templates.CaseStudyTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI-backed implementation of {@link CaseStudyDraftService}. Sends the extracted text of all
 * uploaded source documents to the chat completions API with a JSON-object response format so one
 * call drafts every case study form field.
 */
@Service
@RequiredArgsConstructor
public class OpenAiCaseStudyDraftServiceImpl implements CaseStudyDraftService {

	private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(120);

	private final CaseStudyProperties properties;
	private final ObjectMapper objectMapper;
	private final OpenAiChatClient openAiChatClient;
	private final CaseStudyPromptComposer promptComposer;
	private final CaseStudyFallbackTokenResolver fallbackTokenResolver;
	private final CaseStudyDraftParser draftParser;

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public CaseStudyDraft draft(List<SourceDocument> documents) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003,
					"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting");
		}
		String content = callChatCompletion(promptComposer.buildRequestBody(documents));
		String sourceFileNames = documents.stream()
				.map(SourceDocument::fileName)
				.collect(Collectors.joining(", "));
		return draftParser.parseDraft(content, sourceFileNames);
	}

	@Override
	public Map<String, String> buildTemplateTokenValues(CreateCaseStudyCommand command) {
		if (!isConnected()) {
			return fallbackTokenResolver.resolve(command);
		}
		try {
			String content = callChatCompletion(promptComposer.buildTokenMappingRequestBody(command));
			JsonNode root = objectMapper.readTree(content);
			Map<String, String> values = new LinkedHashMap<>();
			for (String key : CaseStudyTemplateTokens.TOKEN_KEYS) {
				String value = draftParser.textOrNull(root, key);
				values.put(key, value == null ? "" : value);
			}
			return values;
		} catch (IOException | RuntimeException e) {
			return fallbackTokenResolver.resolve(command);
		}
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
