package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.domain.categoryanalysis.repositories.CategoryAnalysisRepository;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
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
	private static final String TEMPLATE_KIND_STANDARD = "STANDARD";

	private final CategoryAnalysisRepository categoryAnalysisRepository;
	private final CategoryAnalysisMapper categoryAnalysisMapper;
	private final StandardDraftService standardDraftService;
	private final GoogleDeckService googleDeckService;
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
		String deckTitle = "Category Analysis — " + command.category() + " — " + command.clientName();
		String slidesUrl = googleDeckService.createDeck(
				command.createdBy(), deckTitle, command.category(), tokenValues, publicBaseUrl);

		CategoryAnalysisEntity entity = categoryAnalysisMapper.toStandardDeckEntity(
				command,
				deckTitle,
				slidesUrl,
				TEMPLATE_KIND_STANDARD,
				STATUS_SUBMITTED,
				currentTime.nowLocalDateTime());
		return categoryAnalysisMapper.toRecord(categoryAnalysisRepository.save(entity));
	}
}
