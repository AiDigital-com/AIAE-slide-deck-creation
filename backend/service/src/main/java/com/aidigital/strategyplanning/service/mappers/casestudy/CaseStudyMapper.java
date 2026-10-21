package com.aidigital.strategyplanning.service.mappers.casestudy;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudySummary;
import com.aidigital.strategyplanning.service.common.mapping.ServiceMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Converts between {@link CaseStudyEntity} and {@link CaseStudyRecord}.
 */
@Mapper(config = ServiceMapperConfig.class)
public interface CaseStudyMapper {

	/**
	 * Converts a case study entity to an immutable service record.
	 *
	 * @param entity persisted case study entity
	 * @return service-layer case study record
	 */
	CaseStudyRecord toRecord(CaseStudyEntity entity);

	/**
	 * Converts a list of case study entities to service records.
	 *
	 * @param entities persisted case study entities
	 * @return service-layer case study records
	 */
	List<CaseStudyRecord> toRecords(List<CaseStudyEntity> entities);

	/**
	 * Builds a new case study entity from the create command and the values derived while
	 * generating the deck.
	 *
	 * @param command     validated create command
	 * @param templateUrl tokenized Google Slides template the deck was copied from
	 * @param slidesUrl   URL of the generated deck in the caller's Drive
	 * @param status      lifecycle status to persist
	 * @param createdAt   creation timestamp from the application clock
	 * @return unsaved case study entity
	 */
	@Mapping(target = "id", ignore = true)
	CaseStudyEntity toEntity(CreateCaseStudyCommand command, String templateUrl, String slidesUrl,
			String status, LocalDateTime createdAt);

	/**
	 * Builds a new case study entity for a One-Page deck from the summary derived from its
	 * template tokens and the values produced while generating the deck.
	 *
	 * @param summary         record-level summary derived from the filled tokens
	 * @param sourceFileName  comma-separated names of the source files, may be null
	 * @param templateUrl     One-Page Google Slides template the deck was copied from
	 * @param slidesUrl       URL of the generated deck in the caller's Drive
	 * @param status          lifecycle status to persist
	 * @param createdBy       Clerk user ID of the creator
	 * @param createdAt       creation timestamp from the application clock
	 * @return unsaved case study entity
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "testimonial", ignore = true)
	CaseStudyEntity toOnePageEntity(OnePageCaseStudySummary summary, String sourceFileName, String templateUrl,
			String slidesUrl, String status, String createdBy, LocalDateTime createdAt);
}
