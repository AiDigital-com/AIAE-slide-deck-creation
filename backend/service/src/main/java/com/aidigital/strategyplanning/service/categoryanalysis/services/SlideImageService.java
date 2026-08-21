package com.aidigital.strategyplanning.service.categoryanalysis.services;

import java.util.Map;

/**
 * Service contract for generating friendly, on-vertical photographs used to fill the image
 * slots of the Standard Category Analysis deck. Images replace the old auto-generated data
 * graph and the other slide image placeholders so every deck opens with warm, energetic,
 * category-relevant visuals.
 */
public interface SlideImageService {

	/**
	 * Reports whether the image generation engine is configured (an OpenAI key is present).
	 *
	 * @return true when slide images can be generated
	 */
	boolean isConnected();

	/**
	 * Generates one themed image per image-bearing slide for the given category. Each image is
	 * stored briefly and returned as a public URL under {@code publicBaseUrl} so Google Slides can
	 * fetch it. Generation is best-effort: slides whose image could not be generated are simply
	 * omitted from the result so deck creation never fails because of an image.
	 *
	 * @param category      market category or industry the deck is about
	 * @param publicBaseUrl externally reachable base URL of this backend (scheme + host), used to
	 *                      build the public image URLs Google fetches
	 * @return map of slide number (1-based) to a publicly fetchable image URL, only for successes
	 */
	Map<Integer, String> generateSlideImageUrls(String category, String publicBaseUrl);
}
