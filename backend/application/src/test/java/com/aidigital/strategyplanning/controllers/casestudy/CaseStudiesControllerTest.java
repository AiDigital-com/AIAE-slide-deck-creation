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
import com.aidigital.strategyplanning.service.casestudy.models.CreateOnePageCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyIcon;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSource;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconOption;
import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyDraftService;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceFileTextService;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import com.aidigital.strategyplanning.uploads.MultipartSourceDocumentReader;
import com.aidigital.strategyplanning.uploads.MultipartSourceUploadReader;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
		MultipartSourceUploadReader.class,
		SourceImageServiceImpl.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class CaseStudiesControllerTest {

	private static final String CASE_STUDIES_URL = "/api/v1/case-studies";
	private static final String DRAFTS_URL = "/api/v1/case-studies/drafts";
	private static final String ONE_PAGE_DRAFTS_URL = "/api/v1/case-studies/one-page-drafts";
	private static final String ONE_PAGE_FIT_URL = "/api/v1/case-studies/one-page-fit";
	private static final String ONE_PAGE_DECKS_URL = "/api/v1/case-studies/one-page-decks";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private CaseStudyService caseStudyService;

	@MockitoBean
	private CaseStudyDraftService caseStudyDraftService;

	@MockitoBean
	private OnePageCaseStudyDraftService onePageCaseStudyDraftService;

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
		when(caseStudyDraftService.draft(List.of(new SourceDocument("brief.pdf", "Body")), List.of()))
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
	void shouldForwardScreenshotsAsImagesWithoutTextExtractionTest() throws Exception {
		// Given: a Markdown note and a PNG screenshot; only the note goes through text extraction
		when(sourceFileTextService.extractText("notes.md", "# Notes".getBytes())).thenReturn("# Notes");
		when(caseStudyDraftService.draft(eq(List.of(new SourceDocument("notes.md", "# Notes"))),
				argThat(images -> images.size() == 1 && images.get(0).fileName().equals("dashboard.png"))))
				.thenReturn(new CaseStudyDraft("Drafted", null, null, null, null, null, "40% lift",
						null, null, "notes.md, dashboard.png"));

		// When: both files are uploaded to the draft endpoint
		ResultActions response = mvc.perform(multipart(DRAFTS_URL)
				.file(new MockMultipartFile("files", "notes.md", null, "# Notes".getBytes()))
				.file(new MockMultipartFile("files", "dashboard.png", null, new byte[]{1, 2, 3}))
				.with(validJwt()));

		// Then: the draft comes back and the screenshot reached the service as a PNG image
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.keyMetrics").value("40% lift"))
				.andExpect(jsonPath("$.sourceFileNames").value("notes.md, dashboard.png"));
		ArgumentCaptor<List<SourceImage>> images = ArgumentCaptor.captor();
		verify(caseStudyDraftService).draft(eq(List.of(new SourceDocument("notes.md", "# Notes"))),
				images.capture());
		assertThat(images.getValue()).hasSize(1);
		assertThat(images.getValue().get(0).fileName()).isEqualTo("dashboard.png");
		assertThat(images.getValue().get(0).type()).isEqualTo(SourceImageType.PNG);
		assertThat(images.getValue().get(0).bytes()).containsExactly(1, 2, 3);
		verify(sourceFileTextService).extractText("notes.md", "# Notes".getBytes());
		verifyNoMoreInteractions(sourceFileTextService);
	}

	@Test
	void shouldRejectAScreenshotOverTheImageSizeLimitAsBadRequestTest() throws Exception {
		// Given: a PNG one byte past the documented 10 MB ceiling
		MockMultipartFile huge = new MockMultipartFile("files", "huge.png", null,
				new byte[SourceImageServiceImpl.MAX_IMAGE_BYTES + 1]);

		// When: it is uploaded to the draft endpoint
		ResultActions response = mvc.perform(multipart(DRAFTS_URL).file(huge).with(validJwt()));

		// Then: the C002 rejection surfaces as 400 and nothing is drafted
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyDraftService);
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

	@Test
	void shouldDraftAOnePageCaseStudyFromUploadedFilesTest() throws Exception {
		// Given: one upload the one-page draft service turns into measured fields and icons
		when(sourceFileTextService.extractText("brief.pdf", "raw".getBytes())).thenReturn("Body");
		when(onePageCaseStudyDraftService.draft(List.of(new SourceDocument("brief.pdf", "Body")), List.of()))
				.thenReturn(new OnePageCaseStudyDraft(
						List.of(new OnePageCaseStudyField("challenge", "The challenge", "Challenge & campaign",
								"CPA rose.", OnePageFieldSource.FILES, 600, 5, 6, 2, OnePageFit.SHORT,
								"Add detail")),
						List.of(new OnePageCaseStudyIcon("plan_icon", "Game plan icon", "compass")),
						List.of(new OnePageIconOption("compass", "Compass", "/case-study-icons/compass.png")),
						"brief.pdf"));

		// When: the one-page draft endpoint receives the upload
		ResultActions response = mvc.perform(multipart(ONE_PAGE_DRAFTS_URL)
				.file(new MockMultipartFile("files", "brief.pdf", null, "raw".getBytes()))
				.with(validJwt()));

		// Then: fields, fit, icons, and the library are rendered into the API DTO
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields[0].key").value("challenge"))
				.andExpect(jsonPath("$.fields[0].source").value("FILES"))
				.andExpect(jsonPath("$.fields[0].fit").value("SHORT"))
				.andExpect(jsonPath("$.fields[0].lines").value(2))
				.andExpect(jsonPath("$.icons[0].icon").value("compass"))
				.andExpect(jsonPath("$.iconLibrary[0].imageUrl").value("/case-study-icons/compass.png"))
				.andExpect(jsonPath("$.sourceFileNames").value("brief.pdf"));
	}

	@Test
	void shouldRejectAOnePageDraftRequestWithNoFilesAsBadRequestTest() throws Exception {
		// Given: a multipart request that carries no files part
		// When: the one-page draft endpoint is called
		ResultActions response = mvc.perform(multipart(ONE_PAGE_DRAFTS_URL).with(validJwt()));

		// Then: the upload reader's C002 rejection surfaces as 400
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(onePageCaseStudyDraftService);
	}

	@Test
	void shouldMeasureReviewedOnePageValuesTest() throws Exception {
		// Given: a service that measures the posted values
		when(onePageCaseStudyDraftService.measureFields(List.of(new OnePageFieldValue("challenge", "CPA rose."))))
				.thenReturn(List.of(new OnePageCaseStudyField("challenge", "The challenge", "Challenge & campaign",
						"CPA rose.", OnePageFieldSource.FILES, 600, 5, 6, 1, OnePageFit.SHORT, "Add detail")));

		// When: the fit endpoint receives one value
		ResultActions response = mvc.perform(post(ONE_PAGE_FIT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"fields\":[{\"key\":\"challenge\",\"value\":\"CPA rose.\"}]}"));

		// Then: the measured field comes back with its fit
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].key").value("challenge"))
				.andExpect(jsonPath("$[0].fit").value("SHORT"))
				.andExpect(jsonPath("$[0].note").value("Add detail"));
	}

	@Test
	void shouldRejectAMeasureRequestWithABlankKeyAsBadRequestTest() throws Exception {
		// Given: a field value violating the contract's minLength on key
		// When: it is posted
		ResultActions response = mvc.perform(post(ONE_PAGE_FIT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"fields\":[{\"key\":\"\",\"value\":\"CPA rose.\"}]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(onePageCaseStudyDraftService);
	}

	@Test
	void shouldCreateAOnePageDeckAndForwardTheCallerAndRequestBaseUrlTest() throws Exception {
		// Given: a service that publishes the one-page deck
		CreateOnePageCaseStudyCommand command = new CreateOnePageCaseStudyCommand(
				List.of(new OnePageFieldValue("title_line_1", "Scaling")),
				List.of(new OnePageIconChoice("plan_icon", "compass"), new OnePageIconChoice("stat_1_icon", null)),
				"brief.pdf", "user_123");
		when(caseStudyService.createOnePage(eq(command), eq("http://localhost")))
				.thenReturn(new CaseStudyRecord(9L, "SCALING", null, null, null, null, null, null, null, null,
						"brief.pdf", null, "https://slides/9", "GENERATED", "user_123",
						LocalDateTime.of(2026, 8, 1, 12, 0)));

		// When: reviewed content is posted
		ResultActions response = mvc.perform(post(ONE_PAGE_DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"fields\":[{\"key\":\"title_line_1\",\"value\":\"Scaling\"}],"
						+ "\"icons\":[{\"slot\":\"plan_icon\",\"icon\":\"compass\"},"
						+ "{\"slot\":\"stat_1_icon\",\"icon\":null}],\"sourceFileNames\":\"brief.pdf\"}"));

		// Then: 201 with the record, and the command carries the JWT's user and the request's base URL
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(9))
				.andExpect(jsonPath("$.slidesUrl").value("https://slides/9"));
		ArgumentCaptor<CreateOnePageCaseStudyCommand> captured =
				ArgumentCaptor.forClass(CreateOnePageCaseStudyCommand.class);
		verify(caseStudyService).createOnePage(captured.capture(), eq("http://localhost"));
		assertThat(captured.getValue().createdBy()).isEqualTo("user_123");
		assertThat(captured.getValue().icons()).hasSize(2);
	}

	@Test
	void shouldRejectAOnePageDeckWithAnOverlongValueAsBadRequestTest() throws Exception {
		// Given: a field value one character past the contract's maxLength of 2000
		String value = "x".repeat(2001);

		// When: it is posted
		ResultActions response = mvc.perform(post(ONE_PAGE_DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"fields\":[{\"key\":\"challenge\",\"value\":\"" + value + "\"}],\"icons\":[]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(caseStudyService);
	}
}
