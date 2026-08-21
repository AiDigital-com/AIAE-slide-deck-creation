package com.aidigital.strategyplanning.uploads;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MultipartSourceDocumentReader}.
 *
 * <p>Covers the batch rules both draft endpoints rely on: empty batch, over-limit batch,
 * upload order, and the C002 translation of an upload whose bytes cannot be read.
 */
class MultipartSourceDocumentReaderTest {

	@Test
	void shouldExtractOneDocumentPerUploadInUploadOrderTest() {
		// Given: two uploads whose text the extraction service returns
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceDocumentReader reader = new MultipartSourceDocumentReader(textService);
		MultipartFile first = new MockMultipartFile("files", "brief.pdf", null, "raw-1".getBytes());
		MultipartFile second = new MockMultipartFile("files", "terms.docx", null, "raw-2".getBytes());
		when(textService.extractText("brief.pdf", "raw-1".getBytes())).thenReturn("Brief text");
		when(textService.extractText("terms.docx", "raw-2".getBytes())).thenReturn("Terms text");

		// When: the batch is read
		List<SourceDocument> documents = reader.read(List.of(first, second), 5);

		// Then: both filenames are extracted and paired with their text, in upload order
		verify(textService).extractText("brief.pdf", "raw-1".getBytes());
		verify(textService).extractText("terms.docx", "raw-2".getBytes());
		assertThat(documents).containsExactly(
				new SourceDocument("brief.pdf", "Brief text"),
				new SourceDocument("terms.docx", "Terms text"));
	}

	@Test
	void shouldRejectAnEmptyUploadBatchTest() {
		// Given: a reader and no uploads
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceDocumentReader reader = new MultipartSourceDocumentReader(textService);

		// When-Then: the empty batch is rejected as a C002 client error, nothing is extracted
		assertThatThrownBy(() -> reader.read(List.of(), 5))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("At least one source document is required");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldRejectAMissingFilesPartTest() {
		// Given: a request that carried no files part at all
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceDocumentReader reader = new MultipartSourceDocumentReader(textService);

		// When-Then: the missing part is rejected the same way as an empty batch
		assertThatThrownBy(() -> reader.read(null, 5))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("At least one source document is required");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldRejectABatchLargerThanTheFeatureLimitTest() {
		// Given: three uploads against a limit of two
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceDocumentReader reader = new MultipartSourceDocumentReader(textService);
		List<MultipartFile> files = List.of(
				new MockMultipartFile("files", "a.pdf", null, "a".getBytes()),
				new MockMultipartFile("files", "b.pdf", null, "b".getBytes()),
				new MockMultipartFile("files", "c.pdf", null, "c".getBytes()));

		// When-Then: the batch is rejected naming both the count and the limit
		assertThatThrownBy(() -> reader.read(files, 2))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Too many files: 3")
				.hasMessageContaining("the maximum is 2");
		verifyNoInteractions(textService);
	}

	@Test
	void shouldTranslateAnUnreadableUploadIntoAClientErrorTest() {
		// Given: an upload whose bytes cannot be read from the request
		SourceFileTextService textService = mock(SourceFileTextService.class);
		MultipartSourceDocumentReader reader = new MultipartSourceDocumentReader(textService);
		MultipartFile broken = new MockMultipartFile("files", "broken.pdf", null, new byte[0]) {
			@Override
			public byte[] getBytes() throws IOException {
				throw new IOException("stream closed");
			}
		};

		// When-Then: the I/O failure surfaces as C002 naming the file
		assertThatThrownBy(() -> reader.read(List.of(broken), 5))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Could not read uploaded file: broken.pdf")
				.hasRootCauseMessage("stream closed");
	}
}
