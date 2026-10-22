package com.aidigital.strategyplanning.service.common.files;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Image formats accepted as drafting sources, each with the MIME type the AI drafting engine
 * expects in an inline data URL and the filename extensions that identify it.
 */
@Getter
@RequiredArgsConstructor
public enum SourceImageType {

	/** Portable Network Graphics. */
	PNG("image/png", List.of("png")),

	/** JPEG photographs and screenshots, with either common extension. */
	JPEG("image/jpeg", List.of("jpg", "jpeg")),

	/** WebP images. */
	WEBP("image/webp", List.of("webp"));

	/** MIME type used when the image is embedded in an AI request. */
	private final String mimeType;

	/** Lower-case filename extensions, without the dot, that identify this type. */
	private final List<String> extensions;
}
