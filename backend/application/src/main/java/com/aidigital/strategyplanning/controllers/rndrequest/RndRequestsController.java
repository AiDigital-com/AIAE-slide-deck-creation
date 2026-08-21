package com.aidigital.strategyplanning.controllers.rndrequest;

import com.aidigital.strategyplanning.api.v1.RndRequestsApi;
import com.aidigital.strategyplanning.api.v1.model.CreateRndRequestRequestV1;
import com.aidigital.strategyplanning.api.v1.model.RndRequestSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.RndRequestV1;
import com.aidigital.strategyplanning.mappers.rndrequest.RndRequestApiMapper;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.service.rndrequest.services.RndRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the RnD Request Triage aggregate.
 * Implements the generated {@link RndRequestsApi} contract — no raw mapping annotations.
 */
@RestController
@RequiredArgsConstructor
public class RndRequestsController implements RndRequestsApi {

	private final RndRequestService rndRequestService;
	private final RndRequestApiMapper rndRequestApiMapper;
	private final AppUserFactory appUserFactory;

	@Override
	public ResponseEntity<List<RndRequestSummaryV1>> listRndRequests() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				rndRequestApiMapper.toSummaryV1List(rndRequestService.listByUser(user.userId())));
	}

	@Override
	public ResponseEntity<RndRequestV1> createRndRequest(CreateRndRequestRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = rndRequestApiMapper.toCommand(request, user.userId());
		var record = rndRequestService.create(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(rndRequestApiMapper.toV1(record));
	}

	@Override
	public ResponseEntity<RndRequestV1> getRndRequest(Long id) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				rndRequestApiMapper.toV1(rndRequestService.getById(id, user.userId())));
	}
}
