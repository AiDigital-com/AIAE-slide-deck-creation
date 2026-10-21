package com.aidigital.strategyplanning.mappers.casestudy;

import com.aidigital.strategyplanning.api.v1.model.CaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudySummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudyV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCaseStudyRequestV1;
import com.aidigital.strategyplanning.api.v1.model.CreateOnePageCaseStudyRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyFieldV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyFieldValueV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.CreateOnePageCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps between Case Study API DTOs and service-layer models.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface CaseStudyApiMapper {

	/**
	 * Converts a service record to a full API response DTO.
	 *
	 * @param record service-layer case study record
	 * @return API response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	CaseStudyV1 toV1(CaseStudyRecord record);

	/**
	 * Converts a service record to a summary API response DTO.
	 *
	 * @param record service-layer case study record
	 * @return API summary response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	CaseStudySummaryV1 toSummaryV1(CaseStudyRecord record);

	/**
	 * Converts a list of service records to summary API response DTOs.
	 *
	 * @param records service-layer case study records
	 * @return list of API summary response DTOs
	 */
	List<CaseStudySummaryV1> toSummaryV1List(List<CaseStudyRecord> records);

	/**
	 * Converts a service-layer AI draft to its API response DTO.
	 *
	 * @param draft service-layer case study draft
	 * @return API draft response DTO
	 */
	CaseStudyDraftV1 toDraftV1(CaseStudyDraft draft);

	/**
	 * Converts an API create request DTO to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateCaseStudyCommand toCommand(CreateCaseStudyRequestV1 request, String createdBy);

	/**
	 * Converts a One-Page Case Study draft to its API response DTO, including the icon library
	 * the user can pick from.
	 *
	 * @param draft service-layer One-Page draft
	 * @return API draft response DTO
	 */
	OnePageCaseStudyDraftV1 toOnePageDraftV1(OnePageCaseStudyDraft draft);

	/**
	 * Converts measured One-Page fields to their API DTOs.
	 *
	 * @param fields measured service-layer fields
	 * @return API field DTOs
	 */
	List<OnePageCaseStudyFieldV1> toOnePageFieldV1List(List<OnePageCaseStudyField> fields);

	/**
	 * Converts reviewed One-Page field values to the service-layer values.
	 *
	 * @param fields API field value DTOs, may be null
	 * @return service-layer field values
	 */
	List<OnePageFieldValue> toOnePageFieldValues(List<OnePageCaseStudyFieldValueV1> fields);

	/**
	 * Converts a One-Page create request to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateOnePageCaseStudyCommand toOnePageCommand(CreateOnePageCaseStudyRequestV1 request, String createdBy);

	/**
	 * Maps a string status code to the API enum.
	 *
	 * @param status status code string
	 * @return API generation status enum value
	 */
	default GenerationStatusV1 mapStatus(String status) {
		return GenerationStatusV1.fromValue(status);
	}
}
