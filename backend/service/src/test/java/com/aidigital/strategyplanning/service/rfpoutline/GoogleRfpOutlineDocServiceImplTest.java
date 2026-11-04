package com.aidigital.strategyplanning.service.rfpoutline;

import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.clerk.model.ClerkOAuthGrant;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleGrantServiceImpl;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleTemplateFileGatewayImpl;
import com.aidigital.strategyplanning.service.rfpoutline.services.impl.RfpOutlineDocRequestFactoryImpl;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.services.impl.GoogleRfpOutlineDocServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the RFP outline Google Docs writer.
 *
 * <p>The doc flow needs the Docs scope where a deck needs the Slides scope, so a user who
 * connected Google for the deck builder must still be asked to reconnect here.
 */
class GoogleRfpOutlineDocServiceImplTest {

	private static final String DRIVE = "https://www.googleapis.com/auth/drive";
	private static final String DOCS = "https://www.googleapis.com/auth/documents";
	private static final String SLIDES = "https://www.googleapis.com/auth/presentations";
	private static final String SECRET = "sk_test";
	private static final String TOKEN = "ya29.token";

	@Test
	void shouldFailWithC003WhenNoClerkSecretIsConfiguredTest() {
		// Given: the feature has no Clerk secret, so no grant can be looked up
		RfpOutlineProperties properties = new RfpOutlineProperties();
		ObjectMapper objectMapper = new ObjectMapper();
		GoogleRfpOutlineDocServiceImpl service = new GoogleRfpOutlineDocServiceImpl(properties,
				new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class)),
				new GoogleTemplateFileGatewayImpl(objectMapper, mock(GoogleWorkspaceClient.class)),
				new RfpOutlineDocRequestFactoryImpl(objectMapper));

		// When-Then: the caller sees the external-call error code, not a 500
		assertThatThrownBy(() -> service.createDoc("user_1", "Outline", Map.of()))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C003"));
	}

	@Test
	void shouldTellTheUserToSignInWithGoogleWhenNoGrantExistsTest() {
		// Given: the user has never connected Google
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setClerkSecretKey(SECRET);
		ObjectMapper objectMapper = new ObjectMapper();
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant(SECRET, "user_1", "oauth_google")).thenReturn(null);
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		GoogleRfpOutlineDocServiceImpl service = new GoogleRfpOutlineDocServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new RfpOutlineDocRequestFactoryImpl(objectMapper));

		// When-Then: the message tells them what to do, and Google is never called
		assertThatThrownBy(() -> service.createDoc("user_1", "Outline", Map.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google — sign in with your Google account "
						+ "to create the doc");
		verifyNoInteractions(workspaceClient);
	}

	@Test
	void shouldAskForAReconnectWhenTheDocsScopeIsMissingTest() {
		// Given: a grant that covers Drive and Slides but not Docs
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setClerkSecretKey(SECRET);
		ObjectMapper objectMapper = new ObjectMapper();
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant(SECRET, "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant(TOKEN, List.of(DRIVE, SLIDES)));
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		GoogleRfpOutlineDocServiceImpl service = new GoogleRfpOutlineDocServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new RfpOutlineDocRequestFactoryImpl(objectMapper));

		// When-Then: a deck-shaped grant does not silently produce a broken doc
		assertThatThrownBy(() -> service.createDoc("user_1", "Outline", Map.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Google Drive & Docs access not granted — reconnect Google "
						+ "and allow Drive & Docs access");
		verifyNoInteractions(workspaceClient);
	}

	@Test
	void shouldCreateABlankDocWriteTheSectionsAndReturnTheUrlTest() {
		// Given: a fully granted user and a Docs API that creates the document
		RfpOutlineProperties properties = new RfpOutlineProperties();
		properties.setClerkSecretKey(SECRET);
		ObjectMapper objectMapper = new ObjectMapper();
		ClerkOAuthClient clerkClient = mock(ClerkOAuthClient.class);
		when(clerkClient.fetchGrant(SECRET, "user_1", "oauth_google"))
				.thenReturn(new ClerkOAuthGrant(TOKEN, List.of(DRIVE, DOCS)));
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		when(workspaceClient.createDocument(TOKEN, "Outline"))
				.thenReturn(objectMapper.createObjectNode().put("documentId", "doc_1"));
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		when(workspaceClient.batchUpdateDocument(eq(TOKEN), eq("doc_1"), body.capture()))
				.thenReturn(objectMapper.createObjectNode());
		GoogleRfpOutlineDocServiceImpl service = new GoogleRfpOutlineDocServiceImpl(properties,
				new GoogleGrantServiceImpl(clerkClient),
				new GoogleTemplateFileGatewayImpl(objectMapper, workspaceClient),
				new RfpOutlineDocRequestFactoryImpl(objectMapper));

		// When: the doc is created
		String url = service.createDoc("user_1", "Outline", Map.of("challenge", "Costs were rising"));

		// Then: the user gets an editable Docs link, no template was copied, and the written text
		// carries the title and the headed section
		assertThat(url).isEqualTo("https://docs.google.com/document/d/doc_1/edit");
		verify(workspaceClient).createDocument(TOKEN, "Outline");
		verify(workspaceClient).batchUpdateDocument(eq(TOKEN), eq("doc_1"), body.capture());
		verifyNoMoreInteractions(workspaceClient);
		assertThat(body.getValue()).contains("\"insertText\"")
				.contains("Outline\\n\\nChallenge\\nCosts were rising\\n");
	}

}
