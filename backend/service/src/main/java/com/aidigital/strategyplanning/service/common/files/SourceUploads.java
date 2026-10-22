package com.aidigital.strategyplanning.service.common.files;

import java.util.List;

/**
 * One upload batch split by kind: documents whose text was extracted, and images that the AI
 * drafting engine reads directly.
 *
 * @param documents parsed text documents, in upload order
 * @param images    uploaded images, in upload order
 */
public record SourceUploads(List<SourceDocument> documents, List<SourceImage> images) {

}
