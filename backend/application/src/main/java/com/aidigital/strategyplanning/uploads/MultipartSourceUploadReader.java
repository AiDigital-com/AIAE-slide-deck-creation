package com.aidigital.strategyplanning.uploads;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageService;
import com.aidigital.strategyplanning.service.common.files.SourceUploads;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a mixed multipart upload batch into {@link SourceUploads}: text documents whose readable
 * content is extracted, and images that are passed through for the AI engine to read directly.
 *
 * <p>Builds on {@link MultipartSourceDocumentReader} for the batch rules and text extraction so
 * features that accept documents only keep their existing reader untouched.
 */
@Component
@RequiredArgsConstructor
public class MultipartSourceUploadReader {

	private final MultipartSourceDocumentReader multipartSourceDocumentReader;
	private final SourceImageService sourceImageService;

	/**
	 * Validates an upload batch and splits it into parsed text documents and images, each list in
	 * upload order.
	 *
	 * @param files    uploaded multipart files, in the order the client sent them
	 * @param maxFiles maximum number of files the calling feature accepts per request
	 * @return the batch split by kind
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C002 reason when the batch is
	 * empty, exceeds {@code maxFiles}, contains an image over the size limit, or contains a document that
	 * cannot be read or parsed
	 */
	public SourceUploads read(List<MultipartFile> files, int maxFiles) {
		multipartSourceDocumentReader.requireBatchWithinLimit(files, maxFiles);
		List<SourceDocument> documents = new ArrayList<>();
		List<SourceImage> images = new ArrayList<>();
		for (MultipartFile file : files) {
			String fileName = file.getOriginalFilename();
			if (sourceImageService.imageType(fileName).isPresent()) {
				images.add(sourceImageService.toSourceImage(fileName,
						multipartSourceDocumentReader.readBytes(file)));
			} else {
				documents.add(multipartSourceDocumentReader.toSourceDocument(file));
			}
		}
		return new SourceUploads(documents, images);
	}
}
