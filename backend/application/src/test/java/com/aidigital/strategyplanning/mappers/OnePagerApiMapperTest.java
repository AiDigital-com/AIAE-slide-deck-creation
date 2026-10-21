package com.aidigital.strategyplanning.mappers;

import com.aidigital.strategyplanning.api.v1.model.CreateOnePagerRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerCapabilityCardV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDeckV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftRequestV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerDraftV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerInsightV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerKpiV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerMilestoneV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerModuleKeyV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerShellVariantV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerSolutionV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerSpotlightCardV1;
import com.aidigital.strategyplanning.api.v1.model.OnePagerStatV1;
import com.aidigital.strategyplanning.mappers.onepager.OnePagerApiMapper;
import com.aidigital.strategyplanning.service.onepager.models.CreateOnePagerCommand;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDeck;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsight;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestone;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolution;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStat;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One Page Generator API boundary mapping: every field of the draft crosses in
 * both directions, since a dropped mapping shows up as a blank module on the page.
 */
class OnePagerApiMapperTest {

	private static OnePagerDraft fullDraft() {
		return new OnePagerDraft("AUTOMOTIVE", List.of("SOLVING", "FRAGMENTATION"), "With speed",
				List.of(OnePagerModuleKey.TRACKER, OnePagerModuleKey.SOLUTIONS_VISUAL), "HOW IT WORKS",
				List.of("UNDERSTAND"), "SOLUTIONS", List.of(new OnePagerSolution("VIN", "Value", "Body")), "VISUALS",
				"SIGNALS", List.of(new OnePagerKpi("30%", "LIFT")), "Based on data.", "WHY", "Because.", "Takeaway.",
				List.of(new OnePagerStat("+45%", "FASTER")),
				List.of(new OnePagerCapabilityCard("SYNC", "Desc", List.of("A", "B"), "Foot")),
				List.of(new OnePagerSpotlightCard("CHIPS", "Desc", List.of("X"), List.of(new OnePagerKpi("2X", "SALES")),
						List.of("CONNECT"))),
				List.of(new OnePagerInsight("SPEED", "Fast.")), "THREE STEPS",
				List.of(new OnePagerMilestone("CONNECT", "Week one")));
	}

	@Test
	void shouldMapEveryDraftFieldOntoTheResponseTest() {
		// Given: a draft with a distinct value in every field
		OnePagerApiMapper mapper = Mappers.getMapper(OnePagerApiMapper.class);

		// When: it is converted for the API
		OnePagerDraftV1 response = mapper.toV1(fullDraft());

		// Then: nothing is lost on the way to the client
		assertThat(response.getTopicPill()).isEqualTo("AUTOMOTIVE");
		assertThat(response.getMainTitle()).containsExactly("SOLVING", "FRAGMENTATION");
		assertThat(response.getSupportingCopy()).isEqualTo("With speed");
		assertThat(response.getModuleOrder())
				.containsExactly(OnePagerModuleKeyV1.TRACKER, OnePagerModuleKeyV1.SOLUTIONS_VISUAL);
		assertThat(response.getProcessSectionLabel()).isEqualTo("HOW IT WORKS");
		assertThat(response.getProcessSteps()).containsExactly("UNDERSTAND");
		assertThat(response.getSolutionsSectionLabel()).isEqualTo("SOLUTIONS");
		assertThat(response.getSolutions()).containsExactly(new OnePagerSolutionV1("VIN", "Value", "Body"));
		assertThat(response.getSolutionsVisualLabel()).isEqualTo("VISUALS");
		assertThat(response.getKpiSectionLabel()).isEqualTo("SIGNALS");
		assertThat(response.getKpis()).containsExactly(new OnePagerKpiV1("30%", "LIFT"));
		assertThat(response.getKpiDisclaimer()).isEqualTo("Based on data.");
		assertThat(response.getImageTextSectionLabel()).isEqualTo("WHY");
		assertThat(response.getImageTextBody()).isEqualTo("Because.");
		assertThat(response.getKeyTakeaway()).isEqualTo("Takeaway.");
		assertThat(response.getImpactStats()).containsExactly(new OnePagerStatV1("+45%", "FASTER"));
		assertThat(response.getCapabilityCards())
				.containsExactly(new OnePagerCapabilityCardV1("SYNC", "Desc", List.of("A", "B"), "Foot"));
		assertThat(response.getSpotlightCards()).containsExactly(new OnePagerSpotlightCardV1("CHIPS", "Desc",
				List.of("X"), List.of(new OnePagerKpiV1("2X", "SALES")), List.of("CONNECT")));
		assertThat(response.getInsightCards()).containsExactly(new OnePagerInsightV1("SPEED", "Fast."));
		assertThat(response.getMilestoneHeadline()).isEqualTo("THREE STEPS");
		assertThat(response.getMilestones()).containsExactly(new OnePagerMilestoneV1("CONNECT", "Week one"));
	}

