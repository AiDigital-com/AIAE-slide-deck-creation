package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.DonutChartRenderer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Unit tests for {@link InDepthChartRequestBuilderImpl}.
 */
class InDepthChartRequestBuilderImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SlideImageStore imageStore = new SlideImageStore();
	private final InDepthChartRequestBuilderImpl builder = new InDepthChartRequestBuilderImpl(objectMapper,
			imageStore, new DonutChartRenderer(),
			new InDepthValueFormatterImpl(new InDepthTemplateTokenRegistryImpl()));

	@Test
	void shouldScaleBarsToTheirValuesFromTheLongestMasterBarTest() {
		// Given: seven master bars, bar 7 the longest at 1000 EMU, all starting at x = 100, each
		// with a 120-wide value label sitting inside its right end
		ArrayNode elements = objectMapper.createArrayNode();
		for (int n = 1; n <= 7; n++) {
			elements.add(shape("bar" + n, "{{bar_" + n + "}}", 100, 100.0 * n + 300, ""));
			double barEnd = 100 + 100.0 * n + 300;
			elements.add(shape("value" + n, "", barEnd - 130, 120, "{{stat_chart_bar_" + n + "_value}}"));
		}
		Map<String, String> values = new HashMap<>();
		for (int n = 1; n <= 7; n++) {
			values.put("stat_chart_bar_" + n + "_value", (10 * n) + "%");
		}

		// When: the bar requests are built
		ArrayNode requests = builder.buildBarRequests(slide(elements), values);

		// Then: bar 7 keeps the longest length; bar 1 is 1/7 of it but never shorter than what keeps
		// its value label inside the bar, and the label shifts with the bar's end
		assertThat(scaleXFor(requests, "bar7")).isCloseTo(1000.0 / 100, offset(1e-6));
		double bar1Length = scaleXFor(requests, "bar1") * 100;
		assertThat(bar1Length).isCloseTo(Math.max(1000.0 / 7, 400 - (100 + 400 - 130 - 100)), offset(1e-6));
		JsonNode labelShift = requestFor(requests, "value1");
		assertThat(labelShift.path("applyMode").asText()).isEqualTo("RELATIVE");
		assertThat(labelShift.path("transform").path("translateX").asDouble())
				.isCloseTo(bar1Length - 400, offset(1e-6));
	}

	@Test
	void shouldLeaveBarsAloneWhenAnyBarAltTextIsMissingTest() {
		// Given: only one tagged bar
		ArrayNode elements = objectMapper.createArrayNode();
		elements.add(shape("bar1", "{{bar_1}}", 100, 400, ""));

		// When-Then: no requests
		assertThat(builder.buildBarRequests(slide(elements), Map.of("stat_chart_bar_1_value", "10%"))).isEmpty();
	}

	@Test
	void shouldLeaveBarsAloneWhenNoValueIsPositiveTest() {
		// Given: seven tagged bars and no values
		ArrayNode elements = objectMapper.createArrayNode();
		for (int n = 1; n <= 7; n++) {
			elements.add(shape("bar" + n, "{{bar_" + n + "}}", 100, 400, ""));
		}

		// When-Then: no requests
		assertThat(builder.buildBarRequests(slide(elements), Map.of())).isEmpty();
	}

	@Test
	void shouldReplaceATaggedDonutImageInPlaceTest() {
		// Given: the donut is a single tagged image
		ArrayNode elements = objectMapper.createArrayNode();
		ObjectNode donut = shape("donutImage", "{{donut_chart}}", 0, 500, "");
		donut.remove("shape");
		donut.putObject("image");
		elements.add(donut);
		Map<String, String> values = Map.of("donut_1_value", "40%", "donut_2_value", "30%",
				"donut_3_value", "20%", "donut_4_value", "10%");

		// When: the donut requests are built
		ArrayNode requests = builder.buildDonutRequests(slide(elements), values, "https://app.example.com/");

		// Then: one replaceImage pointing at the hosted PNG
		assertThat(requests).hasSize(1);
		JsonNode replace = requests.get(0).path("replaceImage");
		assertThat(replace.path("imageObjectId").asText()).isEqualTo("donutImage");
		assertThat(replace.path("imageReplaceMethod").asText()).isEqualTo("CENTER_INSIDE");
		assertThat(replace.path("url").asText()).startsWith("https://app.example.com/public/slide-images/");
		String token = replace.path("url").asText().substring("https://app.example.com/public/slide-images/".length());
		assertThat(imageStore.get(token)).isPresent();
		assertThat(imageStore.get(token).orElseThrow().contentType()).isEqualTo("image/png");
	}

	@Test
	void shouldSwapANativeDonutGroupForTheImageOnTheSameLayerTest() {
		// Given: back to front — card, donut group (two tagged segments), two label pills; the group
		// is translated and its children carry alt text because Slides cannot tag a group
		ArrayNode elements = objectMapper.createArrayNode();
		elements.add(shape("card", "", 0, 2000, ""));
		ObjectNode group = objectMapper.createObjectNode().put("objectId", "donutGroup");
		group.putObject("transform").put("scaleX", 1).put("scaleY", 1)
				.put("translateX", 1000).put("translateY", 500).put("unit", "EMU");
		ArrayNode children = group.putObject("elementGroup").putArray("children");
		children.add(shape("segA", "{{donut_chart}}", 0, 300, ""));
		children.add(shape("segB", "{{donut_chart}}", 200, 400, ""));
		elements.add(group);
		ObjectNode swatch = shape("dot1", "{{donut_1_swatch}}", 0, 10, "");
		swatch.with("shape").putObject("shapeProperties").putObject("shapeBackgroundFill")
				.putObject("solidFill").putObject("color").putObject("rgbColor").put("red", 1.0);
		elements.add(swatch);
		elements.add(shape("pill1", "", 0, 100, "{{donut_1_label}}"));
		elements.add(shape("pill2", "", 0, 100, "{{donut_2_label}}"));
		ObjectNode slide = (ObjectNode) slide(elements);
		slide.put("objectId", "slide8");
		Map<String, String> values = Map.of("donut_1_value", "40%", "donut_2_value", "30%",
				"donut_3_value", "20%", "donut_4_value", "10%");

		// When: the donut requests are built
		ArrayNode requests = builder.buildDonutRequests(slide, values, "https://app.example.com");

		// Then: the image covers the group's box, the group is deleted whole, and the image steps
		// back once per element that sat above the donut (swatch + two pills)
		JsonNode create = requests.get(0).path("createImage");
		assertThat(create.path("elementProperties").path("pageObjectId").asText()).isEqualTo("slide8");
		assertThat(create.path("elementProperties").path("transform").path("translateX").asDouble()).isEqualTo(1000);
		assertThat(create.path("elementProperties").path("size").path("width").path("magnitude").asDouble())
				.isEqualTo(600);
		assertThat(requests.get(1).path("deleteObject").path("objectId").asText()).isEqualTo("donutGroup");
		assertThat(requests).hasSize(5);
		for (int i = 2; i < 5; i++) {
			JsonNode zOrder = requests.get(i).path("updatePageElementsZOrder");
			assertThat(zOrder.path("operation").asText()).isEqualTo("SEND_BACKWARD");
			assertThat(zOrder.path("pageElementObjectIds").get(0).asText()).isEqualTo(create.path("objectId").asText());
		}
	}

	@Test
	void shouldDeleteUngroupedSlicesOneByOneAndTakeTheTopmostSliceLayerTest() {
		// Given: card, two loose slices, one pill on top
		ArrayNode elements = objectMapper.createArrayNode();
		elements.add(shape("card", "", 0, 2000, ""));
		elements.add(shape("slice1", "{{donut_slice_1}}", 100, 300, ""));
		elements.add(shape("slice2", "{{donut_slice_2}}", 300, 300, ""));
		elements.add(shape("pill", "", 0, 100, "{{donut_1_label}}"));
		ObjectNode slide = (ObjectNode) slide(elements);
		slide.put("objectId", "slide8");

		// When: the donut requests are built
		ArrayNode requests = builder.buildDonutRequests(slide,
				Map.of("donut_1_value", "60%", "donut_2_value", "40%"), "https://app.example.com");

		// Then: the image spans both slices, each slice is deleted, and one step back passes the pill
		assertThat(requests.get(0).path("createImage").path("elementProperties").path("size")
				.path("width").path("magnitude").asDouble()).isEqualTo(500);
		assertThat(requests.findValuesAsText("objectId")).contains("slice1", "slice2");
		assertThat(requests).hasSize(4);
		assertThat(requests.get(3).path("updatePageElementsZOrder").path("operation").asText())
				.isEqualTo("SEND_BACKWARD");
	}

	@Test
	void shouldSkipTheDonutWithoutATaggedElementOrBaseUrlOrValuesTest() {
		// Given: a slide with an untagged shape, and one with a tagged donut
		ArrayNode untagged = objectMapper.createArrayNode();
		untagged.add(shape("other", "", 0, 500, ""));
		ArrayNode tagged = objectMapper.createArrayNode();
		tagged.add(shape("donut", "{{donut_chart}}", 0, 500, ""));

		// When-Then: no tag, no base URL, or no positive value each leave the master donut alone
		assertThat(builder.buildDonutRequests(slide(untagged), Map.of("donut_1_value", "40%"),
				"https://app.example.com")).isEmpty();
		assertThat(builder.buildDonutRequests(slide(tagged), Map.of("donut_1_value", "40%"), " ")).isEmpty();
		assertThat(builder.buildDonutRequests(slide(tagged), Map.of(), "https://app.example.com")).isEmpty();
	}

	@Test
	void shouldBatchOnlyTheChartsOfCheckedSlidesTest() {
		// Given: a 24-slide presentation whose slide 8 has a tagged donut and slide 18 has no bars
		ArrayNode slides = objectMapper.createArrayNode();
		for (int n = 1; n <= 24; n++) {
			ArrayNode elements = objectMapper.createArrayNode();
			if (n == 8) {
				elements.add(shape("donut", "{{donut_chart}}", 0, 500, ""));
			}
			slides.add(slide(elements));
		}
		ObjectNode presentation = objectMapper.createObjectNode();
		presentation.set("slides", slides);
		Map<String, String> values = Map.of("donut_1_value", "60%", "donut_2_value", "40%");

		// When: chart batches are built for both chart slides
		List<ArrayNode> batches = builder.buildChartRequests(presentation, List.of(8, 18), values,
				"https://app.example.com");

		// Then: only the donut batch survives (the empty bar batch is dropped), and nothing is built
		// when neither chart slide is checked
		assertThat(batches).hasSize(1);
		assertThat(batches.get(0).get(0).has("createImage")).isTrue();
		assertThat(builder.buildChartRequests(presentation, List.of(2), values, "https://app.example.com")).isEmpty();
	}

	@Test
	void shouldReadASwatchColourFromItsSolidFillTest() {
		// Given: a legend dot with a magenta fill and one with a theme colour
		ObjectNode swatch = shape("dot", "{{donut_1_swatch}}", 0, 10, "");
		ObjectNode rgb = swatch.with("shape").putObject("shapeProperties").putObject("shapeBackgroundFill")
				.putObject("solidFill").putObject("color").putObject("rgbColor");
		rgb.put("red", 1.0).put("blue", 1.0);

		// When-Then: the RGB fill is read; a missing fill or element yields null
		assertThat(builder.solidFillColor(swatch)).isEqualTo(new Color(255, 0, 255));
		assertThat(builder.solidFillColor(shape("plain", "", 0, 10, ""))).isNull();
		assertThat(builder.solidFillColor(null)).isNull();
	}

	@Test
	void shouldSearchInsideGroupsForAltTextAndConvertPointsToEmuTest() {
		// Given: a bar tagged inside a group, with a PT-unit transform and size
		ArrayNode elements = objectMapper.createArrayNode();
		ObjectNode group = objectMapper.createObjectNode().put("objectId", "group");
		ObjectNode inner = shape("inner", "{{bar_3}}", 10, 10, "");
		inner.with("transform").put("unit", "PT");
		inner.with("size").with("width").put("unit", "PT");
		group.putObject("elementGroup").putArray("children").add(inner);
		elements.add(group);

		// When-Then: the lookup descends into the group and the geometry is converted to EMU
		assertThat(builder.findByAltText(elements, "{{bar_3}}").path("objectId").asText()).isEqualTo("inner");
		assertThat(builder.findByAltText(elements, "{{bar_4}}")).isNull();
		double[] box = builder.boundingBox(group, new double[] {1, 0, 0, 1, 0, 0});
		assertThat(box[0]).isEqualTo(10 * 12700);
		assertThat(box[2]).isEqualTo(10 * 12700 + 100 * 12700 * 0.1);
	}

	private ObjectNode shape(String id, String altText, double translateX, double width, String text) {
		ObjectNode element = objectMapper.createObjectNode();
		element.put("objectId", id);
		if (!altText.isEmpty()) {
			element.put("description", altText);
		}
		element.putObject("size").putObject("width").put("magnitude", 100).put("unit", "EMU");
		((ObjectNode) element.path("size")).putObject("height").put("magnitude", 20).put("unit", "EMU");
		element.putObject("transform").put("scaleX", width / 100).put("scaleY", 1)
				.put("translateX", translateX).put("translateY", 0).put("unit", "EMU");
		ObjectNode shape = element.putObject("shape");
		if (!text.isEmpty()) {
			shape.putObject("text").putArray("textElements").addObject().putObject("textRun").put("content", text);
		}
		return element;
	}

	private JsonNode slide(ArrayNode elements) {
		ObjectNode slide = objectMapper.createObjectNode();
		slide.set("pageElements", elements);
		return slide;
	}

	private JsonNode requestFor(ArrayNode requests, String objectId) {
		for (JsonNode request : requests) {
			JsonNode inner = request.path("updatePageElementTransform");
			if (objectId.equals(inner.path("objectId").asText())) {
				return inner;
			}
		}
		throw new AssertionError("no transform request for " + objectId);
	}

	private double scaleXFor(ArrayNode requests, String objectId) {
		return requestFor(requests, objectId).path("transform").path("scaleX").asDouble();
	}
}
