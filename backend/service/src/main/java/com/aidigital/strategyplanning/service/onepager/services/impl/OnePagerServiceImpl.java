package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.RecordOnePagerCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDeckService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerService;
import com.aidigital.strategyplanning.usagelogging.LogUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link OnePagerService}.
 *
 * <p>Not transactional anywhere: the Google and OpenAI calls take seconds and must never hold a
 * database connection. The created page is recorded afterwards in the category-analysis entity
 * service's own short transaction, so a failed page creation leaves no row behind. The Google
 * connection check is the same one the Category Analysis tools use, since one Clerk Google
 * connection backs every deck tool.
 */
@Service
@RequiredArgsConstructor
public class OnePagerServiceImpl implements OnePagerService {

	private static final String DECK_TITLE_PREFIX = "One Page Generator";
	private static final String DECK_TITLE_SEPARATOR = " — ";

	private final OnePagerDraftService onePagerDraftService;
	private final OnePagerDeckService onePagerDeckService;
	private final GoogleDeckService googleDeckService;
	private final CategoryAnalysisService categoryAnalysisService;

	@Override
	@LogUsage(action = "one-pager.connections")
	public StandardConnections getConnections(String userId) {
		GoogleConnectionStatus googleStatus = googleDeckService.checkGoogleConnection(userId);
		return new StandardConnections(
				onePagerDraftService.isConnected(),
				googleStatus == GoogleConnectionStatus.CONNECTED,
				googleStatus);
	}

	@Override
	@LogUsage(action = "one-pager.draft")
	public OnePagerDraft draft(OnePagerBrief brief) {
		return onePagerDraftService.draft(brief);
	}

	@Override
	@LogUsage(action = "one-pager.create")
	public OnePagerDeck createDeck(CreateOnePagerCommand command) {
		String deckTitle = deckTitle(command.category(), command.clientName());
		String slidesUrl = onePagerDeckService.createDeck(
				command.createdBy(), deckTitle, command.shellVariant(), command.content());
		CategoryAnalysisRecord record = categoryAnalysisService.recordOnePager(
				new RecordOnePagerCommand(deckTitle, command.category(), slidesUrl, command.createdBy()));
		return new OnePagerDeck(record.id(), deckTitle, slidesUrl);
	}

	/**
	 * Composes the file title the user sees in Drive from the category and client.
	 *
	 * @param category   market category the page targets
	 * @param clientName client the page is prepared for
	 * @return file title
	 */
	String deckTitle(String category, String clientName) {
		return DECK_TITLE_PREFIX + DECK_TITLE_SEPARATOR + category + DECK_TITLE_SEPARATOR + clientName;
	}
}
