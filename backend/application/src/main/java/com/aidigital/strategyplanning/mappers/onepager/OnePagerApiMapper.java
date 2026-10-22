package com.aidigital.strategyplanning.mappers.onepager;

import com.aidigital.strategyplanning.api.v1.model.CreateOnePagerRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDeckV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps between One Page Generator API DTOs and service-layer models.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface OnePagerApiMapper {

	/**
	 * Converts a service draft to the API DTO.
	 *
	 * @param draft service-layer drafted page content
	 * @return API draft DTO
	 */
	OnePagerDraftV1 toV1(OnePagerDraft draft);

	/**
	 * Converts an API draft DTO back to the service-layer model, so a reviewed draft can be
	 * submitted for page creation.
	 *
	 * @param dto API draft DTO
	 * @return service-layer page content
	 */
	OnePagerDraft toModel(OnePagerDraftV1 dto);

	/**
	 * Converts an API drafting request to the service brief.
	 *
	 * @param request API draft request DTO
	 * @return service brief
	 */
	OnePagerBrief toBrief(OnePagerDraftRequestV1 request);

	/**
	 * Converts an API page-creation request to a service command.
	 *
	 * @param request   API create request DTO
	 * @param createdBy Clerk user ID of the creator
	 * @return service command
	 */
	@Mapping(target = "createdBy", source = "createdBy")
	CreateOnePagerCommand toCommand(CreateOnePagerRequestV1 request, String createdBy);

	/**
	 * Converts the created page to its API DTO.
	 *
	 * @param deck created page
	 * @return API deck DTO
	 */
	OnePagerDeckV1 toDeckV1(OnePagerDeck deck);
}
