package com.aidigital.strategyplanning.service.onepager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the One Page Generator tool.
 * Binds from {@code app.one-pager.*} in application.yml.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.one-pager")
public class OnePagerProperties {

	/**
	 * Presentation ID of the One Page Generator master template ("Extended - One Pager
	 * Templates"): slides 1-3 are header/footer shells, slides 4-6 the body-module library. The
	 * master is only ever exported, never copied or edited.
	 */
	private String templatePresentationId;

	/**
	 * Clerk secret key used to fetch the user's Google OAuth access token for Drive/Slides calls.
	 */
	private String clerkSecretKey;

	/**
	 * OpenAI API key for the one-pager drafting engine. Blank means "AI not connected".
	 */
	private String openaiApiKey;

	/**
	 * Base URL of the OpenAI-compatible API used for drafting.
	 */
	private String openaiBaseUrl;

	/**
	 * Chat completions model used for drafting one-pager copy.
	 */
	private String openaiModel;
}
