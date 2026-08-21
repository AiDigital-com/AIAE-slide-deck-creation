package com.aidigital.strategyplanning.controllers.categoryanalysis;

import com.aidigital.strategyplanning.api.v1.CategoryAnalysesApi;
import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCategoryAnalysisRequestV1;
import com.aidigital.strategyplanning.api.v1.model.CreateStandardDeckRequestV1;
import com.aidigital.strategyplanning.api.v1.model.RedraftSlideRequestV1;
import com.aidigital.strategyplanning.api.v1.model.StandardConnectionsV1;
import com.aidigital.strategyplanning.api.v1.model.StandardDraftRequestV1;
import com.aidigital.strategyplanning.api.v1.model.StandardDraftV1;
import com.aidigital.strategyplanning.mappers.categoryanalysis.CategoryAnalysisApiMapper;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * REST controller for the Category Analysis Builder aggregate.
 * Implements the generated {@link CategoryAnalysesApi} contract — no raw mapping annotations.
 */
@RestController
@RequiredArgsConstructor
public class CategoryAnalysesController implements CategoryAnalysesApi {

	private final CategoryAnalysisService categoryAnalysisService;
	private final CategoryAnalysisApiMapper categoryAnalysisApiMapper;
	private final AppUserFactory appUserFactory;

	@Override
	public ResponseEntity<List<CategoryAnalysisSummaryV1>> listCategoryAnalyses() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				categoryAnalysisApiMapper.toSummaryV1List(
						categoryAnalysisService.listByUser(user.userId())));
	}

	@Override
	public ResponseEntity<CategoryAnalysisV1> createCategoryAnalysis(CreateCategoryAnalysisRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = categoryAnalysisApiMapper.toCommand(request, user.userId());
		var record = categoryAnalysisService.create(command);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(categoryAnalysisApiMapper.toV1(record));
	}

	@Override
	public ResponseEntity<CategoryAnalysisV1> getCategoryAnalysis(Long id) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				categoryAnalysisApiMapper.toV1(
						categoryAnalysisService.getById(id, user.userId())));
	}

	@Override
	public ResponseEntity<StandardConnectionsV1> getStandardConnections() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				categoryAnalysisApiMapper.toV1(
						categoryAnalysisService.getStandardConnections(user.userId())));
	}

	@Override
	public ResponseEntity<StandardDraftV1> draftStandardCategoryAnalysis(StandardDraftRequestV1 request) {
		return ResponseEntity.ok(
				categoryAnalysisApiMapper.toV1(
						categoryAnalysisService.draftStandard(
								request.getCategory(),
								request.getClientName(),
								request.getGuidanceNotes(),
								request.getClientWebsite(),
								request.getStoryTheme())));
	}

	@Override
	public ResponseEntity<StandardDraftV1> redraftStandardCategorySlide(RedraftSlideRequestV1 request) {
		return ResponseEntity.ok(
				categoryAnalysisApiMapper.toV1(
						categoryAnalysisService.redraftSlide(
								request.getCategory(),
								request.getClientName(),
								request.getGuidanceNotes(),
								request.getSlideNumber(),
								request.getSlideNote(),
								categoryAnalysisApiMapper.toFieldValues(request.getCurrentFields()))));
	}

	@Override
	public ResponseEntity<CategoryAnalysisV1> createStandardCategoryDeck(CreateStandardDeckRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = categoryAnalysisApiMapper.toCommand(request, user.userId());
		String publicBaseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		var record = categoryAnalysisService.createStandardDeck(command, publicBaseUrl);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(categoryAnalysisApiMapper.toV1(record));
	}
}
