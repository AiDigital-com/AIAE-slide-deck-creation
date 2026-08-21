package com.aidigital.strategyplanning.mappers;

import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisSummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CategoryAnalysisV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCategoryAnalysisRequestV1;
import com.aidigital.strategyplanning.api.v1.model.CreateStandardDeckRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.GoogleConnectionStatusV1;
import com.aidigital.strategyplanning.api.v1.model.StandardConnectionsV1;
import com.aidigital.strategyplanning.api.v1.model.StandardDraftV1;
import com.aidigital.strategyplanning.api.v1.model.StandardFieldValueV1;
import com.aidigital.strategyplanning.mappers.categoryanalysis.CategoryAnalysisApiMapper;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.DraftAlignment;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardConnections;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraft;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the category analysis API boundary mapping.
 *
 * <p>This aggregate has the widest API surface of the four: two write paths, a connection probe
 * the UI gates on, and the standard-draft review payload. Each is pinned here because a dropped
 * mapping is invisible until the frontend renders a blank.
 */
class CategoryAnalysisApiMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 14, 45);

	@Test
	void shouldMapEveryRecordFieldOntoTheResponseTest() {
		// Given: a saved analysis with a distinct value in every field
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		CategoryAnalysisRecord record = new CategoryAnalysisRecord(31L, "Pet Care 2026",
				"Pet Care", "Growing steadily.", "Acme, Globex", "Premiumisation",
				"Subscription refills", "Launch a bundle", "market.pdf",
				"https://docs.google.com/presentation/d/deck_1/edit", "STANDARD", "GENERATED",
				"user_1", CREATED_AT);

		// When: it is converted for the API
		CategoryAnalysisV1 response = mapper.toV1(record);

		// Then: nothing is lost on the way to the client
		assertThat(response.getId()).isEqualTo(31L);
		assertThat(response.getTitle()).isEqualTo("Pet Care 2026");
		assertThat(response.getCategory()).isEqualTo("Pet Care");
		assertThat(response.getMarketOverview()).isEqualTo("Growing steadily.");
		assertThat(response.getKeyPlayers()).isEqualTo("Acme, Globex");
		assertThat(response.getTrends()).isEqualTo("Premiumisation");
		assertThat(response.getOpportunities()).isEqualTo("Subscription refills");
		assertThat(response.getRecommendations()).isEqualTo("Launch a bundle");
		assertThat(response.getSourceFileNames()).isEqualTo("market.pdf");
		assertThat(response.getSlidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(response.getTemplateKind()).isEqualTo("STANDARD");
		assertThat(response.getStatus()).isEqualTo(GenerationStatusV1.GENERATED);
		assertThat(response.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldKeepTheSummaryToTheListColumnsButKeepTheDeckLinkTest() {
		// Given: the same saved analysis
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		CategoryAnalysisRecord record = new CategoryAnalysisRecord(31L, "Pet Care 2026",
				"Pet Care", "Growing steadily.", "Acme, Globex", "Premiumisation",
				"Subscription refills", "Launch a bundle", "market.pdf",
				"https://docs.google.com/presentation/d/deck_1/edit", "STANDARD", "GENERATED",
				"user_1", CREATED_AT);

		// When: it is converted for the list endpoint
		CategoryAnalysisSummaryV1 summary = mapper.toSummaryV1(record);

		// Then: the bodies are left out, but the deck link stays because the list offers
		// "open in Slides" directly
		assertThat(summary.getId()).isEqualTo(31L);
		assertThat(summary.getTitle()).isEqualTo("Pet Care 2026");
		assertThat(summary.getCategory()).isEqualTo("Pet Care");
		assertThat(summary.getSlidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(summary.getStatus()).isEqualTo(GenerationStatusV1.GENERATED);
		assertThat(summary.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two saved analyses in newest-first order
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		CategoryAnalysisRecord newer = new CategoryAnalysisRecord(2L, "Newer", null, null, null,
				null, null, null, null, null, null, "GENERATED", "user_1", CREATED_AT);
		CategoryAnalysisRecord older = new CategoryAnalysisRecord(1L, "Older", null, null, null,
				null, null, null, null, null, null, "SUBMITTED", "user_1", CREATED_AT);

		// When: the list is converted
		List<CategoryAnalysisSummaryV1> summaries = mapper.toSummaryV1List(List.of(newer, older));

		// Then: the service's order is the order the client renders
		assertThat(summaries).extracting(CategoryAnalysisSummaryV1::getTitle)
				.containsExactly("Newer", "Older");
	}

	@Test
	void shouldMapNullInputsToNullTest() {
		// Given: nothing to convert
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);

		// When-Then: the absence passes through rather than becoming an empty payload
		assertThat(mapper.toV1((CategoryAnalysisRecord) null)).isNull();
		assertThat(mapper.toSummaryV1(null)).isNull();
		assertThat(mapper.toSummaryV1List(null)).isNull();
		assertThat(mapper.toV1((StandardConnections) null)).isNull();
		assertThat(mapper.toV1((StandardDraft) null)).isNull();
		assertThat(mapper.toCommand((CreateCategoryAnalysisRequestV1) null, null)).isNull();
		assertThat(mapper.toCommand((CreateStandardDeckRequestV1) null, null)).isNull();
		assertThat(mapper.toFieldValues(null)).isNull();
	}

	@Test
	void shouldTakeTheCreatorFromTheSessionOnAManualCreateTest() {
		// Given: a manual create request from the browser
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		CreateCategoryAnalysisRequestV1 request = new CreateCategoryAnalysisRequestV1();
		request.setTitle("Pet Care 2026");
		request.setCategory("Pet Care");
		request.setMarketOverview("Growing steadily.");
		request.setKeyPlayers("Acme, Globex");
		request.setTrends("Premiumisation");
		request.setOpportunities("Subscription refills");
		request.setRecommendations("Launch a bundle");
		request.setSourceFileNames("market.pdf");

		// When: the command is built with the authenticated user id
		CreateCategoryAnalysisCommand command = mapper.toCommand(request, "user_1");

		// Then: the request's sections land, and ownership comes from the verified session
		assertThat(command.title()).isEqualTo("Pet Care 2026");
		assertThat(command.category()).isEqualTo("Pet Care");
		assertThat(command.marketOverview()).isEqualTo("Growing steadily.");
		assertThat(command.keyPlayers()).isEqualTo("Acme, Globex");
		assertThat(command.trends()).isEqualTo("Premiumisation");
		assertThat(command.opportunities()).isEqualTo("Subscription refills");
		assertThat(command.recommendations()).isEqualTo("Launch a bundle");
		assertThat(command.sourceFileNames()).isEqualTo("market.pdf");
		assertThat(command.createdBy()).isEqualTo("user_1");
	}

	@Test
	void shouldTakeTheCreatorFromTheSessionOnAStandardDeckCreateTest() {
		// Given: a reviewed standard deck request carrying its field values
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		StandardFieldValueV1 field = new StandardFieldValueV1();
		field.setKey("headline");
		field.setValue("Pets are premium now");
		CreateStandardDeckRequestV1 request = new CreateStandardDeckRequestV1();
		request.setCategory("Pet Care");
		request.setClientName("Acme");
		request.setFields(List.of(field));

		// When: the command is built with the authenticated user id
		CreateStandardDeckCommand command = mapper.toCommand(request, "user_1");

		// Then: the reviewed field values reach the deck builder unchanged
		assertThat(command.category()).isEqualTo("Pet Care");
		assertThat(command.clientName()).isEqualTo("Acme");
		assertThat(command.fields()).containsExactly(
				new StandardFieldValue("headline", "Pets are premium now"));
		assertThat(command.createdBy()).isEqualTo("user_1");
	}

	@Test
	void shouldMapFieldValuesOnTheirOwnTest() {
		// Given: the field values the review form submits
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		StandardFieldValueV1 first = new StandardFieldValueV1();
		first.setKey("headline");
		first.setValue("Pets are premium now");
		StandardFieldValueV1 second = new StandardFieldValueV1();
		second.setKey("trend_1");
		second.setValue(null);

		// When: they are converted
		List<StandardFieldValue> values = mapper.toFieldValues(List.of(first, second));

		// Then: order and a deliberately empty field both survive, since the deck fills tokens
		// positionally and an empty value must still clear its placeholder
		assertThat(values).containsExactly(
				new StandardFieldValue("headline", "Pets are premium now"),
				new StandardFieldValue("trend_1", null));
	}

	@Test
	void shouldReportEachGoogleConnectionStateToTheUiTest() {
		// Given: the three states the connection probe can return
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);

		// When-Then: each maps to its own API value, so the UI can distinguish "sign in" from
		// "reconnect and grant access"
		assertThat(mapper.toV1(new StandardConnections(true, true,
				GoogleConnectionStatus.CONNECTED)).getGoogleStatus())
				.isEqualTo(GoogleConnectionStatusV1.CONNECTED);
		assertThat(mapper.toV1(new StandardConnections(true, false,
				GoogleConnectionStatus.MISSING_SCOPES)).getGoogleStatus())
				.isEqualTo(GoogleConnectionStatusV1.MISSING_SCOPES);
		assertThat(mapper.toV1(new StandardConnections(false, false,
				GoogleConnectionStatus.NOT_CONNECTED)).getGoogleStatus())
				.isEqualTo(GoogleConnectionStatusV1.NOT_CONNECTED);
	}

	@Test
	void shouldMapBothConnectionFlagsIndependentlyTest() {
		// Given: AI configured but Google not connected
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);

		// When: the probe result is converted
		StandardConnectionsV1 response = mapper.toV1(
				new StandardConnections(true, false, GoogleConnectionStatus.NOT_CONNECTED));

		// Then: the two flags stay independent — the UI enables drafting while still blocking
		// deck publication
		assertThat(response.getAiConnected()).isTrue();
		assertThat(response.getGoogleConnected()).isFalse();
	}

	@Test
	void shouldMapTheStandardDraftWithItsAlignmentVerdictTest() {
		// Given: a drafted deck plus the check of whether it matches the client's business
		CategoryAnalysisApiMapper mapper = Mappers.getMapper(CategoryAnalysisApiMapper.class);
		StandardDraft draft = new StandardDraft(
				List.of(new StandardDraftField("headline", "Headline", 1, "Pets are premium now")),
				new DraftAlignment("Premium pet food retail", true, false,
						"The client sells food, not accessories."));

		// When: it is converted for the review form
		StandardDraftV1 response = mapper.toV1(draft);

		// Then: the field carries its slide number so the form can group by slide, and the
		// alignment verdict reaches the user even when it disagrees with the draft
		assertThat(response.getFields()).hasSize(1);
		assertThat(response.getFields().get(0).getKey()).isEqualTo("headline");
		assertThat(response.getFields().get(0).getLabel()).isEqualTo("Headline");
		assertThat(response.getFields().get(0).getSlideNumber()).isEqualTo(1);
		assertThat(response.getFields().get(0).getValue()).isEqualTo("Pets are premium now");
		assertThat(response.getAlignment().getClientBusinessFocus())
				.isEqualTo("Premium pet food retail");
		assertThat(response.getAlignment().getConfirmed()).isTrue();
		assertThat(response.getAlignment().getMatches()).isFalse();
		assertThat(response.getAlignment().getMessage())
				.isEqualTo("The client sells food, not accessories.");
	}
}
