package com.aidigital.strategyplanning.controllers.casestudy;

import com.aidigital.strategyplanning.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.strategyplanning.mappers.casestudy.CaseStudyApiMapperImpl;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.security.AuthProperties;
import com.aidigital.strategyplanning.security.ClerkJwtClaimsValidator;
import com.aidigital.strategyplanning.security.ClerkPublishableKeyDecoder;
import com.aidigital.strategyplanning.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.strategyplanning.security.SecurityConfig;
import com.aidigital.strategyplanning.security.SecurityProperties;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyService;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
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
 * Web-layer tests for {@link CaseStudiesController}.
 *
 * <p>Covers auth enforcement, the contract's status codes, the arguments the controller forwards
 * to the service, and the 400 responses the OpenAPI input constraints promise. The service layer
 * is mocked — its behaviour is covered by unit tests in the service module.
 */
@WebMvcTest(controllers = CaseStudiesController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class, CaseStudyProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		CaseStudyApiMapperImpl.class,
		MultipartSourceDocumentReader.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class CaseStudiesControllerTest {

	private static final String CASE_STUDIES_URL = "/api/v1/case-studies";
	private static final String DRAFTS_URL = "/api/v1/case-studies/drafts";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private CaseStudyService caseStudyService;

	@MockitoBean
	private CaseStudyDraftService caseStudyDraftService;

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
		ResultActions response = mvc.perform(get(CASE_STUDIES_URL));

		// Then: the request is refused before it reaches the service
		response.andExpect(status().isUnauthorized());
		verifyNoInteractions(caseStudyService);
	}

	@Test
	void shouldCreateCaseStudyAndForwardTheCallerAsCreatedByTest() throws Exception {
		// Given: a service that persists the submitted case study
		when(caseStudyService.create(new CreateCaseStudyCommand(
				"Acme rollout", "Acme", null, null, null, null, null, null, null, null, "user_123")))
				.thenReturn(new CaseStudyRecord(7L, "Acme rollout", "Acme", null, null, null, null,
						null, null, null, null, null, "https://slides/7", "GENERATED", "user_123",
						LocalDateTime.of(2026, 8, 1, 12, 0)));

		// When: a valid payload is posted
		ResultActions response = mvc.perform(post(CASE_STUDIES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Acme rollout\",\"clientName\":\"Acme\"}"));

		// Then: the contract's 201 and body come back, and createdBy is the JWT's user
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.title").value("Acme rollout"))
				.andExpect(jsonPath("$.slidesUrl").value("https://slides/7"))
				.andExpect(jsonPath("$.status").value("GENERATED"));
		ArgumentCaptor<CreateCaseStudyCommand> command =
				ArgumentCaptor.forClass(CreateCaseStudyCommand.class);
		verify(caseStudyService).create(command.capture());
		assertThat(command.getValue().createdBy()).isEqualTo("user_123");
	}

	@Test
	void shouldRejectCreateWithBlankTitleAsBadRequestTest() throws Exception {
		// Given: a payload violating the contract's minLength on title
		// When: it is posted
		ResultActions response = mvc.perform(post(CASE_STUDIES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyService);
	}

	@Test
	void shouldRejectCreateWithOverlongTitleAsBadRequestTest() throws Exception {
		// Given: a title one character past the contract's maxLength of 255
		String title = "x".repeat(256);

		// When: it is posted
		ResultActions response = mvc.perform(post(CASE_STUDIES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyService);
	}

	@Test
	void shouldDraftFromUploadedDocumentsTest() throws Exception {
		// Given: one upload whose extracted text the draft service turns into a case study
		when(sourceFileTextService.extractText("brief.pdf", "raw".getBytes())).thenReturn("Body");
		when(caseStudyDraftService.draft(List.of(new SourceDocument("brief.pdf", "Body"))))
				.thenReturn(new CaseStudyDraft("Drafted", "Acme", "Retail", "Challenge",
						"Solution", "Results", "Metrics", "Q1", "Great work", "brief.pdf"));

		// When: the draft endpoint receives the upload
		ResultActions response = mvc.perform(multipart(DRAFTS_URL)
				.file(new MockMultipartFile("files", "brief.pdf", null, "raw".getBytes()))
				.with(validJwt()));

		// Then: the drafted fields are rendered into the API DTO
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Drafted"))
				.andExpect(jsonPath("$.clientName").value("Acme"))
				.andExpect(jsonPath("$.sourceFileNames").value("brief.pdf"));
	}

	@Test
	void shouldRejectADraftRequestWithNoFilesAsBadRequestTest() throws Exception {
		// Given: a multipart request that carries no files part
		// When: the draft endpoint is called
		ResultActions response = mvc.perform(multipart(DRAFTS_URL).with(validJwt()));

		// Then: the upload reader's C002 rejection surfaces as 400
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyDraftService);
	}

	@Test
	void shouldRejectNonNumericCaseStudyIdAsBadRequestTest() throws Exception {
		// Given: an id that cannot bind to the contract's int64 path parameter
		// When: the detail endpoint is called with it
		ResultActions response = mvc.perform(get(CASE_STUDIES_URL + "/not-a-number").with(validJwt()));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyService);
	}

	@Test
	void shouldReturnOneCaseStudyForTheCallerTest() throws Exception {
		// Given: a stored case study owned by the caller
		when(caseStudyService.getById(7L, "user_123"))
				.thenReturn(new CaseStudyRecord(7L, "Acme rollout", "Acme", "Retail", null, null,
						null, null, null, null, null, null, null, "DRAFT", "user_123",
						LocalDateTime.of(2026, 8, 1, 12, 0)));

		// When: the detail endpoint is called
		ResultActions response = mvc.perform(get(CASE_STUDIES_URL + "/7").with(validJwt()));

		// Then: the record is returned and the caller's id scoped the lookup
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.industry").value("Retail"));
		verify(caseStudyService).getById(7L, "user_123");
	}
}
