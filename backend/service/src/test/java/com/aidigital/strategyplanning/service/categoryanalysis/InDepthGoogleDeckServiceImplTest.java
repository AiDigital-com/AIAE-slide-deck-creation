package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthChartRequestBuilder;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDeckRequestAssembler;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthValueFormatter;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.GoogleInDepthDeckServiceImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GoogleInDepthDeckServiceImpl}: access checks, the copy-delete-fill
 * sequence, and best-effort chart updates.
 */
class InDepthGoogleDeckServiceImplTest {

	private static final String TOKEN = "ya29.token";
	private static final Set<GoogleOAuthScope> SCOPES = Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);

	private final ObjectMapper objectMapper = new ObjectMapper();
	private CategoryAnalysisProperties properties;
	private GoogleGrantService grantService;
	private GoogleSlidesGateway gateway;
	private SlidesPresentationInspector inspector;
	private InDepthDeckRequestAssembler assembler;
	private InDepthChartRequestBuilder chartBuilder;
	private InDepthValueFormatter formatter;
	private GoogleInDepthDeckServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new CategoryAnalysisProperties();
		properties.setClerkSecretKey("sk_clerk");
		properties.setInDepthTemplatePresentationId("master_in_depth");
		grantService = mock(GoogleGrantService.class);
		gateway = mock(GoogleSlidesGateway.class);
		inspector = mock(SlidesPresentationInspector.class);
		assembler = mock(InDepthDeckRequestAssembler.class);
		chartBuilder = mock(InDepthChartRequestBuilder.class);
		formatter = mock(InDepthValueFormatter.class);
		service = new GoogleInDepthDeckServiceImpl(properties, grantService, gateway, inspector,
				assembler, chartBuilder, formatter);
	}

	@Test
	void shouldRejectAnEmptySelectionBeforeTouchingGoogleTest() {
		// Given-When-Then: no checked slides means no deck, and no grant lookup
		assertThatThrownBy(() -> service.createInDepthDeck("user_1", "Deck", List.of(), Map.of(), "https://app"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("No slides selected");
		assertThatThrownBy(() -> service.createInDepthDeck("user_1", "Deck", null, Map.of(), "https://app"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("No slides selected");
		verifyNoInteractions(grantService, gateway);
	}

	@Test
	void shouldStopWhenTheSharedAccessCheckRejectsTheUserTest() {
		// Given: the shared grant check refuses the user
		when(grantService.requireAccessToken("sk_clerk", "user_1", SCOPES))
				.thenThrow(new AppException(com.aidigital.strategyplanning.service.common.error.ErrorReason.C003,
						"Not signed in with Google"));

		// When-Then: the failure propagates and nothing is copied
		assertThatThrownBy(() -> service.createInDepthDeck("user_1", "Deck", List.of(2), Map.of(), "https://app"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google");
		verifyNoInteractions(gateway);
	}

	@Test
	void shouldCopyTheInDepthMasterDeleteUncheckedSlidesFillTokensAndApplyChartsTest() {
		// Given: a connected user, a copied presentation, one delete and one replace request, and
		// two chart batches
		grantConnected();
		JsonNode presentation = objectMapper.createObjectNode().put("presentationId", "copy_1");
		when(gateway.copyPresentation(TOKEN, "master_in_depth", "Deck")).thenReturn("copy_1");
		when(gateway.getPresentation(TOKEN, "copy_1")).thenReturn(presentation);
		when(inspector.extractSlideIds(presentation)).thenReturn(List.of("s1", "s2"));
		Map<String, String> raw = Map.of("donut_1_value", "10%");
		Map<String, String> sorted = Map.of("donut_1_value", "sorted");
		when(formatter.sortChartSeries(raw)).thenReturn(sorted);
		ArrayNode deletes = objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("deleteObject", "s1"));
		ArrayNode replaces = objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("replaceAllText", "x"));
		when(assembler.buildDeleteUnselectedSlidesRequests(List.of("s1", "s2"), List.of(2))).thenReturn(deletes);
		when(assembler.buildReplaceTextRequests(sorted, List.of("s1", "s2"), List.of(2))).thenReturn(replaces);
		ArrayNode donutBatch = objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("createImage", "d"));
		ArrayNode barBatch = objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("transform", "b"));
		when(chartBuilder.buildChartRequests(presentation, List.of(2), sorted, "https://app"))
				.thenReturn(List.of(donutBatch, barBatch));

		// When: the deck is created
		String url = service.createInDepthDeck("user_1", "Deck", List.of(2), raw, "https://app");

		// Then: the text batch carries deletes then replaces, each chart batch is applied on its
		// own, and the edit URL of the copy is returned
		assertThat(url).isEqualTo("https://docs.google.com/presentation/d/copy_1/edit");
		ArgumentCaptor<ArrayNode> batches = ArgumentCaptor.forClass(ArrayNode.class);
		verify(gateway, org.mockito.Mockito.times(3)).batchUpdate(eq(TOKEN), eq("copy_1"), batches.capture());
		ArrayNode textBatch = batches.getAllValues().get(0);
		assertThat(textBatch).hasSize(2);
		assertThat(textBatch.get(0).has("deleteObject")).isTrue();
		assertThat(textBatch.get(1).has("replaceAllText")).isTrue();
		assertThat(batches.getAllValues().get(1)).isSameAs(donutBatch);
		assertThat(batches.getAllValues().get(2)).isSameAs(barBatch);
	}

	@Test
	void shouldSkipTheTextBatchWhenThereIsNothingToChangeTest() {
		// Given: every slide checked and no tokens — no requests at all
		grantConnected();
		JsonNode presentation = objectMapper.createObjectNode();
		when(gateway.copyPresentation(TOKEN, "master_in_depth", "Deck")).thenReturn("copy_1");
		when(gateway.getPresentation(TOKEN, "copy_1")).thenReturn(presentation);
		when(inspector.extractSlideIds(presentation)).thenReturn(List.of("s1"));
		when(formatter.sortChartSeries(Map.of())).thenReturn(Map.of());
		when(assembler.buildDeleteUnselectedSlidesRequests(List.of("s1"), List.of(1)))
				.thenReturn(objectMapper.createArrayNode());
		when(assembler.buildReplaceTextRequests(Map.of(), List.of("s1"), List.of(1)))
				.thenReturn(objectMapper.createArrayNode());
		when(chartBuilder.buildChartRequests(presentation, List.of(1), Map.of(), "https://app")).thenReturn(List.of());

		// When: the deck is created
		service.createInDepthDeck("user_1", "Deck", List.of(1), Map.of(), "https://app");

		// Then: no batchUpdate is sent
		verify(gateway, never()).batchUpdate(eq(TOKEN), eq("copy_1"), org.mockito.ArgumentMatchers.any(ArrayNode.class));
	}

	@Test
	void shouldStillReturnTheDeckWhenChartBuildingOrApplyingFailsTest() {
		// Given: a filled deck whose chart building throws, then one whose chart batch is refused
		grantConnected();
		JsonNode presentation = objectMapper.createObjectNode();
		when(gateway.copyPresentation(TOKEN, "master_in_depth", "Deck")).thenReturn("copy_1");
		when(gateway.getPresentation(TOKEN, "copy_1")).thenReturn(presentation);
		when(inspector.extractSlideIds(presentation)).thenReturn(List.of("s1"));
		when(formatter.sortChartSeries(Map.of())).thenReturn(Map.of());
		when(assembler.buildDeleteUnselectedSlidesRequests(anyList(), anyList()))
				.thenReturn(objectMapper.createArrayNode());
		when(assembler.buildReplaceTextRequests(eq(Map.of()), anyList(), anyList()))
				.thenReturn(objectMapper.createArrayNode());
		when(chartBuilder.buildChartRequests(presentation, List.of(8), Map.of(), "https://app"))
				.thenThrow(new IllegalStateException("cannot draw"));
		ArrayNode refused = objectMapper.createArrayNode().add(objectMapper.createObjectNode());
		when(chartBuilder.buildChartRequests(presentation, List.of(18), Map.of(), "https://app"))
				.thenReturn(List.of(refused));
		doThrow(new AppException(com.aidigital.strategyplanning.service.common.error.ErrorReason.C003, "refused"))
				.when(gateway).batchUpdate(TOKEN, "copy_1", refused);

		// When-Then: both decks are returned despite the chart problems
		assertThat(service.createInDepthDeck("user_1", "Deck", List.of(8), Map.of(), "https://app"))
				.isEqualTo("https://docs.google.com/presentation/d/copy_1/edit");
		assertThat(service.createInDepthDeck("user_1", "Deck", List.of(18), Map.of(), "https://app"))
				.isEqualTo("https://docs.google.com/presentation/d/copy_1/edit");
	}

	private void grantConnected() {
		when(grantService.requireAccessToken("sk_clerk", "user_1", SCOPES)).thenReturn(TOKEN);
	}
}
