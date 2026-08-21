package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardDeckTemplateAdjuster;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardDeckLayout;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link StandardDeckTemplateAdjuster}.
 */
@Service
@RequiredArgsConstructor
public class StandardDeckTemplateAdjusterImpl implements StandardDeckTemplateAdjuster {

	private final ObjectMapper objectMapper;
	private final SlidesPresentationInspector inspector;

	@Override
	public ArrayNode buildAdjustmentRequests(JsonNode presentation) {
		ArrayNode requests = objectMapper.createArrayNode();
		int circleSlideIndex = StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1;
		JsonNode circle = inspector.findTargetImageElement(presentation, circleSlideIndex, true);
		JsonNode largest = inspector.findTargetImageElement(presentation, circleSlideIndex, false);
		if (circle != null && largest != null
				&& !circle.path("objectId").asText().equals(largest.path("objectId").asText())) {
			ObjectNode delete = objectMapper.createObjectNode();
			delete.putObject("deleteObject").put("objectId", largest.path("objectId").asText());
			requests.add(delete);
		}
		if (circle != null) {
			double rawWidth = circle.path("size").path("width").path("magnitude").asDouble(0);
			double rawHeight = circle.path("size").path("height").path("magnitude").asDouble(0);
			if (rawWidth > 0 && rawHeight > 0) {
				ObjectNode transform = objectMapper.createObjectNode();
				ObjectNode inner = transform.putObject("updatePageElementTransform");
				inner.put("objectId", circle.path("objectId").asText());
				inner.put("applyMode", "ABSOLUTE");
				ObjectNode t = inner.putObject("transform");
				t.put("scaleX", StandardDeckLayout.CIRCLE_TARGET_SIZE_EMU / rawWidth);
				t.put("scaleY", StandardDeckLayout.CIRCLE_TARGET_SIZE_EMU / rawHeight);
				t.put("shearX", 0);
				t.put("shearY", 0);
				t.put("translateX", StandardDeckLayout.CIRCLE_TARGET_TRANSLATE_X);
				t.put("translateY", StandardDeckLayout.CIRCLE_TARGET_TRANSLATE_Y);
				t.put("unit", "EMU");
				requests.add(transform);
			}
		}
		String chartTitleId = inspector.findShapeIdByText(presentation, StandardDeckLayout.FIT_INSIDE_SLIDE - 1,
				StandardTemplateTokens.CHART_TITLE_SAMPLE);
		if (chartTitleId != null) {
			ObjectNode delete = objectMapper.createObjectNode();
			delete.putObject("deleteObject").put("objectId", chartTitleId);
			requests.add(delete);
		}
		String sectionLabelId = inspector.findShapeIdByText(presentation, StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1,
				StandardTemplateTokens.DRIVERS_SECTION_LABEL_SAMPLE);
		if (sectionLabelId != null) {
			ObjectNode delete = objectMapper.createObjectNode();
			delete.putObject("deleteObject").put("objectId", sectionLabelId);
			requests.add(delete);
		}
		String bulletsId = inspector.findShapeIdByText(presentation, StandardDeckLayout.FIT_INSIDE_SLIDE - 1,
				StandardTemplateTokens.TREND_BULLETS_SAMPLE);
		if (bulletsId != null) {
			ObjectNode bold = objectMapper.createObjectNode();
			ObjectNode inner = bold.putObject("updateTextStyle");
			inner.put("objectId", bulletsId);
			inner.putObject("textRange").put("type", "ALL");
			ObjectNode bulletsStyle = inner.putObject("style");
			bulletsStyle.put("bold", true);
			bulletsStyle.putObject("fontSize")
					.put("magnitude", StandardDeckLayout.TREND_BULLETS_FONT_SIZE_PT).put("unit", "PT");
			inner.put("fields", "bold,fontSize");
			requests.add(bold);
		}
		String trendingHeadingId = inspector.findShapeIdByText(presentation, StandardDeckLayout.FIT_INSIDE_SLIDE - 1,
				StandardDeckLayout.TRENDING_HEADING_TEXT);
		if (trendingHeadingId != null) {
			ObjectNode restyle = objectMapper.createObjectNode();
			ObjectNode inner = restyle.putObject("updateTextStyle");
			inner.put("objectId", trendingHeadingId);
			inner.putObject("textRange").put("type", "ALL");
			ObjectNode style = inner.putObject("style");
			ObjectNode rgb = style.putObject("foregroundColor")
					.putObject("opaqueColor").putObject("rgbColor");
			rgb.put("red", StandardDeckLayout.BRAND_GREEN_RED);
			rgb.put("green", StandardDeckLayout.BRAND_GREEN_GREEN);
			rgb.put("blue", StandardDeckLayout.BRAND_GREEN_BLUE);
			style.putObject("fontSize")
					.put("magnitude", StandardDeckLayout.TRENDING_HEADING_FONT_SIZE_PT).put("unit", "PT");
			inner.put("fields", "foregroundColor,fontSize");
			requests.add(restyle);
		}
		String headingId = inspector.findShapeIdByText(presentation, 0, StandardDeckLayout.TITLE_HEADING_TEXT);
		if (headingId != null) {
			ObjectNode recolor = objectMapper.createObjectNode();
			ObjectNode inner = recolor.putObject("updateTextStyle");
			inner.put("objectId", headingId);
			inner.putObject("textRange").put("type", "ALL");
			ObjectNode rgb = inner.putObject("style").putObject("foregroundColor")
					.putObject("opaqueColor").putObject("rgbColor");
			rgb.put("red", StandardDeckLayout.BRAND_GREEN_RED);
			rgb.put("green", StandardDeckLayout.BRAND_GREEN_GREEN);
			rgb.put("blue", StandardDeckLayout.BRAND_GREEN_BLUE);
			inner.put("fields", "foregroundColor");
			requests.add(recolor);
		}
		return requests;
	}
}
