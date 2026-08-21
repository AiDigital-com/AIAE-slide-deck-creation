package com.aidigital.strategyplanning.mappers.rndrequest;

import com.aidigital.strategyplanning.api.v1.model.CreateRndRequestRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.RndDecisionV1;
import com.aidigital.strategyplanning.api.v1.model.RndRequestSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.RndRequestV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Maps between RnD Request API DTOs and service-layer models.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface RndRequestApiMapper {

	/**
	 * Converts a service record to a full API response DTO.
	 *
	 * @param record service-layer RnD request record
	 * @return API response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	@Mapping(target = "decision", expression = "java(mapDecision(record.decision()))")
	RndRequestV1 toV1(RndRequestRecord record);

	/**
	 * Converts a service record to a summary API response DTO.
	 *
	 * @param record service-layer RnD request record
	 * @return API summary response DTO
	 */
	@Mapping(target = "status", expression = "java(mapStatus(record.status()))")
	@Mapping(target = "decision", expression = "java(mapDecision(record.decision()))")
	RndRequestSummaryV1 toSummaryV1(RndRequestRecord record);

	/**
	 * Converts a list of service records to summary API response DTOs.
	 *
	 * @param records service-layer RnD request records
	 * @return list of API summary response DTOs
	 */
	List<RndRequestSummaryV1> toSummaryV1List(List<RndRequestRecord> records);

	/**
	 * Converts an API create request DTO to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateRndRequestCommand toCommand(CreateRndRequestRequestV1 request, String createdBy);

	/**
	 * Maps a string status code to the API enum.
	 *
	 * @param status status code string
	 * @return API generation status enum value
	 */
	default GenerationStatusV1 mapStatus(String status) {
		return GenerationStatusV1.fromValue(status);
	}

	/**
	 * Maps a string decision code to the API enum.
	 *
	 * @param decision decision code string
	 * @return API triage decision enum value
	 */
	default RndDecisionV1 mapDecision(String decision) {
		return RndDecisionV1.fromValue(decision);
	}
}
