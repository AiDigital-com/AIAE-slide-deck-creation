package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import com.aidigital.strategyplanning.domain.casestudy.repositories.CaseStudyRepository;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDeckService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.time.CurrentTime;
import com.aidigital.strategyplanning.service.mappers.casestudy.CaseStudyMapper;
import com.aidigital.strategyplanning.usagelogging.LogUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link CaseStudyService}.
 */
@Service
@RequiredArgsConstructor
public class CaseStudyServiceImpl implements CaseStudyService {

	private static final String STATUS_GENERATED = "GENERATED";

	private final CaseStudyRepository caseStudyRepository;
	private final CaseStudyMapper caseStudyMapper;
	private final CaseStudyProperties caseStudyProperties;
	private final CaseStudyDraftService caseStudyDraftService;
	private final CaseStudyDeckService caseStudyDeckService;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	@LogUsage(action = "case-study.create")
	public CaseStudyRecord create(CreateCaseStudyCommand command) {
		String slidesUrl = caseStudyDeckService.createDeck(
				command.createdBy(),
				command.title(),
				caseStudyDraftService.buildTemplateTokenValues(command));
		CaseStudyEntity entity = caseStudyMapper.toEntity(
				command,
				caseStudyProperties.getTemplateUrl(),
				slidesUrl,
				STATUS_GENERATED,
				currentTime.nowLocalDateTime());
		return caseStudyMapper.toRecord(caseStudyRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "case-study.list")
	public List<CaseStudyRecord> listByUser(String userId) {
		return caseStudyMapper.toRecords(
				caseStudyRepository.findByCreatedByOrderByCreatedAtDesc(userId));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "case-study.get")
	public CaseStudyRecord getById(Long id, String userId) {
		return caseStudyRepository.findByIdAndCreatedBy(id, userId)
				.map(caseStudyMapper::toRecord)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));
	}
}
