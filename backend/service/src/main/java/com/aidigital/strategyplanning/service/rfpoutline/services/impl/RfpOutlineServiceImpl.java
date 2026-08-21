package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.domain.rfpoutline.entities.RfpOutlineEntity;
import com.aidigital.strategyplanning.domain.rfpoutline.repositories.RfpOutlineRepository;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.time.CurrentTime;
import com.aidigital.strategyplanning.service.mappers.rfpoutline.RfpOutlineMapper;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDocService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineService;
import com.aidigital.strategyplanning.usagelogging.LogUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link RfpOutlineService}.
 */
@Service
@RequiredArgsConstructor
public class RfpOutlineServiceImpl implements RfpOutlineService {

	private static final String STATUS_GENERATED = "GENERATED";

	private final RfpOutlineRepository rfpOutlineRepository;
	private final RfpOutlineMapper rfpOutlineMapper;
	private final RfpOutlineDraftService rfpOutlineDraftService;
	private final RfpOutlineDocService rfpOutlineDocService;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	@LogUsage(action = "rfp-outline.create")
	public RfpOutlineRecord create(CreateRfpOutlineCommand command) {
		String docUrl = rfpOutlineDocService.createDoc(
				command.createdBy(),
				command.title(),
				rfpOutlineDraftService.buildTemplateTokenValues(command));
		RfpOutlineEntity entity = rfpOutlineMapper.toEntity(
				command, docUrl, STATUS_GENERATED, currentTime.nowLocalDateTime());
		return rfpOutlineMapper.toRecord(rfpOutlineRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "rfp-outline.list")
	public List<RfpOutlineRecord> listByUser(String userId) {
		return rfpOutlineMapper.toRecords(rfpOutlineRepository.findByCreatedByOrderByCreatedAtDesc(userId));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "rfp-outline.get")
	public RfpOutlineRecord getById(Long id, String userId) {
		return rfpOutlineRepository.findByIdAndCreatedBy(id, userId)
				.map(rfpOutlineMapper::toRecord)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));
	}
}
