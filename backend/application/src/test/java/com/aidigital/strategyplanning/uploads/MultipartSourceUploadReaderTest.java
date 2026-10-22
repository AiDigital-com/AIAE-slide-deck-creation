package com.aidigital.strategyplanning.uploads;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.aidigital.strategyplanning.service.common.files.SourceUploads;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MultipartSourceUploadReader}.
 *
 * <p>Covers the split of a mixed batch into documents and images, that images bypass text
 * extraction, and that the batch and image-size rules surface as C002 client errors.
 */
class MultipartSourceUploadReaderTest {

	@Test
	void shouldSplitABatchIntoDocumentsAndImagesInUploadOrderTest() {
		// Given: a document, a screenshot, and a Markdown note
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceUploadReader reader = new MultipartSourceUploadReader(
				new MultipartSourceDocumentReader(textService), new SourceImageServiceImpl());
		MultipartFile brief = new MockMultipartFile("files", "brief.pdf", null, "raw-1".getBytes());
		MultipartFile screenshot = new MockMultipartFile("files", "dashboard.PNG", null, new byte[]{9, 9});
		MultipartFile notes = new MockMultipartFile("files", "notes.md", null, "raw-2".getBytes());
		when(textService.extractText("brief.pdf", "raw-1".getBytes())).thenReturn("Brief text");
		when(textService.extractText("notes.md", "raw-2".getBytes())).thenReturn("# Notes");

		// When: the batch is read
		SourceUploads uploads = reader.read(List.of(brief, screenshot, notes), 5);

		// Then: documents are extracted in order and the screenshot is passed through as an image
		assertThat(uploads.documents()).containsExactly(
				new SourceDocument("brief.pdf", "Brief text"),
				new SourceDocument("notes.md", "# Notes"));
		assertThat(uploads.images()).hasSize(1);
		SourceImage image = uploads.images().get(0);
		assertThat(image.fileName()).isEqualTo("dashboard.PNG");
		assertThat(image.type()).isEqualTo(SourceImageType.PNG);
		assertThat(image.bytes()).containsExactly(9, 9);
		verify(textService).extractText("brief.pdf", "raw-1".getBytes());
		verify(textService).extractText("notes.md", "raw-2".getBytes());
	}

	@Test
	void shouldNeverSendAnImageThroughTextExtractionTest() {
		// Given: a batch of screenshots only
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceUploadReader reader = new MultipartSourceUploadReader(
				new MultipartSourceDocumentReader(textService), new SourceImageServiceImpl());
		List<MultipartFile> files = List.of(
				new MockMultipartFile("files", "a.jpg", null, new byte[]{1}),
				new MockMultipartFile("files", "b.webp", null, new byte[]{2}));

		// When: the batch is read
		SourceUploads uploads = reader.read(files, 5);

		// Then: both are images, no document is produced, and the text service is never consulted
		assertThat(uploads.documents()).isEmpty();
		assertThat(uploads.images()).extracting(SourceImage::fileName).containsExactly("a.jpg", "b.webp");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldRejectAnEmptyBatchBeforeReadingAnythingTest() {
		// Given: a reader and no uploads
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceUploadReader reader = new MultipartSourceUploadReader(
				new MultipartSourceDocumentReader(textService), new SourceImageServiceImpl());

		// When-Then: the empty batch is rejected as a C002 client error
		assertThatThrownBy(() -> reader.read(List.of(), 5))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("At least one source document is required");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldRejectABatchLargerThanTheFeatureLimitTest() {
		// Given: two uploads against a limit of one
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceUploadReader reader = new MultipartSourceUploadReader(
				new MultipartSourceDocumentReader(textService), new SourceImageServiceImpl());
		List<MultipartFile> files = List.of(
				new MockMultipartFile("files", "a.pdf", null, "a".getBytes()),
				new MockMultipartFile("files", "b.png", null, new byte[]{1}));

		// When-Then: the batch is rejected naming the count and the limit
		assertThatThrownBy(() -> reader.read(files, 1))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Too many files: 2")
				.hasMessageContaining("the maximum is 1");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldRejectAScreenshotOverTheImageSizeLimitTest() {
		// Given: a PNG one byte past the per-image ceiling
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceUploadReader reader = new MultipartSourceUploadReader(
				new MultipartSourceDocumentReader(textService), new SourceImageServiceImpl());
		MultipartFile huge = new MockMultipartFile("files", "huge.png", null,
				new byte[SourceImageServiceImpl.MAX_IMAGE_BYTES + 1]);

		// When-Then: the upload is rejected as a C002 client error naming the file
		assertThatThrownBy(() -> reader.read(List.of(huge), 5))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("huge.png is larger than 10 MB");
		verifyNoInteractions(textService);
	}
}
