package com.aidigital.strategyplanning.controllers.onepager;

import com.aidigital.strategyplanning.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.strategyplanning.mappers.categoryanalysis.CategoryAnalysisApiMapperImpl;
import com.aidigital.strategyplanning.mappers.onepager.OnePagerApiMapperImpl;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.security.AuthProperties;
import com.aidigital.strategyplanning.security.ClerkJwtClaimsValidator;
import com.aidigital.strategyplanning.security.ClerkPublishableKeyDecoder;
import com.aidigital.strategyplanning.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.strategyplanning.security.SecurityConfig;
import com.aidigital.strategyplanning.security.SecurityProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
 * Web-layer tests for {@link OnePagersController}: auth enforcement, the contract's status codes,
 * the arguments forwarded to the service, and the 400 responses the input constraints promise.
 */
@WebMvcTest(controllers = OnePagersController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		OnePagerApiMapperImpl.class,
		CategoryAnalysisApiMapperImpl.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class OnePagersControllerTest {

	private static final String CONNECTIONS_URL = "/api/v1/one-pagers/connections";
	private static final String DRAFTS_URL = "/api/v1/one-pagers/drafts";
	private static final String DECKS_URL = "/api/v1/one-pagers/decks";
	private static final String DRAFT_CONTENT = """
			{"topicPill":"AUTOMOTIVE","mainTitle":["SOLVING"],"supportingCopy":"Copy","moduleOrder":["KPIS"],
			"processSectionLabel":null,"processSteps":[],"solutionsSectionLabel":null,"solutions":[],
			"solutionsVisualLabel":null,"kpiSectionLabel":"SIGNALS","kpis":[{"value":"30%","label":"LIFT"}],
			"kpiDisclaimer":"Based on data.","imageTextSectionLabel":null,"imageTextBody":null,"keyTakeaway":null,
			"impactStats":[],"capabilityCards":[],"spotlightCards":[],"insightCards":[],"milestoneHeadline":null,
			"milestones":[]}""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private OnePagerService onePagerService;

	private static RequestPostProcessor validJwt() {
		return jwt().jwt(j -> j
				.subject("user_123")
				.claim("user_id", "user_123")
				.claim("email", "alice@aidigital.com")
				.claim("full_name", "Alice Example"));
	}

	private static OnePagerDraft kpiDraft() {
		return new OnePagerDraft("AUTOMOTIVE", List.of("SOLVING"), "Copy", List.of(OnePagerModuleKey.KPIS), null,
				List.of(), null, List.of(), null, "SIGNALS", List.of(new OnePagerKpi("30%", "LIFT")),
				"Based on data.", null, null, null, List.of(), List.of(), List.of(), List.of(), null, List.of());
	}

	@Test
	void shouldRejectUnauthenticatedRequestsTest() throws Exception {
		// Given: no bearer token
		// When: the connections endpoint is called
		ResultActions response = mvc.perform(get(CONNECTIONS_URL));

		// Then: the request is refused before it reaches the service
		response.andExpect(status().isUnauthorized());
		verifyNoInteractions(onePagerService);
	}

	@Test
	void shouldReportConnectionsForTheCallerTest() throws Exception {
		// Given: AI connected, Google missing scopes
		when(onePagerService.getConnections("user_123"))
				.thenReturn(new StandardConnections(true, false, GoogleConnectionStatus.MISSING_SCOPES));

		// When: connections are requested
		ResultActions response = mvc.perform(get(CONNECTIONS_URL).with(validJwt()));

		// Then: the flags and the detailed Google state are returned
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiConnected").value(true))
				.andExpect(jsonPath("$.googleConnected").value(false))
				.andExpect(jsonPath("$.googleStatus").value("MISSING_SCOPES"));
	}

	@Test
	void shouldDraftFromTheBriefInModuleOrderTest() throws Exception {
		// Given: a service that drafts
		when(onePagerService.draft(any(OnePagerBrief.class))).thenReturn(kpiDraft());

		// When: a brief with two modules is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"guidanceNotes\":\"Sell cars.\","
						+ "\"moduleOrder\":[\"KPIS\",\"TRACKER\"]}"));

		// Then: the draft is returned and the brief kept the modules in posted order
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.topicPill").value("AUTOMOTIVE"))
				.andExpect(jsonPath("$.kpis[0].value").value("30%"))
				.andExpect(jsonPath("$.moduleOrder[0]").value("KPIS"))
				.andExpect(jsonPath("$.processSectionLabel").doesNotExist());
		ArgumentCaptor<OnePagerBrief> brief = ArgumentCaptor.forClass(OnePagerBrief.class);
		verify(onePagerService).draft(brief.capture());
		assertThat(brief.getValue()).isEqualTo(new OnePagerBrief("Automotive", "Acme", "Sell cars.",
				List.of(OnePagerModuleKey.KPIS, OnePagerModuleKey.TRACKER)));
	}

	@Test
	void shouldRejectADraftRequestWithABlankBriefTest() throws Exception {
		// Given: a brief whose guidance notes are blank and whose module key is unknown
		// When: it is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"guidanceNotes\":\"\","
						+ "\"moduleOrder\":[\"KPIS\"]}"));

		// Then: validation refuses it before the service is called
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(onePagerService);
	}

	@Test
	void shouldSurfaceADraftingFailureAsBadGatewayTest() throws Exception {
		// Given: a service whose AI engine is not connected
		when(onePagerService.draft(any(OnePagerBrief.class)))
				.thenThrow(new AppException(ErrorReason.C003, "AI drafting engine is not connected"));

		// When: a valid brief is posted
		ResultActions response = mvc.perform(post(DRAFTS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"guidanceNotes\":\"Brief\","
						+ "\"moduleOrder\":[]}"));

		// Then: the external failure is reported with its code
		response.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("C003"));
	}

	@Test
	void shouldCreateTheDeckForTheCallerTest() throws Exception {
		// Given: a service that saves the page
		when(onePagerService.createDeck(any(CreateOnePagerCommand.class)))
				.thenReturn(new OnePagerDeck("One Page Generator — Automotive — Acme",
						"https://docs.google.com/presentation/d/new_1/edit"));

		// When: the reviewed content is posted
		ResultActions response = mvc.perform(post(DECKS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"shellVariant\":\"TOP_BAR\","
						+ "\"content\":" + DRAFT_CONTENT + "}"));

		// Then: 201 with the link, and the command carried the caller, shell, and content
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("One Page Generator — Automotive — Acme"))
				.andExpect(jsonPath("$.slidesUrl").value("https://docs.google.com/presentation/d/new_1/edit"));
		ArgumentCaptor<CreateOnePagerCommand> command = ArgumentCaptor.forClass(CreateOnePagerCommand.class);
		verify(onePagerService).createDeck(command.capture());
		assertThat(command.getValue().createdBy()).isEqualTo("user_123");
		assertThat(command.getValue().shellVariant()).isEqualTo(OnePagerShell.TOP_BAR);
		assertThat(command.getValue().category()).isEqualTo("Automotive");
		assertThat(command.getValue().content()).isEqualTo(kpiDraft());
	}

	@Test
	void shouldRejectADeckRequestWithoutContentTest() throws Exception {
		// Given: a request that names a shell but carries no content
		// When: it is posted
		ResultActions response = mvc.perform(post(DECKS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"shellVariant\":\"TOP_BAR\"}"));

		// Then: validation refuses it before the service is called
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(onePagerService);
	}

	@Test
	void shouldRejectADeckRequestWhoseTitleLineIsTooLongTest() throws Exception {
		// Given: content whose main title line exceeds its budget
		String longLine = "X".repeat(121);
		// When: it is posted
		ResultActions response = mvc.perform(post(DECKS_URL).with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"category\":\"Automotive\",\"clientName\":\"Acme\",\"shellVariant\":\"BLUE_HEADER\","
						+ "\"content\":" + DRAFT_CONTENT.replace("\"SOLVING\"", "\"" + longLine + "\"") + "}"));

		// Then: the nested constraint is enforced
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(onePagerService);
	}
}
