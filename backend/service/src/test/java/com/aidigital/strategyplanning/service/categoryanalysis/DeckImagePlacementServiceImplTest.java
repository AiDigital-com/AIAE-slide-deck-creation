package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.DeckImagePlacementServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardDeckLayout;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for placing generated artwork into a published Standard deck.
 *
 * <p>Two slides are handled differently on purpose. The "fit inside" slide cannot use Slides'
 * {@code replaceImage} because that crops to fill, so its placeholder is deleted and a new image
 * created instead. The scaled slide needs an explicit placement after replacement, because
 * {@code replaceImage} keeps the placeholder's own geometry.
 */
class DeckImagePlacementServiceImplTest {

	@Test
	void shouldNotCallGoogleWhenNoImagesWereGeneratedTest() {
		// Given: AI is not connected, so no artwork exists
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of());

		// When: image placement runs
		new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector, requestFactory,
				gateway).replaceSlideImages("token", "deck_1", objectMapper.createObjectNode(),
				"Pet Care", "https://app.example.com");

		// Then: the deck is left exactly as published
		verifyNoInteractions(gateway, inspector, requestFactory);
	}

	@Test
	void shouldReplaceTheCroppedImageInPlaceTest() {
		// Given: artwork for the circular-crop slide, whose placeholder exists
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectNode presentation = objectMapper.createObjectNode();
		ObjectNode placeholder = objectMapper.createObjectNode();
		placeholder.put("objectId", "img_3");
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of(StandardDeckLayout.CIRCULAR_IMAGE_SLIDE,
						"https://app.example.com/img/3.png"));
		when(inspector.findTargetImageElement(presentation,
				StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1, true)).thenReturn(placeholder);

		// When: image placement runs
		new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector, requestFactory,
				gateway).replaceSlideImages("token", "deck_1", presentation, "Pet Care",
				"https://app.example.com");

		// Then: a single replaceImage keeps the template's circular crop rather than creating a
		// new, uncropped image on top of it
		ArgumentCaptor<ArrayNode> requests = ArgumentCaptor.forClass(ArrayNode.class);
		verify(gateway).batchUpdate(org.mockito.ArgumentMatchers.eq("token"),
				org.mockito.ArgumentMatchers.eq("deck_1"), requests.capture());
		assertThat(requests.getValue()).hasSize(1);
		assertThat(requests.getValue().get(0).path("replaceImage").path("imageObjectId").asText())
				.isEqualTo("img_3");
		assertThat(requests.getValue().get(0).path("replaceImage").path("imageReplaceMethod")
				.asText()).isEqualTo("CENTER_CROP");
		assertThat(requests.getValue().get(0).path("replaceImage").path("url").asText())
				.isEqualTo("https://app.example.com/img/3.png");
	}

	@Test
	void shouldRepositionTheScaledSlideAfterReplacingItsImageTest() {
		// Given: artwork for the scaled slide, which needs explicit geometry afterwards
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectNode presentation = objectMapper.createObjectNode();
		ObjectNode placeholder = objectMapper.createObjectNode();
		placeholder.put("objectId", "img_4");
		ObjectNode placement = objectMapper.createObjectNode();
		placement.putObject("updatePageElementTransform").put("objectId", "img_4");
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of(StandardDeckLayout.SCALED_IMAGE_SLIDE,
						"https://app.example.com/img/4.png"));
		when(inspector.findTargetImageElement(presentation,
				StandardDeckLayout.SCALED_IMAGE_SLIDE - 1, false)).thenReturn(placeholder);
		when(requestFactory.buildAbsolutePlacementRequest(placeholder,
				StandardDeckLayout.SCALED_TARGET_SIZE_EMU,
				StandardDeckLayout.SCALED_TARGET_TRANSLATE_X,
				StandardDeckLayout.SCALED_TARGET_TRANSLATE_Y)).thenReturn(placement);

		// When: image placement runs
		new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector, requestFactory,
				gateway).replaceSlideImages("token", "deck_1", presentation, "Pet Care",
				"https://app.example.com");

		// Then: the replacement is followed by the placement, in that order — the other way
		// round would be overwritten by replaceImage
		ArgumentCaptor<ArrayNode> requests = ArgumentCaptor.forClass(ArrayNode.class);
		verify(gateway).batchUpdate(org.mockito.ArgumentMatchers.eq("token"),
				org.mockito.ArgumentMatchers.eq("deck_1"), requests.capture());
		assertThat(requests.getValue()).hasSize(2);
		assertThat(requests.getValue().get(0).has("replaceImage")).isTrue();
		assertThat(requests.getValue().get(1).has("updatePageElementTransform")).isTrue();
	}

	@Test
	void shouldDeleteAndRecreateTheImageOnTheFitInsideSlideTest() {
		// Given: artwork for the slide whose image must fit inside rather than be cropped
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectNode presentation = objectMapper.createObjectNode();
		ArrayNode slides = presentation.putArray("slides");
		for (int i = 0; i < StandardDeckLayout.FIT_INSIDE_SLIDE; i++) {
			slides.addObject().put("objectId", "slide_" + (i + 1));
		}
		ObjectNode placeholder = objectMapper.createObjectNode();
		placeholder.put("objectId", "img_2");
		ObjectNode createImage = objectMapper.createObjectNode();
		createImage.putObject("createImage").put("url", "https://app.example.com/img/2.png");
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of(StandardDeckLayout.FIT_INSIDE_SLIDE,
						"https://app.example.com/img/2.png"));
		when(inspector.findTargetImageElement(presentation,
				StandardDeckLayout.FIT_INSIDE_SLIDE - 1, false)).thenReturn(placeholder);
		when(requestFactory.buildCreateImageRequest(
				"slide_" + StandardDeckLayout.FIT_INSIDE_SLIDE,
				"https://app.example.com/img/2.png")).thenReturn(createImage);

		// When: image placement runs
		new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector, requestFactory,
				gateway).replaceSlideImages("token", "deck_1", presentation, "Pet Care",
				"https://app.example.com");

		// Then: the placeholder is deleted and a fresh image created, because replaceImage
		// would crop the chart artwork
		ArgumentCaptor<ArrayNode> requests = ArgumentCaptor.forClass(ArrayNode.class);
		verify(gateway).batchUpdate(org.mockito.ArgumentMatchers.eq("token"),
				org.mockito.ArgumentMatchers.eq("deck_1"), requests.capture());
		assertThat(requests.getValue()).hasSize(2);
		assertThat(requests.getValue().get(0).path("deleteObject").path("objectId").asText())
				.isEqualTo("img_2");
		assertThat(requests.getValue().get(1).has("createImage")).isTrue();
	}

	@Test
	void shouldSendNothingWhenTheTemplateHasNoPlaceholderForTheGeneratedImageTest() {
		// Given: artwork for a slide whose placeholder the template no longer has
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectNode presentation = objectMapper.createObjectNode();
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of(StandardDeckLayout.CIRCULAR_IMAGE_SLIDE,
						"https://app.example.com/img/3.png"));
		when(inspector.findTargetImageElement(presentation,
				StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1, true)).thenReturn(null);

		// When: image placement runs
		new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector, requestFactory,
				gateway).replaceSlideImages("token", "deck_1", presentation, "Pet Care",
				"https://app.example.com");

		// Then: no empty batchUpdate is sent
		verifyNoInteractions(gateway);
	}

	@Test
	void shouldLeaveThePublishedDeckAloneWhenTheImageUpdateFailsTest() {
		// Given: a batchUpdate Google rejects, for example an image URL it cannot fetch
		ObjectMapper objectMapper = new ObjectMapper();
		SlideImageService imageService = mock(SlideImageService.class);
		SlidesPresentationInspector inspector = mock(SlidesPresentationInspector.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectNode presentation = objectMapper.createObjectNode();
		ObjectNode placeholder = objectMapper.createObjectNode();
		placeholder.put("objectId", "img_3");
		when(imageService.generateSlideImageUrls("Pet Care", "https://app.example.com"))
				.thenReturn(Map.of(StandardDeckLayout.CIRCULAR_IMAGE_SLIDE,
						"https://app.example.com/img/3.png"));
		when(inspector.findTargetImageElement(presentation,
				StandardDeckLayout.CIRCULAR_IMAGE_SLIDE - 1, true)).thenReturn(placeholder);
		ArrayNode expected = objectMapper.createArrayNode();
		ObjectNode inner = expected.addObject().putObject("replaceImage");
		inner.put("imageObjectId", "img_3");
		inner.put("imageReplaceMethod", "CENTER_CROP");
		inner.put("url", "https://app.example.com/img/3.png");
		org.mockito.Mockito.doThrow(new IllegalStateException("bad image URL"))
				.when(gateway).batchUpdate("token", "deck_1", expected);

		// When-Then: the user still gets their deck, with the template's stock artwork
		assertThatCode(() -> new DeckImagePlacementServiceImpl(objectMapper, imageService,
				inspector, requestFactory, gateway).replaceSlideImages("token", "deck_1",
				presentation, "Pet Care", "https://app.example.com"))
				.doesNotThrowAnyException();
	}
}
