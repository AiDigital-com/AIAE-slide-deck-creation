package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.GeneratedSlideImage;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Short-lived, in-memory holder for generated slide images. The current OpenAI image model returns
 * raw image bytes (base64), but Google Slides {@code replaceImage} needs a publicly fetchable URL —
 * so each image is stored here under a random token and served from a public endpoint for the few
 * seconds Google needs to fetch it. Entries expire quickly and the map is capacity-bounded so
 * nothing accumulates.
 */
@Component
public class SlideImageStore {

	/**
	 * How long a stored image stays fetchable before it is evicted.
	 */
	private static final long TTL_MILLIS = Duration.ofMinutes(15).toMillis();

	/**
	 * Hard cap on retained images so a burst of decks cannot grow memory without bound.
	 */
	private static final int MAX_ENTRIES = 80;

	private final ConcurrentMap<String, GeneratedSlideImage> images = new ConcurrentHashMap<>();

	/**
	 * Stores image bytes under a fresh random token, first evicting anything expired and, if still
	 * at capacity, the oldest entry. Best-effort housekeeping keeps the store small.
	 *
	 * @param bytes       raw image bytes to serve
	 * @param contentType MIME type to serve the bytes with (e.g. "image/png")
	 * @return the token used to build the public fetch URL
	 */
	public String put(byte[] bytes, String contentType) {
		long now = System.currentTimeMillis();
		images.values().removeIf(image -> isExpired(image, now));
		if (images.size() >= MAX_ENTRIES) {
			images.entrySet().stream()
					.min((a, b) -> Long.compare(a.getValue().createdAtMillis(), b.getValue().createdAtMillis()))
					.ifPresent(oldest -> images.remove(oldest.getKey()));
		}
		String token = UUID.randomUUID().toString().replace("-", "");
		images.put(token, new GeneratedSlideImage(bytes, contentType, now));
		return token;
	}

	/**
	 * Returns the stored image for a token when it exists and has not expired, removing it if it
	 * has expired.
	 *
	 * @param token token previously returned by {@link #put(byte[], String)}
	 * @return the stored image, or empty when unknown or expired
	 */
	public Optional<GeneratedSlideImage> get(String token) {
		GeneratedSlideImage image = images.get(token);
		if (image == null) {
			return Optional.empty();
		}
		if (isExpired(image, System.currentTimeMillis())) {
			images.remove(token);
			return Optional.empty();
		}
		return Optional.of(image);
	}

	/**
	 * Reports whether a stored image is older than the time-to-live.
	 *
	 * @param image stored image to check
	 * @param now   current epoch millis
	 * @return true when the image has outlived its time-to-live
	 */
	boolean isExpired(GeneratedSlideImage image, long now) {
		return now - image.createdAtMillis() > TTL_MILLIS;
	}
}
