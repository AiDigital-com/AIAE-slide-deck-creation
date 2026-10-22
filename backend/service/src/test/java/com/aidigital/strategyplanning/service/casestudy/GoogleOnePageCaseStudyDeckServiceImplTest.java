package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDeckRequestAssembler;
import com.aidigital.strategyplanning.service.casestudy.services.impl.GoogleOnePageCaseStudyDeckServiceImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.TokenReplacementRequestFactory;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the One-Page deck flow: copy, fill, then best-effort icon swap.
 */
class GoogleOnePageCaseStudyDeckServiceImplTest {

	private static final String TOKEN = "ya29.token";
	private static final String COPY_ACTION = "copy the one-page case study template into your Google Drive";
	private static final String FILL_ACTION = "fill the one-page case study deck";
	private static final String READ_ACTION = "read the one-page case study deck";
	private static final String SWAP_ACTION = "swap the one-page case study icons";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private CaseStudyProperties properties() {
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setClerkSecretKey("sk_test");
		properties.setOnePageTemplatePresentationId("one_page_template");
		return properties;
	}

	@Test
	void shouldCopyFillAndSwapIconsThenReturnTheDeckUrlTest() {
		// Given: a granted user, a Drive that copies, and one chosen icon
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES)))
				.thenReturn(TOKEN);
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);
		when(gateway.copyTemplate(TOKEN, "one_page_template", "Deck", COPY_ACTION)).thenReturn("deck_1");
		ObjectNode presentation = objectMapper.createObjectNode().put("presentationId", "deck_1");
		when(gateway.readPresentation(TOKEN, "deck_1", READ_ACTION)).thenReturn(presentation);
		TokenReplacementRequestFactory textRequests = mock(TokenReplacementRequestFactory.class);
		ArrayNode fill = objectMapper.createArrayNode().add("fill");
		Map<String, String> tokens = Map.of("challenge", "CPA rose.");
		when(textRequests.buildReplaceTextRequests(tokens)).thenReturn(fill);
		OnePageDeckRequestAssembler assembler = mock(OnePageDeckRequestAssembler.class);
		ArrayNode swap = objectMapper.createArrayNode().add("swap");
		Map<String, String> icons = Map.of("tactic_1_icon", "https://app/case-study-icons/target.png");
		when(assembler.buildReplaceImageRequests(presentation, icons)).thenReturn(swap);

		// When: the deck is created
		String url = new GoogleOnePageCaseStudyDeckServiceImpl(properties(), grants, gateway, textRequests, assembler)
				.createDeck("user_1", "Deck", tokens, icons);

		// Then: the user gets an editable link and the three Google steps ran in order
		assertThat(url).isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		verify(gateway).fillPresentation(TOKEN, "deck_1", fill, FILL_ACTION);
		verify(gateway).fillPresentation(TOKEN, "deck_1", swap, SWAP_ACTION);
	}

	@Test
	void shouldSkipTheIconSwapWhenNoIconWasChosenTest() {
		// Given: a granted user and no icon URLs
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken(eq("sk_test"), eq("user_1"), eq(Set.of(GoogleOAuthScope.DRIVE,
				GoogleOAuthScope.SLIDES)))).thenReturn(TOKEN);
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);
		when(gateway.copyTemplate(TOKEN, "one_page_template", "Deck", COPY_ACTION)).thenReturn("deck_1");
		TokenReplacementRequestFactory textRequests = mock(TokenReplacementRequestFactory.class);
		when(textRequests.buildReplaceTextRequests(Map.of())).thenReturn(objectMapper.createArrayNode());

		// When: the deck is created
		String url = new GoogleOnePageCaseStudyDeckServiceImpl(properties(), grants, gateway, textRequests,
				mock(OnePageDeckRequestAssembler.class)).createDeck("user_1", "Deck", Map.of(), Map.of());

		// Then: the deck is never read back
		assertThat(url).endsWith("/deck_1/edit");
		verify(gateway, never()).readPresentation(anyString(), anyString(), anyString());
	}

	@Test
	void shouldKeepTheTemplateIconsWhenTheSwapFailsTest() {
		// Given: a filled deck Google refuses to read back
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken(eq("sk_test"), eq("user_1"), eq(Set.of(GoogleOAuthScope.DRIVE,
				GoogleOAuthScope.SLIDES)))).thenReturn(TOKEN);
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);
		when(gateway.copyTemplate(TOKEN, "one_page_template", "Deck", COPY_ACTION)).thenReturn("deck_1");
		when(gateway.readPresentation(TOKEN, "deck_1", READ_ACTION))
				.thenThrow(new AppException(ErrorReason.C003, "Could not read the deck"));
		TokenReplacementRequestFactory textRequests = mock(TokenReplacementRequestFactory.class);
		when(textRequests.buildReplaceTextRequests(Map.of())).thenReturn(objectMapper.createArrayNode());

		// When: the deck is created with an icon chosen
		String url = new GoogleOnePageCaseStudyDeckServiceImpl(properties(), grants, gateway, textRequests,
				mock(OnePageDeckRequestAssembler.class))
				.createDeck("user_1", "Deck", Map.of(), Map.of("plan_icon", "https://app/icons/compass.png"));

		// Then: the user still gets their deck, with all its text
		assertThat(url).endsWith("/deck_1/edit");
	}

	@Test
	void shouldNotTouchGoogleWhenTheUserHasNoGrantTest() {
		// Given: a grant lookup that rejects the user
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken(eq("sk_test"), eq("user_1"), eq(Set.of(GoogleOAuthScope.DRIVE,
				GoogleOAuthScope.SLIDES)))).thenThrow(new AppException(ErrorReason.C003, "Not signed in with Google"));
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);

		// When-Then: the rejection surfaces and no copy is made
		assertThatThrownBy(() -> new GoogleOnePageCaseStudyDeckServiceImpl(properties(), grants, gateway,
				mock(TokenReplacementRequestFactory.class), mock(OnePageDeckRequestAssembler.class))
				.createDeck("user_1", "Deck", Map.of(), Map.of()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google");
		verify(gateway, never()).copyTemplate(anyString(), anyString(), anyString(), anyString());
	}
}
