package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for mapping the model's reply onto a draft, wired to the real field reader.
 */
class OnePagerDraftParserImplTest {

	private static final String FULL_REPLY = """
			{"topicPill":"AUTOMOTIVE","mainTitle":["SOLVING","FRAGMENTATION"],"supportingCopy":"With speed",
			"processSectionLabel":"HOW IT WORKS","processSteps":["UNDERSTAND","ACTIVATE"],
			"solutionsSectionLabel":"SOLUTIONS","solutions":[{"title":"VIN","valueLine":"Value","body":"Body"}],
			"solutionsVisualLabel":"VISUALS",
			"kpiSectionLabel":"SIGNALS","kpis":[{"value":"30%","label":"LIFT"}],"kpiDisclaimer":"Based on data.",
			"imageTextSectionLabel":"WHY","imageTextBody":"Because.","keyTakeaway":"Takeaway.",
			"impactStats":[{"value":"+45%","description":"FASTER"}],
			"capabilityCards":[{"title":"SYNC","description":"Desc","bullets":["A","B"],"footer":"Foot"}],
			"spotlightCards":[{"title":"IMG","description":"D","bullets":["X"],"kpiChips":[],"stageNames":[]},
			{"title":"CHIPS","description":"D","bullets":["X"],"kpiChips":[{"value":"2X","label":"SALES"}],
			"stageNames":[]},{"title":"STAGES","description":"D","bullets":["X"],"kpiChips":[],
			"stageNames":["CONNECT","LAUNCH"]}],
			"insightCards":[{"title":"SPEED","insight":"Fast."}],
			"milestoneHeadline":"THREE STEPS","milestones":[{"title":"CONNECT","subtext":"Week one"}]}
			""";

	private OnePagerDraftParserImpl parser() {
		return new OnePagerDraftParserImpl(new OnePagerJsonFieldReaderImpl(new ObjectMapper()));
	}

	@Test
	void shouldReadEveryFieldOfEveryPickedModuleTest() {
		// Given: a brief that picks every module and a complete reply
		List<OnePagerModuleKey> order = List.of(OnePagerModuleKey.values());
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Brief", order);

		// When: the reply is parsed
		OnePagerDraft draft = parser().parseDraft(FULL_REPLY, brief);

		// Then: every module's content is present and the stacking order is carried through
		assertThat(draft.topicPill()).isEqualTo("AUTOMOTIVE");
		assertThat(draft.mainTitle()).containsExactly("SOLVING", "FRAGMENTATION");
		assertThat(draft.supportingCopy()).isEqualTo("With speed");
		assertThat(draft.moduleOrder()).isEqualTo(order);
		assertThat(draft.processSectionLabel()).isEqualTo("HOW IT WORKS");
		assertThat(draft.processSteps()).containsExactly("UNDERSTAND", "ACTIVATE");
		assertThat(draft.solutionsSectionLabel()).isEqualTo("SOLUTIONS");
		assertThat(draft.solutions()).hasSize(1);
		assertThat(draft.solutions().get(0).body()).isEqualTo("Body");
		assertThat(draft.solutionsVisualLabel()).isEqualTo("VISUALS");
		assertThat(draft.kpiSectionLabel()).isEqualTo("SIGNALS");
		assertThat(draft.kpis()).containsExactly(new OnePagerKpi("30%", "LIFT"));
		assertThat(draft.kpiDisclaimer()).isEqualTo("Based on data.");
		assertThat(draft.imageTextSectionLabel()).isEqualTo("WHY");
		assertThat(draft.imageTextBody()).isEqualTo("Because.");
		assertThat(draft.keyTakeaway()).isEqualTo("Takeaway.");
		assertThat(draft.impactStats().get(0).value()).isEqualTo("+45%");
		assertThat(draft.capabilityCards().get(0).bullets()).containsExactly("A", "B");
		assertThat(draft.capabilityCards().get(0).footer()).isEqualTo("Foot");
		assertThat(draft.spotlightCards()).hasSize(3);
		assertThat(draft.spotlightCards().get(1).kpiChips()).containsExactly(new OnePagerKpi("2X", "SALES"));
		assertThat(draft.spotlightCards().get(2).stageNames()).containsExactly("CONNECT", "LAUNCH");
		assertThat(draft.insightCards().get(0).insight()).isEqualTo("Fast.");
		assertThat(draft.milestoneHeadline()).isEqualTo("THREE STEPS");
		assertThat(draft.milestones().get(0).subtext()).isEqualTo("Week one");
	}

	@Test
	void shouldIgnoreModulesThatWereNotPickedTest() {
		// Given: a brief with only the KPIs module, and a reply that offers everything
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Brief", List.of(OnePagerModuleKey.KPIS));

		// When: the reply is parsed
		OnePagerDraft draft = parser().parseDraft(FULL_REPLY, brief);

		// Then: only the header and the KPIs are kept; everything else is empty or null
		assertThat(draft.kpis()).hasSize(1);
		assertThat(draft.kpiDisclaimer()).isEqualTo("Based on data.");
		assertThat(draft.processSteps()).isEmpty();
		assertThat(draft.processSectionLabel()).isNull();
		assertThat(draft.solutions()).isEmpty();
		assertThat(draft.solutionsVisualLabel()).isNull();
		assertThat(draft.imageTextBody()).isNull();
		assertThat(draft.keyTakeaway()).isNull();
		assertThat(draft.impactStats()).isEmpty();
		assertThat(draft.capabilityCards()).isEmpty();
		assertThat(draft.spotlightCards()).isEmpty();
		assertThat(draft.insightCards()).isEmpty();
		assertThat(draft.milestoneHeadline()).isNull();
		assertThat(draft.milestones()).isEmpty();
	}

	@Test
	void shouldNameEveryMissingFieldOfAnIncompleteReplyTest() {
		// Given: a brief with solutions and a reply that leaves fields blank and nested ones as placeholders
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Brief",
				List.of(OnePagerModuleKey.SOLUTIONS, OnePagerModuleKey.MILESTONE_BANNER));
		String reply = "{\"topicPill\":\"AUTO\",\"mainTitle\":[\"T\"],\"supportingCopy\":\"\","
				+ "\"solutionsSectionLabel\":\"S\",\"solutions\":[{\"title\":\"VIN\",\"valueLine\":\"N/A\"}],"
				+ "\"milestoneHeadline\":\"H\",\"milestones\":[]}";

		// When-Then: the failure lists each missing path so the user knows what the brief lacked
		assertThatThrownBy(() -> parser().parseDraft(reply, brief))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("AI draft incomplete")
				.hasMessageContaining("supportingCopy")
				.hasMessageContaining("solutions[0].valueLine")
				.hasMessageContaining("solutions[0].body");
	}
}
