package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Map;

/**
 * Builds the Slides API requests that make the In-Depth template's charts match their data.
 *
 * <ul>
 *   <li>Slide 8 donut — the master's donut (alt text {@code {{donut_chart}}}) is replaced in the
 *       new deck by a freshly drawn donut at the same size, position, and layer, under the
 *       legend labels. Slice colours are read from the legend dots when present.</li>
 *   <li>Slide 18 bars — each bar with alt text {@code {{bar_N}}} (1 = bottom) is stretched or
 *       shrunk from its left edge in proportion to its value; the longest bar keeps the master's
 *       longest length. The value label sitting inside the bar's right end moves with it.</li>
 * </ul>
 * Element lookups use the presentation as copied from the master, before any edits — object ids
 * survive the text replacement and slide deletion that run first.
 */
public interface InDepthChartRequestBuilder {

	/**
	 * Builds the chart requests for the checked slides, one batch per chart, so a failure in one
	 * (e.g. Google cannot fetch the donut image) never blocks the other.
	 *
	 * @param presentation   presentation JSON as copied from the master, before any edits
	 * @param selectedSlides checked slide numbers (1-based)
	 * @param values         final field values, chart series already sorted
	 * @param publicBaseUrl  externally reachable base URL Google fetches the donut image from
	 * @return non-empty request batches, one per chart to update
	 */
	List<ArrayNode> buildChartRequests(JsonNode presentation, List<Integer> selectedSlides,
			Map<String, String> values, String publicBaseUrl);

	/**
	 * Builds the requests that swap the drawn donut into slide 8.
	 *
	 * @param slide         slide-8 JSON
	 * @param values        final field values (donut series sorted largest first)
	 * @param publicBaseUrl base URL the stored PNG is served from
	 * @return the requests that swap the donut, or none when the donut cannot be drawn
	 */
	ArrayNode buildDonutRequests(JsonNode slide, Map<String, String> values, String publicBaseUrl);

	/**
	 * Builds the requests that resize the slide-18 bars to their values and move each bar's
	 * value label with its end.
	 *
	 * @param slide  slide-18 JSON
	 * @param values final field values (bar series sorted smallest first)
	 * @return transform requests, or none when any bar is missing or no value is positive
	 */
	ArrayNode buildBarRequests(JsonNode slide, Map<String, String> values);
}
