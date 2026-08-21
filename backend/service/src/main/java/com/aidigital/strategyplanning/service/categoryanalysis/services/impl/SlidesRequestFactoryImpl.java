package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link SlidesRequestFactory}.
 */
@Service
@RequiredArgsConstructor
public class SlidesRequestFactoryImpl implements SlidesRequestFactory {

	/**
	 * Size the fitted artwork is placed at (EMU).
	 */
	public static final double FIT_IMAGE_SIZE_EMU = 4876800;

	/**
	 * Absolute X offset of the fitted artwork (EMU).
	 */
	public static final double FIT_IMAGE_TRANSLATE_X = -633762;

	/**
	 * Absolute Y offset of the fitted artwork (EMU).
	 */
	public static final double FIT_IMAGE_TRANSLATE_Y = 1215602;

	private final ObjectMapper objectMapper;
	private final SlidesPresentationInspector inspector;

	@Override
	public ArrayNode buildSourceLinkRequests(JsonNode presentation, Map<String, String> sourceValues,
	                                         Map<String, List<SourceLink>> linksByKey) {
		ArrayNode requests = objectMapper.createArrayNode();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			List<SourceLink> links = linksByKey.get(spec.key());
			String value = sourceValues.get(spec.key());
			if (links == null || links.isEmpty() || !StringUtils.hasText(value)) {
				continue;
			}
			JsonNode shape = inspector.findShapeContainingText(presentation, spec.slideNumber() - 1, value);
			if (shape == null) {
				continue;
			}
			String shapeText = inspector.shapeText(shape);
			String objectId = shape.path("objectId").asText();
			for (SourceLink link : links) {
				int start = shapeText.indexOf(link.text());
				if (start < 0) {
					continue;
				}
				requests.add(updateTextLinkRequest(objectId, start, start + link.text().length(),
						link.url(), inspector.foregroundColorAt(shape, start)));
			}
		}
		return requests;
	}

	/**
	 * Builds a single updateTextStyle request that links a text range while preserving its
	 * appearance. The fields mask always includes foregroundColor and underline so the Slides
	 * API does not apply its hyperlink defaults: when the run had an explicit color it is
	 * re-applied unchanged, otherwise leaving the value unset resets the range to the color
	 * it inherits from its placeholder.
	 *
	 * @param objectId        shape object ID
	 * @param startIndex      range start (inclusive, UTF-16)
	 * @param endIndex        range end (exclusive, UTF-16)
	 * @param url             link target URL
	 * @param foregroundColor explicit foreground color to keep, or null to inherit
	 * @return request node
	 */
	@Override
	public ObjectNode updateTextLinkRequest(String objectId, int startIndex, int endIndex,
	                                        String url, JsonNode foregroundColor) {
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject("updateTextStyle");
		inner.put("objectId", objectId);
		ObjectNode range = inner.putObject("textRange");
		range.put("type", "FIXED_RANGE");
		range.put("startIndex", startIndex);
		range.put("endIndex", endIndex);
		ObjectNode style = inner.putObject("style");
		style.putObject("link").put("url", url);
		style.put("underline", false);
		if (foregroundColor != null) {
			style.set("foregroundColor", foregroundColor.deepCopy());
		}
		inner.put("fields", "link,underline,foregroundColor");
		return request;
	}

	/**
	 * Builds replaceAllText requests for every template token, scoped to the token's slide
	 * so identical sample texts on different slides get their own values. Both the
	 * {@code {{token}}} form and the master template's current sample literal are replaced
	 * for every token — a blank reviewed value replaces with an empty string so no
	 * {@code {{token}}} or sample literal is ever left visible in the finished deck.
	 *
	 * @param tokenValues final reviewed values keyed by token key
	 * @param slideIds    slide object IDs in deck order
	 * @return array of Slides API requests
	 */
	@Override
	public ArrayNode buildReplaceTextRequests(Map<String, String> tokenValues, List<String> slideIds) {
		ArrayNode requests = objectMapper.createArrayNode();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			String rawValue = tokenValues.get(spec.key());
			String value = StringUtils.hasText(rawValue) ? rawValue : "";
			int slideIndex = spec.slideNumber() - 1;
			String pageId = slideIndex < slideIds.size() ? slideIds.get(slideIndex) : null;
			requests.add(replaceAllTextRequest("{{" + spec.key() + "}}", value, pageId));
			requests.add(replaceAllTextRequest(spec.sampleText(), value, pageId));
		}
		return requests;
	}

	/**
	 * Builds a single replaceAllText request.
	 *
	 * @param find    text to find (exact, case-sensitive)
	 * @param replace replacement text
	 * @param pageId  slide object ID to scope the replacement to, or null for deck-wide
	 * @return request node
	 */
	@Override
	public ObjectNode replaceAllTextRequest(String find, String replace, String pageId) {
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject("replaceAllText");
		ObjectNode containsText = inner.putObject("containsText");
		containsText.put("text", find);
		containsText.put("matchCase", true);
		inner.put("replaceText", replace);
		if (pageId != null) {
			inner.putArray("pageObjectIds").add(pageId);
		}
		return request;
	}

	/**
	 * Builds an updatePageElementTransform request that places a page element at an absolute
	 * position with an absolute displayed size, matching the approved reference deck: the
	 * transform scale is derived from the element's intrinsic size so its displayed square
	 * bounding box equals the target size.
	 *
	 * @param element    page element to place (must carry size)
	 * @param targetSize target displayed size (EMU) for both width and height
	 * @param translateX absolute X position (EMU)
	 * @param translateY absolute Y position (EMU)
	 * @return request node
	 */
	@Override
	public ObjectNode buildAbsolutePlacementRequest(JsonNode element, double targetSize,
	                                                double translateX, double translateY) {
		double rawWidth = element.path("size").path("width").path("magnitude").asDouble(0);
		double rawHeight = element.path("size").path("height").path("magnitude").asDouble(0);
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject("updatePageElementTransform");
		inner.put("objectId", element.path("objectId").asText());
		inner.put("applyMode", "ABSOLUTE");
		ObjectNode t = inner.putObject("transform");
		t.put("scaleX", rawWidth > 0 ? targetSize / rawWidth : 1);
		t.put("scaleY", rawHeight > 0 ? targetSize / rawHeight : 1);
		t.put("shearX", 0);
		t.put("shearY", 0);
		t.put("translateX", translateX);
		t.put("translateY", translateY);
		t.put("unit", "EMU");
		return request;
	}

	/**
	 * Builds a createImage request that inserts a generated cutout into the fixed slide-2
	 * placeholder area matching the approved reference deck. The Slides API scales the image
	 * to fit inside the given bounding box while preserving its aspect ratio, so the cutout is
	 * never distorted.
	 *
	 * @param slideObjectId object ID of the slide receiving the image
	 * @param imageUrl      fetchable URL of the generated image
	 * @return request node
	 */
	@Override
	public ObjectNode buildCreateImageRequest(String slideObjectId, String imageUrl) {
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject("createImage");
		inner.put("url", imageUrl);
		ObjectNode props = inner.putObject("elementProperties");
		props.put("pageObjectId", slideObjectId);
		ObjectNode size = props.putObject("size");
		size.putObject("width").put("magnitude", FIT_IMAGE_SIZE_EMU).put("unit", "EMU");
		size.putObject("height").put("magnitude", FIT_IMAGE_SIZE_EMU).put("unit", "EMU");
		ObjectNode t = props.putObject("transform");
		t.put("scaleX", 1);
		t.put("scaleY", 1);
		t.put("translateX", FIT_IMAGE_TRANSLATE_X);
		t.put("translateY", FIT_IMAGE_TRANSLATE_Y);
		t.put("unit", "EMU");
		return request;
	}
}
