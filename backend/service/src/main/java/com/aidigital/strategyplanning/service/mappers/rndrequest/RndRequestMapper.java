package com.aidigital.strategyplanning.service.mappers.rndrequest;

import com.aidigital.strategyplanning.domain.rndrequest.entities.RndRequestEntity;
import com.aidigital.strategyplanning.service.common.mapping.ServiceMapperConfig;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converts between {@link RndRequestEntity} and {@link RndRequestRecord}.
 */
@Mapper(config = ServiceMapperConfig.class)
public interface RndRequestMapper {

	/**
	 * Converts an RnD request entity to an immutable service record.
	 *
	 * @param entity persisted RnD request entity
	 * @return service-layer RnD request record
	 */
	RndRequestRecord toRecord(RndRequestEntity entity);

	/**
	 * Converts a list of RnD request entities to service records.
	 *
	 * @param entities persisted RnD request entities
	 * @return service-layer RnD request records
	 */
	List<RndRequestRecord> toRecords(List<RndRequestEntity> entities);

	/**
	 * Builds a new RnD request entity from the create command and the triage outcome.
	 *
	 * @param command           validated create command
	 * @param decision          triage decision derived from the deal size
	 * @param capabilitySummary result of the capability scan across connected knowledge sources
	 * @param responseDraft     drafted escalation summary or workaround response
	 * @param status            lifecycle status to persist
	 * @param createdAt         creation timestamp from the application clock
	 * @return unsaved RnD request entity
	 */
	@Mapping(target = "id", ignore = true)
	RndRequestEntity toEntity(CreateRndRequestCommand command, String decision,
			String capabilitySummary, String responseDraft, String status, LocalDateTime createdAt);
}
