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

	private final ObjectMapper objectMapper;
	private final GoogleWorkspaceClient googleWorkspaceClient;

	@Override
	public String copyTemplate(String accessToken, String templateFileId, String title, String action) {
		JsonNode result = call(
				() -> googleWorkspaceClient.copyDriveFile(accessToken, templateFileId, title), action);
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
	public void fillDocument(String accessToken, String documentId, ArrayNode requests, String action) {
		if (requests.isEmpty()) {
			return;
		}
		call(() -> googleWorkspaceClient.batchUpdateDocument(accessToken, documentId,
				requestsBody(requests)), action);
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
	 * Runs a Google call and translates a provider failure into an application error that names
	 * the action the user was trying to perform.
	 *
	 * @param call   the Google call to run
	 * @param action human-readable description of what the call was for
	 * @return the call's response
	 * @throws AppException with C003 reason when the call fails
	 */
	JsonNode call(Supplier<JsonNode> call, String action) {
		try {
			return call.get();
		} catch (GoogleApiException e) {
			throw switch (e.getFailure()) {
				case HTTP_STATUS -> new AppException(ErrorReason.C003,
						"Could not " + action + " (Google returned HTTP " + e.getStatusCode() + ")");
				case INTERRUPTED -> new AppException(ErrorReason.C003, e,
						"Could not " + action + " — call interrupted");
				case TRANSPORT -> new AppException(ErrorReason.C003, e,
						"Could not " + action + ": " + e.getMessage());
			};
		}
	}
}
