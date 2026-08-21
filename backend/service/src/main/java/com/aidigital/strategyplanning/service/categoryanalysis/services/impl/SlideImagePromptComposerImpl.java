package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImagePromptComposer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link SlideImagePromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class SlideImagePromptComposerImpl implements SlideImagePromptComposer {

	private static final int CIRCULAR_CROP_SLIDE = 3;
	private static final int ROUNDED_SQUARE_SLIDE = 1;
	private static final String OPAQUE_BACKGROUND = "opaque";

	private final CategoryAnalysisProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String buildPrompt(String category, int slideNumber) {
		if (slideNumber == CIRCULAR_CROP_SLIDE) {
			return "A photorealistic professional photograph for a marketing strategy deck about the "
					+ category.trim() + " industry. A warm, candid real-world scene with one clear, "
					+ "friendly focal subject of the industry placed exactly in the center of the frame, "
					+ "in its natural environment with a softly blurred background. The photograph "
					+ "completely fills the frame edge-to-edge with no borders or empty margins, and is "
					+ "composed so a circular center crop keeps the subject fully visible. Shot on a "
					+ "DSLR camera with a 50mm lens, soft natural lighting, crisp true-to-life detail "
					+ "and color — a real photograph, not an illustration or 3D render. Bright, warm, "
					+ "friendly and energetic mood; when people are shown they look happy, positive and "
					+ "genuinely engaged. No text, no words, no letters, no logos, no watermarks, "
					+ "no charts or graphs.";
		}
		if (slideNumber == ROUNDED_SQUARE_SLIDE) {
			return "A photorealistic professional photograph for a marketing strategy deck about the "
					+ category.trim() + " industry. A warm, candid real-world scene showing the single "
					+ "most iconic subject of the industry in its natural environment with a softly "
					+ "blurred background. The photograph completely fills the frame edge-to-edge with "
					+ "no borders, no empty margins and no transparent areas — a full-bleed picture "
					+ "suitable for a square frame with rounded corners, with the subject centered and "
					+ "fully visible. Shot on a DSLR camera with a 50mm lens, soft natural lighting, "
					+ "crisp true-to-life detail and color — a real photograph, not an illustration or "
					+ "3D render. Bright, warm, friendly and energetic mood; when people are shown they "
					+ "look happy, positive and genuinely engaged. No text, no words, no letters, "
					+ "no logos, no watermarks, no charts or graphs.";
		}
		String framing = switch (slideNumber) {
			case 2 -> "A warm cutout of one or two smiling professionals of the industry at work, "
					+ "shown full-body or three-quarter, genuinely engaged with their tools or product.";
			default -> "An upbeat cutout moment showing service, care, or hands-on engagement "
					+ "in the industry.";
		};
		return "A photorealistic professional photograph for a marketing strategy deck about the "
				+ category.trim() + " industry. " + framing
				+ " Shot on a DSLR camera with a 50mm lens, soft natural studio lighting, crisp "
				+ "true-to-life detail and color — a real photograph, not an illustration or 3D render. "
				+ "Bright, warm, friendly and energetic mood. When people are shown they look happy, "
				+ "positive and genuinely engaged. The subject is fully visible and isolated on a "
				+ "transparent background, product-photography cutout style, with a subtle natural "
				+ "contact shadow. The whole subject sits well inside the picture with generous empty "
				+ "padding on every side — especially clear empty space above the head — so no part of "
				+ "the subject ever touches or crosses the edge of the image. No text, no words, "
				+ "no letters, no logos, no watermarks, no charts or graphs.";
	}

	/**
	 * Picks the background style for a slide's generated image. The circular-crop slide and the
	 * rounded-square title slide always get an opaque full-bleed photograph so the picture fills
	 * its frame edge-to-edge with no background removal; all other slides use the configured
	 * background (transparent cutouts by default).
	 *
	 * @param slideNumber slide the image is for
	 * @return background value to send to the Images API, blank to omit the parameter
	 */
	@Override
	public String backgroundForSlide(int slideNumber) {
		if (slideNumber == CIRCULAR_CROP_SLIDE || slideNumber == ROUNDED_SQUARE_SLIDE) {
			return OPAQUE_BACKGROUND;
		}
		return properties.getOpenaiImageBackground();
	}

	/**
	 * Builds the OpenAI Images API request body for a single prompt, requesting one image of the
	 * configured model, size, and quality with the given background style. No
	 * {@code response_format} is sent because {@code gpt-image-1} always returns base64 image bytes.
	 *
	 * @param prompt     image generation prompt
	 * @param background background style for the image, blank to omit the parameter
	 * @return serialized JSON request body
	 */
	@Override
	public String buildImageRequestBody(String prompt, String background) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiImageModel());
		payload.put("prompt", prompt);
		payload.put("n", 1);
		payload.put("size", properties.getOpenaiImageSize());
		if (StringUtils.hasText(properties.getOpenaiImageQuality())) {
			payload.put("quality", properties.getOpenaiImageQuality());
		}
		if (StringUtils.hasText(background)) {
			payload.put("background", background);
		}
		return payload.toString();
	}
}
