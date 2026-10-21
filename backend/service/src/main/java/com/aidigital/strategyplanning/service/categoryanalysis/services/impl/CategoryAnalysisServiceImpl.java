package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.domain.categoryanalysis.repositories.CategoryAnalysisRepository;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisTemplateKind;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateInDepthDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.RecordOnePagerCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDraftService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.time.CurrentTime;
import com.aidigital.strategyplanning.service.mappers.categoryanalysis.CategoryAnalysisMapper;
import com.aidigital.strategyplanning.usagelogging.LogUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link CategoryAnalysisService}.
 */
@Service
@RequiredArgsConstructor
public class CategoryAnalysisServiceImpl implements CategoryAnalysisService {

	private static final String STATUS_SUBMITTED = "SUBMITTED";
	private static final String DECK_TITLE_SEPARATOR = " — ";
	private static final String DECK_TITLE_PREFIX = "Category Analysis";

	private final CategoryAnalysisRepository categoryAnalysisRepository;
	private final CategoryAnalysisMapper categoryAnalysisMapper;
	private final StandardDraftService standardDraftService;
	private final GoogleDeckService googleDeckService;
	private final InDepthDraftService inDepthDraftService;
	private final InDepthDeckService inDepthDeckService;
	private final InDepthTemplateTokenRegistry inDepthTokenRegistry;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	@LogUsage(action = "category-analysis.create")
	public CategoryAnalysisRecord create(CreateCategoryAnalysisCommand command) {
		CategoryAnalysisEntity entity = categoryAnalysisMapper.toEntity(
				command, STATUS_SUBMITTED, currentTime.nowLocalDateTime());
		return categoryAnalysisMapper.toRecord(categoryAnalysisRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "category-analysis.list")
	public List<CategoryAnalysisRecord> listByUser(String userId) {
		return categoryAnalysisMapper.toRecords(
				categoryAnalysisRepository.findByCreatedByOrderByCreatedAtDesc(userId));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "category-analysis.get")
	public CategoryAnalysisRecord getById(Long id, String userId) {
		return categoryAnalysisRepository.findByIdAndCreatedBy(id, userId)
				.map(categoryAnalysisMapper::toRecord)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));
	}

	@Override
	@LogUsage(action = "category-analysis.standard-connections")
	public StandardConnections getStandardConnections(String userId) {
		GoogleConnectionStatus googleStatus = googleDeckService.checkGoogleConnection(userId);
		return new StandardConnections(
				standardDraftService.isConnected(),
				googleStatus == GoogleConnectionStatus.CONNECTED,
				googleStatus);
	}

	@Override
	@LogUsage(action = "category-analysis.standard-draft")
	public StandardDraft draftStandard(String category, String clientName, String guidanceNotes,
	                                   String clientWebsite, String storyTheme) {
		return standardDraftService.draftStandard(category, clientName, guidanceNotes, clientWebsite, storyTheme);
	}

	@Override
	@LogUsage(action = "category-analysis.standard-redraft-slide")
	public StandardDraft redraftSlide(String category, String clientName, String guidanceNotes,
	                                  int slideNumber, String slideNote,
	                                  List<StandardFieldValue> currentFields) {
		return standardDraftService.redraftSlide(category, clientName, guidanceNotes,
				slideNumber, slideNote, currentFields);
	}

	@Override
	@Transactional
	@LogUsage(action = "category-analysis.standard-deck")
	public CategoryAnalysisRecord createStandardDeck(CreateStandardDeckCommand command, String publicBaseUrl) {
		Map<String, String> tokenValues = new LinkedHashMap<>();
		for (StandardFieldValue field : command.fields()) {
			tokenValues.put(field.key(), field.value());
		}
		String deckTitle = deckTitle(command.category(), command.clientName());
		String slidesUrl = googleDeckService.createDeck(
				command.createdBy(), deckTitle, command.category(), tokenValues, publicBaseUrl);

		CategoryAnalysisEntity entity = categoryAnalysisMapper.toStandardDeckEntity(
				command,
				deckTitle,
				slidesUrl,
				CategoryAnalysisTemplateKind.STANDARD.getCode(),
				STATUS_SUBMITTED,
				currentTime.nowLocalDateTime());
		return categoryAnalysisMapper.toRecord(categoryAnalysisRepository.save(entity));
	}

	@Override
	@LogUsage(action = "category-analysis.in-depth-slides")
	public List<InDepthSlideSpec> listInDepthSlides() {
		return inDepthTokenRegistry.slides();
	}

	@Override
	@LogUsage(action = "category-analysis.in-depth-draft")
	public InDepthDraft draftInDepth(String category, String clientName, String guidanceNotes,
	                                 String clientWebsite, List<Integer> selectedSlides) {
		return inDepthDraftService.draftInDepth(category, clientName, guidanceNotes, clientWebsite, selectedSlides);
	}

	@Override
	@LogUsage(action = "category-analysis.in-depth-redraft-slide")
	public InDepthDraft redraftInDepthSlide(String category, String clientName, String guidanceNotes,
	                                        int slideNumber, String slideNote,
	                                        List<InDepthFieldValue> currentFields) {
		return inDepthDraftService.redraftSlide(category, clientName, guidanceNotes,
				slideNumber, slideNote, currentFields);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Not transactional: the Google calls take seconds and must not hold a database
	 * connection. The record is saved in the repository's own short transaction once the deck
	 * exists, so a failed deck creation leaves no row behind.
	 */
	@Override
	@LogUsage(action = "category-analysis.in-depth-deck")
	public CategoryAnalysisRecord createInDepthDeck(CreateInDepthDeckCommand command, String publicBaseUrl) {
		Map<String, String> tokenValues = new LinkedHashMap<>();
		for (InDepthFieldValue field : command.fields()) {
			tokenValues.put(field.key(), field.value());
		}
		String deckTitle = deckTitle(command.category(), command.clientName());
		String slidesUrl = inDepthDeckService.createInDepthDeck(
				command.createdBy(), deckTitle, command.selectedSlides(), tokenValues, publicBaseUrl);

		CategoryAnalysisEntity entity = categoryAnalysisMapper.toInDepthDeckEntity(
				command,
				deckTitle,
				slidesUrl,
				CategoryAnalysisTemplateKind.IN_DEPTH.getCode(),
				STATUS_SUBMITTED,
				currentTime.nowLocalDateTime());
		return categoryAnalysisMapper.toRecord(categoryAnalysisRepository.save(entity));
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Carries no {@code @LogUsage} of its own: the One Page Generator already logs
	 * {@code one-pager.create} for the same user action, and a second event would double-count it.
	 */
	@Override
	@Transactional
	public CategoryAnalysisRecord recordOnePager(RecordOnePagerCommand command) {
		CategoryAnalysisEntity entity = categoryAnalysisMapper.toOnePagerEntity(
				command,
				CategoryAnalysisTemplateKind.ONE_PAGER.getCode(),
				STATUS_SUBMITTED,
				currentTime.nowLocalDateTime());
		return categoryAnalysisMapper.toRecord(categoryAnalysisRepository.save(entity));
	}

	/**
	 * Composes the deck title the user sees in Drive from the category and client.
	 *
	 * @param category   market category being analysed
	 * @param clientName client the analysis is prepared for
	 * @return deck title
	 */
	String deckTitle(String category, String clientName) {
		return DECK_TITLE_PREFIX + DECK_TITLE_SEPARATOR + category + DECK_TITLE_SEPARATOR + clientName;
	}
}
