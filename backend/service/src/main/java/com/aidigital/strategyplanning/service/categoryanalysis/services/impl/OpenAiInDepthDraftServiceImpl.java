package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.ClientWebsiteReader;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftParser;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftPromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthFactChecker;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthOpenAiGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * OpenAI-backed implementation of {@link InDepthDraftService}.
 *
 * <p><b>Drafting is several passes, not one.</b> Asking the model to both generate every field
 * and ground it in live search in a single {@code web_search}-enabled call scales its output with
 * how much of the deck is checked, so larger selections get cut off mid-draft — and
 * {@code web_search} cannot be combined with the API's strict JSON output mode at all. Instead:
 * <ol>
 *   <li>{@link #draftAllFieldsFromKnowledge} drafts every checked field in one chat completions
 *       call from the model's own knowledge, in strict JSON mode.</li>
 *   <li>{@link InDepthFactChecker} fact-checks just the quantitative fields against live search,
 *       one small {@code web_search} call per topic group, best-effort.</li>
 *   <li>{@link #checkAlignment} separately confirms the category fits the client's business,
 *       another small JSON-mode call, also best-effort.</li>
 * </ol>
 * It shares the same OpenAI key and base URL as the Standard drafting service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiInDepthDraftServiceImpl implements InDepthDraftService {

	private static final String NOT_CONNECTED_MESSAGE =
			"AI drafting engine is not connected — add OPENAI_API_KEY to enable drafting";

	private final CategoryAnalysisProperties properties;
	private final InDepthOpenAiGateway openAiGateway;
	private final ClientWebsiteReader clientWebsiteReader;
	private final InDepthTemplateTokenRegistry tokenRegistry;
	private final InDepthDraftPromptComposer promptComposer;
	private final InDepthDraftParser draftParser;
	private final InDepthFactChecker factChecker;

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public InDepthDraft draftInDepth(String category, String clientName, String guidanceNotes,
	                                 String clientWebsite, List<Integer> selectedSlides) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003, NOT_CONNECTED_MESSAGE);
		}
		if (selectedSlides == null || selectedSlides.isEmpty()) {
			throw new AppException(ErrorReason.C003,
					"No slides selected — check at least one slide to include before drafting");
		}
		List<InDepthTokenSpec> tokens = tokenRegistry.tokensForSlides(selectedSlides);
		if (tokens.isEmpty()) {
			// Every checked slide is content-free (the cover or a divider): a valid selection with
			// nothing to draft, not an error, so no AI call is made.
			return new InDepthDraft(List.of(), null);
		}
		String websiteText = clientWebsiteReader.fetchWebsiteText(clientWebsite);
		List<InDepthDraftField> drafted = draftAllFieldsFromKnowledge(category, clientName, guidanceNotes,
				clientWebsite, websiteText, tokens);
		List<InDepthDraftField> verified = factChecker.verifyFactualFields(category, tokens, drafted);
		DraftAlignment alignment = checkAlignment(category, clientName, clientWebsite, websiteText);
		return new InDepthDraft(verified, alignment);
	}

	@Override
	public InDepthDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                                 int slideNumber, String slideNote,
	                                 List<InDepthFieldValue> currentFields) {
		if (!isConnected()) {
			throw new AppException(ErrorReason.C003, NOT_CONNECTED_MESSAGE);
		}
		List<InDepthTokenSpec> slideTokens = tokenRegistry.tokensForSlide(slideNumber);
		if (slideTokens.isEmpty()) {
			throw new AppException(ErrorReason.C003, "Unknown slide number: " + slideNumber);
		}
		String body = promptComposer.buildSlideRequestBody(category, clientName, guidanceNotes,
				slideNumber, slideNote, slideTokens, currentFields);
		return draftParser.parseSlideDraft(openAiGateway.callResponses(body), slideTokens);
	}

	/**
	 * Drafts every checked field in one chat completions call from the model's own knowledge.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param guidanceNotes optional focus areas or constraints, may be null
	 * @param clientWebsite client website URL, included for wording context
	 * @param websiteText   readable website text for wording context, or null when unavailable
	 * @param tokens        every distinct token to draft, across all checked slides
	 * @return drafted fields, in token registry order
	 * @throws AppException with C003 reason when the call fails or any field is missing
	 */
	List<InDepthDraftField> draftAllFieldsFromKnowledge(String category, String clientName,
			String guidanceNotes, String clientWebsite, String websiteText, List<InDepthTokenSpec> tokens) {
		String body = promptComposer.buildAllFieldsRequestBody(category, clientName, guidanceNotes,
				clientWebsite, websiteText, tokens);
		return draftParser.parseFields(draftParser.parseJson(openAiGateway.callChatCompletion(body)), tokens);
	}

	/**
	 * Checks whether the category fits the client's actual business, based on their website.
	 * Best-effort: a failure degrades to an "unconfirmed" verdict rather than blocking an
	 * otherwise successful draft.
	 *
	 * @param category      market category being analysed
	 * @param clientName    client the analysis is prepared for
	 * @param clientWebsite client website URL under review
	 * @param websiteText   readable website text used for alignment, or null when unavailable
	 * @return alignment result; unconfirmed when the website could not be read or the check failed
	 */
	DraftAlignment checkAlignment(String category, String clientName, String clientWebsite,
			String websiteText) {
		try {
			String body = promptComposer.buildAlignmentRequestBody(category, clientName, clientWebsite, websiteText);
			DraftAlignment alignment = draftParser.parseAlignment(
					draftParser.parseJson(openAiGateway.callResponses(body)));
			if (websiteText == null) {
				return new DraftAlignment(alignment.clientBusinessFocus(), false, false,
						"We couldn't read " + clientWebsite + ", so we couldn't confirm the deck matches the "
								+ "client's business. Double-check the website address before sharing.");
			}
			return alignment;
		} catch (RuntimeException e) {
			log.warn("Business-focus alignment check failed, continuing without it: {}", e.getMessage());
			return new DraftAlignment(null, false, false,
					"Alignment could not be checked automatically — review the category against the "
							+ "client's business before sharing.");
		}
	}
}
