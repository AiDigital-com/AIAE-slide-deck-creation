package com.aidigital.strategyplanning.service.rndrequest.services.impl;

import com.aidigital.strategyplanning.domain.rndrequest.entities.RndRequestEntity;
import com.aidigital.strategyplanning.domain.rndrequest.repositories.RndRequestRepository;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.time.CurrentTime;
import com.aidigital.strategyplanning.service.mappers.rndrequest.RndRequestMapper;
import com.aidigital.strategyplanning.service.rndrequest.config.RndRequestProperties;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;
import com.aidigital.strategyplanning.service.rndrequest.services.CapabilityScanService;
import com.aidigital.strategyplanning.service.rndrequest.services.RndRequestService;
import com.aidigital.strategyplanning.service.rndrequest.services.RndResponseDraftComposer;
import com.aidigital.strategyplanning.usagelogging.LogUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link RndRequestService}.
 */
@Service
@RequiredArgsConstructor
public class RndRequestServiceImpl implements RndRequestService {

	private static final String STATUS_SUBMITTED = "SUBMITTED";
	private static final String DECISION_ESCALATE = "ESCALATE_TO_RND";
	private static final String DECISION_WORKAROUND = "WORKAROUND";

	private final RndRequestRepository rndRequestRepository;
	private final RndRequestMapper rndRequestMapper;
	private final RndRequestProperties rndRequestProperties;
	private final CapabilityScanService capabilityScanService;
	private final RndResponseDraftComposer rndResponseDraftComposer;
	private final CurrentTime currentTime;

	@Override
	@Transactional
	@LogUsage(action = "rnd-request.create")
	public RndRequestRecord create(CreateRndRequestCommand command) {
		if (command.title() == null || command.title().isBlank()) {
			throw new AppException(ErrorReason.C002, "title must not be blank");
		}
		if (command.buyAmount() == null || command.buyAmount().signum() < 0) {
			throw new AppException(ErrorReason.C002, "buyAmount must be zero or greater");
		}
		boolean escalate = command.buyAmount()
				.compareTo(rndRequestProperties.getEscalationThreshold()) >= 0;
		RndRequestEntity entity = rndRequestMapper.toEntity(
				command,
				escalate ? DECISION_ESCALATE : DECISION_WORKAROUND,
				capabilityScanService.scan(command.title(), command.requestDetails()),
				escalate
						? rndResponseDraftComposer.composeEscalation(
								command, rndRequestProperties.getEscalationThreshold())
						: rndResponseDraftComposer.composeWorkaround(command),
				STATUS_SUBMITTED,
				currentTime.nowLocalDateTime());
		return rndRequestMapper.toRecord(rndRequestRepository.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "rnd-request.list")
	public List<RndRequestRecord> listByUser(String userId) {
		return rndRequestMapper.toRecords(
				rndRequestRepository.findByCreatedByOrderByCreatedAtDesc(userId));
	}

	@Override
	@Transactional(readOnly = true)
	@LogUsage(action = "rnd-request.get")
	public RndRequestRecord getById(Long id, String userId) {
		return rndRequestRepository.findByIdAndCreatedBy(id, userId)
				.map(rndRequestMapper::toRecord)
				.orElseThrow(() -> new AppException(ErrorReason.C001, id));
	}
}
