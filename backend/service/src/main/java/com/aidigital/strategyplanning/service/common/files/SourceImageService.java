package com.aidigital.strategyplanning.service.common.files;

import java.util.Optional;

/**
 * Service contract for image uploads used as drafting sources: recognising an upload as an image
 * by its filename, validating it, and encoding it for an AI request. Shared across features that
 * accept screenshot uploads alongside text documents.
 */
public interface SourceImageService {

	/**
	 * Detects whether a filename denotes a supported image.
	 *
	 * @param fileName original filename, may be null
	 * @return the image type when the extension is a supported image format, otherwise empty
	 */
	Optional<SourceImageType> imageType(String fileName);

	/**
	 * Validates an uploaded image and wraps it as a drafting source.
	 *
	 * @param fileName original filename of the upload
	 * @param bytes    raw image bytes
	 * @return the image source
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C002 reason when the file is not
	 * a supported image, is empty, or exceeds the size limit
	 */
	SourceImage toSourceImage(String fileName, byte[] bytes);

	/**
	 * Encodes an image as an inline {@code data:} URL suitable for an AI chat request.
	 *
	 * @param image image source
	 * @return data URL carrying the image's MIME type and base64 contents
	 */
	String toDataUrl(SourceImage image);
}
