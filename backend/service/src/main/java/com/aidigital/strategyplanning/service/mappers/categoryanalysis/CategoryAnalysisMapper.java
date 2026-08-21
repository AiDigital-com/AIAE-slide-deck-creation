package com.aidigital.strategyplanning.service.mappers.categoryanalysis;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.common.mapping.ServiceMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converts between {@link CategoryAnalysisEntity} and {@link CategoryAnalysisRecord}.
 */
@Mapper(config = ServiceMapperConfig.class)
public interface CategoryAnalysisMapper {

	/**
	 * Converts a category analysis entity to an immutable service record.
	 *
	 * @param entity persisted category analysis entity
	 * @return service-layer category analysis record
	 */
	CategoryAnalysisRecord toRecord(CategoryAnalysisEntity entity);

	/**
	 * Converts a list of category analysis entities to service records.
	 *
	 * @param entities persisted category analysis entities
	 * @return service-layer category analysis records
	 */
	List<CategoryAnalysisRecord> toRecords(List<CategoryAnalysisEntity> entities);

	/**
	 * Builds a new category analysis entity from a manually submitted create command. No deck is
	 * generated on this path, so the deck-specific columns stay empty.
	 *
	 * @param command   validated create command
	 * @param status    lifecycle status to persist
	 * @param createdAt creation timestamp from the application clock
	 * @return unsaved category analysis entity
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "slidesUrl", ignore = true)
	@Mapping(target = "templateKind", ignore = true)
	CategoryAnalysisEntity toEntity(CreateCategoryAnalysisCommand command, String status,
			LocalDateTime createdAt);

	/**
	 * Builds a new category analysis entity for a Standard deck created in Google Slides. The
	 * narrative columns stay empty because the reviewed values live in the deck itself.
	 *
	 * @param command      validated deck command carrying the reviewed field values
	 * @param title        deck title composed from the category and client
	 * @param slidesUrl    URL of the created deck in the caller's Drive
	 * @param templateKind template the deck was built from
	 * @param status       lifecycle status to persist
	 * @param createdAt    creation timestamp from the application clock
	 * @return unsaved category analysis entity
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "marketOverview", ignore = true)
	@Mapping(target = "keyPlayers", ignore = true)
	@Mapping(target = "trends", ignore = true)
	@Mapping(target = "opportunities", ignore = true)
	@Mapping(target = "recommendations", ignore = true)
	@Mapping(target = "sourceFileNames", ignore = true)
	CategoryAnalysisEntity toStandardDeckEntity(CreateStandardDeckCommand command, String title,
			String slidesUrl, String templateKind, String status, LocalDateTime createdAt);
}