	@Test
	void shouldRoundTripAReviewedDraftIntoTheCreateCommandTest() {
		// Given: the API form of the full draft inside a create request
		OnePagerApiMapper mapper = Mappers.getMapper(OnePagerApiMapper.class);
		OnePagerDraftV1 reviewed = mapper.toV1(fullDraft());
		CreateOnePagerRequestV1 request = new CreateOnePagerRequestV1("Automotive", "Acme",
				OnePagerShellVariantV1.HERO_GRAPHIC, reviewed);

		// When: it is converted to the service command
		CreateOnePagerCommand command = mapper.toCommand(request, "user_1");

		// Then: the draft survives the round trip and the shell and creator are attached
		assertThat(command.content()).isEqualTo(fullDraft());
		assertThat(mapper.toModel(reviewed)).isEqualTo(fullDraft());
		assertThat(command.shellVariant()).isEqualTo(OnePagerShell.HERO_GRAPHIC);
		assertThat(command.category()).isEqualTo("Automotive");
		assertThat(command.clientName()).isEqualTo("Acme");
		assertThat(command.createdBy()).isEqualTo("user_1");
	}

	@Test
	void shouldMapTheBriefAndTheCreatedDeckTest() {
		// Given: a draft request and a created page
		OnePagerApiMapper mapper = Mappers.getMapper(OnePagerApiMapper.class);
		OnePagerDraftRequestV1 request = new OnePagerDraftRequestV1("Automotive", "Acme", "Sell cars.",
				List.of(OnePagerModuleKeyV1.KPIS, OnePagerModuleKeyV1.PHOTO));

		// When: both are converted
		OnePagerBrief brief = mapper.toBrief(request);
		OnePagerDeckV1 deck = mapper.toDeckV1(new OnePagerDeck("Title", "https://docs.google.com/x"));

		// Then: the brief keeps the module order and the deck its link
		assertThat(brief).isEqualTo(new OnePagerBrief("Automotive", "Acme", "Sell cars.",
				List.of(OnePagerModuleKey.KPIS, OnePagerModuleKey.PHOTO)));
		assertThat(deck.getTitle()).isEqualTo("Title");
		assertThat(deck.getSlidesUrl()).isEqualTo("https://docs.google.com/x");
	}

	@Test
	void shouldMapAbsentInputsAndAbsentModulesToNothingTest() {
		// Given: no input at all, and a draft whose every module list and label is absent
		OnePagerApiMapper mapper = Mappers.getMapper(OnePagerApiMapper.class);
		OnePagerDraft bare = new OnePagerDraft(null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null);
		OnePagerDraftV1 bareV1 = new OnePagerDraftV1();
		bareV1.setMainTitle(null);
		bareV1.setModuleOrder(null);
		bareV1.setProcessSteps(null);
		bareV1.setSolutions(null);
		bareV1.setKpis(null);
		bareV1.setImpactStats(null);
		bareV1.setCapabilityCards(null);
		bareV1.setSpotlightCards(null);
		bareV1.setInsightCards(null);
		bareV1.setMilestones(null);
		OnePagerSpotlightCardV1 bareCard = new OnePagerSpotlightCardV1();
		bareCard.setBullets(null);
		bareCard.setKpiChips(null);
		bareCard.setStageNames(null);
		OnePagerDraftV1 cardOnly = new OnePagerDraftV1();
		cardOnly.setSpotlightCards(List.of(bareCard));
		OnePagerCapabilityCardV1 bareCapability = new OnePagerCapabilityCardV1();
		bareCapability.setBullets(null);
		cardOnly.setCapabilityCards(List.of(bareCapability));
		OnePagerDraftRequestV1 bareRequest = new OnePagerDraftRequestV1();
		bareRequest.setModuleOrder(null);
		CreateOnePagerRequestV1 noContent = new CreateOnePagerRequestV1("Automotive", "Acme",
				OnePagerShellVariantV1.TOP_BAR, null);

		// When-Then: null in gives null out; absent lists stay null on the service side and come
		// back as the DTO's empty defaults on the API side
		assertThat(mapper.toV1(null)).isNull();
		assertThat(mapper.toModel(null)).isNull();
		assertThat(mapper.toBrief(null)).isNull();
		assertThat(mapper.toCommand(null, null)).isNull();
		assertThat(mapper.toDeckV1(null)).isNull();
		OnePagerDraftV1 bareOut = mapper.toV1(bare);
		assertThat(bareOut.getMainTitle()).isNullOrEmpty();
		assertThat(bareOut.getModuleOrder()).isNullOrEmpty();
		assertThat(bareOut.getSolutions()).isNullOrEmpty();
		assertThat(bareOut.getSpotlightCards()).isNullOrEmpty();
		assertThat(bareOut.getMilestones()).isNullOrEmpty();
		OnePagerDraft bareBack = mapper.toModel(bareV1);
		assertThat(bareBack.mainTitle()).isNull();
		assertThat(bareBack.moduleOrder()).isNull();
		assertThat(bareBack.kpis()).isNull();
		assertThat(bareBack.insightCards()).isNull();
		OnePagerDraft cardBack = mapper.toModel(cardOnly);
		assertThat(cardBack.spotlightCards().get(0).bullets()).isNull();
		assertThat(cardBack.spotlightCards().get(0).kpiChips()).isNull();
		assertThat(cardBack.spotlightCards().get(0).stageNames()).isNull();
		assertThat(cardBack.capabilityCards().get(0).bullets()).isNull();
		assertThat(mapper.toV1(cardBack).getSpotlightCards().get(0).getKpiChips()).isNullOrEmpty();
		assertThat(mapper.toV1(cardBack).getCapabilityCards().get(0).getBullets()).isNullOrEmpty();
		assertThat(mapper.toBrief(bareRequest).moduleOrder()).isNull();
		assertThat(mapper.toCommand(noContent, "user_1").content()).isNull();
		assertThat(mapper.toCommand(noContent, null).createdBy()).isNull();
	}
}
