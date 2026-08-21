package com.aidigital.strategyplanning.service.rndrequest.services.impl;

import com.aidigital.strategyplanning.domain.rndrequest.entities.RndRequestEntity;
import com.aidigital.strategyplanning.domain.rndrequest.repositories.RndRequestRepository;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.mappers.rndrequest.RndRequestMapper;
import com.aidigital.strategyplanning.service.rndrequest.config.RndRequestProperties;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RndRequestServiceImpl} triage behavior.
 */
class RndRequestServiceImplTest {

	private RndRequestRepository repository;
	private RndRequestServiceImpl service;

	@BeforeEach
	void setUp() {
		repository = mock(RndRequestRepository.class);
		when(repository.save(any(RndRequestEntity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		RndRequestProperties properties = new RndRequestProperties();
		service = new RndRequestServiceImpl(
				repository,
				Mappers.getMapper(RndRequestMapper.class),
				properties,
				new CapabilityScanServiceImpl(properties),
				new RndResponseDraftComposerImpl(),
				new CurrentTimeImpl());
	}

	@Test
	@DisplayName("buy at exactly the $100k threshold escalates to RnD with a drafted summary")
	void createEscalatesAtThreshold() {
		var record = service.create(command(new BigDecimal("100000")));

		assertThat(record.decision()).isEqualTo("ESCALATE_TO_RND");
		assertThat(record.responseDraft()).contains("RND ESCALATION");
		assertThat(record.status()).isEqualTo("SUBMITTED");
	}

	@Test
	@DisplayName("buy below the threshold produces a positive workaround response that never says no")
	void createDraftsWorkaroundBelowThreshold() {
		var record = service.create(command(new BigDecimal("99999.99")));

		assertThat(record.decision()).isEqualTo("WORKAROUND");
		assertThat(record.responseDraft())
				.contains("RELAY-READY RESPONSE")
				.doesNotContainIgnoringCase("we cannot")
				.doesNotContainIgnoringCase("declined");
	}

	@Test
	@DisplayName("capability summary reports Slite and Asana as not connected when keys are blank")
	void capabilitySummaryReportsNotConnected() {
		var record = service.create(command(new BigDecimal("5000")));

		assertThat(record.capabilitySummary())
				.contains("Slite knowledge base: not connected")
				.contains("Asana projects: not connected");
	}

	@Test
	@DisplayName("negative buy amount is rejected as a malformed request")
	void createRejectsNegativeBuyAmount() {
		assertThatThrownBy(() -> service.create(command(new BigDecimal("-1"))))
				.isInstanceOf(AppException.class);
	}

	@Test
	@DisplayName("blank title is rejected as a malformed request")
	void createRejectsBlankTitle() {
		var blankTitle = new CreateRndRequestCommand(
				"   ", null, null, null, new BigDecimal("5000"), "user_1");
		assertThatThrownBy(() -> service.create(blankTitle))
				.isInstanceOf(AppException.class);
	}

	@Test
	@DisplayName("persisted entity carries the creator and triage decision")
	void createPersistsOwnershipAndDecision() {
		service.create(command(new BigDecimal("250000")));

		ArgumentCaptor<RndRequestEntity> captor = ArgumentCaptor.forClass(RndRequestEntity.class);
		org.mockito.Mockito.verify(repository).save(captor.capture());
		assertThat(captor.getValue().getCreatedBy()).isEqualTo("user_1");
		assertThat(captor.getValue().getDecision()).isEqualTo("ESCALATE_TO_RND");
		assertThat(captor.getValue().getCreatedAt()).isNotNull();
	}

	private CreateRndRequestCommand command(BigDecimal buyAmount) {
		return new CreateRndRequestCommand(
				"Real-time inventory sync",
				"Growth — EMEA",
				"Client needs real-time stock sync across channels.",
				"Batch sync every 15 minutes exists today.",
				buyAmount,
				"user_1");
	}
}
