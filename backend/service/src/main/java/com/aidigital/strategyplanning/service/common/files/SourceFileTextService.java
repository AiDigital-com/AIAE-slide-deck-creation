package com.aidigital.strategyplanning.service.common.files;

/**
 * Service contract for extracting readable text from uploaded source documents. Supports PDF,
 * Word (DOCX/DOC), PowerPoint (PPTX/PPT), Excel (XLSX/XLS), and CSV/TXT files. Excel workbooks
 * are rendered sheet by sheet as delimited rows so tabular data survives extraction. Shared
 * across features that accept source-document uploads (Case Study Builder, RFP Outline Generator).
 */
public interface SourceFileTextService {

	/**
	 * Extracts readable text from one uploaded document.
	 *
	 * @param fileName original filename, used to detect the document format by extension
	 * @param bytes    raw file contents
	 * @return readable text extracted from the document, truncated to a prompt-friendly length
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C002 reason when the file type is
	 * unsupported or the file cannot be parsed
	 */
	String extractText(String fileName, byte[] bytes);
}
