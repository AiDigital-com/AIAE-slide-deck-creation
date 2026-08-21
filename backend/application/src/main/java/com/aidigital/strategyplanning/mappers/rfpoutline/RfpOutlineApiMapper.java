package com.aidigital.strategyplanning.mappers.rfpoutline;

import com.aidigital.strategyplanning.api.v1.model.CreateRfpOutlineRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineDraftV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.RfpOutlineV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps between RFP outline API DTOs and service-layer models.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface RfpOutlineApiMapper {

	/**
	 * Converts a service record to a full API response DTO.
	 *
	 * @param record service-layer RFP outline record
	 * @return API response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	RfpOutlineV1 toV1(RfpOutlineRecord record);

	/**
	 * Converts a service record to a summary API response DTO.
	 *
	 * @param record service-layer RFP outline record
	 * @return API summary response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	RfpOutlineSummaryV1 toSummaryV1(RfpOutlineRecord record);

	/**
	 * Converts a list of service records to summary API response DTOs.
	 *
	 * @param records service-layer RFP outline records
	 * @return list of API summary response DTOs
	 */
	List<RfpOutlineSummaryV1> toSummaryV1List(List<RfpOutlineRecord> records);

	/**
	 * Converts a service-layer AI draft to its API response DTO.
	 *
	 * @param draft service-layer RFP outline draft
	 * @return API draft response DTO
	 */
	RfpOutlineDraftV1 toDraftV1(RfpOutlineDraft draft);

	/**
	 * Converts an API create request DTO to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateRfpOutlineCommand toCommand(CreateRfpOutlineRequestV1 request, String createdBy);

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
