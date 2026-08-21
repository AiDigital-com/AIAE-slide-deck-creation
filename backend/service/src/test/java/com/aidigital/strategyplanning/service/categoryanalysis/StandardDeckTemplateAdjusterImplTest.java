package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.StandardDeckTemplateAdjusterImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardDeckLayout;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the corrections applied to a freshly copied Standard deck.
 *
 * <p>Every correction is best-effort by design: the template is owned in Drive and can be edited
 * without touching this code, so an element that is no longer there must be skipped rather than
 * failing deck creation. These tests pin that each correction is emitted when its element exists
 * and silently omitted when it does not.
 */
class StandardDeckTemplateAdjusterImplTest {

	private static final int CIRCLE_SLIDE = StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1;
	private static final int CHART_SLIDE = StandardDeckLayout.FIT_INSIDE_SLIDE - 1;

	@Test
	void shouldEmitNothingWhenTheTemplateHasNoneOfTheSampleElementsTest() {
		// Given: a template edited so that no sample artwork or copy remains
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: nothing is sent, so a re-styled template still produces a deck
		assertThat(requests).isEmpty();
	}

	@Test
	void shouldDeleteTheDuplicateImageBehindTheCircularCropTest() {
		// Given: the template still carries a second, larger copy of the circular image
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		ObjectNode circle = objectMapper.createObjectNode();
		circle.put("objectId", "circle_1");
		ObjectNode largest = objectMapper.createObjectNode();
		largest.put("objectId", "backdrop_1");
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, true)).thenReturn(circle);
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, false)).thenReturn(largest);

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: only the duplicate is removed, never the cropped image the deck needs
		assertThat(requests).hasSize(1);
		assertThat(requests.get(0).path("deleteObject").path("objectId").asText())
				.isEqualTo("backdrop_1");
	}

	@Test
	void shouldNotDeleteAnythingWhenTheCircleIsAlsoTheLargestImageTest() {
		// Given: a template where the cropped image is the only image on the slide
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		ObjectNode circle = objectMapper.createObjectNode();
		circle.put("objectId", "circle_1");
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, true)).thenReturn(circle);
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, false)).thenReturn(circle);

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: nothing is deleted — deleting it would leave the slide with no image at all
		assertThat(requests).isEmpty();
	}

	@Test
	void shouldScaleAndPositionTheCircularImageToTheBrandGeometryTest() {
		// Given: a circular image at a size that is not the target
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		ObjectNode circle = objectMapper.createObjectNode();
		circle.put("objectId", "circle_1");
		ObjectNode size = circle.putObject("size");
		size.putObject("width").put("magnitude", 2056200.0);
		size.putObject("height").put("magnitude", 1028100.0);
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, true)).thenReturn(circle);
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, false)).thenReturn(circle);

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: the scale is derived from the image's own size, so a re-cropped template still
		// lands at the same place on the slide
		assertThat(requests).hasSize(1);
		JsonNode transform = requests.get(0).path("updatePageElementTransform");
		assertThat(transform.path("objectId").asText()).isEqualTo("circle_1");
		assertThat(transform.path("applyMode").asText()).isEqualTo("ABSOLUTE");
		assertThat(transform.path("transform").path("scaleX").asDouble())
				.isEqualTo(StandardDeckLayout.CIRCLE_TARGET_SIZE_EMU / 2056200.0);
		assertThat(transform.path("transform").path("scaleY").asDouble())
				.isEqualTo(StandardDeckLayout.CIRCLE_TARGET_SIZE_EMU / 1028100.0);
		assertThat(transform.path("transform").path("translateX").asDouble())
				.isEqualTo(StandardDeckLayout.CIRCLE_TARGET_TRANSLATE_X);
		assertThat(transform.path("transform").path("translateY").asDouble())
				.isEqualTo(StandardDeckLayout.CIRCLE_TARGET_TRANSLATE_Y);
		assertThat(transform.path("transform").path("unit").asText()).isEqualTo("EMU");
	}

	@Test
	void shouldSkipTheTransformWhenTheImageReportsNoSizeTest() {
		// Given: an image element whose size the API did not report
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		ObjectNode circle = objectMapper.createObjectNode();
		circle.put("objectId", "circle_1");
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, true)).thenReturn(circle);
		when(inspector.findTargetImageElement(presentation, CIRCLE_SLIDE, false)).thenReturn(circle);

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: no transform is emitted — dividing by a zero size would scale the image away
		assertThat(requests).isEmpty();
	}

	@Test
	void shouldDeleteTheSampleChartTitleAndSectionLabelTest() {
		// Given: a template that still shows the sample chart title and section label
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		when(inspector.findShapeIdByText(presentation, CHART_SLIDE,
				StandardTemplateTokens.CHART_TITLE_SAMPLE)).thenReturn("chart_title");
		when(inspector.findShapeIdByText(presentation, CIRCLE_SLIDE,
				StandardTemplateTokens.DRIVERS_SECTION_LABEL_SAMPLE)).thenReturn("section_label");

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: both sample strings are removed, so no placeholder copy ships in the deck
		assertThat(requests).hasSize(2);
		assertThat(requests.get(0).path("deleteObject").path("objectId").asText())
				.isEqualTo("chart_title");
		assertThat(requests.get(1).path("deleteObject").path("objectId").asText())
				.isEqualTo("section_label");
	}

	@Test
	void shouldBoldAndResizeTheTrendBulletsTest() {
		// Given: the trend bullets shape from the template
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		when(inspector.findShapeIdByText(presentation, CHART_SLIDE,
				StandardTemplateTokens.TREND_BULLETS_SAMPLE)).thenReturn("bullets");

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: only bold and font size are declared as changed, so the template's own font
		// family and colour are left intact
		assertThat(requests).hasSize(1);
		JsonNode style = requests.get(0).path("updateTextStyle");
		assertThat(style.path("objectId").asText()).isEqualTo("bullets");
		assertThat(style.path("textRange").path("type").asText()).isEqualTo("ALL");
		assertThat(style.path("style").path("bold").asBoolean()).isTrue();
		assertThat(style.path("style").path("fontSize").path("magnitude").asDouble())
				.isEqualTo(StandardDeckLayout.TREND_BULLETS_FONT_SIZE_PT);
		assertThat(style.path("style").path("fontSize").path("unit").asText()).isEqualTo("PT");
		assertThat(style.path("fields").asText()).isEqualTo("bold,fontSize");
	}

	@Test
	void shouldRecolourTheTrendingHeadingToBrandGreenTest() {
		// Given: the "what's trending" heading, matched by its own text
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		when(inspector.findShapeIdByText(presentation, CHART_SLIDE,
				StandardDeckLayout.TRENDING_HEADING_TEXT)).thenReturn("trending");

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: the brand green and the heading size are applied together
		assertThat(requests).hasSize(1);
		JsonNode style = requests.get(0).path("updateTextStyle");
		assertThat(style.path("objectId").asText()).isEqualTo("trending");
		JsonNode rgb = style.path("style").path("foregroundColor").path("opaqueColor")
				.path("rgbColor");
		assertThat(rgb.path("red").asDouble()).isEqualTo(StandardDeckLayout.BRAND_GREEN_RED);
		assertThat(rgb.path("green").asDouble()).isEqualTo(StandardDeckLayout.BRAND_GREEN_GREEN);
		assertThat(rgb.path("blue").asDouble()).isEqualTo(StandardDeckLayout.BRAND_GREEN_BLUE);
		assertThat(style.path("style").path("fontSize").path("magnitude").asDouble())
				.isEqualTo(StandardDeckLayout.TRENDING_HEADING_FONT_SIZE_PT);
		assertThat(style.path("fields").asText()).isEqualTo("foregroundColor,fontSize");
	}

	@Test
	void shouldRecolourTheTitleHeadingWithoutResizingItTest() {
		// Given: the cover slide's heading
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode presentation = objectMapper.createObjectNode();
		when(inspector.findShapeIdByText(presentation, 0, StandardDeckLayout.TITLE_HEADING_TEXT))
				.thenReturn("cover_heading");

		// When: the corrections are built
		ArrayNode requests = new StandardDeckTemplateAdjusterImpl(objectMapper, inspector)
				.buildAdjustmentRequests(presentation);

		// Then: only the colour is declared as changed, so the cover keeps its own type size
		assertThat(requests).hasSize(1);
		JsonNode style = requests.get(0).path("updateTextStyle");
		assertThat(style.path("objectId").asText()).isEqualTo("cover_heading");
		assertThat(style.path("fields").asText()).isEqualTo("foregroundColor");
		assertThat(style.path("style").path("fontSize").isMissingNode()).isTrue();
	}
}
