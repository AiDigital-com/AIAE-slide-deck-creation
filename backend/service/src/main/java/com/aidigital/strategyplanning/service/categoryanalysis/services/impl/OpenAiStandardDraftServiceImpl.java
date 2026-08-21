package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.ClientWebsiteReader;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftParser;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftPromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftService;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI-backed implementation of {@link StandardDraftService}. Drafts the Standard five-slide
 * Category Analysis deck, and redrafts one slide at a time, with a JSON-object response format so
 * one call fills every template token of the requested scope.
 */
@Service
@RequiredArgsConstructor
public class OpenAiStandardDraftServiceImpl implements StandardDraftService {

	private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(120);

	private final CategoryAnalysisProperties properties;
	private final OpenAiChatClient openAiChatClient;
	private final ClientWebsiteReader clientWebsiteReader;
	private final StandardDraftPromptComposer promptComposer;
	private final StandardDraftParser draftParser;

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public StandardDraft draftStandard(String category, String clientName, String guidanceNotes,
	                                   String clientWebsite, String storyTheme) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003,
					"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting");
		}
		String websiteText = clientWebsiteReader.fetchWebsiteText(clientWebsite);
		String body = promptComposer.buildRequestBody(category, clientName, guidanceNotes,
				clientWebsite, storyTheme, websiteText);
		String content = callChatCompletion(body);
		StandardDraft draft = draftParser.parseDraft(content);
		if (websiteText == null) {
			return unconfirmedAlignment(draft, clientWebsite);
		}
		return draft;
	}

	@Override
	public StandardDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                                  int slideNumber, String slideNote,
	                                  List<StandardFieldValue> currentFields) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003,
					"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting");
		}
		List<StandardTemplateTokens.TokenSpec> slideTokens = promptComposer.slideTokens(slideNumber);
		if (slideTokens.isEmpty()) {
			throw new AppException(ErrorReason.C003, "Unknown slide number: " + slideNumber);
		}
		String body = promptComposer.buildSlideRequestBody(category, clientName, guidanceNotes,
				slideNumber, slideNote, currentFields);
		String content = callChatCompletion(body);
		return draftParser.parseSlideDraft(content, slideNumber);
	}

	/**
	 * Rebuilds the draft with an alignment verdict that says the client site could not be read.
	 *
	 * <p>The deck is still returned: the draft is usable, but the user is told it was not checked
	 * against the client's business rather than being left to assume it was.
	 *
	 * @param draft         draft parsed from the model reply
	 * @param clientWebsite client website URL that could not be read
	 * @return draft carrying an unconfirmed alignment verdict
	 */
	StandardDraft unconfirmedAlignment(StandardDraft draft, String clientWebsite) {
		DraftAlignment unconfirmed = new DraftAlignment(
				draft.alignment().clientBusinessFocus(),
				false,
				false,
				"We couldn't read " + clientWebsite + ", so we couldn't confirm the deck matches the "
						+ "client's business. Double-check the website address before sharing.");
		return new StandardDraft(draft.fields(), unconfirmed);
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
