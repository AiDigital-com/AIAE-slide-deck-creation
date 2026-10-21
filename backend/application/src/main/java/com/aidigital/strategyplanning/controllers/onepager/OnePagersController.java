package com.aidigital.strategyplanning.controllers.onepager;

import com.aidigital.strategyplanning.api.v1.OnePagersApi;
import com.aidigital.strategyplanning.api.v1.model.CreateOnePagerRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDeckV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftV1;
import com.aidigital.strategyplanning.api.v1.model.StandardConnectionsV1;
import com.aidigital.strategyplanning.mappers.categoryanalysis.CategoryAnalysisApiMapper;
import com.aidigital.strategyplanning.mappers.onepager.OnePagerApiMapper;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the One Page Generator.
 * Implements the generated {@link OnePagersApi} contract — no raw mapping annotations.
 */
@RestController
@RequiredArgsConstructor
public class OnePagersController implements OnePagersApi {

	private final OnePagerService onePagerService;
	private final OnePagerApiMapper onePagerApiMapper;
	private final CategoryAnalysisApiMapper categoryAnalysisApiMapper;
	private final AppUserFactory appUserFactory;

	@Override
	public ResponseEntity<StandardConnectionsV1> getOnePagerConnections() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(categoryAnalysisApiMapper.toV1(onePagerService.getConnections(user.userId())));
	}

	@Override
	public ResponseEntity<OnePagerDraftV1> draftOnePager(OnePagerDraftRequestV1 request) {
		appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(onePagerApiMapper.toV1(onePagerService.draft(onePagerApiMapper.toBrief(request))));
	}

	@Override
	public ResponseEntity<OnePagerDeckV1> createOnePagerDeck(CreateOnePagerRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = onePagerApiMapper.toCommand(request, user.userId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(onePagerApiMapper.toDeckV1(onePagerService.createDeck(command)));
	}
}
