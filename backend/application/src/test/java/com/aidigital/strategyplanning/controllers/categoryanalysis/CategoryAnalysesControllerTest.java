package com.aidigital.strategyplanning.controllers.categoryanalysis;

import com.aidigital.strategyplanning.mappers.categoryanalysis.CategoryAnalysisApiMapperImpl;
import com.aidigital.strategyplanning.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.security.AuthProperties;
import com.aidigital.strategyplanning.security.ClerkJwtClaimsValidator;
import com.aidigital.strategyplanning.security.ClerkPublishableKeyDecoder;
import com.aidigital.strategyplanning.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.strategyplanning.security.SecurityConfig;
import com.aidigital.strategyplanning.security.SecurityProperties;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateInDepthDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the Standard deck endpoints on {@link CategoryAnalysesController}:
 * GET /standard-connections, POST /standard-drafts, POST /standard-decks.
 *
 * <p>Verifies auth is enforced, status codes match the contract, and the real
 * MapStruct mapper renders service records into the API DTO shape. The service
 * layer itself is mocked — its logic is covered by unit tests in the service module.
 */
@WebMvcTest(controllers = CategoryAnalysesController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		CategoryAnalysisApiMapperImpl.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class CategoryAnalysesControllerTest {

	private static final String CONNECTIONS_URL = "/api/v1/category-analyses/standard-connections";
	private static final String DRAFTS_URL = "/api/v1/category-analyses/standard-drafts";
	private static final String DECKS_URL = "/api/v1/category-analyses/standard-decks";
	private static final String SLIDE_REDRAFT_URL = "/api/v1/category-analyses/standard-drafts/slide";
	private static final String ANALYSES_URL = "/api/v1/category-analyses";
	private static final String IN_DEPTH_SLIDES_URL = "/api/v1/category-analyses/in-depth-slides";
	private static final String IN_DEPTH_DRAFTS_URL = "/api/v1/category-analyses/in-depth-drafts";
	private static final String IN_DEPTH_SLIDE_REDRAFT_URL = "/api/v1/category-analyses/in-depth-drafts/slide";
	private static final String IN_DEPTH_DECKS_URL = "/api/v1/category-analyses/in-depth-decks";
	private static final int FOCUS_NOTES_MAX_LENGTH = 10_000;

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private CategoryAnalysisService categoryAnalysisService;

	private static org.springframework.test.web.servlet.request.RequestPostProcessor validJwt() {
		return jwt().jwt(j -> j
				.subject("user_123")
				.claim("user_id", "user_123")
				.claim("email", "alice@aidigital.com")
				.claim("full_name", "Alice Example"));
	}

	// --- GET /standard-connections ---

	@Test
	void shouldRejectUnauthenticatedConnectionsRequestTest() throws Exception {
		ResultActions response = mvc.perform(get(CONNECTIONS_URL));
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnConnectionStatusForAuthenticatedUserTest() throws Exception {
		when(categoryAnalysisService.getStandardConnections("user_123"))
				.thenReturn(new StandardConnections(true, false, GoogleConnectionStatus.NOT_CONNECTED));

		ResultActions response = mvc.perform(get(CONNECTIONS_URL).with(validJwt()));

		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiConnected").value(true))
				.andExpect(jsonPath("$.googleConnected").value(false))
				.andExpect(jsonPath("$.googleStatus").value("NOT_CONNECTED"));
	}

	// --- POST /standard-drafts ---

	@Test
	void shouldRejectUnauthenticatedDraftRequestTest() throws Exception {
		ResultActions response = mvc.perform(post(DRAFTS_URL)
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\"}"));
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnDraftFieldsForAuthenticatedUserTest() throws Exception {
		StandardDraft draft = new StandardDraft(
				List.of(new StandardDraftField("trends_headline", "Trends", 3, "Growth is accelerating")),
				new DraftAlignment("Premium beverage brand", true, true, "The deck fits Acme's business."));
		when(categoryAnalysisService.draftStandard(
				"Beverages", "Acme", "focus on premium", "https://acme.com", "premium repositioning"))
				.thenReturn(draft);

		ResultActions response = mvc.perform(post(DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\",\"storyTheme\":\"premium repositioning\","
						+ "\"guidanceNotes\":\"focus on premium\"}"));

		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields[0].key").value("trends_headline"))
				.andExpect(jsonPath("$.fields[0].label").value("Trends"))
				.andExpect(jsonPath("$.fields[0].slideNumber").value(3))
				.andExpect(jsonPath("$.fields[0].value").value("Growth is accelerating"))
				.andExpect(jsonPath("$.alignment.clientBusinessFocus").value("Premium beverage brand"))
				.andExpect(jsonPath("$.alignment.confirmed").value(true))
				.andExpect(jsonPath("$.alignment.matches").value(true))
				.andExpect(jsonPath("$.alignment.message").value("The deck fits Acme's business."));
	}

	// --- POST /standard-decks ---

	@Test
	void shouldRejectUnauthenticatedDeckRequestTest() throws Exception {
		ResultActions response = mvc.perform(post(DECKS_URL)
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\"}"));
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldCreateDeckAndReturnSlidesUrlWithCreatedStatusTest() throws Exception {
		CategoryAnalysisRecord record = new CategoryAnalysisRecord(
				42L,
				"Category Analysis — Beverages — Acme",
				"Beverages",
				null, null, null, null, null, null,
				"https://docs.google.com/presentation/d/deck123/edit",
				"STANDARD",
				"SUBMITTED",
				"user_123",
				LocalDateTime.of(2026, 7, 10, 12, 0));
		when(categoryAnalysisService.createStandardDeck(any(CreateStandardDeckCommand.class), anyString()))
				.thenReturn(record);

		ResultActions response = mvc.perform(post(DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"fields\":[{\"key\":\"trends_headline\",\"value\":\"Growth\"}]}"));

		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(42))
				.andExpect(jsonPath("$.slidesUrl")
						.value("https://docs.google.com/presentation/d/deck123/edit"))
				.andExpect(jsonPath("$.templateKind").value("STANDARD"))
				.andExpect(jsonPath("$.status").value("SUBMITTED"));
	}

	@Test
	void shouldMapAuthenticatedUserIntoDeckCommandTest() throws Exception {
		CategoryAnalysisRecord record = new CategoryAnalysisRecord(
				1L, "title", "Beverages",
				null, null, null, null, null, null,
				"https://slides", "STANDARD", "SUBMITTED", "user_123",
				LocalDateTime.of(2026, 7, 10, 12, 0));
		when(categoryAnalysisService.createStandardDeck(any(CreateStandardDeckCommand.class), anyString()))
				.thenReturn(record);

		mvc.perform(post(DECKS_URL)
						.with(validJwt())
						.contentType(APPLICATION_JSON)
						.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
								+ "\"fields\":[{\"key\":\"trends_headline\",\"value\":\"Growth\"}]}"))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateStandardDeckCommand> captor =
				ArgumentCaptor.forClass(CreateStandardDeckCommand.class);
		verify(categoryAnalysisService).createStandardDeck(captor.capture(), anyString());
		CreateStandardDeckCommand command = captor.getValue();
		assertThat(command.createdBy()).isEqualTo("user_123");
		assertThat(command.category()).isEqualTo("Beverages");
		assertThat(command.clientName()).isEqualTo("Acme");
		assertThat(command.fields()).hasSize(1);
		assertThat(command.fields().get(0).key()).isEqualTo("trends_headline");
	}

	// --- Negative 400s promised by the OpenAPI input constraints ---

	@Test
	void shouldRejectAnalysisCreateWithBlankTitleAsBadRequestTest() throws Exception {
		// Given: a payload violating the contract's minLength on title
		// When: it is posted
		ResultActions response = mvc.perform(post(ANALYSES_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"\",\"category\":\"Beverages\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectNonNumericAnalysisIdAsBadRequestTest() throws Exception {
		// Given: an id that cannot bind to the contract's int64 path parameter
		// When: the detail endpoint is called with it
		ResultActions response = mvc.perform(get(ANALYSES_URL + "/not-a-number").with(validJwt()));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectDraftRequestWithBlankCategoryAsBadRequestTest() throws Exception {
		// Given: a brief violating the contract's minLength on category
		// When: it is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectSlideRedraftOutsideTheFiveSlideDeckAsBadRequestTest() throws Exception {
		// Given: a slide number past the contract's maximum of 5
		// When: the redraft endpoint is called
		ResultActions response = mvc.perform(post(SLIDE_REDRAFT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"slideNumber\":6,\"currentFields\":[]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectDeckRequestWithBlankCategoryAsBadRequestTest() throws Exception {
		// Given: reviewed values submitted without the required category
		// When: the deck endpoint is called
		ResultActions response = mvc.perform(post(DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"\",\"clientName\":\"Acme\","
						+ "\"fields\":[{\"key\":\"trends_headline\",\"value\":\"Growth\"}]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldAcceptDraftRequestWithFocusNotesAtTheContractLimitTest() throws Exception {
		// Given: a brief whose focus notes fill the contract's character budget exactly
		String notes = "n".repeat(FOCUS_NOTES_MAX_LENGTH);
		StandardDraft draft = new StandardDraft(
				List.of(new StandardDraftField("trends_headline", "Trends", 2, "Growth is accelerating")),
				new DraftAlignment("Premium beverage brand", true, true, "The deck fits Acme's business."));
		when(categoryAnalysisService.draftStandard(
				"Beverages", "Acme", notes, "https://acme.com", null))
				.thenReturn(draft);

		// When: it is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\",\"guidanceNotes\":\"" + notes + "\"}"));

		// Then: the long brief reaches the service and is drafted
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields[0].key").value("trends_headline"));
		verify(categoryAnalysisService)
				.draftStandard("Beverages", "Acme", notes, "https://acme.com", null);
	}

	@Test
	void shouldRejectDraftRequestWithFocusNotesOverTheContractLimitTest() throws Exception {
		// Given: focus notes one character past the contract's maxLength
		String notes = "n".repeat(FOCUS_NOTES_MAX_LENGTH + 1);

		// When: the brief is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\",\"guidanceNotes\":\"" + notes + "\"}"));

		// Then: it is rejected with a field-level reason the UI can render
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("guidanceNotes"))
				.andExpect(jsonPath("$.errors[0].error").value("size must be between 0 and 10000"));
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectSlideRedraftWithFocusNotesOverTheContractLimitTest() throws Exception {
		// Given: a redraft carrying the same brief notes, one character past the limit
		String notes = "n".repeat(FOCUS_NOTES_MAX_LENGTH + 1);

		// When: the redraft endpoint is called
		ResultActions response = mvc.perform(post(SLIDE_REDRAFT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"slideNumber\":2,\"currentFields\":[],"
						+ "\"guidanceNotes\":\"" + notes + "\"}"));

		// Then: it is rejected without reaching the service
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("guidanceNotes"));
		verifyNoInteractions(categoryAnalysisService);
	}

	// --- In-Depth template ---

	@Test
	void shouldRejectUnauthenticatedInDepthSlidesRequestTest() throws Exception {
		// Given-When: the catalog is requested without a token
		ResultActions response = mvc.perform(get(IN_DEPTH_SLIDES_URL));

		// Then: it is refused
		response.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldListInDepthSlidesForAuthenticatedUserTest() throws Exception {
		// Given: a two-slide catalog
		when(categoryAnalysisService.listInDepthSlides()).thenReturn(List.of(
				new InDepthSlideSpec(1, "Cover", true), new InDepthSlideSpec(2, "Market Trend", false)));

		// When: the catalog is requested
		ResultActions response = mvc.perform(get(IN_DEPTH_SLIDES_URL).with(validJwt()));

		// Then: the mapper renders every slide in order
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].slideNumber").value(1))
				.andExpect(jsonPath("$[0].title").value("Cover"))
				.andExpect(jsonPath("$[0].defaultChecked").value(true))
				.andExpect(jsonPath("$[1].slideNumber").value(2))
				.andExpect(jsonPath("$[1].defaultChecked").value(false));
	}

	@Test
	void shouldReturnInDepthDraftFieldsForAuthenticatedUserTest() throws Exception {
		// Given: a one-field draft for the checked slides
		InDepthDraft draft = new InDepthDraft(
				List.of(new InDepthDraftField("highlights_headline", "Headline", List.of(3, 4), "GROWTH")),
				new DraftAlignment("Premium beverage brand", true, true, "The deck fits Acme's business."));
		when(categoryAnalysisService.draftInDepth("Beverages", "Acme", "focus on premium", "https://acme.com",
				List.of(3, 4))).thenReturn(draft);

		// When: the brief is posted
		ResultActions response = mvc.perform(post(IN_DEPTH_DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\",\"guidanceNotes\":\"focus on premium\","
						+ "\"selectedSlides\":[3,4]}"));

		// Then: the field carries both slide numbers and the alignment verdict is rendered
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields[0].key").value("highlights_headline"))
				.andExpect(jsonPath("$.fields[0].slideNumbers[0]").value(3))
				.andExpect(jsonPath("$.fields[0].slideNumbers[1]").value(4))
				.andExpect(jsonPath("$.fields[0].value").value("GROWTH"))
				.andExpect(jsonPath("$.alignment.confirmed").value(true))
				.andExpect(jsonPath("$.alignment.message").value("The deck fits Acme's business."));
	}

	@Test
	void shouldPassCurrentFieldsIntoTheInDepthSlideRedraftTest() throws Exception {
		// Given: a redraft of slide 2 with one current field
		InDepthDraft draft = new InDepthDraft(
				List.of(new InDepthDraftField("trend_headline", "Headline", List.of(2), "NEW")), null);
		when(categoryAnalysisService.redraftInDepthSlide("Beverages", "Acme", null, 2, "punchier",
				List.of(new InDepthFieldValue("trend_headline", "OLD")))).thenReturn(draft);

		// When: the redraft is posted
		ResultActions response = mvc.perform(post(IN_DEPTH_SLIDE_REDRAFT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\",\"slideNumber\":2,"
						+ "\"slideNote\":\"punchier\",\"currentFields\":[{\"key\":\"trend_headline\",\"value\":\"OLD\"}]}"));

		// Then: the redrafted field is returned and no alignment is attached
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fields[0].value").value("NEW"))
				.andExpect(jsonPath("$.alignment").doesNotExist());
	}

	@Test
	void shouldCreateInDepthDeckAndMapTheCheckedSlidesIntoTheCommandTest() throws Exception {
		// Given: a deck record for the created deck
		CategoryAnalysisRecord record = new CategoryAnalysisRecord(
				7L, "Category Analysis — Beverages — Acme", "Beverages",
				null, null, null, null, null, null,
				"https://docs.google.com/presentation/d/deck_in_depth/edit", "IN_DEPTH", "SUBMITTED", "user_123",
				LocalDateTime.of(2026, 7, 10, 12, 0));
		when(categoryAnalysisService.createInDepthDeck(any(CreateInDepthDeckCommand.class), anyString()))
				.thenReturn(record);

		// When: the deck request is posted
		ResultActions response = mvc.perform(post(IN_DEPTH_DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\",\"selectedSlides\":[1,8],"
						+ "\"fields\":[{\"key\":\"drivers_headline\",\"value\":\"WHY NOW\"}]}"));

		// Then: 201 with the deck URL, and the command carries the user, slides, and fields
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.templateKind").value("IN_DEPTH"))
				.andExpect(jsonPath("$.slidesUrl").value("https://docs.google.com/presentation/d/deck_in_depth/edit"));
		ArgumentCaptor<CreateInDepthDeckCommand> captor = ArgumentCaptor.forClass(CreateInDepthDeckCommand.class);
		ArgumentCaptor<String> baseUrl = ArgumentCaptor.forClass(String.class);
		verify(categoryAnalysisService).createInDepthDeck(captor.capture(), baseUrl.capture());
		assertThat(captor.getValue().createdBy()).isEqualTo("user_123");
		assertThat(captor.getValue().selectedSlides()).containsExactly(1, 8);
		assertThat(captor.getValue().fields()).containsExactly(new InDepthFieldValue("drivers_headline", "WHY NOW"));
		assertThat(baseUrl.getValue()).isEqualTo("http://localhost");
	}

	@Test
	void shouldRejectInDepthDraftWithoutAnyCheckedSlideAsBadRequestTest() throws Exception {
		// Given: a brief violating the contract's minItems on selectedSlides
		// When: it is posted
		ResultActions response = mvc.perform(post(IN_DEPTH_DRAFTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"clientWebsite\":\"https://acme.com\",\"selectedSlides\":[]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("selectedSlides"));
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectInDepthSlideRedraftBeyondTheCatalogAsBadRequestTest() throws Exception {
		// Given: a slide number past the contract's maximum of 24
		// When: the redraft endpoint is called
		ResultActions response = mvc.perform(post(IN_DEPTH_SLIDE_REDRAFT_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\","
						+ "\"slideNumber\":25,\"currentFields\":[]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("slideNumber"));
		verifyNoInteractions(categoryAnalysisService);
	}

	@Test
	void shouldRejectInDepthDeckWithASlideNumberOutsideTheCatalogAsBadRequestTest() throws Exception {
		// Given: a checked slide number below the contract's minimum of 1
		// When: the deck endpoint is called
		ResultActions response = mvc.perform(post(IN_DEPTH_DECKS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Beverages\",\"clientName\":\"Acme\",\"selectedSlides\":[0],"
						+ "\"fields\":[]}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(categoryAnalysisService);
	}
}
