package com.aidigital.strategyplanning.service.common.files.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageService;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * Default implementation of {@link SourceImageService}. Recognises images by file extension via
 * {@link SourceImageType}, enforces a per-image size ceiling that stays inside the AI API's own
 * limit, and base64-encodes images as data URLs.
 */
@Service
public class SourceImageServiceImpl implements SourceImageService {

	/**
	 * Largest image accepted per file, in bytes, well inside the AI API's own limit.
	 */
	public static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;

	private static final int BYTES_PER_MEGABYTE = 1024 * 1024;
	private static final String DATA_URL_PREFIX = "data:";
	private static final String DATA_URL_BASE64_MARKER = ";base64,";

	@Override
	public Optional<SourceImageType> imageType(String fileName) {
		String extension = fileExtension(fileName);
		for (SourceImageType type : SourceImageType.values()) {
			if (type.getExtensions().contains(extension)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

	@Override
	public SourceImage toSourceImage(String fileName, byte[] bytes) {
		SourceImageType type = imageType(fileName).orElseThrow(() -> new AppException(ErrorReason.C002,
				"Unsupported image type for " + fileName + " — supported: PNG, JPG, JPEG, WEBP"));
		if (bytes == null || bytes.length == 0) {
			throw new AppException(ErrorReason.C002, "Uploaded file is empty: " + fileName);
		}
		if (bytes.length > MAX_IMAGE_BYTES) {
			throw new AppException(ErrorReason.C002,
					fileName + " is larger than " + (MAX_IMAGE_BYTES / BYTES_PER_MEGABYTE)
							+ " MB — upload a smaller screenshot");
		}
		return new SourceImage(fileName, type, bytes);
	}

	@Override
	public String toDataUrl(SourceImage image) {
		return DATA_URL_PREFIX + image.type().getMimeType() + DATA_URL_BASE64_MARKER
				+ Base64.getEncoder().encodeToString(image.bytes());
	}

	/**
	 * Returns the lower-case file extension of a filename, or an empty string when absent.
	 *
	 * @param fileName original filename, may be null
	 * @return lower-case extension without the dot
	 */
	String fileExtension(String fileName) {
		if (fileName == null) {
			return "";
		}
		int dot = fileName.lastIndexOf('.');
		return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
