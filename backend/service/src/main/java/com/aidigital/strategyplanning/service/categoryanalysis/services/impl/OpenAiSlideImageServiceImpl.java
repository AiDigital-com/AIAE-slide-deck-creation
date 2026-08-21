package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.OpenAiImageClient;
import com.aidigital.strategyplanning.external.openai.model.OpenAiImageCall;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageCutoutInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImagePromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI Images implementation of {@link SlideImageService}. Builds a deterministic, theme-aware
 * prompt for each slide and asks the OpenAI Images API ({@code gpt-image-1}) for a photograph. That
 * model returns the image as base64 bytes rather than a URL, so the decoded bytes are held briefly
 * in the {@link SlideImageStore} and returned as a public URL under this backend's own host — Google
 * Slides fetches that URL during the {@code replaceImage} batch update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiSlideImageServiceImpl implements SlideImageService {

	/**
	 * Slides that carry a themed image placeholder, in deck order.
	 */
	public static final List<Integer> IMAGE_SLIDES = List.of(1, 2, 3, 4);




	/**
	 * Public path prefix under which stored images are served for Google to fetch.
	 */
	public static final String PUBLIC_IMAGE_PATH = "/public/slide-images/";

	/**
	 * Maximum generation attempts per cutout image when the subject touches the top edge.
	 */
	public static final int MAX_CUTOUT_ATTEMPTS = 3;

	private static final Duration IMAGE_TIMEOUT = Duration.ofSeconds(90);




	private final CategoryAnalysisProperties properties;
	private final SlideImageStore slideImageStore;
	private final OpenAiImageClient openAiImageClient;
	private final SlideImagePromptComposer promptComposer;
	private final SlideImageCutoutInspector cutoutInspector;
	private final ExecutorService imageExecutor = Executors.newFixedThreadPool(IMAGE_SLIDES.size());

	@Override
	public boolean isConnected() {
		return StringUtils.hasText(properties.getOpenaiApiKey());
	}

	@Override
	public Map<Integer, String> generateSlideImageUrls(String category, String publicBaseUrl) {
		Map<Integer, String> urls = new LinkedHashMap<>();
		if (!isConnected() || !StringUtils.hasText(category) || !StringUtils.hasText(publicBaseUrl)) {
			return urls;
		}
		Map<Integer, CompletableFuture<String>> futures = new LinkedHashMap<>();
		for (Integer slide : IMAGE_SLIDES) {
			String prompt = promptComposer.buildPrompt(category, slide);
			String background = promptComposer.backgroundForSlide(slide);
			futures.put(slide, CompletableFuture.supplyAsync(
					() -> generateImageUrl(prompt, background, publicBaseUrl), imageExecutor));
		}
		for (Map.Entry<Integer, CompletableFuture<String>> entry : futures.entrySet()) {
			try {
				String url = entry.getValue().join();
				if (StringUtils.hasText(url)) {
					urls.put(entry.getKey(), url);
				}
			} catch (RuntimeException e) {
				log.warn("Slide image generation failed for slide {}: {}", entry.getKey(), e.getMessage());
			}
		}
		return urls;
	}

	/**
	 * Calls the OpenAI Images API for one prompt, stores the returned base64 image bytes, and
	 * returns a public URL from which Google Slides can fetch them. Cutout images are inspected
	 * and regenerated up to {@link #MAX_CUTOUT_ATTEMPTS} times; when no attempt passes inspection
	 * the last generated image is used anyway so the slide never keeps the template's stale
	 * placeholder. This is otherwise best-effort: any hard failure (not connected, non-200
	 * response, missing image bytes, network error, or interruption) returns null so the caller
	 * can simply skip that slide's image.
	 *
	 * @param prompt        image generation prompt
	 * @param background    background style for the image, blank to omit the parameter
	 * @param publicBaseUrl externally reachable base URL of this backend (scheme + host)
	 * @return a fetchable image URL, or null when generation failed
	 */
	public String generateImageUrl(String prompt, String background, String publicBaseUrl) {
		if (!isConnected()) {
			return null;
		}
		boolean cutout = "transparent".equals(background);
		int attempts = cutout ? MAX_CUTOUT_ATTEMPTS : 1;
		byte[] bytes = null;
		byte[] lastCandidate = null;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			byte[] candidate = requestImageBytes(prompt, background);
			if (candidate == null) {
				break;
			}
			lastCandidate = candidate;
			if (!cutout || (cutoutInspector.hasClearTopMargin(candidate)
					&& cutoutInspector.hasTransparentBackground(candidate))) {
				bytes = candidate;
				break;
			}
			log.warn("Generated cutout failed inspection — touches the top edge or has an opaque "
					+ "background (attempt {}/{}) — regenerating", attempt, attempts);
		}
		if (bytes == null && lastCandidate != null) {
			log.warn("No cutout passed inspection after {} attempt(s) — using the last generated "
					+ "image anyway so the slide still gets a category image", attempts);
			bytes = lastCandidate;
		}
		if (bytes == null) {
			log.warn("No image produced after {} attempt(s) — keeping the template image", attempts);
			return null;
		}
		String token = slideImageStore.put(bytes, "image/png");
		return stripTrailingSlash(publicBaseUrl) + PUBLIC_IMAGE_PATH + token;
	}

	/**
	 * Calls the OpenAI Images API once and returns the decoded PNG bytes. Best-effort: any failure
	 * (non-200 response, missing image bytes, network error, or interruption) returns null.
	 *
	 * @param prompt     image generation prompt
	 * @param background background style for the image, blank to omit the parameter
	 * @return decoded image bytes, or null when generation failed
	 */
	public byte[] requestImageBytes(String prompt, String background) {
		try {
			return openAiImageClient.generate(new OpenAiImageCall(
					properties.getOpenaiBaseUrl(),
					properties.getOpenaiApiKey(),
					promptComposer.buildImageRequestBody(prompt, background),
					IMAGE_TIMEOUT));
		} catch (OpenAiExternalException e) {
			logImageFailure(e);
			return null;
		}
	}

	/**
	 * Logs a provider failure with the wording this feature has always used.
	 *
	 * @param failure provider failure raised by the OpenAI image client
	 */
	void logImageFailure(OpenAiExternalException failure) {
		switch (failure.getFailure()) {
			case HTTP_STATUS -> log.warn("Slide image generation failed with HTTP {}: {}",
					failure.getStatusCode(), failure.getResponseBody());
			case EMPTY_CONTENT -> log.warn("Slide image generation returned no image bytes");
			case INTERRUPTED -> Thread.currentThread().interrupt();
			case TRANSPORT -> log.warn("Slide image generation failed: {}",
					failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage());
			default -> log.warn("Slide image generation failed: {}", failure.getMessage());
		}
	}
	/**
	 * Removes a single trailing slash from a base URL so it can be concatenated with an
	 * absolute path without producing a double slash.
	 *
	 * @param baseUrl backend base URL
	 * @return the base URL without a trailing slash
	 */
	String stripTrailingSlash(String baseUrl) {
		return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}
}
