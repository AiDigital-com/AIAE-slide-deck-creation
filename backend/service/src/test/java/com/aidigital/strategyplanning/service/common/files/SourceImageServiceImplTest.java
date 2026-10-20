package com.aidigital.strategyplanning.service.common.files;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SourceImageServiceImpl}.
 *
 * <p>Covers image detection by extension (case-insensitive, both JPEG spellings), the guards an
 * uploaded screenshot passes before it is sent to the AI engine, and the data URL encoding.
 */
class SourceImageServiceImplTest {

	@Test
	void shouldDetectEverySupportedImageExtensionCaseInsensitivelyTest() {
		// Given: the image service
		SourceImageServiceImpl service = new SourceImageServiceImpl();

		// When-Then: each supported extension maps to its type regardless of case
		assertThat(service.imageType("dashboard.png")).contains(SourceImageType.PNG);
		assertThat(service.imageType("photo.JPG")).contains(SourceImageType.JPEG);
		assertThat(service.imageType("photo.jpeg")).contains(SourceImageType.JPEG);
		assertThat(service.imageType("chart.WebP")).contains(SourceImageType.WEBP);
	}

	@Test
	void shouldNotTreatDocumentsOrUnnamedUploadsAsImagesTest() {
		// Given: the image service
		SourceImageServiceImpl service = new SourceImageServiceImpl();

		// When-Then: documents, extension-less names and a missing name are not images
		assertThat(service.imageType("brief.pdf")).isEqualTo(Optional.empty());
		assertThat(service.imageType("notes.md")).isEqualTo(Optional.empty());
		assertThat(service.imageType("README")).isEqualTo(Optional.empty());
		assertThat(service.imageType(null)).isEqualTo(Optional.empty());
	}

	@Test
	void shouldWrapAValidImageWithItsDetectedTypeTest() {
		// Given: a small PNG upload
		SourceImageServiceImpl service = new SourceImageServiceImpl();
		byte[] bytes = {1, 2, 3};

		// When: it is wrapped as a source image
		SourceImage image = service.toSourceImage("dashboard.png", bytes);

		// Then: filename, type and bytes are carried through untouched
		assertThat(image.fileName()).isEqualTo("dashboard.png");
		assertThat(image.type()).isEqualTo(SourceImageType.PNG);
		assertThat(image.bytes()).isSameAs(bytes);
	}

	@Test
	void shouldRejectAnImageOverTheSizeLimitTest() {
		// Given: an upload one byte past the per-image ceiling
		SourceImageServiceImpl service = new SourceImageServiceImpl();
		byte[] bytes = new byte[SourceImageServiceImpl.MAX_IMAGE_BYTES + 1];

		// When-Then: the upload is rejected as a C002 client error naming the limit
		assertThatThrownBy(() -> service.toSourceImage("huge.png", bytes))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("huge.png is larger than 10 MB");
	}

	@Test
	void shouldRejectAnEmptyImageTest() {
		// Given: an image upload with no bytes
		SourceImageServiceImpl service = new SourceImageServiceImpl();

		// When-Then: the upload is rejected as a C002 client error
		assertThatThrownBy(() -> service.toSourceImage("blank.webp", new byte[0]))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("Uploaded file is empty: blank.webp");
	}

	@Test
	void shouldRejectANonImageFilenameTest() {
		// Given: a document filename
		SourceImageServiceImpl service = new SourceImageServiceImpl();

		// When-Then: wrapping it as an image is refused with the supported formats listed
		assertThatThrownBy(() -> service.toSourceImage("brief.pdf", new byte[]{1}))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("Unsupported image type for brief.pdf")
				.hasMessageContaining("PNG, JPG, JPEG, WEBP");
	}

	@Test
	void shouldEncodeAnImageAsAnInlineDataUrlTest() {
		// Given: a JPEG image with known bytes
		SourceImageServiceImpl service = new SourceImageServiceImpl();
		byte[] bytes = "jpeg-bytes".getBytes(StandardCharsets.UTF_8);
		SourceImage image = new SourceImage("photo.jpg", SourceImageType.JPEG, bytes);

		// When: it is encoded
		String dataUrl = service.toDataUrl(image);

		// Then: the data URL carries the MIME type and the base64 body
		assertThat(dataUrl).isEqualTo("data:image/jpeg;base64,"
				+ Base64.getEncoder().encodeToString(bytes));
	}
}
