package com.aidigital.strategyplanning.service.categoryanalysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Category Analysis deck generator.
 * Binds from {@code app.category-analysis.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.category-analysis")
public class CategoryAnalysisProperties {

	/**
	 * OpenAI API key used for AI drafting. Blank means the AI engine is not connected.
	 */
	private String openaiApiKey = "";

	/**
	 * Base URL of the OpenAI-compatible chat completions API.
	 */
	private String openaiBaseUrl = "https://api.openai.com/v1";

	/**
	 * Model used for drafting the analysis content.
	 */
	private String openaiModel = "gpt-4o";

	/**
	 * Google Slides presentation ID of the master Standard Category Analysis template.
	 */
	private String templatePresentationId = "164QKu8KTgpxYYGtX9lGXp-M_5IHXMm-oiGromJtfI3k";

	/**
	 * Clerk secret key used to fetch the user's Google OAuth access token.
	 */
	private String clerkSecretKey = "";

	/**
	 * OpenAI image model used to generate the friendly, on-vertical slide images.
	 */
	private String openaiImageModel = "gpt-image-1";

	/**
	 * Pixel size requested for generated slide images (e.g. "1024x1024").
	 */
	private String openaiImageSize = "1024x1024";

	/**
	 * Rendering quality for generated slide images. For {@code gpt-image-1} one of
	 * {@code low}, {@code medium}, {@code high}, or {@code auto}; {@code medium} produces
	 * noticeably more realistic, usable photographs than {@code low} at moderate extra latency.
	 */
	private String openaiImageQuality = "medium";

	/**
	 * Background style for generated slide images. For {@code gpt-image-1} one of
	 * {@code transparent}, {@code opaque}, or {@code auto}. {@code transparent} produces a
	 * cutout-style PNG (subject isolated on a transparent background), matching the
	 * generate-then-remove-background workflow. Blank omits the parameter.
	 */
	private String openaiImageBackground = "transparent";

	/**
	 * Returns the OpenAI API key.
	 *
	 * @return OpenAI API key, blank when not connected
	 */
	public String getOpenaiApiKey() {
		return openaiApiKey;
	}

	/**
	 * Sets the OpenAI API key.
	 *
	 * @param openaiApiKey OpenAI API key to set
	 */
	public void setOpenaiApiKey(String openaiApiKey) {
		this.openaiApiKey = openaiApiKey;
	}

	/**
	 * Returns the OpenAI base URL.
	 *
	 * @return base URL of the chat completions API
	 */
	public String getOpenaiBaseUrl() {
		return openaiBaseUrl;
	}

	/**
	 * Sets the OpenAI base URL.
	 *
	 * @param openaiBaseUrl base URL to set
	 */
	public void setOpenaiBaseUrl(String openaiBaseUrl) {
		this.openaiBaseUrl = openaiBaseUrl;
	}

	/**
	 * Returns the OpenAI model name.
	 *
	 * @return model used for drafting
	 */
	public String getOpenaiModel() {
		return openaiModel;
	}

	/**
	 * Sets the OpenAI model name.
	 *
	 * @param openaiModel model name to set
	 */
	public void setOpenaiModel(String openaiModel) {
		this.openaiModel = openaiModel;
	}

	/**
	 * Returns the master template presentation ID.
	 *
	 * @return Google Slides presentation ID of the master template
	 */
	public String getTemplatePresentationId() {
		return templatePresentationId;
	}

	/**
	 * Sets the master template presentation ID.
	 *
	 * @param templatePresentationId presentation ID to set
	 */
	public void setTemplatePresentationId(String templatePresentationId) {
		this.templatePresentationId = templatePresentationId;
	}

	/**
	 * Returns the Clerk secret key.
	 *
	 * @return Clerk secret key, blank when unset
	 */
	public String getClerkSecretKey() {
		return clerkSecretKey;
	}

	/**
	 * Sets the Clerk secret key.
	 *
	 * @param clerkSecretKey Clerk secret key to set
	 */
	public void setClerkSecretKey(String clerkSecretKey) {
		this.clerkSecretKey = clerkSecretKey;
	}

	/**
	 * Returns the OpenAI image model name.
	 *
	 * @return model used to generate slide images
	 */
	public String getOpenaiImageModel() {
		return openaiImageModel;
	}

	/**
	 * Sets the OpenAI image model name.
	 *
	 * @param openaiImageModel model name to set
	 */
	public void setOpenaiImageModel(String openaiImageModel) {
		this.openaiImageModel = openaiImageModel;
	}

	/**
	 * Returns the requested pixel size for generated slide images.
	 *
	 * @return image size string (e.g. "1024x1024")
	 */
	public String getOpenaiImageSize() {
		return openaiImageSize;
	}

	/**
	 * Sets the requested pixel size for generated slide images.
	 *
	 * @param openaiImageSize image size string to set
	 */
	public void setOpenaiImageSize(String openaiImageSize) {
		this.openaiImageSize = openaiImageSize;
	}

	/**
	 * Returns the requested rendering quality for generated slide images.
	 *
	 * @return image quality string (e.g. "low")
	 */
	public String getOpenaiImageQuality() {
		return openaiImageQuality;
	}

	/**
	 * Sets the requested rendering quality for generated slide images.
	 *
	 * @param openaiImageQuality image quality string to set
	 */
	public void setOpenaiImageQuality(String openaiImageQuality) {
		this.openaiImageQuality = openaiImageQuality;
	}

	/**
	 * Returns the requested background style for generated slide images.
	 *
	 * @return image background string (e.g. "transparent"), blank to omit the parameter
	 */
	public String getOpenaiImageBackground() {
		return openaiImageBackground;
	}

	/**
	 * Sets the requested background style for generated slide images.
	 *
	 * @param openaiImageBackground image background string to set
	 */
	public void setOpenaiImageBackground(String openaiImageBackground) {
		this.openaiImageBackground = openaiImageBackground;
	}
}
