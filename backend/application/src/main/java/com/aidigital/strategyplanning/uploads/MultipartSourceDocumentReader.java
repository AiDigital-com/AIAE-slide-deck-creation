package com.aidigital.strategyplanning.uploads;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a multipart upload batch into {@link SourceDocument} values.
 *
 * <p>Multipart is an HTTP transport concern, so it stops at the application layer: the draft
 * services below receive filenames and extracted text only. Batch size is validated here because
 * the limit is per feature and arrives from that feature's configuration properties.
 */
@Component
@RequiredArgsConstructor
public class MultipartSourceDocumentReader {

	private final SourceFileTextService sourceFileTextService;

	/**
	 * Validates an upload batch and extracts readable text from every file in it.
	 *
	 * @param files    uploaded multipart files, in the order the client sent them
	 * @param maxFiles maximum number of files the calling feature accepts per request
	 * @return one source document per upload, in upload order
	 * @throws AppException with C002 reason when the batch is empty, exceeds {@code maxFiles},
	 *                      or contains a file that cannot be read or parsed
	 */
	public List<SourceDocument> read(List<MultipartFile> files, int maxFiles) {
		requireBatchWithinLimit(files, maxFiles);
		List<SourceDocument> documents = new ArrayList<>();
		for (MultipartFile file : files) {
			documents.add(toSourceDocument(file));
		}
		return documents;
	}

	/**
	 * Rejects an empty upload batch and one that exceeds the calling feature's limit.
	 *
	 * @param files    uploaded multipart files
	 * @param maxFiles maximum number of files the calling feature accepts per request
	 * @throws AppException with C002 reason when the batch is empty or too large
	 */
	void requireBatchWithinLimit(List<MultipartFile> files, int maxFiles) {
		if (files == null || files.isEmpty()) {
			throw new AppException(ErrorReason.C002, "At least one source document is required");
		}
		if (files.size() > maxFiles) {
			throw new AppException(ErrorReason.C002,
					"Too many files: " + files.size() + " — the maximum is " + maxFiles);
		}
	}

	/**
	 * Extracts the readable text of one upload.
	 *
	 * @param file uploaded multipart file
	 * @return the upload's filename paired with its extracted text
	 */
	SourceDocument toSourceDocument(MultipartFile file) {
		return new SourceDocument(file.getOriginalFilename(),
				sourceFileTextService.extractText(file.getOriginalFilename(), readBytes(file)));
	}

	/**
	 * Reads the raw bytes of an uploaded multipart file.
	 *
	 * @param file uploaded multipart file
	 * @return raw file contents
	 * @throws AppException with C002 reason when the upload cannot be read
	 */
	byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new AppException(ErrorReason.C002, e,
					"Could not read uploaded file: " + file.getOriginalFilename());
		}
	}
}
