package com.aidigital.strategyplanning.controllers.rfpoutline;

import com.aidigital.strategyplanning.api.v1.RfpOutlinesApi;
import com.aidigital.strategyplanning.api.v1.model.CreateRfpOutlineRequestV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineDraftV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineV1;
import com.aidigital.strategyplanning.mappers.rfpoutline.RfpOutlineApiMapper;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineService;
import com.aidigital.strategyplanning.uploads.MultipartSourceDocumentReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for the RFP Outline Generator aggregate.
 * Implements the generated {@link RfpOutlinesApi} contract — no raw mapping annotations.
 */
@RestController
@RequiredArgsConstructor
public class RfpOutlinesController implements RfpOutlinesApi {

	private final RfpOutlineService rfpOutlineService;
	private final RfpOutlineDraftService rfpOutlineDraftService;
	private final MultipartSourceDocumentReader multipartSourceDocumentReader;
	private final RfpOutlineProperties rfpOutlineProperties;
	private final RfpOutlineApiMapper rfpOutlineApiMapper;
	private final AppUserFactory appUserFactory;

	@Override
	public ResponseEntity<List<RfpOutlineSummaryV1>> listRfpOutlines() {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(rfpOutlineApiMapper.toSummaryV1List(rfpOutlineService.listByUser(user.userId())));
	}

	@Override
	public ResponseEntity<RfpOutlineV1> createRfpOutline(CreateRfpOutlineRequestV1 request) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		var command = rfpOutlineApiMapper.toCommand(request, user.userId());
		var record = rfpOutlineService.create(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(rfpOutlineApiMapper.toV1(record));
	}

	@Override
	public ResponseEntity<RfpOutlineDraftV1> draftRfpOutline(List<MultipartFile> files, String notes) {
		appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		List<SourceDocument> documents =
				multipartSourceDocumentReader.read(files, rfpOutlineProperties.getMaxSourceFiles());
		return ResponseEntity.ok(rfpOutlineApiMapper.toDraftV1(rfpOutlineDraftService.draft(documents, notes)));
	}

	@Override
	public ResponseEntity<RfpOutlineV1> getRfpOutline(Long id) {
		var user = appUserFactory.from(SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.ok(rfpOutlineApiMapper.toV1(rfpOutlineService.getById(id, user.userId())));
	}
}
