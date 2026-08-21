package com.aidigital.strategyplanning.web;

import com.aidigital.strategyplanning.service.categoryanalysis.models.GeneratedSlideImage;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Serves generated slide images by token from the in-memory {@link SlideImageStore}.
 *
 * <p>The path is public and unauthenticated because Google Slides fetches the image itself during
 * the {@code replaceImage} batch update. The image model returns base64 bytes rather than a URL,
 * so the backend hosts the decoded bytes for a few minutes instead of handing Google a provider
 * URL. Tokens are single-purpose and expire with the store entry.
 */
@RestController
@RequiredArgsConstructor
public class SlideImagePublicController {

	private final SlideImageStore slideImageStore;

	/**
	 * Returns the stored image for a token, or 404 when the token is unknown or expired.
	 *
	 * @param token token issued when the image was generated
	 * @return the image with its content type, or a 404 response
	 */
	@GetMapping("/public/slide-images/{token}")
	public ResponseEntity<Resource> slideImage(@PathVariable String token) {
		return slideImageStore.get(token)
				.map(this::toResponse)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	/**
	 * Wraps a stored image in a cacheable binary HTTP response.
	 *
	 * @param image stored image to serve
	 * @return response entity streaming the image with its content type and length
	 */
	ResponseEntity<Resource> toResponse(GeneratedSlideImage image) {
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(image.contentType()))
				.contentLength(image.bytes().length)
				.cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
				.body(new ByteArrayResource(image.bytes()));
	}
}
