package com.aidigital.strategyplanning.service.rfpoutline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the RFP Outline Generator tool.
 * Binds from {@code app.rfp-outline.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.rfp-outline")
public class RfpOutlineProperties {

	/**
	 * Clerk secret key used to fetch the user's Google OAuth access token for Drive/Docs calls.
	 */
	private String clerkSecretKey;

	/**
	 * OpenAI API key for the RFP outline auto-draft engine. Blank means "AI not connected".
	 */
	private String openaiApiKey;

	/**
	 * Base URL of the OpenAI-compatible API used for auto-drafting.
	 */
	private String openaiBaseUrl;

	/**
	 * Chat completions model used for auto-drafting RFP outline fields.
	 */
	private String openaiModel;

	/**
	 * Maximum number of source documents accepted per auto-draft request.
	 */
	private int maxSourceFiles = 10;

	public String getClerkSecretKey() {
		return clerkSecretKey;
	}

	public void setClerkSecretKey(String clerkSecretKey) {
		this.clerkSecretKey = clerkSecretKey;
	}

	public String getOpenaiApiKey() {
		return openaiApiKey;
	}

	public void setOpenaiApiKey(String openaiApiKey) {
		this.openaiApiKey = openaiApiKey;
	}

	public String getOpenaiBaseUrl() {
		return openaiBaseUrl;
	}

	public void setOpenaiBaseUrl(String openaiBaseUrl) {
		this.openaiBaseUrl = openaiBaseUrl;
	}

	public String getOpenaiModel() {
		return openaiModel;
	}

	public void setOpenaiModel(String openaiModel) {
		this.openaiModel = openaiModel;
	}

	public int getMaxSourceFiles() {
		return maxSourceFiles;
	}

	public void setMaxSourceFiles(int maxSourceFiles) {
		this.maxSourceFiles = maxSourceFiles;
	}
}
