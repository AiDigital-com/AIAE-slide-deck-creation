package com.aidigital.strategyplanning.controllers.rfpoutline;

import com.aidigital.strategyplanning.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.strategyplanning.mappers.rfpoutline.RfpOutlineApiMapperImpl;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.security.AuthProperties;
import com.aidigital.strategyplanning.security.ClerkJwtClaimsValidator;
import com.aidigital.strategyplanning.security.ClerkPublishableKeyDecoder;
import com.aidigital.strategyplanning.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.strategyplanning.security.SecurityConfig;
import com.aidigital.strategyplanning.security.SecurityProperties;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineDraft;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineService;
import com.aidigital.strategyplanning.uploads.MultipartSourceDocumentReader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link RfpOutlinesController}.
 *
 * <p>Covers auth enforcement, the contract's status codes, the arguments the controller forwards
 * to the service, and the 400 responses the OpenAPI input constraints promise.
 */
@WebMvcTest(controllers = RfpOutlinesController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class, RfpOutlineProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		RfpOutlineApiMapperImpl.class,
		MultipartSourceDocumentReader.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class RfpOutlinesControllerTest {

	private static final String RFP_OUTLINES_URL = "/api/v1/rfp-outlines";
	private static final String DRAFTS_URL = "/api/v1/rfp-outlines/drafts";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private RfpOutlineService rfpOutlineService;

	@MockitoBean
	private RfpOutlineDraftService rfpOutlineDraftService;

	@MockitoBean
	private SourceFileTextService sourceFileTextService;

	private static RequestPostProcessor validJwt() {
		return jwt().jwt(j -> j
				.subject("user_123")
				.claim("user_id", "user_123")
				.claim("email", "alice@aidigital.com")
				.claim("full_name", "Alice Example"));
	}

	@Test
	void shouldRejectUnauthenticatedListRequestTest() throws Exception {
		// Given: no bearer token
		// When: the list endpoint is called
		ResultActions response = mvc.perform(get(RFP_OUTLINES_URL));

		// Then: the request is refused before it reaches the service
		response.andExpect(status().isUnauthorized());
		verifyNoInteractions(rfpOutlineService);
	}

	@Test
	void shouldListOutlinesOwnedByTheCallerTest() throws Exception {
		// Given: one stored outline for the caller
		when(rfpOutlineService.listByUser("user_123")).thenReturn(List.of(
				new RfpOutlineRecord(3L, "Acme RFP", "Acme", "Retail", null, null, null, null,
						null, null, null, "https://docs/3", "GENERATED", "user_123",
						LocalDateTime.of(2026, 8, 2, 9, 30))));

		// When: the list endpoint is called
		ResultActions response = mvc.perform(get(RFP_OUTLINES_URL).with(validJwt()));

		// Then: the summary payload is returned, scoped to the caller
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(3))
				.andExpect(jsonPath("$[0].title").value("Acme RFP"))
				.andExpect(jsonPath("$[0].status").value("GENERATED"));
		verify(rfpOutlineService).listByUser("user_123");
	}

	@Test
	void shouldCreateOutlineAndForwardTheCallerAsCreatedByTest() throws Exception {
		// Given: a service that persists the submitted outline
		when(rfpOutlineService.create(new CreateRfpOutlineCommand(
				"Acme RFP", "Acme", null, null, null, null, null, null, null, null, "user_123")))
				.thenReturn(new RfpOutlineRecord(3L, "Acme RFP", "Acme", null, null, null, null,
						null, null, null, null, "https://docs/3", "GENERATED", "user_123",
						LocalDateTime.of(2026, 8, 2, 9, 30)));

		// When: a valid payload is posted
		ResultActions response = mvc.perform(post(RFP_OUTLINES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Acme RFP\",\"clientName\":\"Acme\"}"));

		// Then: the contract's 201 comes back and createdBy is the JWT's user
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.docUrl").value("https://docs/3"));
		ArgumentCaptor<CreateRfpOutlineCommand> command =
				ArgumentCaptor.forClass(CreateRfpOutlineCommand.class);
		verify(rfpOutlineService).create(command.capture());
		assertThat(command.getValue().createdBy()).isEqualTo("user_123");
	}

	@Test
	void shouldRejectCreateWithBlankTitleAsBadRequestTest() throws Exception {
		// Given: a payload violating the contract's minLength on title
		// When: it is posted
		ResultActions response = mvc.perform(post(RFP_OUTLINES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rfpOutlineService);
	}

	@Test
	void shouldRejectCreateWithOverlongDeckOutlineAsBadRequestTest() throws Exception {
		// Given: a deck outline one character past the contract's maxLength of 20000
		String outline = "x".repeat(20001);

		// When: it is posted
		ResultActions response = mvc.perform(post(RFP_OUTLINES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Acme RFP\",\"deckOutline\":\"" + outline + "\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rfpOutlineService);
	}

	@Test
	void shouldDraftFromUploadedDocumentsAndForwardTheNotesTest() throws Exception {
		// Given: one upload plus strategist notes
		when(sourceFileTextService.extractText("rfp.pdf", "raw".getBytes())).thenReturn("Body");
		when(rfpOutlineDraftService.draft(
				List.of(new SourceDocument("rfp.pdf", "Body")), "Budget is fixed"))
				.thenReturn(new RfpOutlineDraft("Drafted", "Acme", "Retail", "Challenge",
						"Opportunity", "Solution", "Outcome", "Slide 1 — Title", "rfp.pdf"));

		// When: the draft endpoint receives the upload and the notes
		ResultActions response = mvc.perform(multipart(DRAFTS_URL)
				.file(new MockMultipartFile("files", "rfp.pdf", null, "raw".getBytes()))
				.param("notes", "Budget is fixed")
				.with(validJwt()));

		// Then: the drafted fields are rendered and the notes reached the draft service
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Drafted"))
				.andExpect(jsonPath("$.deckOutline").value("Slide 1 — Title"));
		verify(rfpOutlineDraftService)
				.draft(List.of(new SourceDocument("rfp.pdf", "Body")), "Budget is fixed");
	}

	@Test
	void shouldRejectADraftRequestWithNoFilesAsBadRequestTest() throws Exception {
		// Given: a multipart request that carries no files part
		// When: the draft endpoint is called
		ResultActions response = mvc.perform(multipart(DRAFTS_URL).with(validJwt()));

		// Then: the upload reader's C002 rejection surfaces as 400
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rfpOutlineDraftService);
	}

	@Test
	void shouldRejectNonNumericOutlineIdAsBadRequestTest() throws Exception {
		// Given: an id that cannot bind to the contract's int64 path parameter
		// When: the detail endpoint is called with it
		ResultActions response = mvc.perform(get(RFP_OUTLINES_URL + "/not-a-number").with(validJwt()));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rfpOutlineService);
	}

	@Test
	void shouldReturnOneOutlineForTheCallerTest() throws Exception {
		// Given: a stored outline owned by the caller
		when(rfpOutlineService.getById(3L, "user_123"))
				.thenReturn(new RfpOutlineRecord(3L, "Acme RFP", "Acme", "Retail", null, null,
						null, null, null, null, null, null, "DRAFT", "user_123",
						LocalDateTime.of(2026, 8, 2, 9, 30)));

		// When: the detail endpoint is called
		ResultActions response = mvc.perform(get(RFP_OUTLINES_URL + "/3").with(validJwt()));

		// Then: the record is returned and the caller's id scoped the lookup
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.industry").value("Retail"));
		verify(rfpOutlineService).getById(3L, "user_123");
	}
}
