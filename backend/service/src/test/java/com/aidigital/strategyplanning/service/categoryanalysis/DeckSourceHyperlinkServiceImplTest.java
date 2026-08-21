package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SourceLinkService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.DeckSourceHyperlinkServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for turning the deck's source citations into real hyperlinks.
 *
 * <p>Hyperlinks are a finishing touch on an already-published deck, so every early exit here
 * exists to avoid spending Google calls on nothing, and the final failure is swallowed: a deck
 * with plain-text sources is still a usable deck.
 */
class DeckSourceHyperlinkServiceImplTest {

	/**
	 * Returns the first token key that carries source citations.
	 *
	 * @return a {@code *_sources} token key from the standard template
	 */
	private String sourcesTokenKey() {
		return StandardTemplateTokens.TOKENS.stream()
				.map(StandardTemplateTokens.TokenSpec::key)
				.filter(key -> key.endsWith("_sources"))
				.findFirst()
				.orElseThrow();
	}

	@Test
	void shouldNotCallGoogleWhenNoSourcesWereEnteredTest() {
		// Given: token values that contain no source citations at all
		SourceLinkService linkService = mock(SourceLinkService.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		DeckSourceHyperlinkServiceImpl service =
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway);

		// When: hyperlinks are applied
		service.applySourceHyperlinks("token", "deck_1", Map.of("headline", "Pets are premium"));

		// Then: nothing is resolved and no Google call is spent
		verifyNoInteractions(linkService, requestFactory, gateway);
	}

	@Test
	void shouldNotCallGoogleWhenTheSourceTextResolvesToNoLinksTest() {
		// Given: sources were entered but none of them resolved to a URL
		SourceLinkService linkService = mock(SourceLinkService.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		String key = sourcesTokenKey();
		when(linkService.resolveSourceLinks(Map.of(key, "Statista 2026"))).thenReturn(Map.of());
		DeckSourceHyperlinkServiceImpl service =
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway);

		// When: hyperlinks are applied
		service.applySourceHyperlinks("token", "deck_1", Map.of(key, "Statista 2026"));

		// Then: the deck is not re-read for nothing
		verifyNoInteractions(gateway, requestFactory);
	}

	@Test
	void shouldNotCallGoogleWhenNoLinkableTextWasFoundInTheDeckTest() {
		// Given: links resolved, but the citation text is not present in the published deck
		SourceLinkService linkService = mock(SourceLinkService.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectMapper objectMapper = new ObjectMapper();
		String key = sourcesTokenKey();
		Map<String, String> sourceValues = Map.of(key, "Statista 2026");
		Map<String, List<SourceLink>> links =
				Map.of(key, List.of(new SourceLink("Statista 2026", "https://statista.com")));
		ObjectNode presentation = objectMapper.createObjectNode();
		when(linkService.resolveSourceLinks(sourceValues)).thenReturn(links);
		when(gateway.getPresentation("token", "deck_1")).thenReturn(presentation);
		when(requestFactory.buildSourceLinkRequests(presentation, sourceValues, links))
				.thenReturn(objectMapper.createArrayNode());
		DeckSourceHyperlinkServiceImpl service =
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway);

		// When: hyperlinks are applied
		service.applySourceHyperlinks("token", "deck_1", sourceValues);

		// Then: no empty batchUpdate is sent
		verify(gateway).getPresentation("token", "deck_1");
		verify(gateway, org.mockito.Mockito.never())
				.batchUpdate("token", "deck_1", objectMapper.createArrayNode());
	}

	@Test
	void shouldSendTheLinkRequestsForTheResolvedSourcesTest() {
		// Given: citations that resolved and were located in the deck
		SourceLinkService linkService = mock(SourceLinkService.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectMapper objectMapper = new ObjectMapper();
		String key = sourcesTokenKey();
		Map<String, String> sourceValues = Map.of(key, "Statista 2026");
		Map<String, List<SourceLink>> links =
				Map.of(key, List.of(new SourceLink("Statista 2026", "https://statista.com")));
		ObjectNode presentation = objectMapper.createObjectNode();
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("updateTextStyle").put("objectId", "shape_1");
		when(linkService.resolveSourceLinks(sourceValues)).thenReturn(links);
		when(gateway.getPresentation("token", "deck_1")).thenReturn(presentation);
		when(requestFactory.buildSourceLinkRequests(presentation, sourceValues, links))
				.thenReturn(requests);
		DeckSourceHyperlinkServiceImpl service =
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway);

		// When: hyperlinks are applied
		service.applySourceHyperlinks("token", "deck_1", sourceValues);

		// Then: exactly the built requests reach the deck
		verify(gateway).batchUpdate("token", "deck_1", requests);
	}

	@Test
	void shouldLeaveThePublishedDeckAloneWhenTheLinkUpdateFailsTest() {
		// Given: a batchUpdate that Google rejects
		SourceLinkService linkService = mock(SourceLinkService.class);
		SlidesRequestFactory requestFactory = mock(SlidesRequestFactory.class);
		GoogleSlidesGateway gateway = mock(GoogleSlidesGateway.class);
		ObjectMapper objectMapper = new ObjectMapper();
		String key = sourcesTokenKey();
		Map<String, String> sourceValues = Map.of(key, "Statista 2026");
		Map<String, List<SourceLink>> links =
				Map.of(key, List.of(new SourceLink("Statista 2026", "https://statista.com")));
		ObjectNode presentation = objectMapper.createObjectNode();
		ArrayNode requests = objectMapper.createArrayNode();
		requests.addObject().putObject("updateTextStyle").put("objectId", "shape_1");
		when(linkService.resolveSourceLinks(sourceValues)).thenReturn(links);
		when(gateway.getPresentation("token", "deck_1")).thenReturn(presentation);
		when(requestFactory.buildSourceLinkRequests(presentation, sourceValues, links))
				.thenReturn(requests);
		org.mockito.Mockito.doThrow(new IllegalStateException("rejected"))
				.when(gateway).batchUpdate("token", "deck_1", requests);
		DeckSourceHyperlinkServiceImpl service =
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway);

		// When-Then: the failure is swallowed — the deck is already published and usable with
		// plain-text sources, so this must not fail the user's request
		assertThatCode(() -> service.applySourceHyperlinks("token", "deck_1", sourceValues))
				.doesNotThrowAnyException();
	}
}
