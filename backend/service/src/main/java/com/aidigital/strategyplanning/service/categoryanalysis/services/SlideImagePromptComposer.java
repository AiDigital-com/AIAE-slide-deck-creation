package com.aidigital.strategyplanning.service.categoryanalysis.services;

/**
 * Composes the artwork brief for each illustrated slide of the Standard deck.
 *
 * <p>The prompt and the background mode decide what the artwork looks like — a circular cutout on
 * one slide, a rounded square on another — so they are product behaviour kept in one place.
 */
public interface SlideImagePromptComposer {

	/**
	 * Builds the image generation prompt for one slide.
	 *
	 * @param category    market category being analysed
	 * @param slideNumber slide the artwork belongs to
	 * @return image generation prompt
	 */
	String buildPrompt(String category, int slideNumber);

	/**
	 * Returns the background mode the slide's artwork needs.
	 *
	 * @param slideNumber slide the artwork belongs to
	 * @return background mode, blank when the parameter should be omitted
	 */
	String backgroundForSlide(int slideNumber);

	/**
	 * Builds the image generation request body.
	 *
	 * @param prompt     image generation prompt
	 * @param background background mode, blank to omit the parameter
	 * @return serialized JSON request body
	 */
	String buildImageRequestBody(String prompt, String background);
}
