package com.aidigital.strategyplanning.controllers.rndrequest;

import com.aidigital.strategyplanning.error.mapper.GlobalExceptionResponseHelperImpl;
import com.aidigital.strategyplanning.mappers.rndrequest.RndRequestApiMapperImpl;
import com.aidigital.strategyplanning.security.AppUserFactory;
import com.aidigital.strategyplanning.security.AuthProperties;
import com.aidigital.strategyplanning.security.ClerkJwtClaimsValidator;
import com.aidigital.strategyplanning.security.ClerkPublishableKeyDecoder;
import com.aidigital.strategyplanning.security.CompanyEmailDomainAuthorizationManager;
import com.aidigital.strategyplanning.security.SecurityConfig;
import com.aidigital.strategyplanning.security.SecurityProperties;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;
import com.aidigital.strategyplanning.service.rndrequest.services.RndRequestService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Web-layer tests for {@link RndRequestsController}.
 *
 * <p>Covers auth enforcement, the contract's status codes, the arguments the controller forwards
 * to the service, and the 400 responses the OpenAPI input constraints promise.
 */
@WebMvcTest(controllers = RndRequestsController.class)
@EnableConfigurationProperties({AuthProperties.class, SecurityProperties.class})
@Import({
		SecurityConfig.class,
		AppUserFactory.class,
		RndRequestApiMapperImpl.class,
		ClerkJwtClaimsValidator.class,
		ClerkPublishableKeyDecoder.class,
		CompanyEmailDomainAuthorizationManager.class,
		GlobalExceptionResponseHelperImpl.class,
		CurrentTimeImpl.class
})
class RndRequestsControllerTest {

	private static final String RND_REQUESTS_URL = "/api/v1/rnd-requests";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private RndRequestService rndRequestService;

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
		ResultActions response = mvc.perform(get(RND_REQUESTS_URL));

		// Then: the request is refused before it reaches the service
		response.andExpect(status().isUnauthorized());
		verifyNoInteractions(rndRequestService);
	}

	@Test
	void shouldListRequestsOwnedByTheCallerTest() throws Exception {
		// Given: one stored request for the caller
		when(rndRequestService.listByUser("user_123")).thenReturn(List.of(
				new RndRequestRecord(11L, "Inventory sync", "Growth — EMEA", null, null,
						new BigDecimal("150000.00"), "ESCALATE_TO_RND", null, null, "SUBMITTED",
						"user_123", LocalDateTime.of(2026, 8, 3, 8, 0))));

		// When: the list endpoint is called
		ResultActions response = mvc.perform(get(RND_REQUESTS_URL).with(validJwt()));

		// Then: the summary payload is returned, scoped to the caller
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(11))
				.andExpect(jsonPath("$[0].decision").value("ESCALATE_TO_RND"))
				.andExpect(jsonPath("$[0].buyAmount").value(150000.00));
		verify(rndRequestService).listByUser("user_123");
	}

	@Test
	void shouldCreateRequestAndForwardTheCallerAsCreatedByTest() throws Exception {
		// Given: a service that triages the submitted request
		when(rndRequestService.create(new CreateRndRequestCommand(
				"Inventory sync", "Growth — EMEA", null, null, new BigDecimal("150000"), "user_123")))
				.thenReturn(new RndRequestRecord(11L, "Inventory sync", "Growth — EMEA", null, null,
						new BigDecimal("150000"), "ESCALATE_TO_RND", null, "Escalation summary",
						"SUBMITTED", "user_123", LocalDateTime.of(2026, 8, 3, 8, 0)));

		// When: a valid payload is posted
		ResultActions response = mvc.perform(post(RND_REQUESTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Inventory sync\",\"requesterTeam\":\"Growth — EMEA\","
						+ "\"buyAmount\":150000}"));

		// Then: the contract's 201 comes back and createdBy is the JWT's user
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(11))
				.andExpect(jsonPath("$.decision").value("ESCALATE_TO_RND"))
				.andExpect(jsonPath("$.responseDraft").value("Escalation summary"));
		ArgumentCaptor<CreateRndRequestCommand> command =
				ArgumentCaptor.forClass(CreateRndRequestCommand.class);
		verify(rndRequestService).create(command.capture());
		assertThat(command.getValue().createdBy()).isEqualTo("user_123");
	}

	@Test
	void shouldRejectCreateWithNegativeBuyAmountAsBadRequestTest() throws Exception {
		// Given: a buy amount below the contract's minimum of zero
		// When: it is posted
		ResultActions response = mvc.perform(post(RND_REQUESTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Inventory sync\",\"buyAmount\":-1}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rndRequestService);
	}

	@Test
	void shouldRejectCreateWithOverlongRequestDetailsAsBadRequestTest() throws Exception {
		// Given: request details one character past the contract's maxLength of 5000
		String details = "x".repeat(5001);

		// When: it is posted
		ResultActions response = mvc.perform(post(RND_REQUESTS_URL)
				.with(validJwt())
				.contentType(APPLICATION_JSON)
				.content("{\"title\":\"Inventory sync\",\"buyAmount\":1000,"
						+ "\"requestDetails\":\"" + details + "\"}"));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rndRequestService);
	}

	@Test
	void shouldRejectNonNumericRequestIdAsBadRequestTest() throws Exception {
		// Given: an id that cannot bind to the contract's int64 path parameter
		// When: the detail endpoint is called with it
		ResultActions response = mvc.perform(get(RND_REQUESTS_URL + "/not-a-number").with(validJwt()));

		// Then: the request is rejected without reaching the service
		response.andExpect(status().isBadRequest());
		verifyNoInteractions(rndRequestService);
	}

	@Test
	void shouldReturnOneRequestForTheCallerTest() throws Exception {
		// Given: a stored request owned by the caller
		when(rndRequestService.getById(11L, "user_123"))
				.thenReturn(new RndRequestRecord(11L, "Inventory sync", "Growth — EMEA",
						"Full details", "Capability notes", new BigDecimal("40000"), "WORKAROUND",
						"Slite: partial match", "Workaround response", "SUBMITTED", "user_123",
						LocalDateTime.of(2026, 8, 3, 8, 0)));

		// When: the detail endpoint is called
		ResultActions response = mvc.perform(get(RND_REQUESTS_URL + "/11").with(validJwt()));

		// Then: the record is returned and the caller's id scoped the lookup
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.decision").value("WORKAROUND"))
				.andExpect(jsonPath("$.capabilitySummary").value("Slite: partial match"));
		verify(rndRequestService).getById(11L, "user_123");
	}
}
