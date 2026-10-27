package com.aidigital.strategyplanning.service.common.google.impl;

import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Default implementation of {@link GoogleTemplateFileGateway}.
 */
@Service
@RequiredArgsConstructor
public class GoogleTemplateFileGatewayImpl implements GoogleTemplateFileGateway {

	/** Drive MIME type of a native Google Slides presentation. */
	public static final String SLIDES_MIME_TYPE = "application/vnd.google-apps.presentation";

	/** MIME type of a PowerPoint (OOXML) presentation. */
	public static final String PPTX_MIME_TYPE =
			"application/vnd.openxmlformats-officedocument.presentationml.presentation";

	private static final String NO_FILE_ID_MESSAGE = "Google Drive upload returned no file ID";

	/** HTTP status Drive answers with when a file does not exist or is not shared with the caller. */
	public static final int DRIVE_NOT_FOUND_STATUS = 404;

	/** Suffix explaining a template-copy 404: the file is not visible to the user's own Google account. */
	public static final String TEMPLATE_NOT_SHARED_SUFFIX = ": Google cannot find the template for your Google"
			+ " account. Ask the template owner to share it with everyone at the company domain (template ID ";

	/** Message suffix when a tool has no template configured at all. */
	public static final String TEMPLATE_NOT_CONFIGURED_SUFFIX =
			": no template is configured for this tool. Ask an administrator to set its template ID.";

	private final ObjectMapper objectMapper;
	private final GoogleWorkspaceClient googleWorkspaceClient;

	@Override
	public String copyTemplate(String accessToken, String templateFileId, String title, String action) {
		if (!StringUtils.hasText(templateFileId)) {
			throw new AppException(ErrorReason.C003, "Could not " + action + TEMPLATE_NOT_CONFIGURED_SUFFIX);
		}
		JsonNode result;
		try {
			result = call(() -> googleWorkspaceClient.copyDriveFile(accessToken, templateFileId, title), action);
		} catch (AppException e) {
			throw templateCopyFailure(e, templateFileId, action);
		}
		String id = result.path("id").asText("");
		if (!StringUtils.hasText(id)) {
			throw new AppException(ErrorReason.C003, "Google Drive copy returned no file ID");
		}
		return id;
	}

	@Override
	public void fillPresentation(String accessToken, String presentationId, ArrayNode requests,
			String action) {
		if (requests.isEmpty()) {
			return;
		}
		call(() -> googleWorkspaceClient.batchUpdatePresentation(accessToken, presentationId,
				requestsBody(requests)), action);
	}

	@Override
	public JsonNode readPresentation(String accessToken, String presentationId, String action) {
		return call(() -> googleWorkspaceClient.getPresentation(accessToken, presentationId), action);
	}

	@Override
	public void fillDocument(String accessToken, String documentId, ArrayNode requests, String action) {
		if (requests.isEmpty()) {
			return;
		}
		call(() -> googleWorkspaceClient.batchUpdateDocument(accessToken, documentId,
				requestsBody(requests)), action);
	}

	@Override
	public byte[] exportPresentation(String accessToken, String presentationId, String action) {
		return call(() -> googleWorkspaceClient.exportDriveFile(accessToken, presentationId, PPTX_MIME_TYPE), action);
	}

	@Override
	public String uploadPresentation(String accessToken, String title, byte[] pptxBytes, String action) {
		JsonNode result = call(() -> googleWorkspaceClient.uploadDriveFile(accessToken, title, SLIDES_MIME_TYPE,
				pptxBytes, PPTX_MIME_TYPE), action);
		String id = result.path("id").asText("");
		if (!StringUtils.hasText(id)) {
			throw new AppException(ErrorReason.C003, NO_FILE_ID_MESSAGE);
		}
		return id;
	}

	/**
	 * Wraps a request array in the batchUpdate envelope both APIs expect.
	 *
	 * @param requests API requests to send
	 * @return serialized {@code {"requests": [...]}} body
	 */
	String requestsBody(ArrayNode requests) {
		ObjectNode body = objectMapper.createObjectNode();
		body.set("requests", requests);
		return body.toString();
	}

	/**
	 * Rewrites a template-copy failure so a Drive 404 explains the real cause. Drive answers 404
	 * both for a missing file and for a file the caller cannot see, and every copy runs on the
	 * user's own OAuth token, so a 404 almost always means the template was never shared with
	 * that user. Other failures pass through unchanged.
	 *
	 * @param failure        the error {@link #call(Supplier, String)} produced
	 * @param templateFileId the template the copy was attempted from
	 * @param action         human-readable description of what the call was for
	 * @return the exception to throw instead
	 */
	AppException templateCopyFailure(AppException failure, String templateFileId, String action) {
		if (!(failure.getCause() instanceof GoogleApiException google)
				|| google.getStatusCode() != DRIVE_NOT_FOUND_STATUS) {
			return failure;
		}
		return new AppException(ErrorReason.C003, google,
				"Could not " + action + TEMPLATE_NOT_SHARED_SUFFIX + templateFileId + ", HTTP 404)");
	}

	/**
	 * Runs a Google call and translates a provider failure into an application error that names
	 * the action the user was trying to perform.
	 *
	 * @param <T>    the call's response type
	 * @param call   the Google call to run
	 * @param action human-readable description of what the call was for
	 * @return the call's response
	 * @throws AppException with C003 reason when the call fails
	 */
	<T> T call(Supplier<T> call, String action) {
		try {
			return call.get();
		} catch (GoogleApiException e) {
			throw switch (e.getFailure()) {
				case HTTP_STATUS -> new AppException(ErrorReason.C003, e,
						"Could not " + action + " (Google returned HTTP " + e.getStatusCode() + ")");
				case INTERRUPTED -> new AppException(ErrorReason.C003, e,
						"Could not " + action + " — call interrupted");
				case TRANSPORT -> new AppException(ErrorReason.C003, e,
						"Could not " + action + ": " + e.getMessage());
			};
		}
	}
}
