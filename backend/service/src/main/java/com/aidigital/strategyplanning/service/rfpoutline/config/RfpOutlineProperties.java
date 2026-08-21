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
	 * URL of the tokenized Google Doc template used for RFP outline doc generation.
	 */
	private String templateUrl;

	/**
	 * File ID of the tokenized Google Doc master template copied for each RFP outline.
	 */
	private String templateDocId;

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

	public String getTemplateUrl() {
		return templateUrl;
	}

	public void setTemplateUrl(String templateUrl) {
		this.templateUrl = templateUrl;
	}

	public String getTemplateDocId() {
		return templateDocId;
	}

	public void setTemplateDocId(String templateDocId) {
		this.templateDocId = templateDocId;
	}

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
