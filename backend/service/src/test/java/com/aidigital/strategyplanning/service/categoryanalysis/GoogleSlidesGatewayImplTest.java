package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.GoogleSlidesGatewayImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the category analysis deck flow's view of Google Slides.
 *
 * <p>Every failure message has to name the action the user was attempting, because "HTTP 403" on
 * its own does not tell them whether to reconnect Google or ask for access to the template.
 */
class GoogleSlidesGatewayImplTest {

	private static final String TOKEN = "ya29.token";

	@Test
	void shouldCopyTheMasterTemplateAndReturnItsIdTest() {
		// Given: the configured master template and a Drive that copies it
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		properties.setTemplatePresentationId("master_1");
		when(client.copyDriveFile(TOKEN, "master_1", "Pet Care 2026"))
				.thenReturn(objectMapper.createObjectNode().put("id", "deck_1"));

		// When: the template is copied
		String id = new GoogleSlidesGatewayImpl(properties, objectMapper, client)
				.copyTemplate(TOKEN, "Pet Care 2026");

		// Then: the copy's id is what the rest of the flow works on
		assertThat(id).isEqualTo("deck_1");
		verify(client).copyDriveFile(TOKEN, "master_1", "Pet Care 2026");
	}

	@Test
	void shouldRejectACopyResponseWithoutAFileIdTest() {
		// Given: a 2xx response that carries no id
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		properties.setTemplatePresentationId("master_1");
		when(client.copyDriveFile(TOKEN, "master_1", "Pet Care 2026"))
				.thenReturn(objectMapper.createObjectNode());

		// When-Then: the flow stops rather than filling a deck that does not exist
		assertThatThrownBy(() -> new GoogleSlidesGatewayImpl(properties, objectMapper, client)
				.copyTemplate(TOKEN, "Pet Care 2026"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Google Drive copy returned no file ID");
	}

	@Test
	void shouldReadTheCopiedPresentationTest() {
		// Given: the copied deck
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		when(client.getPresentation(TOKEN, "deck_1"))
				.thenReturn(objectMapper.createObjectNode().put("presentationId", "deck_1"));

		// When: it is read back
		JsonNode presentation = new GoogleSlidesGatewayImpl(new CategoryAnalysisProperties(),
				objectMapper, client).getPresentation(TOKEN, "deck_1");

		// Then: the flow gets the object ids it needs to place content
		assertThat(presentation.path("presentationId").asText()).isEqualTo("deck_1");
	}

	@Test
	void shouldWrapBatchRequestsInTheSlidesEnvelopeTest() {
		// Given: one Slides request to apply
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("deleteObject").put("objectId", "img_1");
		when(client.batchUpdatePresentation(TOKEN, "deck_1",
				"{\"requests\":[{\"deleteObject\":{\"objectId\":\"img_1\"}}]}"))
				.thenReturn(objectMapper.createObjectNode());

		// When: the batch is applied
		new GoogleSlidesGatewayImpl(new CategoryAnalysisProperties(), objectMapper, client)
				.batchUpdate(TOKEN, "deck_1", requests);

		// Then: the Slides API receives the envelope shape it requires
		verify(client).batchUpdatePresentation(TOKEN, "deck_1",
				"{\"requests\":[{\"deleteObject\":{\"objectId\":\"img_1\"}}]}");
	}

	@Test
	void shouldNameTheActionAndStatusOnAnHttpFailureTest() {
		// Given: Drive refuses access to the master template
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		properties.setTemplatePresentationId("master_1");
		when(client.copyDriveFile(TOKEN, "master_1", "Pet Care 2026"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 403,
						"{\"error\":\"forbidden\"}", "Google API returned HTTP 403"));

		// When-Then: the message says what the user was trying to do
		assertThatThrownBy(() -> new GoogleSlidesGatewayImpl(properties, objectMapper, client)
				.copyTemplate(TOKEN, "Pet Care 2026"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Google API call failed while trying to "
								+ "copy the master template into your Google Drive (HTTP 403)"));
	}

	@Test
	void shouldReportAnInterruptedCallDistinctlyTest() {
		// Given: the thread was interrupted while reading the deck
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		when(client.getPresentation(TOKEN, "deck_1"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.INTERRUPTED,
						"Google API call interrupted", new InterruptedException("stop")));

		// When-Then: an interruption reads differently from a provider rejection
		assertThatThrownBy(() -> new GoogleSlidesGatewayImpl(new CategoryAnalysisProperties(),
				objectMapper, client).getPresentation(TOKEN, "deck_1"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Google API call interrupted while trying "
								+ "to read the copied presentation"));
	}

	@Test
	void shouldCarryTheTransportCauseIntoTheMessageTest() {
		// Given: the connection dropped before any status arrived
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleWorkspaceClient client = mock(GoogleWorkspaceClient.class);
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("deleteObject").put("objectId", "img_1");
		when(client.batchUpdatePresentation(TOKEN, "deck_1",
				"{\"requests\":[{\"deleteObject\":{\"objectId\":\"img_1\"}}]}"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.TRANSPORT,
						"connection reset", new IOException("connection reset")));

		// When-Then: the transport reason reaches the message instead of being swallowed
		assertThatThrownBy(() -> new GoogleSlidesGatewayImpl(new CategoryAnalysisProperties(),
				objectMapper, client).batchUpdate(TOKEN, "deck_1", requests))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Google API call failed while trying to "
								+ "fill the presentation content: connection reset"));
	}
}
