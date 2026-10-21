package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthOpenAiGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link InDepthFactCheckerImpl}.
 */
class InDepthFactCheckerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final InDepthTemplateTokenRegistryImpl registry = new InDepthTemplateTokenRegistryImpl();
	private InDepthOpenAiGateway gateway;
	private InDepthFactCheckerImpl checker;

	@BeforeEach
	void setUp() {
		gateway = mock(InDepthOpenAiGateway.class);
		InDepthValueFormatterImpl formatter = new InDepthValueFormatterImpl(registry);
		checker = new InDepthFactCheckerImpl(gateway,
				new InDepthDraftPromptComposerImpl(new CategoryAnalysisProperties(), objectMapper, registry),
				new InDepthDraftParserImpl(objectMapper, formatter), formatter);
	}

	@Test
	void shouldClassifySourcesAndDigitBearingSamplesAsFactualTest() {
		// Given-When-Then: sources and stats are checked, headlines are not
		assertThat(checker.isFactual(tokenByKey("trend_sources"))).isTrue();
		assertThat(checker.isFactual(tokenByKey("trend_stat_1_value"))).isTrue();
		assertThat(checker.isFactual(tokenByKey("trend_headline"))).isFalse();
	}

	@Test
	void shouldGroupTokensSharingTheSameSlidesIntoOneTopicTest() {
		// Given: tokens of slides 2, 3, and 4 (3 and 4 share every token)
		List<InDepthTokenSpec> tokens = registry.tokensForSlides(List.of(2, 3, 4));

		// When: they are grouped
		List<List<InDepthTokenSpec>> groups = checker.groupBySlideNumbers(tokens);

		// Then: two groups, each homogeneous in slide numbers, slide 2 first
		assertThat(groups).hasSize(2);
		assertThat(groups).allSatisfy(group ->
				assertThat(group.stream().map(InDepthTokenSpec::slideNumbers).distinct()).hasSize(1));
		assertThat(groups.get(0).get(0).slideNumbers()).containsExactly(2);
	}

	@Test
	void shouldSkipTheProviderWhenNoFieldIsFactualTest() {
		// Given: a headline-only draft
		InDepthTokenSpec headline = tokenByKey("hi_headline");
		InDepthDraftField drafted = new InDepthDraftField(headline.key(), headline.label(), headline.slideNumbers(),
				"A DRAFTED HEADLINE");

		// When: it is verified
		List<InDepthDraftField> result = checker.verifyFactualFields("Travel", List.of(headline), List.of(drafted));

		// Then: nothing changes and no call is made
		assertThat(result).containsExactly(drafted);
		verifyNoInteractions(gateway);
	}

	@Test
	void shouldApplyCorrectionsAndIgnorePlaceholdersAndKeepValuesWhenUnchangedTest() {
		// Given: slide 2's drafted fields, a fact-check that corrects one stat, gives up on the
		// sources, and leaves the rest unchanged
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		List<InDepthDraftField> drafted = new ArrayList<>();
		for (InDepthTokenSpec spec : tokens) {
			drafted.add(new InDepthDraftField(spec.key(), spec.label(), spec.slideNumbers(), "drafted " + spec.key()));
		}
		when(gateway.callResponses(anyString()))
				.thenReturn("{\"trend_stat_1_value\":\"12.4M\",\"trend_sources\":\"n/a\"}");

		// When: the draft is verified
		List<InDepthDraftField> result = checker.verifyFactualFields("Travel", tokens, drafted);

		// Then: one correction is applied, everything else keeps its drafted value, and the single
		// topic group produced exactly one search-grounded call quoting the drafted stat
		assertThat(result).hasSize(tokens.size());
		assertThat(valueOf(result, "trend_stat_1_value")).isEqualTo("12.4M");
		assertThat(valueOf(result, "trend_sources")).isEqualTo("drafted trend_sources");
		assertThat(valueOf(result, "trend_headline")).isEqualTo("drafted trend_headline");
		ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
		verify(gateway).callResponses(body.capture());
		assertThat(body.getValue()).contains("web_search").contains("drafted trend_stat_1_value");
	}

	@Test
	void shouldKeepDraftedValuesWhenAGroupFailsTest() {
		// Given: a fact-check call that fails
		List<InDepthTokenSpec> tokens = registry.tokensForSlide(2);
		List<InDepthDraftField> drafted = new ArrayList<>();
		for (InDepthTokenSpec spec : tokens) {
			drafted.add(new InDepthDraftField(spec.key(), spec.label(), spec.slideNumbers(), "drafted " + spec.key()));
		}
		when(gateway.callResponses(anyString())).thenThrow(new AppException(ErrorReason.C003, "boom"));

		// When-Then: the drafted fields come back untouched
		assertThat(checker.verifyFactualFields("Travel", tokens, drafted)).isSameAs(drafted);
	}

	private String valueOf(List<InDepthDraftField> fields, String key) {
		return fields.stream().filter(f -> f.key().equals(key)).map(InDepthDraftField::value).findFirst().orElseThrow();
	}

	private InDepthTokenSpec tokenByKey(String key) {
		return InDepthTemplateTokens.TOKENS.stream().filter(t -> t.key().equals(key)).findFirst().orElseThrow();
	}
}
