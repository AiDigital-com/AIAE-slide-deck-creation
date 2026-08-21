package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * A generated slide image held briefly in memory so it can be served from a short-lived public
 * URL. Google Slides fetches that URL during {@code replaceImage}, so the raw bytes only need to
 * outlive a single deck creation.
 *
 * @param bytes           raw image bytes (PNG)
 * @param contentType     MIME type to serve the bytes with (e.g. "image/png")
 * @param createdAtMillis epoch millis when the image was stored, used for time-to-live eviction
 */
public record GeneratedSlideImage(byte[] bytes, String contentType, long createdAtMillis) {

}
