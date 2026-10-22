package com.aidigital.strategyplanning.service.common.files;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.impl.SourceFileTextServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SourceFileTextServiceImpl} covering CSV and Markdown pass-through, Excel
 * row rendering, Word (docx) extraction, unsupported extensions, and empty uploads.
 */
class SourceFileTextServiceImplTest {

	private final SourceFileTextServiceImpl service = new SourceFileTextServiceImpl();

	/**
	 * CSV files should be decoded as UTF-8 text.
	 */
	@Test
	void extractsCsvAsPlainText() {
		String csv = "metric,value\nrevenue,120000\n";
		String text = service.extractText("data.csv", csv.getBytes(StandardCharsets.UTF_8));
		assertThat(text).contains("metric,value").contains("revenue,120000");
	}

	/**
	 * Markdown files should be decoded as UTF-8 text with their markup left intact.
	 */
	@Test
	void shouldExtractMarkdownAsPlainTextTest() {
		// Given: a Markdown brief
		String markdown = "# Acme results\n\n- Revenue **+40%**\n";

		// When: the text is extracted
		String text = service.extractText("brief.md", markdown.getBytes(StandardCharsets.UTF_8));

		// Then: headings and emphasis survive as text for the prompt
		assertThat(text).contains("# Acme results").contains("- Revenue **+40%**");
	}

	/**
	 * Excel workbooks should be rendered sheet by sheet with pipe-separated cell values.
	 */
	@Test
	void extractsXlsxRowsWithDelimiters() throws IOException {
		byte[] bytes;
		try (XSSFWorkbook workbook = new XSSFWorkbook();
		     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Results");
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("KPI");
			header.createCell(1).setCellValue("Change");
			Row row = sheet.createRow(1);
			row.createCell(0).setCellValue("Cost");
			row.createCell(1).setCellValue(-0.4);
			workbook.write(out);
			bytes = out.toByteArray();
		}
		String text = service.extractText("results.xlsx", bytes);
		assertThat(text).contains("Sheet: Results")
				.contains("KPI | Change")
				.contains("Cost | -0.4");
	}

	/**
	 * Word (.docx) documents should have their paragraph text extracted.
	 */
	@Test
	void extractsDocxParagraphText() throws IOException {
		byte[] bytes;
		try (XWPFDocument document = new XWPFDocument();
		     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			XWPFParagraph paragraph = document.createParagraph();
			XWPFRun run = paragraph.createRun();
			run.setText("Client objective: increase bookable demand.");
			document.write(out);
			bytes = out.toByteArray();
		}
		String text = service.extractText("brief.docx", bytes);
		assertThat(text).contains("Client objective: increase bookable demand.");
	}

	/**
	 * Unsupported file extensions should be rejected with a C002 error listing the text formats.
	 */
	@Test
	void rejectsUnsupportedExtension() {
		assertThatThrownBy(() -> service.extractText("archive.zip", new byte[]{1, 2, 3}))
				.isInstanceOf(AppException.class)
				.hasMessageContaining(ErrorReason.C002.getCode())
				.hasMessageContaining("Unsupported file type \".zip\" for archive.zip")
				.hasMessageContaining("PDF, DOCX, DOC, PPTX, PPT, XLSX, XLS, CSV, TXT, MD");
	}

	/**
	 * Empty uploads should be rejected with a C002 error.
	 */
	@Test
	void rejectsEmptyFile() {
		assertThatThrownBy(() -> service.extractText("empty.pdf", new byte[0]))
				.isInstanceOf(AppException.class)
				.extracting(e -> ((AppException) e).getValidationMessage().getCode())
				.isEqualTo(ErrorReason.C002.getCode());
	}
}
