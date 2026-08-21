package com.aidigital.strategyplanning.mappers.categoryanalysis;

import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCategoryAnalysisRequestV1;
import com.aidigital.strategyplanning.api.v1.model.CreateStandardDeckRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.StandardConnectionsV1;
import com.aidigital.strategyplanning.api.v1.model.StandardDraftV1;
import com.aidigital.strategyplanning.api.v1.model.StandardFieldValueV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps between Category Analysis API DTOs and service-layer models.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface CategoryAnalysisApiMapper {

	/**
	 * Converts a service record to a full API response DTO.
	 *
	 * @param record service-layer category analysis record
	 * @return API response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	CategoryAnalysisV1 toV1(CategoryAnalysisRecord record);

	/**
	 * Converts a service record to a summary API response DTO.
	 *
	 * @param record service-layer category analysis record
	 * @return API summary response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	CategoryAnalysisSummaryV1 toSummaryV1(CategoryAnalysisRecord record);

	/**
	 * Converts a list of service records to summary API response DTOs.
	 *
	 * @param records service-layer category analysis records
	 * @return list of API summary response DTOs
	 */
	List<CategoryAnalysisSummaryV1> toSummaryV1List(List<CategoryAnalysisRecord> records);

	/**
	 * Converts an API create request DTO to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateCategoryAnalysisCommand toCommand(CreateCategoryAnalysisRequestV1 request, String createdBy);

	/**
	 * Converts service connection status to the API DTO.
	 *
	 * @param connections service connection status
	 * @return API connection status DTO
	 */
	StandardConnectionsV1 toV1(StandardConnections connections);

	/**
	 * Converts a service draft to the API DTO.
	 *
	 * @param draft service-layer drafted field set
	 * @return API draft DTO
	 */
	StandardDraftV1 toV1(StandardDraft draft);

	/**
	 * Converts an API standard deck request DTO to a service command.
	 *
	 * @param request   API create standard deck request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateStandardDeckCommand toCommand(CreateStandardDeckRequestV1 request, String createdBy);

	/**
	 * Converts a list of API field value DTOs to service-layer field values.
	 *
	 * @param fields API field value DTOs, may be null
	 * @return service-layer field values
	 */
	List<StandardFieldValue> toFieldValues(List<StandardFieldValueV1> fields);

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
