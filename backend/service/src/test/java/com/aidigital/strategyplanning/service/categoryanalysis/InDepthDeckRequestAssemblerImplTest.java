package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthDeckRequestAssemblerImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthTemplateTokenRegistryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.InDepthValueFormatterImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlidesPresentationInspectorImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlidesRequestFactoryImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InDepthDeckRequestAssemblerImpl}: checked-slide deletion and token filling.
 */
class InDepthDeckRequestAssemblerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private final InDepthDeckRequestAssemblerImpl assembler = new InDepthDeckRequestAssemblerImpl(objectMapper,
			registry, new InDepthValueFormatterImpl(registry),
			new SlidesRequestFactoryImpl(objectMapper, new SlidesPresentationInspectorImpl()));
	private final List<String> slideIds = IntStream.rangeClosed(1, 24).mapToObj(n -> "slide" + n).toList();

	@Test
	void shouldDeleteEveryUncheckedSlideAndOnlyThoseTest() {
		// Given: three checked slides
		List<Integer> selected = List.of(1, 2, 16);

		// When: delete requests are built
		ArrayNode requests = assembler.buildDeleteUnselectedSlidesRequests(slideIds, selected);

		// Then: one delete per unchecked slide, none for a checked one
		assertThat(requests).hasSize(slideIds.size() - selected.size());
		for (JsonNode request : requests) {
			String deletedId = request.path("deleteObject").path("objectId").asText();
			assertThat(selected).doesNotContain(Integer.parseInt(deletedId.substring("slide".length())));
		}
	}

	@Test
	void shouldDeleteNothingWhenEverySlideIsCheckedTest() {
		// Given-When-Then: a full selection keeps the whole deck
		List<Integer> all = IntStream.rangeClosed(1, slideIds.size()).boxed().toList();
		assertThat(assembler.buildDeleteUnselectedSlidesRequests(slideIds, all)).isEmpty();
	}

	@Test
	void shouldOnlyTargetCheckedSlidesTest() {
		// Given: slide 3 checked with every one of its fields drafted
		Map<String, String> values = new LinkedHashMap<>();
		for (InDepthTokenSpec spec : registry.tokensForSlide(3)) {
			values.put(spec.key(), "drafted-" + spec.key());
		}

		// When: replace requests are built
		ArrayNode requests = assembler.buildReplaceTextRequests(values, slideIds, List.of(3));

		// Then: every request is scoped to slide 3 alone
		assertThat(requests).isNotEmpty();
		for (JsonNode request : requests) {
			JsonNode pageIds = request.path("replaceAllText").path("pageObjectIds");
			assertThat(pageIds).hasSize(1);
			assertThat(pageIds.get(0).asText()).isEqualTo("slide3");
		}
	}

	@Test
	void shouldWriteASharedFieldToEveryCheckedVariantTest() {
		// Given: slides 3 and 4 (two layouts of one topic) both checked
		Map<String, String> values = Map.of("highlights_headline", "MY DRAFTED HEADLINE");

		// When: replace requests are built
		ArrayNode requests = assembler.buildReplaceTextRequests(values, slideIds, List.of(3, 4));

		// Then: the value lands on both slides, via both the token and the sample literal
		List<String> targetedPageIds = new ArrayList<>();
		requests.forEach(request -> {
			if ("MY DRAFTED HEADLINE".equals(request.path("replaceAllText").path("replaceText").asText())) {
				targetedPageIds.add(request.path("replaceAllText").path("pageObjectIds").get(0).asText());
			}
		});
		assertThat(targetedPageIds).containsExactly("slide3", "slide3", "slide4", "slide4");
	}

	@Test
	void shouldReplaceMissingValuesWithAnEmptyStringAndSkipChartDataTest() {
		// Given: slide 8 checked with no values at all
		// When: replace requests are built
		ArrayNode requests = assembler.buildReplaceTextRequests(Map.of(), slideIds, List.of(8));

		// Then: every token is blanked so no placeholder stays visible, and donut values (chart data
		// only) are never written as text
		assertThat(requests).isNotEmpty()
				.allMatch(request -> request.path("replaceAllText").path("replaceText").asText().isEmpty())
				.noneMatch(request -> request.path("replaceAllText").path("containsText").path("text").asText()
						.equals("{{donut_1_value}}"));
	}

	@Test
	void shouldReplacePlaceholderOnlyKeysByTheirTokenAloneTest() {
		// Given: channel fields whose samples collide with drafted text
		Map<String, String> values = Map.of(
				"channel_1_name", "ctv",
				"channel_1_stat_label", "of adults stream weekly",
				"channel_2_stat_label", "of adults listen daily");

		// When: replace requests are built for slide 14
		ArrayNode requests = assembler.buildReplaceTextRequests(values, slideIds, List.of(14));

		// Then: those values are only ever matched through their {{token}} placeholder, and the
		// channel name is upper-cased on the way in
		assertThat(requests).anyMatch(r -> r.path("replaceAllText").path("replaceText").asText().equals("CTV"));
		for (JsonNode request : requests) {
			String find = request.path("replaceAllText").path("containsText").path("text").asText();
			String replace = request.path("replaceAllText").path("replaceText").asText();
			if (replace.equals("CTV") || replace.startsWith("of adults")) {
				assertThat(find).startsWith("{{").endsWith("}}");
			}
		}
	}

	@Test
	void shouldDropThePercentSignForStatBoxesThatPrintItTest() {
		// Given: a slide-18 stat with a percent sign
		Map<String, String> values = Map.of("stat_1_value", "95%");

		// When: replace requests are built
		ArrayNode requests = assembler.buildReplaceTextRequests(values, slideIds, List.of(18));

		// Then: the bare number is written
		assertThat(requests).anyMatch(request ->
				request.path("replaceAllText").path("containsText").path("text").asText().equals("{{stat_1_value}}")
						&& request.path("replaceAllText").path("replaceText").asText().equals("95"));
	}

	@Test
	void shouldIgnoreCheckedSlidesBeyondTheCopiedDeckTest() {
		// Given: a copied deck shorter than the catalog
		List<String> shortDeck = List.of("slide1", "slide2");

		// When: slide 3 is checked anyway
		ArrayNode requests = assembler.buildReplaceTextRequests(Map.of("highlights_headline", "X"), shortDeck,
				List.of(3));

		// Then: nothing targets a slide that does not exist
		assertThat(requests).isEmpty();
	}
}
