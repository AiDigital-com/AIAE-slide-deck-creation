package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyOpenAiGateway;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFitRevisionPolicy;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageDraftParserImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageFieldMeasurerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageIconLibraryServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTextFitterImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OpenAiOnePageCaseStudyDraftServiceImpl;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the One-Page drafting orchestration.
 */
class OpenAiOnePageCaseStudyDraftServiceImplTest {

	private static final String DRAFT_BODY = "{\"draft\":true}";

	private OpenAiOnePageCaseStudyDraftServiceImpl service(CaseStudyOpenAiGateway gateway,
			OnePageDraftPromptComposer composer, OnePageFitRevisionPolicy policy) {
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		return new OpenAiOnePageCaseStudyDraftServiceImpl(gateway, composer,
				new OnePageDraftParserImpl(new ObjectMapper(), registry, new OnePageIconLibraryServiceImpl()),
				new OnePageFieldMeasurerImpl(registry, new OnePageTextFitterImpl()), policy);
	}

	private OnePageCaseStudyField field(List<OnePageCaseStudyField> fields, String key) {
		return fields.stream().filter(f -> f.key().equals(key)).findFirst().orElseThrow();
	}

	@Test
	void shouldDraftThenReviseThenMeasureEveryFieldTest() {
		// Given: a model reply, a revision policy that lengthens the challenge, and two uploads
		CaseStudyOpenAiGateway gateway = mock(CaseStudyOpenAiGateway.class);
		OnePageDraftPromptComposer composer = mock(OnePageDraftPromptComposer.class);
		OnePageFitRevisionPolicy policy = mock(OnePageFitRevisionPolicy.class);
		List<SourceDocument> documents = List.of(new SourceDocument("report.pdf", "CPA fell 67%"));
		List<SourceImage> images = List.of(new SourceImage("dash.png", SourceImageType.PNG, new byte[]{1}));
		when(composer.buildDraftRequestBody(documents, images)).thenReturn(DRAFT_BODY);
		when(gateway.callChatCompletion(DRAFT_BODY)).thenReturn("{\"fields\":{\"challenge\":\"CPA rose.\","
				+ "\"stat_1_value\":\"-68%\",\"stat_1_label\":\"dem-gen cpa\"},"
				+ "\"icons\":{\"stat_1_icon\":\"dollar\"}}");
		doAnswer(invocation -> {
			Map<String, String> values = invocation.getArgument(0);
			values.put("challenge", "In May, acquisition costs hit a peak of $564.14, 2.5x April's rate, driven by "
					+ "generic creative and scattered ad spend. The challenge was clear: restructure campaigns, "
					+ "align creative with key promotional moments, and hone geographic targeting to drive down "
					+ "costs without sacrificing conversion volume.");
			return null;
		}).when(policy).reviseToFit(org.mockito.ArgumentMatchers.anyMap(), eq(documents), eq(images));

		// When: the draft is produced
		OnePageCaseStudyDraft draft = service(gateway, composer, policy).draft(documents, images);

		// Then: the revised value is measured as fitting, the stat pair fits, icons and library carry over
		assertThat(field(draft.fields(), "challenge").fit()).isEqualTo(OnePageFit.FITS);
		assertThat(field(draft.fields(), "stat_1_label").value()).isEqualTo("DEM-GEN CPA");
		assertThat(field(draft.fields(), "stat_1_label").lines()).isEqualTo(1);
		assertThat(draft.icons().stream().filter(i -> i.slot().equals("stat_1_icon")).findFirst().orElseThrow().icon())
				.isEqualTo("dollar");
		assertThat(draft.iconLibrary()).isNotEmpty();
		assertThat(draft.sourceFileNames()).isEqualTo("report.pdf, dash.png");
		ArgumentCaptor<Map<String, String>> values = ArgumentCaptor.captor();
		verify(policy).reviseToFit(values.capture(), eq(documents), eq(images));
		assertThat(values.getValue()).containsEntry("stat_1_value", "-68%");
		assertThat(values.getValue()).containsKey("driver_5");
	}

	@Test
	void shouldMeasureReviewedValuesKeyedByFieldTest() {
		// Given: two reviewed values, one of them for a field left blank
		OpenAiOnePageCaseStudyDraftServiceImpl service = service(mock(CaseStudyOpenAiGateway.class),
				mock(OnePageDraftPromptComposer.class), mock(OnePageFitRevisionPolicy.class));

		// When: they are measured
		List<OnePageCaseStudyField> fields = service.measureFields(List.of(
				new OnePageFieldValue("headline_stat", "DRIVING 67% CPA REDUCTION WHILE SCALING FULL-FUNNEL YOUTUBE"),
				new OnePageFieldValue("goal", "")));

		// Then: every template field comes back with the right verdict
		assertThat(field(fields, "headline_stat").fit()).isEqualTo(OnePageFit.FITS);
		assertThat(field(fields, "goal").fit()).isEqualTo(OnePageFit.EMPTY);
		assertThat(field(fields, "challenge").fit()).isEqualTo(OnePageFit.EMPTY);
		assertThat(fields).hasSize(new OnePageTemplateRegistryImpl().fields().size());
	}
}
