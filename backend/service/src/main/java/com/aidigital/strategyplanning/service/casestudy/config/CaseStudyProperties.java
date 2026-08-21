package com.aidigital.strategyplanning.service.casestudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Case Study Builder tool.
 * Binds from {@code app.case-study.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.case-study")
public class CaseStudyProperties {

	/**
	 * URL of the tokenized Google Slides template used for case study deck generation.
	 * Engineering must wire Google OAuth to populate slide tokens from this template.
	 */
	private String templateUrl;

	/**
	 * Presentation ID of the tokenized Google Slides master template copied for each deck.
	 */
	private String templatePresentationId;

	/**
	 * Clerk secret key used to fetch the user's Google OAuth access token for Drive/Slides calls.
	 */
	private String clerkSecretKey;

	/**
	 * OpenAI API key for the case study auto-draft engine. Blank means "AI not connected".
	 */
	private String openaiApiKey;

	/**
	 * Base URL of the OpenAI-compatible API used for auto-drafting.
	 */
	private String openaiBaseUrl;

	/**
	 * Chat completions model used for auto-drafting case study fields.
	 */
	private String openaiModel;

	/**
	 * Maximum number of source documents accepted per auto-draft request.
	 */
	private int maxSourceFiles = 10;

	/**
	 * Returns the tokenized Google Slides template URL.
	 *
	 * @return template URL string, or null if not configured
	 */
	public String getTemplateUrl() {
		return templateUrl;
	}

	/**
	 * Sets the tokenized Google Slides template URL.
	 *
	 * @param templateUrl the template URL to set
	 */
	public void setTemplateUrl(String templateUrl) {
		this.templateUrl = templateUrl;
	}

	/**
	 * Returns the presentation ID of the tokenized Google Slides master template.
	 *
	 * @return presentation ID string
	 */
	public String getTemplatePresentationId() {
		return templatePresentationId;
	}

	/**
	 * Sets the presentation ID of the tokenized Google Slides master template.
	 *
	 * @param templatePresentationId the presentation ID to set
	 */
	public void setTemplatePresentationId(String templatePresentationId) {
		this.templatePresentationId = templatePresentationId;
	}

	/**
	 * Returns the Clerk secret key used to fetch Google OAuth tokens.
	 *
	 * @return Clerk secret key, blank when not configured
	 */
	public String getClerkSecretKey() {
		return clerkSecretKey;
	}

	/**
	 * Sets the Clerk secret key used to fetch Google OAuth tokens.
	 *
	 * @param clerkSecretKey the Clerk secret key to set
	 */
	public void setClerkSecretKey(String clerkSecretKey) {
		this.clerkSecretKey = clerkSecretKey;
	}

	/**
	 * Returns the OpenAI API key for auto-drafting.
	 *
	 * @return API key, blank when the drafting engine is not connected
	 */
	public String getOpenaiApiKey() {
		return openaiApiKey;
	}

	/**
	 * Sets the OpenAI API key for auto-drafting.
	 *
	 * @param openaiApiKey the API key to set
	 */
	public void setOpenaiApiKey(String openaiApiKey) {
		this.openaiApiKey = openaiApiKey;
	}

	/**
	 * Returns the OpenAI-compatible API base URL.
	 *
	 * @return base URL string
	 */
	public String getOpenaiBaseUrl() {
		return openaiBaseUrl;
	}

	/**
	 * Sets the OpenAI-compatible API base URL.
	 *
	 * @param openaiBaseUrl the base URL to set
	 */
	public void setOpenaiBaseUrl(String openaiBaseUrl) {
		this.openaiBaseUrl = openaiBaseUrl;
	}

	/**
	 * Returns the chat completions model used for auto-drafting.
	 *
	 * @return model identifier
	 */
	public String getOpenaiModel() {
		return openaiModel;
	}

	/**
	 * Sets the chat completions model used for auto-drafting.
	 *
	 * @param openaiModel the model identifier to set
	 */
	public void setOpenaiModel(String openaiModel) {
		this.openaiModel = openaiModel;
	}

	/**
	 * Returns the maximum number of source documents accepted per auto-draft request.
	 *
	 * @return maximum file count
	 */
	public int getMaxSourceFiles() {
		return maxSourceFiles;
	}

	/**
	 * Sets the maximum number of source documents accepted per auto-draft request.
	 *
	 * @param maxSourceFiles the maximum file count to set
	 */
	public void setMaxSourceFiles(int maxSourceFiles) {
		this.maxSourceFiles = maxSourceFiles;
	}
}
