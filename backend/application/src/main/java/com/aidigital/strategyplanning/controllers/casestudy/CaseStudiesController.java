package com.aidigital.strategyplanning.controllers.casestudy;

import com.aidigital.strategyplanning.api.v1.CaseStudiesApi;
import com.aidigital.strategyplanning.api.v1.model.CaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudySummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudyV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCaseStudyRequestV1;
import com.aidigital.strategyplanning.mappers.casestudy.CaseStudyApiMapper;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyService;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.uploads.MultipartSourceDocumentReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for the Case Study Builder aggregate.
 * Implements the generated {@link CaseStudiesApi} contract — no raw mapping annotations.
 */
@RestController
@RequiredArgsConstructor
public class CaseStudiesController implements CaseStudiesApi {

	private final CaseStudyService caseStudyService;
	private final CaseStudyDraftService caseStudyDraftService;
	private final MultipartSourceDocumentReader multipartSourceDocumentReader;
	private final CaseStudyProperties caseStudyProperties;
	private final CaseStudyApiMapper caseStudyApiMapper;
	private final AppUserFactory appUserFactory;

	@Override
	public ResponseEntity<List<CaseStudySummaryV1>> listCaseStudies() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				caseStudyApiMapper.toSummaryV1List(caseStudyService.listByUser(user.userId())));
	}

	@Override
	public ResponseEntity<CaseStudyV1> createCaseStudy(CreateCaseStudyRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = caseStudyApiMapper.toCommand(request, user.userId());
		var record = caseStudyService.create(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(caseStudyApiMapper.toV1(record));
	}

	@Override
	public ResponseEntity<CaseStudyDraftV1> draftCaseStudy(List<MultipartFile> files) {
		appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		List<SourceDocument> documents =
				multipartSourceDocumentReader.read(files, caseStudyProperties.getMaxSourceFiles());
		return ResponseEntity.ok(caseStudyApiMapper.toDraftV1(caseStudyDraftService.draft(documents)));
	}

	@Override
	public ResponseEntity<CaseStudyV1> getCaseStudy(Long id) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(
				caseStudyApiMapper.toV1(caseStudyService.getById(id, user.userId())));
	}
}
