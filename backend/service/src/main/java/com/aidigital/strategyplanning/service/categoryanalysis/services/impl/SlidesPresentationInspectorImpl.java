package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link SlidesPresentationInspector}.
 */
@Service
public class SlidesPresentationInspectorImpl implements SlidesPresentationInspector {

	/**
	 * How far an image's aspect ratio may stray from 1:1 and still count as square.
	 */
	public static final double SQUARE_ASPECT_TOLERANCE = 0.05;

	@Override
	public JsonNode findShapeContainingText(JsonNode presentation, int slideIndex, String value) {
		JsonNode slides = presentation.path("slides");
		if (slideIndex < 0 || slideIndex >= slides.size()) {
			return null;
		}
		for (JsonNode element : slides.path(slideIndex).path("pageElements")) {
			if (shapeText(element).contains(value)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Concatenates the text runs of a shape into its full text. Indices into the returned
	 * string match the Slides API's UTF-16 text indices because runs are contiguous from 0.
	 *
	 * @param element page element JSON
	 * @return the shape's full text, empty when the element has no text
	 */
	@Override
	public String shapeText(JsonNode element) {
		StringBuilder text = new StringBuilder();
		for (JsonNode textElement : element.path("shape").path("text").path("textElements")) {
			if (textElement.has("textRun")) {
				text.append(textElement.path("textRun").path("content").asText(""));
			}
		}
		return text.toString();
	}

	/**
	 * Returns the explicit foreground color of the text run covering the given index.
	 *
	 * @param element page element JSON
	 * @param index   UTF-16 text index inside the shape's text
	 * @return the run's foregroundColor JSON, or null when the run inherits its color
	 */
	@Override
	public JsonNode foregroundColorAt(JsonNode element, int index) {
		for (JsonNode textElement : element.path("shape").path("text").path("textElements")) {
			if (!textElement.has("textRun")) {
				continue;
			}
			int start = textElement.path("startIndex").asInt(0);
			int end = textElement.path("endIndex").asInt(0);
			if (index >= start && index < end) {
				JsonNode color = textElement.path("textRun").path("style").path("foregroundColor");
				return color.isMissingNode() ? null : color;
			}
		}
		return null;
	}

	/**
	 * Extracts slide object IDs in deck order.
	 *
	 * @param presentation parsed presentation JSON
	 * @return slide object IDs, index 0 = slide 1
	 */
	@Override
	public List<String> extractSlideIds(JsonNode presentation) {
		List<String> ids = new ArrayList<>();
		presentation.path("slides").forEach(slide -> ids.add(slide.path("objectId").asText()));
		return ids;
	}

	/**
	 * Finds the image element to replace on a slide. By default this is the largest image
	 * (the main image placeholder). When {@code preferSquare} is set — used for the slide whose
	 * placeholder is masked into a circle — the largest image with a square displayed frame is
	 * preferred, falling back to the overall largest image when no square image exists. Replacing
	 * the masked image itself preserves its circular crop and avoids leaving the template's
	 * original picture layered on top of the new one.
	 *
	 * @param presentation parsed presentation JSON
	 * @param slideIndex   zero-based slide index (0 = slide 1)
	 * @param preferSquare when true, prefer the largest square-framed image on the slide
	 * @return the image page element, or null when the slide has no images or does not exist
	 */
	@Override
	public JsonNode findTargetImageElement(JsonNode presentation, int slideIndex, boolean preferSquare) {
		JsonNode slides = presentation.path("slides");
		if (slideIndex < 0 || slideIndex >= slides.size()) {
			return null;
		}
		JsonNode best = null;
		double bestArea = -1;
		JsonNode bestSquare = null;
		double bestSquareArea = -1;
		for (JsonNode element : slides.path(slideIndex).path("pageElements")) {
			if (!element.has("image")) {
				continue;
			}
			double width = displayedWidth(element);
			double height = displayedHeight(element);
			double area = width * height;
			if (area > bestArea) {
				bestArea = area;
				best = element;
			}
			boolean square = height > 0 && Math.abs(width / height - 1.0) <= SQUARE_ASPECT_TOLERANCE;
			if (square && area > bestSquareArea) {
				bestSquareArea = area;
				bestSquare = element;
			}
		}
		if (preferSquare && bestSquare != null) {
			return bestSquare;
		}
		return best;
	}

	/**
	 * Computes an image element's displayed width (its intrinsic width times its transform scale).
	 *
	 * @param element image page element
	 * @return displayed width in EMU
	 */
	@Override
	public double displayedWidth(JsonNode element) {
		return element.path("size").path("width").path("magnitude").asDouble(0)
				* element.path("transform").path("scaleX").asDouble(1);
	}

	/**
	 * Computes an image element's displayed height (its intrinsic height times its transform scale).
	 *
	 * @param element image page element
	 * @return displayed height in EMU
	 */
	@Override
	public double displayedHeight(JsonNode element) {
		return element.path("size").path("height").path("magnitude").asDouble(0)
				* element.path("transform").path("scaleY").asDouble(1);
	}

	/**
	 * Finds the object id of the first shape on a slide whose text contains the given literal.
	 *
	 * @param presentation parsed presentation JSON
	 * @param slideIndex   zero-based slide index
	 * @param text         literal to look for in the shape's text runs
	 * @return the shape's object id, or null when no shape on the slide contains the text
	 */
	@Override
	public String findShapeIdByText(JsonNode presentation, int slideIndex, String text) {
		JsonNode slides = presentation.path("slides");
		if (slideIndex < 0 || slideIndex >= slides.size()) {
			return null;
		}
		for (JsonNode element : slides.path(slideIndex).path("pageElements")) {
			JsonNode textElements = element.path("shape").path("text").path("textElements");
			StringBuilder content = new StringBuilder();
			for (JsonNode textElement : textElements) {
				content.append(textElement.path("textRun").path("content").asText(""));
			}
			if (content.toString().contains(text)) {
				return element.path("objectId").asText();
			}
		}
		return null;
	}
}
