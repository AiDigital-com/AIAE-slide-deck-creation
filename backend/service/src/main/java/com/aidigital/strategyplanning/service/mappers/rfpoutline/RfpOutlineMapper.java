package com.aidigital.strategyplanning.service.mappers.rfpoutline;

import com.aidigital.strategyplanning.domain.rfpoutline.entities.RfpOutlineEntity;
import com.aidigital.strategyplanning.service.common.mapping.ServiceMapperConfig;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converts between {@link RfpOutlineEntity} and {@link RfpOutlineRecord}.
 */
@Mapper(config = ServiceMapperConfig.class)
public interface RfpOutlineMapper {

	/**
	 * Converts an RFP outline entity to an immutable service record.
	 *
	 * @param entity persisted RFP outline entity
	 * @return service-layer RFP outline record
	 */
	RfpOutlineRecord toRecord(RfpOutlineEntity entity);

	/**
	 * Converts a list of RFP outline entities to service records.
	 *
	 * @param entities persisted RFP outline entities
	 * @return service-layer RFP outline records
	 */
	List<RfpOutlineRecord> toRecords(List<RfpOutlineEntity> entities);

	/**
	 * Builds a new RFP outline entity from the create command and the generated document.
	 *
	 * @param command   validated create command
	 * @param docUrl    URL of the generated Google Doc in the caller's Drive
	 * @param status    lifecycle status to persist
	 * @param createdAt creation timestamp from the application clock
	 * @return unsaved RFP outline entity
	 */
	@Mapping(target = "id", ignore = true)
	RfpOutlineEntity toEntity(CreateRfpOutlineCommand command, String docUrl, String status,
			LocalDateTime createdAt);
}
