package com.aidigital.strategyplanning.service.common.files;

/**
 * One uploaded source document, reduced to its filename and extracted readable text.
 *
 * @param fileName original filename of the uploaded document
 * @param text     readable text extracted from the document contents
 */
public record SourceDocument(String fileName, String text) {

}
