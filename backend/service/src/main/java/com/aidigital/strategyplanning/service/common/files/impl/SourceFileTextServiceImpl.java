package com.aidigital.strategyplanning.service.common.files.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Default implementation of {@link SourceFileTextService}. Detects the document format by file
 * extension and extracts readable text: PDFBox for PDF, Apache POI for Word/PowerPoint/Excel, and
 * plain UTF-8 decoding for CSV/TXT. Excel sheets are rendered row by row with " | " between cells
 * so the AI drafting engine can interpret the tabular data.
 */
@Service
public class SourceFileTextServiceImpl implements SourceFileTextService {

	/**
	 * Maximum characters of extracted text kept per document, keeping prompts within model limits.
	 */
	public static final int MAX_CHARS_PER_DOCUMENT = 20000;

	@Override
	public String extractText(String fileName, byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new AppException(ErrorReason.C002, "Uploaded file is empty: " + fileName);
		}
		String extension = fileExtension(fileName);
		try {
			String text = switch (extension) {
				case "pdf" -> extractPdfText(bytes);
				case "docx" -> extractDocxText(bytes);
				case "doc" -> extractDocText(bytes);
				case "pptx" -> extractPptxText(bytes);
				case "ppt" -> extractPptText(bytes);
				case "xlsx" -> extractWorkbookText(new XSSFWorkbook(new ByteArrayInputStream(bytes)));
				case "xls" -> extractWorkbookText(new HSSFWorkbook(new ByteArrayInputStream(bytes)));
				case "csv", "txt" -> new String(bytes, StandardCharsets.UTF_8);
				default -> throw new AppException(ErrorReason.C002,
						"Unsupported file type \"." + extension + "\" for " + fileName
								+ " — supported: PDF, DOCX, DOC, PPTX, PPT, XLSX, XLS, CSV");
			};
			String collapsed = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
					.replaceAll("\\n{3,}", "\n\n")
					.trim();
			if (!StringUtils.hasText(collapsed)) {
				throw new AppException(ErrorReason.C002,
						"No readable text could be extracted from " + fileName);
			}
			return collapsed.length() <= MAX_CHARS_PER_DOCUMENT
					? collapsed
					: collapsed.substring(0, MAX_CHARS_PER_DOCUMENT);
		} catch (IOException e) {
			throw new AppException(ErrorReason.C002, e,
					"Could not read " + fileName + " — the file may be corrupted or not a valid "
							+ extension.toUpperCase(Locale.ROOT) + " file");
		}
	}

	/**
	 * Returns the lower-case file extension of a filename, or an empty string when absent.
	 *
	 * @param fileName original filename
	 * @return lower-case extension without the dot
	 */
	public String fileExtension(String fileName) {
		if (fileName == null) {
			return "";
		}
		int dot = fileName.lastIndexOf('.');
		return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	}

	/**
	 * Extracts readable text from a PDF document.
	 *
	 * @param bytes raw PDF bytes
	 * @return extracted text
	 * @throws IOException when the PDF cannot be parsed
	 */
	public String extractPdfText(byte[] bytes) throws IOException {
		try (PDDocument document = Loader.loadPDF(bytes)) {
			return new PDFTextStripper().getText(document);
		}
	}

	/**
	 * Extracts readable text from a modern Word (.docx) document, including tables.
	 *
	 * @param bytes raw DOCX bytes
	 * @return extracted text
	 * @throws IOException when the document cannot be parsed
	 */
	public String extractDocxText(byte[] bytes) throws IOException {
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
		     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			return extractor.getText();
		}
	}

	/**
	 * Extracts readable text from a legacy Word (.doc) document.
	 *
	 * @param bytes raw DOC bytes
	 * @return extracted text
	 * @throws IOException when the document cannot be parsed
	 */
	public String extractDocText(byte[] bytes) throws IOException {
		try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
		     WordExtractor extractor = new WordExtractor(document)) {
			return extractor.getText();
		}
	}

	/**
	 * Extracts readable text from a PPTX presentation, including slide text and notes.
	 *
	 * @param bytes raw PPTX bytes
	 * @return extracted text
	 * @throws IOException when the presentation cannot be parsed
	 */
	public String extractPptxText(byte[] bytes) throws IOException {
		try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(bytes));
		     SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
			extractor.setSlidesByDefault(true);
			extractor.setNotesByDefault(true);
			return extractor.getText();
		}
	}

	/**
	 * Extracts readable text from a legacy PPT presentation, including slide text and notes.
	 *
	 * @param bytes raw PPT bytes
	 * @return extracted text
	 * @throws IOException when the presentation cannot be parsed
	 */
	public String extractPptText(byte[] bytes) throws IOException {
		try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(bytes));
		     SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
			extractor.setSlidesByDefault(true);
			extractor.setNotesByDefault(true);
			return extractor.getText();
		}
	}

	/**
	 * Renders every sheet of an Excel workbook as text: a "Sheet: name" header followed by one
	 * line per row with " | " between formatted cell values, so tabular structure is preserved
	 * for the AI drafting engine. Closes the workbook when done.
	 *
	 * @param workbook opened POI workbook (XSSF or HSSF)
	 * @return sheet-by-sheet textual rendering of the workbook
	 * @throws IOException when the workbook cannot be read or closed
	 */
	public String extractWorkbookText(Workbook workbook) throws IOException {
		DataFormatter formatter = new DataFormatter();
		StringBuilder text = new StringBuilder();
		try (workbook) {
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
				Sheet sheet = workbook.getSheetAt(i);
				text.append("Sheet: ").append(sheet.getSheetName()).append('\n');
				for (Row row : sheet) {
					StringBuilder line = new StringBuilder();
					for (Cell cell : row) {
						if (line.length() > 0) {
							line.append(" | ");
						}
						line.append(formatter.formatCellValue(cell));
					}
					if (StringUtils.hasText(line.toString())) {
						text.append(line).append('\n');
					}
				}
				text.append('\n');
			}
		}
		return text.toString();
	}
}
