package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Default implementation of {@link GoogleSlidesGateway}.
 */
@Service
@RequiredArgsConstructor
public class GoogleSlidesGatewayImpl implements GoogleSlidesGateway {

	private final CategoryAnalysisProperties properties;
	private final ObjectMapper objectMapper;
	private final GoogleWorkspaceClient googleWorkspaceClient;

	@Override
	public String copyTemplate(String accessToken, String deckTitle) {
		JsonNode result = call(
				() -> googleWorkspaceClient.copyDriveFile(accessToken,
						properties.getTemplatePresentationId(), deckTitle),
				"copy the master template into your Google Drive");
		String id = result.path("id").asText("");
		if (!StringUtils.hasText(id)) {
			throw new AppException(ErrorReason.C003, "Google Drive copy returned no file ID");
		}
		return id;
	}

	/**
	 * Loads the full presentation resource.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to load
	 * @return parsed presentation JSON
	 */
	@Override
	public JsonNode getPresentation(String accessToken, String presentationId) {
		return call(() -> googleWorkspaceClient.getPresentation(accessToken, presentationId),
				"read the copied presentation");
	}

	/**
	 * Executes a batchUpdate against the Slides API.
	 *
	 * @param accessToken    user's Google OAuth access token
	 * @param presentationId presentation to update
	 * @param requests       Slides API requests
	 */
	@Override
	public void batchUpdate(String accessToken, String presentationId, ArrayNode requests) {
		ObjectNode body = objectMapper.createObjectNode();
		body.set("requests", requests);
		call(() -> googleWorkspaceClient.batchUpdatePresentation(accessToken, presentationId,
				body.toString()), "fill the presentation content");
	}

	/**
	 * Runs a Google call and translates a provider failure into this feature's application error,
	 * naming the action the user was trying to perform.
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
						"Google API call failed while trying to " + action
								+ " (HTTP " + e.getStatusCode() + ")");
				case INTERRUPTED -> new AppException(ErrorReason.C003, e,
						"Google API call interrupted while trying to " + action);
				case TRANSPORT -> new AppException(ErrorReason.C003, e,
						"Google API call failed while trying to " + action + ": " + e.getMessage());
			};
		}
	}
}
