package com.aidigital.strategyplanning.service.common.google;

import com.aidigital.strategyplanning.external.google.GoogleApiException;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.external.google.model.GoogleApiFailure;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleTemplateFileGatewayImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the shared copy-then-fill Google gateway.
 *
 * <p>Every failure message must name the action the user was attempting, because "Google returned
 * HTTP 403" on its own tells them nothing about what to do next.
 */
class GoogleTemplateFileGatewayImplTest {

	private static final String TOKEN = "ya29.token";
	private static final String TEMPLATE_ID = "template_1";
	private static final String COPY_ACTION = "copy the case study template into your Google Drive";
	private static final String FILL_ACTION = "fill the case study deck";
	private static final String ONE_REQUEST_BODY =
			"{\"requests\":[{\"replaceAllText\":{\"replaceText\":\"Acme\"}}]}";

	@Test
	void shouldReturnTheNewFileIdAfterCopyingTheTemplateTest() {
		// Given: Drive answers the copy with the id of the new file
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		when(workspaceClient.copyDriveFile(TOKEN, TEMPLATE_ID, "Deck"))
				.thenReturn(objectMapper.createObjectNode().put("id", "copy_1"));

		// When: the template is copied
		String id = new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.copyTemplate(TOKEN, TEMPLATE_ID, "Deck", COPY_ACTION);

		// Then: the caller gets the id it needs to fill the copy
		assertThat(id).isEqualTo("copy_1");
	}

	@Test
	void shouldRejectACopyResponseWithoutAFileIdTest() {
		// Given: a 2xx response that carries no id
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		when(workspaceClient.copyDriveFile(TOKEN, TEMPLATE_ID, "Deck"))
				.thenReturn(objectMapper.createObjectNode());

		// When-Then: the flow stops here rather than filling a file that does not exist
		assertThatThrownBy(() -> new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.copyTemplate(TOKEN, TEMPLATE_ID, "Deck", COPY_ACTION))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Google Drive copy returned no file ID");
	}

	@Test
	void shouldNameTheActionAndStatusOnAnHttpFailureTest() {
		// Given: Drive refuses the copy
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		when(workspaceClient.copyDriveFile(TOKEN, TEMPLATE_ID, "Deck"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 403,
						"{\"error\":\"forbidden\"}", "Google API returned HTTP 403"));

		// When-Then: the message says what failed and what the user was doing
		assertThatThrownBy(() -> new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.copyTemplate(TOKEN, TEMPLATE_ID, "Deck", COPY_ACTION))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Could not " + COPY_ACTION
								+ " (Google returned HTTP 403)"));
	}

	@Test
	void shouldReportAnInterruptedCallDistinctlyTest() {
		// Given: the thread was interrupted mid-call
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		when(workspaceClient.copyDriveFile(TOKEN, TEMPLATE_ID, "Deck"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.INTERRUPTED,
						"Google API call interrupted", new InterruptedException("stop")));

		// When-Then: an interruption reads differently from a provider rejection
		assertThatThrownBy(() -> new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.copyTemplate(TOKEN, TEMPLATE_ID, "Deck", COPY_ACTION))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Could not " + COPY_ACTION
								+ " — call interrupted"));
	}

	@Test
	void shouldCarryTheTransportCauseIntoTheMessageTest() {
		// Given: the connection dropped before any status arrived
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		when(workspaceClient.copyDriveFile(TOKEN, TEMPLATE_ID, "Deck"))
				.thenThrow(new GoogleApiException(GoogleApiFailure.TRANSPORT,
						"connection reset", new IOException("connection reset")));

		// When-Then: the transport reason reaches the message instead of being swallowed
		assertThatThrownBy(() -> new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.copyTemplate(TOKEN, TEMPLATE_ID, "Deck", COPY_ACTION))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Could not " + COPY_ACTION
								+ ": connection reset"));
	}

	@Test
	void shouldWrapPresentationRequestsInTheBatchUpdateEnvelopeTest() {
		// Given: one replaceAllText request for a copied deck
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("replaceAllText").put("replaceText", "Acme");
		when(workspaceClient.batchUpdatePresentation(TOKEN, "copy_1", ONE_REQUEST_BODY))
				.thenReturn(objectMapper.createObjectNode());

		// When: the deck is filled
		new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.fillPresentation(TOKEN, "copy_1", requests, FILL_ACTION);

		// Then: the Slides API receives the envelope shape it requires
		verify(workspaceClient).batchUpdatePresentation(TOKEN, "copy_1", ONE_REQUEST_BODY);
	}

	@Test
	void shouldWrapDocumentRequestsInTheBatchUpdateEnvelopeTest() {
		// Given: one replaceAllText request for a copied doc
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("replaceAllText").put("replaceText", "Acme");
		when(workspaceClient.batchUpdateDocument(TOKEN, "doc_1", ONE_REQUEST_BODY))
				.thenReturn(objectMapper.createObjectNode());

		// When: the doc is filled
		new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.fillDocument(TOKEN, "doc_1", requests, FILL_ACTION);

		// Then: the Docs API receives the same envelope shape
		verify(workspaceClient).batchUpdateDocument(TOKEN, "doc_1", ONE_REQUEST_BODY);
	}

	@Test
	void shouldSkipTheCallEntirelyWhenThereIsNothingToReplaceTest() {
		// Given: no token values to write
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode empty = objectMapper.createArrayNode();
		GoogleTemplateFileGatewayImpl gateway =
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient);

		// When: both fill paths are asked to run
		gateway.fillPresentation(TOKEN, "copy_1", empty, FILL_ACTION);
		gateway.fillDocument(TOKEN, "doc_1", empty, FILL_ACTION);

		// Then: Google is not called at all
		verifyNoInteractions(workspaceClient);
	}

	@Test
	void shouldNameTheDocActionOnAnHttpFailureTest() {
		// Given: the Docs batchUpdate is rate limited
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("replaceAllText").put("replaceText", "Acme");
		when(workspaceClient.batchUpdateDocument(TOKEN, "doc_1", ONE_REQUEST_BODY))
				.thenThrow(new GoogleApiException(GoogleApiFailure.HTTP_STATUS, 429,
						"{\"error\":\"rate limited\"}", "Google API returned HTTP 429"));

		// When-Then: the doc flow's own action text reaches the user
		assertThatThrownBy(() -> new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient)
				.fillDocument(TOKEN, "doc_1", requests, "fill the RFP outline doc"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getMessage())
						.isEqualTo("External call failed: Could not fill the RFP outline doc"
								+ " (Google returned HTTP 429)"));
	}
}
