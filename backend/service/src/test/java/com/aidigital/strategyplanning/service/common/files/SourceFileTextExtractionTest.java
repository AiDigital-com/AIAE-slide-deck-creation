package com.aidigital.strategyplanning.service.common.files;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.impl.SourceFileTextServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the remaining source-document formats and the guards around extraction.
 *
 * <p>These files are uploaded by users, so the failure paths matter as much as the happy ones: a
 * corrupt upload, a scanned page with no text layer, and an oversized document all have to
 * produce an explanation the user can act on rather than an opaque failure or an unusable prompt.
 */
class SourceFileTextExtractionTest {

	@Test
	void shouldExtractTextFromAPdfTest() throws IOException {
		// Given: a one-page PDF carrying a line of text
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();
		byte[] pdf;
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream out =
				new ByteArrayOutputStream()) {
			PDPage page = new PDPage();
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				content.beginText();
				content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				content.newLineAtOffset(72, 700);
				content.showText("Revenue grew 40 percent");
				content.endText();
			}
			document.save(out);
			pdf = out.toByteArray();
		}

		// When: the text is extracted
		String text = service.extractText("report.pdf", pdf);

		// Then: the page's text reaches the prompt
		assertThat(text).contains("Revenue grew 40 percent");
	}

	@Test
	void shouldExtractSlideTextFromAPptxTest() throws IOException {
		// Given: a presentation with one text box
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();
		byte[] pptx;
		try (XMLSlideShow slideShow = new XMLSlideShow(); ByteArrayOutputStream out =
				new ByteArrayOutputStream()) {
			XSLFSlide slide = slideShow.createSlide();
			XSLFTextBox box = slide.createTextBox();
			box.setText("Pets are premium now");
			slideShow.write(out);
			pptx = out.toByteArray();
		}

		// When: the text is extracted
		String text = service.extractText("deck.pptx", pptx);

		// Then: the slide copy reaches the prompt
		assertThat(text).contains("Pets are premium now");
	}

	@Test
	void shouldExplainThatAFileIsCorruptRatherThanFailingOpaquelyTest() {
		// Given: bytes that claim to be a PDF but are not
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();

		// When-Then: the message names the file and the expected format, so the user knows
		// which upload to replace
		assertThatThrownBy(() -> service.extractText("report.pdf",
				"not a pdf at all".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(AppException.class)
				.satisfies(e -> {
					AppException failure = (AppException) e;
					assertThat(failure.getValidationMessage().getCode())
							.isEqualTo(ErrorReason.C002.getCode());
					assertThat(failure.getMessage())
							.contains("Could not read report.pdf")
							.contains("PDF");
				});
	}

	@Test
	void shouldRejectADocumentWithNoReadableTextTest() {
		// Given: a file that parses but contains only whitespace, the way a scanned page with
		// no text layer does
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();

		// When-Then: the user is told the document had nothing to read, rather than the draft
		// being generated from an empty prompt
		assertThatThrownBy(() -> service.extractText("scan.txt",
				"   \n\n  \t ".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getMessage())
						.contains("No readable text could be extracted from scan.txt"));
	}

	@Test
	void shouldTruncateAnOversizedDocumentToThePromptLimitTest() {
		// Given: a document far longer than a model prompt can carry
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();
		String oversized = "word ".repeat(SourceFileTextServiceImpl.MAX_CHARS_PER_DOCUMENT);

		// When: the text is extracted
		String text = service.extractText("long.txt", oversized.getBytes(StandardCharsets.UTF_8));

		// Then: it is cut to the limit rather than being sent whole and rejected by the model
		assertThat(text).hasSize(SourceFileTextServiceImpl.MAX_CHARS_PER_DOCUMENT);
	}

	@Test
	void shouldCollapseRunsOfWhitespaceTest() {
		// Given: text with the ragged spacing document extraction produces
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();
		String ragged = "Revenue    grew\r\n\n\n\n40 percent";

		// When: the text is extracted
		String text = service.extractText("notes.txt", ragged.getBytes(StandardCharsets.UTF_8));

		// Then: runs of spaces and blank lines are collapsed, so the prompt is not padded with
		// layout artefacts. A CR collapses to a space rather than being dropped, which leaves
		// one trailing space before the break — harmless in a prompt, and pinned here so the
		// behaviour is a decision rather than an accident.
		assertThat(text).isEqualTo("Revenue grew \n\n40 percent");
	}

	@Test
	void shouldReadTheExtensionCaseInsensitivelyAndTolerateItsAbsenceTest() {
		// Given: filenames as browsers and operating systems actually supply them
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();

		// When-Then: the extension drives format detection, so it must be normalised, and a
		// name without one must not blow up before the unsupported-type message is produced
		assertThat(service.fileExtension("REPORT.PDF")).isEqualTo("pdf");
		assertThat(service.fileExtension("archive.tar.GZ")).isEqualTo("gz");
		assertThat(service.fileExtension("noextension")).isEmpty();
		assertThat(service.fileExtension(null)).isEmpty();
	}

	@Test
	void shouldListTheSupportedFormatsWhenTheTypeIsUnsupportedTest() {
		// Given: an upload in a format the extractor does not handle
		SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();

		// When-Then: the message says what is supported, so the user can convert and retry
		assertThatThrownBy(() -> service.extractText("chart.png", new byte[]{1, 2, 3}))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getMessage())
						.contains("Unsupported file type \".png\" for chart.png")
						.contains("PDF, DOCX, DOC, PPTX, PPT, XLSX, XLS, CSV"));
	}
}
