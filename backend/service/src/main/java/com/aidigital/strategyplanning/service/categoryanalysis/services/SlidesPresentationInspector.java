package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Read-only queries over a Google Slides presentation as the API returns it.
 *
 * <p>The template is edited by hand from time to time, so nothing here assumes a fixed object id:
 * shapes and images are located by their text or their geometry instead.
 */
public interface SlidesPresentationInspector {

	/**
	 * Finds the first shape on a slide whose text contains a value.
	 *
	 * @param presentation presentation as returned by the Slides API
	 * @param slideIndex   zero-based slide index
	 * @param value        text to look for
	 * @return the shape element, or null when no shape contains the value
	 */
	JsonNode findShapeContainingText(JsonNode presentation, int slideIndex, String value);

	/**
	 * Concatenates the text runs of a shape.
	 *
	 * @param element shape element
	 * @return the shape's text, empty when it has none
	 */
	String shapeText(JsonNode element);

	/**
	 * Reads the foreground colour in effect at a character position.
	 *
	 * @param element shape element
	 * @param index   character index within the shape's text
	 * @return the colour node, or null when the shape sets none
	 */
	JsonNode foregroundColorAt(JsonNode element, int index);

	/**
	 * Lists the slide object ids in presentation order.
	 *
	 * @param presentation presentation as returned by the Slides API
	 * @return slide object ids
	 */
	List<String> extractSlideIds(JsonNode presentation);

	/**
	 * Finds the image element on a slide that the generated artwork should replace.
	 *
	 * @param presentation presentation as returned by the Slides API
	 * @param slideIndex   zero-based slide index
	 * @param preferSquare prefer a near-square image when several are present
	 * @return the image element, or null when the slide has none
	 */
	JsonNode findTargetImageElement(JsonNode presentation, int slideIndex, boolean preferSquare);

	/**
	 * Returns the on-slide width of an element in EMU, scale applied.
	 *
	 * @param element page element
	 * @return displayed width in EMU
	 */
	double displayedWidth(JsonNode element);

	/**
	 * Returns the on-slide height of an element in EMU, scale applied.
	 *
	 * @param element page element
	 * @return displayed height in EMU
	 */
	double displayedHeight(JsonNode element);

	/**
	 * Finds the object id of the first shape on a slide whose text contains a value.
	 *
	 * @param presentation presentation as returned by the Slides API
	 * @param slideIndex   zero-based slide index
	 * @param text         text to look for
	 * @return the shape's object id, or null when no shape contains the text
	 */
	String findShapeIdByText(JsonNode presentation, int slideIndex, String text);
}
