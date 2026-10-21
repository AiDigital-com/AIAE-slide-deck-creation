package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.DonutChartRenderer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthChartRequestBuilder;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthDeckLayout;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of {@link InDepthChartRequestBuilder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InDepthChartRequestBuilderImpl implements InDepthChartRequestBuilder {

	/**
	 * Affine transform as {scaleX, shearY, shearX, scaleY, translateX, translateY}.
	 */
	private static final double[] IDENTITY = {1, 0, 0, 1, 0, 0};
	private static final String PNG_CONTENT_TYPE = "image/png";
	private static final String UNIT_EMU = "EMU";
	private static final String UNIT_PT = "PT";
	private static final String APPLY_ABSOLUTE = "ABSOLUTE";
	private static final String APPLY_RELATIVE = "RELATIVE";
	private static final String SEND_BACKWARD = "SEND_BACKWARD";
	private static final String REPLACE_CENTER_INSIDE = "CENTER_INSIDE";
	private static final String TRAILING_SLASHES = "/+$";
	private static final int MIN_X = 0;
	private static final int MIN_Y = 1;
	private static final int MAX_X = 2;
	private static final int MAX_Y = 3;

	private final ObjectMapper objectMapper;
	private final SlideImageStore slideImageStore;
	private final DonutChartRenderer donutChartRenderer;
	private final InDepthValueFormatter valueFormatter;

	@Override
	public List<ArrayNode> buildChartRequests(JsonNode presentation, List<Integer> selectedSlides,
	                                          Map<String, String> values, String publicBaseUrl) {
		List<ArrayNode> batches = new ArrayList<>();
		JsonNode slides = presentation.path("slides");
		if (selectedSlides.contains(InDepthDeckLayout.DONUT_SLIDE) && slides.size() >= InDepthDeckLayout.DONUT_SLIDE) {
			batches.add(buildDonutRequests(slides.path(InDepthDeckLayout.DONUT_SLIDE - 1), values, publicBaseUrl));
		}
		if (selectedSlides.contains(InDepthDeckLayout.STAT_CHART_SLIDE)
				&& slides.size() >= InDepthDeckLayout.STAT_CHART_SLIDE) {
			batches.add(buildBarRequests(slides.path(InDepthDeckLayout.STAT_CHART_SLIDE - 1), values));
		}
		batches.removeIf(ArrayNode::isEmpty);
		return batches;
	}

	/**
	 * Draws the slide-8 donut from the donut values and puts it where the master's donut is. A
	 * single tagged image is swapped in place; a native donut (slices tagged one by one, grouped
	 * or not) is deleted, the image is created covering the slices' combined bounding box, then
	 * stepped back one layer at a time to the donut's original position so the legend pills stay
	 * on top (a plain "send to back" would hide it behind the card).
	 *
	 * @param slide         slide-8 JSON
	 * @param values        final field values (donut series sorted largest first)
	 * @param publicBaseUrl base URL the stored PNG is served from
	 * @return the requests that swap the donut, or none when the donut cannot be drawn
	 */
	@Override
	public ArrayNode buildDonutRequests(JsonNode slide, Map<String, String> values, String publicBaseUrl) {
		ArrayNode requests = objectMapper.createArrayNode();
		JsonNode pageElements = slide.path("pageElements");
		List<JsonNode> slices = new ArrayList<>();
		List<double[]> sliceParents = new ArrayList<>();
		collectDonutSlices(pageElements, IDENTITY, slices, sliceParents);
		if (slices.isEmpty()) {
			log.warn("Slide {} has no element with alt text {} — donut left as in the master",
					InDepthDeckLayout.DONUT_SLIDE, InDepthDeckLayout.DONUT_ALT_TEXT);
			return requests;
		}
		if (!StringUtils.hasText(publicBaseUrl)) {
			log.warn("No public base URL to serve the donut image from — donut left as in the master");
			return requests;
		}
		List<Double> numbers = new ArrayList<>();
		List<Color> colors = new ArrayList<>();
		for (int n = 1; n <= InDepthTemplateTokens.DONUT_SLICES; n++) {
			numbers.add(valueFormatter.chartNumber(
					values.get(String.format(InDepthDeckLayout.DONUT_VALUE_KEY_FORMAT, n))));
			JsonNode swatch = findByAltText(pageElements,
					String.format(InDepthDeckLayout.DONUT_SWATCH_ALT_TEXT_FORMAT, n));
			Color color = solidFillColor(swatch);
			colors.add(color != null ? color : InDepthDeckLayout.DEFAULT_DONUT_COLORS.get(n - 1));
		}
		if (numbers.stream().allMatch(number -> number <= 0)) {
			log.warn("Donut values are missing or zero — donut left as in the master");
			return requests;
		}
		double[] box = emptyBox();
		for (int i = 0; i < slices.size(); i++) {
			mergeBox(box, boundingBox(slices.get(i), sliceParents.get(i)));
		}
		double width = box[MAX_X] - box[MIN_X];
		double height = box[MAX_Y] - box[MIN_Y];
		if (width <= 0 || height <= 0) {
			log.warn("Could not measure the donut on slide {} — donut left as in the master",
					InDepthDeckLayout.DONUT_SLIDE);
			return requests;
		}
		byte[] png = donutChartRenderer.render(numbers, colors, width / height);
		String token = slideImageStore.put(png, PNG_CONTENT_TYPE);
		String url = publicBaseUrl.replaceAll(TRAILING_SLASHES, "")
				+ OpenAiSlideImageServiceImpl.PUBLIC_IMAGE_PATH + token;

		if (slices.size() == 1 && slices.get(0).has("image")) {
			ObjectNode request = objectMapper.createObjectNode();
			ObjectNode replace = request.putObject("replaceImage");
			replace.put("imageObjectId", slices.get(0).path("objectId").asText());
			replace.put("imageReplaceMethod", REPLACE_CENTER_INSIDE);
			replace.put("url", url);
			requests.add(request);
			return requests;
		}

		String imageId = InDepthDeckLayout.DONUT_IMAGE_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
		requests.add(createImageRequest(imageId, slide.path("objectId").asText(), url, box));

		// Delete whole top-level elements made only of slices (e.g. the slices' group), and any
		// remaining slices one by one. Track the topmost layer the donut occupied.
		Set<String> sliceIds = new HashSet<>();
		slices.forEach(slice -> sliceIds.add(slice.path("objectId").asText()));
		int topLayer = -1;
		for (int i = 0; i < pageElements.size(); i++) {
			JsonNode element = pageElements.get(i);
			List<String> tagged = new ArrayList<>();
			boolean onlySlices = collectTaggedIds(element, sliceIds, tagged);
			if (tagged.isEmpty()) {
				continue;
			}
			topLayer = i;
			for (String objectId : onlySlices ? List.of(element.path("objectId").asText()) : tagged) {
				ObjectNode delete = objectMapper.createObjectNode();
				delete.putObject("deleteObject").put("objectId", objectId);
				requests.add(delete);
			}
		}

		// The new image starts on top of the slide. Step it back below every top-level element that
		// sat above the donut, which leaves it exactly on the donut's old layer.
		int layersAbove = pageElements.size() - 1 - topLayer;
		for (int step = 0; step < layersAbove; step++) {
			ObjectNode reorder = objectMapper.createObjectNode();
			ObjectNode zOrder = reorder.putObject("updatePageElementsZOrder");
			zOrder.putArray("pageElementObjectIds").add(imageId);
			zOrder.put("operation", SEND_BACKWARD);
			requests.add(reorder);
		}
		return requests;
	}

	@Override
	public ArrayNode buildBarRequests(JsonNode slide, Map<String, String> values) {
		ArrayNode requests = objectMapper.createArrayNode();
		int count = InDepthTemplateTokens.STAT_CHART_BARS;
		JsonNode pageElements = slide.path("pageElements");
		List<JsonNode> bars = new ArrayList<>();
		for (int n = 1; n <= count; n++) {
			JsonNode bar = findByAltText(pageElements, String.format(InDepthDeckLayout.BAR_ALT_TEXT_FORMAT, n));
			if (bar == null) {
				log.warn("Slide {} has no shape with alt text bar_{} — bars left as in the master",
						InDepthDeckLayout.STAT_CHART_SLIDE, n);
				return requests;
			}
			bars.add(bar);
		}
		double maxLength = bars.stream().mapToDouble(this::displayedWidth).max().orElse(0);
		double maxValue = 0;
		List<Double> numbers = new ArrayList<>();
		for (int n = 1; n <= count; n++) {
			double number = valueFormatter.chartNumber(
					values.get(String.format(InDepthDeckLayout.BAR_VALUE_KEY_FORMAT, n)));
			numbers.add(number);
			maxValue = Math.max(maxValue, number);
		}
		if (maxLength <= 0 || maxValue <= 0) {
			log.warn("Bar values are missing or zero — bars left as in the master");
			return requests;
		}
		for (int n = 1; n <= count; n++) {
			JsonNode bar = bars.get(n - 1);
			JsonNode label = findByText(pageElements, String.format(InDepthDeckLayout.BAR_VALUE_PLACEHOLDER_FORMAT, n));
			double intrinsicWidth = bar.path("size").path("width").path("magnitude").asDouble(0);
			double oldLength = displayedWidth(bar);
			if (intrinsicWidth <= 0 || oldLength <= 0) {
				continue;
			}
			double newLength = maxLength * Math.max(numbers.get(n - 1), 0) / maxValue;
			// Never shrink a bar so far that its value label would slide past the bar's left edge.
			double minLength = maxLength * InDepthDeckLayout.MIN_BAR_FRACTION;
			if (label != null) {
				double barLeft = bar.path("transform").path("translateX").asDouble(0);
				double labelLeft = label.path("transform").path("translateX").asDouble(0);
				minLength = Math.max(minLength, oldLength - (labelLeft - barLeft));
			}
			newLength = Math.max(newLength, minLength);

			ObjectNode transform = absoluteTransform(bar.path("transform"));
			transform.put("scaleX", newLength / intrinsicWidth);
			requests.add(transformRequest(bar.path("objectId").asText(), APPLY_ABSOLUTE, transform));
			double delta = newLength - oldLength;
			if (label != null && Math.abs(delta) > InDepthDeckLayout.MIN_LABEL_SHIFT_EMU) {
				ObjectNode shift = objectMapper.createObjectNode();
				shift.put("scaleX", 1).put("scaleY", 1).put("shearX", 0).put("shearY", 0)
						.put("translateX", delta).put("translateY", 0).put("unit", UNIT_EMU);
				requests.add(transformRequest(label.path("objectId").asText(), APPLY_RELATIVE, shift));
			}
		}
		return requests;
	}

	/**
	 * Builds the createImage request that places the drawn donut over the master donut's box.
	 *
	 * @param imageId       object id to give the new image
	 * @param slideObjectId slide receiving the image
	 * @param url           fetchable URL of the drawn donut
	 * @param box           {minX, minY, maxX, maxY} of the master donut in EMU
	 * @return request node
	 */
	ObjectNode createImageRequest(String imageId, String slideObjectId, String url, double[] box) {
		ObjectNode create = objectMapper.createObjectNode();
		ObjectNode createImage = create.putObject("createImage");
		createImage.put("objectId", imageId);
		createImage.put("url", url);
		ObjectNode properties = createImage.putObject("elementProperties");
		properties.put("pageObjectId", slideObjectId);
		ObjectNode size = properties.putObject("size");
		size.putObject("width").put("magnitude", box[MAX_X] - box[MIN_X]).put("unit", UNIT_EMU);
		size.putObject("height").put("magnitude", box[MAX_Y] - box[MIN_Y]).put("unit", UNIT_EMU);
		properties.putObject("transform").put("scaleX", 1).put("scaleY", 1)
				.put("translateX", box[MIN_X]).put("translateY", box[MIN_Y]).put("unit", UNIT_EMU);
		return create;
	}

	/**
	 * Finds the first page element (searching inside groups) whose alt text equals the given text.
	 *
	 * @param elements page elements to search
	 * @param altText  alt text (the Slides "description") to match, ignoring surrounding spaces
	 * @return the matching element, or null
	 */
	JsonNode findByAltText(JsonNode elements, String altText) {
		for (JsonNode element : elements) {
			if (altText.equals(element.path("description").asText("").trim())) {
				return element;
			}
			JsonNode child = findByAltText(element.path("elementGroup").path("children"), altText);
			if (child != null) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Finds the first shape (searching inside groups) whose text contains the given text.
	 *
	 * @param elements page elements to search
	 * @param text     text the shape must contain
	 * @return the matching element, or null
	 */
	JsonNode findByText(JsonNode elements, String text) {
		for (JsonNode element : elements) {
			StringBuilder content = new StringBuilder();
			for (JsonNode textElement : element.path("shape").path("text").path("textElements")) {
				content.append(textElement.path("textRun").path("content").asText(""));
			}
			if (content.toString().contains(text)) {
				return element;
			}
			JsonNode child = findByText(element.path("elementGroup").path("children"), text);
			if (child != null) {
				return child;
			}
		}
		return null;
	}

	/**
	 * Reads a shape's solid RGB fill.
	 *
	 * @param element shape element, may be null
	 * @return the fill colour, or null when the shape has no solid RGB fill (e.g. a theme colour)
	 */
	Color solidFillColor(JsonNode element) {
		if (element == null) {
			return null;
		}
		JsonNode rgb = element.path("shape").path("shapeProperties").path("shapeBackgroundFill")
				.path("solidFill").path("color").path("rgbColor");
		if (rgb.isMissingNode()) {
			return null;
		}
		return new Color((float) rgb.path("red").asDouble(0), (float) rgb.path("green").asDouble(0),
				(float) rgb.path("blue").asDouble(0));
	}

	/**
	 * Computes an element's on-page bounding box in EMU — for a group, the union of its children —
	 * applying every parent group's transform (the Slides API stores a grouped element's
	 * transform relative to its group).
	 *
	 * @param element page element
	 * @param parent  absolute transform of the element's parent group (identity at top level)
	 * @return {minX, minY, maxX, maxY}; an empty box (max below min) when nothing has a size
	 */
	double[] boundingBox(JsonNode element, double[] parent) {
		double[] absolute = concat(parent, transformOf(element.path("transform")));
		double[] box = emptyBox();
		JsonNode children = element.path("elementGroup").path("children");
		if (children.isArray() && !children.isEmpty()) {
			for (JsonNode child : children) {
				mergeBox(box, boundingBox(child, absolute));
			}
			return box;
		}
		double width = toEmu(element.path("size").path("width"));
		double height = toEmu(element.path("size").path("height"));
		for (double[] corner : new double[][] {{0, 0}, {width, 0}, {0, height}, {width, height}}) {
			double x = absolute[0] * corner[0] + absolute[2] * corner[1] + absolute[4];
			double y = absolute[1] * corner[0] + absolute[3] * corner[1] + absolute[5];
			box[MIN_X] = Math.min(box[MIN_X], x);
			box[MIN_Y] = Math.min(box[MIN_Y], y);
			box[MAX_X] = Math.max(box[MAX_X], x);
			box[MAX_Y] = Math.max(box[MAX_Y], y);
		}
		return box;
	}

	/**
	 * Creates an empty bounding box whose minima exceed its maxima, so any merge replaces it.
	 *
	 * @return {minX, minY, maxX, maxY}
	 */
	double[] emptyBox() {
		return new double[] {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
	}

	/**
	 * Grows a bounding box in place to include another.
	 *
	 * @param box   box to grow, {minX, minY, maxX, maxY}
	 * @param other box to include
	 */
	void mergeBox(double[] box, double[] other) {
		box[MIN_X] = Math.min(box[MIN_X], other[MIN_X]);
		box[MIN_Y] = Math.min(box[MIN_Y], other[MIN_Y]);
		box[MAX_X] = Math.max(box[MAX_X], other[MAX_X]);
		box[MAX_Y] = Math.max(box[MAX_Y], other[MAX_Y]);
	}

	/**
	 * Collects every donut slice — any element whose alt text is {@code {{donut_chart}}} or
	 * {@code {{donut_slice_N}}} — searching inside groups (Slides cannot put alt text on a group,
	 * so a grouped donut is tagged slice by slice).
	 *
	 * @param elements     elements to search
	 * @param parent       absolute transform of the elements' parent group
	 * @param slices       receives each slice found
	 * @param sliceParents receives each slice's parent transform, same order as {@code slices}
	 */
	void collectDonutSlices(JsonNode elements, double[] parent, List<JsonNode> slices,
			List<double[]> sliceParents) {
		for (JsonNode element : elements) {
			if (isDonutSlice(element)) {
				slices.add(element);
				sliceParents.add(parent);
				continue;
			}
			JsonNode children = element.path("elementGroup").path("children");
			if (children.isArray() && !children.isEmpty()) {
				collectDonutSlices(children, concat(parent, transformOf(element.path("transform"))),
						slices, sliceParents);
			}
		}
	}

	/**
	 * Reports whether an element is tagged as a donut slice.
	 *
	 * @param element page element
	 * @return true when its alt text is {@code {{donut_chart}}} or {@code {{donut_slice_N}}}
	 */
	boolean isDonutSlice(JsonNode element) {
		String altText = element.path("description").asText("").trim();
		return InDepthDeckLayout.DONUT_ALT_TEXT.equals(altText)
				|| InDepthDeckLayout.DONUT_SLICE_ALT_TEXT.matcher(altText).matches();
	}

	/**
	 * Gathers the ids of the slices inside an element and reports whether the element consists
	 * of slices only (so it can be deleted whole).
	 *
	 * @param element  element to inspect
	 * @param sliceIds ids of every donut slice
	 * @param tagged   receives the ids of the slices found inside the element
	 * @return true when every leaf of the element is a slice
	 */
	boolean collectTaggedIds(JsonNode element, Set<String> sliceIds, List<String> tagged) {
		String objectId = element.path("objectId").asText();
		if (sliceIds.contains(objectId)) {
			tagged.add(objectId);
			return true;
		}
		JsonNode children = element.path("elementGroup").path("children");
		if (!children.isArray() || children.isEmpty()) {
			return false;
		}
		boolean onlySlices = true;
		for (JsonNode child : children) {
			onlySlices &= collectTaggedIds(child, sliceIds, tagged);
		}
		return onlySlices;
	}

	/**
	 * Reads a Slides transform as an affine matrix in EMU.
	 *
	 * @param transform Slides API transform JSON (may be missing)
	 * @return {scaleX, shearY, shearX, scaleY, translateX, translateY}
	 */
	double[] transformOf(JsonNode transform) {
		double unit = UNIT_PT.equals(transform.path("unit").asText(UNIT_EMU)) ? InDepthDeckLayout.EMU_PER_POINT : 1;
		return new double[] {
				transform.path("scaleX").asDouble(1), transform.path("shearY").asDouble(0),
				transform.path("shearX").asDouble(0), transform.path("scaleY").asDouble(1),
				transform.path("translateX").asDouble(0) * unit, transform.path("translateY").asDouble(0) * unit};
	}

	/**
	 * Composes two affine transforms: applies {@code inner} first, then {@code outer}.
	 *
	 * @param outer parent transform
	 * @param inner child transform
	 * @return the combined transform
	 */
	double[] concat(double[] outer, double[] inner) {
		return new double[] {
				outer[0] * inner[0] + outer[2] * inner[1],
				outer[1] * inner[0] + outer[3] * inner[1],
				outer[0] * inner[2] + outer[2] * inner[3],
				outer[1] * inner[2] + outer[3] * inner[3],
				outer[0] * inner[4] + outer[2] * inner[5] + outer[4],
				outer[1] * inner[4] + outer[3] * inner[5] + outer[5]};
	}

	/**
	 * Reads a Slides dimension in EMU.
	 *
	 * @param dimension Slides API dimension JSON (magnitude + unit)
	 * @return the magnitude in EMU, 0 when missing
	 */
	double toEmu(JsonNode dimension) {
		double magnitude = dimension.path("magnitude").asDouble(0);
		return UNIT_PT.equals(dimension.path("unit").asText(UNIT_EMU))
				? magnitude * InDepthDeckLayout.EMU_PER_POINT : magnitude;
	}

	/**
	 * Computes an element's displayed width: its intrinsic width times its horizontal scale.
	 *
	 * @param element page element
	 * @return displayed width in the element's own units
	 */
	double displayedWidth(JsonNode element) {
		return element.path("size").path("width").path("magnitude").asDouble(0)
				* element.path("transform").path("scaleX").asDouble(1);
	}

	/**
	 * Copies a transform into a complete request transform, filling in defaults.
	 *
	 * @param current the element's current transform JSON
	 * @return a full transform node in EMU
	 */
	ObjectNode absoluteTransform(JsonNode current) {
		ObjectNode transform = objectMapper.createObjectNode();
		transform.put("scaleX", current.path("scaleX").asDouble(1));
		transform.put("scaleY", current.path("scaleY").asDouble(1));
		transform.put("shearX", current.path("shearX").asDouble(0));
		transform.put("shearY", current.path("shearY").asDouble(0));
		transform.put("translateX", current.path("translateX").asDouble(0));
		transform.put("translateY", current.path("translateY").asDouble(0));
		transform.put("unit", UNIT_EMU);
		return transform;
	}

	/**
	 * Builds an updatePageElementTransform request.
	 *
	 * @param objectId  element to transform
	 * @param applyMode "ABSOLUTE" or "RELATIVE"
	 * @param transform transform to apply
	 * @return request node
	 */
	ObjectNode transformRequest(String objectId, String applyMode, ObjectNode transform) {
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject("updatePageElementTransform");
		inner.put("objectId", objectId);
		inner.put("applyMode", applyMode);
		inner.set("transform", transform);
		return request;
	}
}
