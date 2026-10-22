package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyOpenAiGateway;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageFieldMeasurerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageFitRevisionPolicyImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageIconLibraryServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTextFitterImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the measure-and-revise loop. The real registry, fitter, measurer, and parser
 * are wired; only the model (gateway) and the prompt composer are mocked, so the loop's decisions
 * — what to send back, what to keep, what to trim — are exercised against real measurements.
 */
class OnePageFitRevisionPolicyImplTest {

	private static final String SHORT_CHALLENGE =
			"In May, CPA soared to $564.14 due to generic ads and unfocused spend.";
	private static final String FULL_CHALLENGE = "In May, acquisition costs hit a peak of $564.14, 2.5x April's "
			+ "rate, driven by generic creative and scattered ad spend. The challenge was clear: restructure "
			+ "campaigns, align creative with key promotional moments, and hone geographic targeting to drive down "
			+ "costs without sacrificing conversion volume.";
	private static final String REVISION_BODY = "{\"revision\":true}";

	private OnePageFitRevisionPolicyImpl policy(CaseStudyOpenAiGateway gateway, OnePageDraftPromptComposer composer) {
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		return new OnePageFitRevisionPolicyImpl(gateway, composer,
				new OnePageDraftParserImpl(new ObjectMapper(), registry, new OnePageIconLibraryServiceImpl()),
				new OnePageFieldMeasurerImpl(registry, fitter), fitter, registry);
	}

	@Test
	void shouldNotCallTheModelWhenEveryFieldFitsTest() {
		// Given: values that already fill their boxes
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		Map<String, String> values = new HashMap<>();
		values.put("challenge", FULL_CHALLENGE);

		// When: the loop runs
		policy(gateway, composer).reviseToFit(values, List.of(), List.of());

		// Then: nothing goes back to the model and the value is untouched
		verifyNoInteractions(gateway, composer);
		assertThat(values).containsEntry("challenge", FULL_CHALLENGE);
	}

	@Test
	void shouldKeepARewriteThatMeasuresCloserToTheBoxTest() {
		// Given: a short challenge the model rewrites to the designer's full-length text
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		when(composer.buildRevisionRequestBody(anyList(), anyMap(), anyList(), anyList())).thenReturn(REVISION_BODY);
		when(gateway.callChatCompletion(REVISION_BODY))
				.thenReturn("{\"fields\":{\"challenge\":\"" + FULL_CHALLENGE + "\"}}");
		Map<String, String> values = new HashMap<>();
		values.put("challenge", SHORT_CHALLENGE);
		List<SourceDocument> documents = List.of(new SourceDocument("report.pdf", "May CPA was $564.14"));

		// When: the loop runs
		policy(gateway, composer).reviseToFit(values, documents, List.of());

		// Then: the rewrite is kept and the loop stopped after one round
		assertThat(values).containsEntry("challenge", FULL_CHALLENGE);
		verify(gateway, times(1)).callChatCompletion(REVISION_BODY);
		verify(composer).buildRevisionRequestBody(anyList(), eq(values), eq(documents), anyList());
	}

	@Test
	void shouldDiscardARewriteThatIsNoCloserAndRetryUpToTheRoundBudgetTest() {
		// Given: a short challenge the model keeps answering with an even shorter text
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		when(composer.buildRevisionRequestBody(anyList(), anyMap(), anyList(), anyList())).thenReturn(REVISION_BODY);
		when(gateway.callChatCompletion(REVISION_BODY)).thenReturn("{\"fields\":{\"challenge\":\"CPA rose.\"}}");
		Map<String, String> values = new HashMap<>();
		values.put("challenge", SHORT_CHALLENGE);

		// When: the loop runs
		policy(gateway, composer).reviseToFit(values, List.of(), List.of());

		// Then: the original stays and the model was asked exactly MAX_REVISION_ROUNDS times
		assertThat(values).containsEntry("challenge", SHORT_CHALLENGE);
		verify(gateway, times(OnePageFitRevisionPolicyImpl.MAX_REVISION_ROUNDS)).callChatCompletion(REVISION_BODY);
	}

	@Test
	void shouldStopRevisingWhenTheModelFailsAndStillTrimOverflowTest() {
		// Given: an overflowing headline lead and a model that is unreachable
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		when(composer.buildRevisionRequestBody(anyList(), anyMap(), anyList(), anyList())).thenReturn(REVISION_BODY);
		when(gateway.callChatCompletion(REVISION_BODY))
				.thenThrow(new AppException(ErrorReason.C003, "AI drafting call failed"));
		Map<String, String> values = new HashMap<>();
		values.put("headline_lead", "HOW SMART RESTRUCTURING WON THE QUARTER FOR EVERY SINGLE REGIONAL MARKET");
		OnePageFitRevisionPolicyImpl policy = policy(gateway, composer);

		// When: the loop runs
		policy.reviseToFit(values, List.of(), List.of());

		// Then: one failed call, and the guarantee step trimmed the lead to its one line
		verify(gateway, times(1)).callChatCompletion(REVISION_BODY);
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		assertThat(fitter.measure(new OnePageTemplateRegistryImpl().field("headline_lead").orElseThrow(),
				values.get("headline_lead"), null).fit()).isNotEqualTo(OnePageFit.OVER);
		assertThat(values.get("headline_lead")).startsWith("HOW SMART RESTRUCTURING");
	}

	@Test
	void shouldGiveShortFactsOnlyFieldsASingleRetryTest() {
		// Given: a short client description (facts only) the model cannot lengthen
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		when(composer.buildRevisionRequestBody(anyList(), anyMap(), anyList(), anyList())).thenReturn(REVISION_BODY);
		when(gateway.callChatCompletion(REVISION_BODY)).thenReturn("{\"fields\":{\"client\":\"A retailer\"}}");
		Map<String, String> values = new HashMap<>();
		values.put("client", "A retailer");

		// When: the loop runs
		policy(gateway, composer).reviseToFit(values, List.of(), List.of());

		// Then: the model was asked once, not three times, and the fact stays as it was
		verify(gateway, times(1)).callChatCompletion(REVISION_BODY);
		assertThat(values).containsEntry("client", "A retailer");
	}

	@Test
	void shouldDropTrailingSentencesFromARewriteThatOvershootsTest() {
		// Given: a short challenge the model rewrites one sentence too long
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		when(composer.buildRevisionRequestBody(anyList(), anyMap(), anyList(), anyList())).thenReturn(REVISION_BODY);
		when(gateway.callChatCompletion(REVISION_BODY)).thenReturn("{\"fields\":{\"challenge\":\"" + FULL_CHALLENGE
				+ " Every market needed a fresh plan before peak season arrived.\"}}");
		Map<String, String> values = new HashMap<>();
		values.put("challenge", SHORT_CHALLENGE);

		// When: the loop runs
		policy(gateway, composer).reviseToFit(values, List.of(), List.of());

		// Then: the overshoot was cut back to whole sentences that fill the box
		assertThat(values).containsEntry("challenge", FULL_CHALLENGE);
	}
}
