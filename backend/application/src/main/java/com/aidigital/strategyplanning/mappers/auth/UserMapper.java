package com.aidigital.strategyplanning.mappers.auth;

import com.aidigital.strategyplanning.api.v1.model.UserV1;
import com.aidigital.strategyplanning.config.ApplicationMapperConfig;
import com.aidigital.strategyplanning.service.common.security.AppUser;
import org.mapstruct.Mapper;

/**
 * Maps the service-layer {@link AppUser} to the API {@link UserV1} payload so
 * the controller stays thin and free of manual DTO assembly.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface UserMapper {

	/**
	 * Converts the caller context into the API user payload.
	 *
	 * @param appUser authenticated caller context
	 * @return API user payload (identity only; no baseline roles)
	 */
	UserV1 toUserV1(AppUser appUser);
}
