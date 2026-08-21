package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import com.aidigital.strategyplanning.external.openai.OpenAiImageClient;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.OpenAiSlideImageServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlideImageCutoutInspectorImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlideImagePromptComposerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenAiSlideImageServiceImpl} prompt and request-body building. These cover
 * the deterministic logic only — no network calls are made.
 */
class OpenAiSlideImageServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private CategoryAnalysisProperties properties;
	private OpenAiSlideImageServiceImpl service;
	private SlideImagePromptComposerImpl composer;
	private SlideImageCutoutInspectorImpl inspector;

	@BeforeEach
	void setUp() {
		properties = new CategoryAnalysisProperties();
		composer = new SlideImagePromptComposerImpl(properties, objectMapper);
		inspector = new SlideImageCutoutInspectorImpl();
		service = new OpenAiSlideImageServiceImpl(properties, new SlideImageStore(),
				mock(OpenAiImageClient.class), composer, inspector);
	}

	@Test
	void isConnectedReflectsOpenAiKeyPresence() {
		assertThat(service.isConnected()).isFalse();
		properties.setOpenaiApiKey("sk-test");
		assertThat(service.isConnected()).isTrue();
	}

	@Test
	void generateSlideImageUrlsReturnsEmptyWhenNotConnected() {
		assertThat(service.generateSlideImageUrls("Tractors", "https://example.com")).isEmpty();
	}

	@Test
	void generateSlideImageUrlsReturnsEmptyWhenBaseUrlMissing() {
		properties.setOpenaiApiKey("sk-test");
		assertThat(service.generateSlideImageUrls("Tractors", "")).isEmpty();
	}

	@Test
	void buildPromptIncludesCategoryThemeGuidanceAndExclusions() {
		String prompt = composer.buildPrompt("Beverages", 2);
		assertThat(prompt).contains("Beverages");
		assertThat(prompt).contains("friendly and energetic");
		assertThat(prompt).contains("happy");
		assertThat(prompt).contains("photorealistic");
		assertThat(prompt).contains("transparent background");
		assertThat(prompt).contains("empty space above the head");
		assertThat(prompt).contains("No text");
		assertThat(prompt).contains("no charts or graphs");
	}

	@Test
	void titleSlideGetsFullBleedOpaquePhotoForRoundedSquareFrame() {
		String prompt = composer.buildPrompt("Beverages", 1);
		assertThat(prompt).contains("fills the frame edge-to-edge");
		assertThat(prompt).contains("rounded corners");
		assertThat(prompt).doesNotContain("transparent background");
		assertThat(prompt).doesNotContain("cutout");
		assertThat(composer.backgroundForSlide(1)).isEqualTo("opaque");
	}

	@Test
	void buildPromptVariesFramingPerSlide() {
		assertThat(composer.buildPrompt("Beverages", 1))
				.isNotEqualTo(composer.buildPrompt("Beverages", 2));
		assertThat(composer.buildPrompt("Beverages", 3))
				.contains("circular center crop");
	}

	@Test
	void circularCropSlideGetsFullBleedOpaquePhotoWhileOthersStayCutouts() {
		String circlePrompt = composer.buildPrompt("Beverages", 3);
		assertThat(circlePrompt).contains("fills the frame edge-to-edge");
		assertThat(circlePrompt).doesNotContain("transparent background");
		assertThat(circlePrompt).doesNotContain("cutout");
		assertThat(composer.backgroundForSlide(3)).isEqualTo("opaque");
		assertThat(composer.backgroundForSlide(2)).isEqualTo("transparent");
		assertThat(composer.backgroundForSlide(4)).isEqualTo("transparent");
	}

	@Test
	void buildImageRequestBodyUsesConfiguredModelSizeQualityAndBackgroundWithoutResponseFormat() throws Exception {
		properties.setOpenaiImageModel("gpt-image-1");
		properties.setOpenaiImageSize("1024x1024");
		properties.setOpenaiImageQuality("medium");
		JsonNode body = objectMapper.readTree(composer.buildImageRequestBody("a friendly photo", "transparent"));
		assertThat(body.path("model").asText()).isEqualTo("gpt-image-1");
		assertThat(body.path("size").asText()).isEqualTo("1024x1024");
		assertThat(body.path("quality").asText()).isEqualTo("medium");
		assertThat(body.path("background").asText()).isEqualTo("transparent");
		assertThat(body.path("prompt").asText()).isEqualTo("a friendly photo");
		assertThat(body.path("n").asInt()).isEqualTo(1);
		assertThat(body.has("response_format")).isFalse();
	}

	@Test
	void buildImageRequestBodyOmitsBackgroundWhenBlank() throws Exception {
		JsonNode body = objectMapper.readTree(composer.buildImageRequestBody("a friendly photo", ""));
		assertThat(body.has("background")).isFalse();
	}

	@Test
	void hasClearTopMarginRejectsSubjectTouchingTopEdge() throws Exception {
		assertThat(inspector.hasClearTopMargin(cutoutPng(true))).isFalse();
		assertThat(inspector.hasClearTopMargin(cutoutPng(false))).isTrue();
	}

	@Test
	void hasClearTopMarginAcceptsUndecodableBytes() {
		assertThat(inspector.hasClearTopMargin(new byte[]{1, 2, 3})).isTrue();
	}

	@Test
	void hasTransparentBackgroundRejectsFullyOpaqueImage() throws Exception {
		assertThat(inspector.hasTransparentBackground(opaquePng())).isFalse();
		assertThat(inspector.hasTransparentBackground(cutoutPng(false))).isTrue();
	}

	@Test
	void hasTransparentBackgroundAcceptsUndecodableBytes() {
		assertThat(inspector.hasTransparentBackground(new byte[]{1, 2, 3})).isTrue();
	}

	@Test
	void generateImageUrlFallsBackToLastCandidateWhenAllCutoutAttemptsFailInspection() throws Exception {
		properties.setOpenaiApiKey("sk-test");
		byte[] opaque = opaquePng();
		java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		OpenAiSlideImageServiceImpl stubbed =
				new OpenAiSlideImageServiceImpl(properties, new SlideImageStore(),
						mock(OpenAiImageClient.class), composer, inspector) {
					@Override
					public byte[] requestImageBytes(String prompt, String background) {
						calls.incrementAndGet();
						return opaque;
					}
				};
		assertThat(stubbed.generateImageUrl("a cutout", "transparent", "https://example.com"))
				.startsWith("https://example.com");
		assertThat(calls.get()).isEqualTo(OpenAiSlideImageServiceImpl.MAX_CUTOUT_ATTEMPTS);
	}

	@Test
	void generateImageUrlReturnsNullWhenNoImageBytesProduced() throws Exception {
		properties.setOpenaiApiKey("sk-test");
		OpenAiSlideImageServiceImpl stubbed =
				new OpenAiSlideImageServiceImpl(properties, new SlideImageStore(),
						mock(OpenAiImageClient.class), composer, inspector) {
					@Override
					public byte[] requestImageBytes(String prompt, String background) {
						return null;
					}
				};
		assertThat(stubbed.generateImageUrl("a cutout", "transparent", "https://example.com")).isNull();
	}

	@Test
	void generateImageUrlAcceptsPassingCutoutOnFirstAttempt() throws Exception {
		properties.setOpenaiApiKey("sk-test");
		byte[] valid = cutoutPng(false);
		OpenAiSlideImageServiceImpl stubbed =
				new OpenAiSlideImageServiceImpl(properties, new SlideImageStore(),
						mock(OpenAiImageClient.class), composer, inspector) {
					@Override
					public byte[] requestImageBytes(String prompt, String background) {
						return valid;
					}
				};
		assertThat(stubbed.generateImageUrl("a cutout", "transparent", "https://example.com"))
				.startsWith("https://example.com");
	}

	/**
	 * Builds a 100x100 PNG where every pixel is opaque white, mimicking a cutout request the
	 * model answered with a solid background.
	 *
	 * @return encoded PNG bytes
	 */
	private byte[] opaquePng() throws Exception {
		java.awt.image.BufferedImage image =
				new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 100; y++) {
			for (int x = 0; x < 100; x++) {
				image.setRGB(x, y, 0xFFFFFFFF);
			}
		}
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	/**
	 * Builds a 100x100 transparent PNG with an opaque block; when {@code touchTop} is set the
	 * block starts at row 0 (subject touching the top edge), otherwise it starts at row 20.
	 *
	 * @param touchTop whether the opaque block touches the top edge
	 * @return encoded PNG bytes
	 */
	private byte[] cutoutPng(boolean touchTop) throws Exception {
		java.awt.image.BufferedImage image =
				new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		int startY = touchTop ? 0 : 20;
		for (int y = startY; y < 80; y++) {
			for (int x = 30; x < 70; x++) {
				image.setRGB(x, y, 0xFF336699);
			}
		}
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(image, "png", out);
		return out.toByteArray();
	}
}
