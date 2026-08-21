package com.aidigital.strategyplanning.service.mappers;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateCategoryAnalysisCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.mappers.categoryanalysis.CategoryAnalysisMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the category analysis entity/record mapping.
 *
 * <p>This aggregate is written by two different flows — the manual builder and the standard deck
 * builder — and each deliberately leaves different columns unset. Both paths are pinned here so a
 * later mapping change cannot start writing a column one flow is supposed to leave alone.
 */
class CategoryAnalysisMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 14, 45);

	@Test
	void shouldMapEveryEntityFieldOntoTheRecordTest() {
		// Given: a persisted analysis with a distinct value in every column
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);
		CategoryAnalysisEntity entity = new CategoryAnalysisEntity();
		entity.setId(31L);
		entity.setTitle("Pet Care 2026");
		entity.setCategory("Pet Care");
		entity.setMarketOverview("Growing steadily.");
		entity.setKeyPlayers("Acme, Globex");
		entity.setTrends("Premiumisation");
		entity.setOpportunities("Subscription refills");
		entity.setRecommendations("Launch a bundle");
		entity.setSourceFileNames("market.pdf,notes.docx");
		entity.setSlidesUrl("https://docs.google.com/presentation/d/deck_1/edit");
		entity.setTemplateKind("STANDARD");
		entity.setStatus("GENERATED");
		entity.setCreatedBy("user_1");
		entity.setCreatedAt(CREATED_AT);

		// When: it is converted for the API layer
		CategoryAnalysisRecord record = mapper.toRecord(entity);

		// Then: every column survives the trip
		assertThat(record.id()).isEqualTo(31L);
		assertThat(record.title()).isEqualTo("Pet Care 2026");
		assertThat(record.category()).isEqualTo("Pet Care");
		assertThat(record.marketOverview()).isEqualTo("Growing steadily.");
		assertThat(record.keyPlayers()).isEqualTo("Acme, Globex");
		assertThat(record.trends()).isEqualTo("Premiumisation");
		assertThat(record.opportunities()).isEqualTo("Subscription refills");
		assertThat(record.recommendations()).isEqualTo("Launch a bundle");
		assertThat(record.sourceFileNames()).isEqualTo("market.pdf,notes.docx");
		assertThat(record.slidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(record.templateKind()).isEqualTo("STANDARD");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapANullEntityToNullTest() {
		// Given: a lookup that found nothing
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);

		// When-Then: the absence passes through rather than becoming an empty analysis
		assertThat(mapper.toRecord(null)).isNull();
		assertThat(mapper.toRecords(null)).isNull();
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two saved analyses in list order
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);
		CategoryAnalysisEntity first = new CategoryAnalysisEntity();
		first.setTitle("First");
		CategoryAnalysisEntity second = new CategoryAnalysisEntity();
		second.setTitle("Second");

		// When: the list is converted
		List<CategoryAnalysisRecord> records = mapper.toRecords(List.of(first, second));

		// Then: the query's order is the order the user sees
		assertThat(records).extracting(CategoryAnalysisRecord::title)
				.containsExactly("First", "Second");
	}

	@Test
	void shouldBuildAManualAnalysisWithoutADeckYetTest() {
		// Given: the manual builder's command, which is saved before any deck exists
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);
		CreateCategoryAnalysisCommand command = new CreateCategoryAnalysisCommand(
				"Pet Care 2026", "Pet Care", "Growing steadily.", "Acme, Globex",
				"Premiumisation", "Subscription refills", "Launch a bundle",
				"market.pdf", "user_1");

		// When: the entity to persist is built
		CategoryAnalysisEntity entity = mapper.toEntity(command, "GENERATED", CREATED_AT);

		// Then: the written sections land, and the deck-only columns stay unset because this
		// flow has not published anything yet
		assertThat(entity.getId()).isNull();
		assertThat(entity.getTitle()).isEqualTo("Pet Care 2026");
		assertThat(entity.getCategory()).isEqualTo("Pet Care");
		assertThat(entity.getMarketOverview()).isEqualTo("Growing steadily.");
		assertThat(entity.getKeyPlayers()).isEqualTo("Acme, Globex");
		assertThat(entity.getTrends()).isEqualTo("Premiumisation");
		assertThat(entity.getOpportunities()).isEqualTo("Subscription refills");
		assertThat(entity.getRecommendations()).isEqualTo("Launch a bundle");
		assertThat(entity.getSourceFileNames()).isEqualTo("market.pdf");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedBy()).isEqualTo("user_1");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(entity.getSlidesUrl()).isNull();
		assertThat(entity.getTemplateKind()).isNull();
	}

	@Test
	void shouldBuildAStandardDeckRowWithoutTheManualSectionsTest() {
		// Given: the standard deck flow, whose content lives in the deck rather than in columns
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);
		CreateStandardDeckCommand command = new CreateStandardDeckCommand("Pet Care", "Acme",
				List.of(new StandardFieldValue("headline", "Pets are premium now")), "user_1");

		// When: the entity to persist is built
		CategoryAnalysisEntity entity = mapper.toStandardDeckEntity(command, "Pet Care — Acme",
				"https://docs.google.com/presentation/d/deck_1/edit", "STANDARD",
				"GENERATED", CREATED_AT);

		// Then: the deck's own values land while the manual sections stay null, so a standard
		// deck is never mistaken for a hand-written analysis
		assertThat(entity.getId()).isNull();
		assertThat(entity.getTitle()).isEqualTo("Pet Care — Acme");
		assertThat(entity.getCategory()).isEqualTo("Pet Care");
		assertThat(entity.getSlidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(entity.getTemplateKind()).isEqualTo("STANDARD");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedBy()).isEqualTo("user_1");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(entity.getMarketOverview()).isNull();
		assertThat(entity.getKeyPlayers()).isNull();
		assertThat(entity.getTrends()).isNull();
		assertThat(entity.getOpportunities()).isNull();
		assertThat(entity.getRecommendations()).isNull();
		assertThat(entity.getSourceFileNames()).isNull();
	}

	@Test
	void shouldBuildEntitiesWhenTheCommandIsAbsentTest() {
		// Given: only the flow-side values, with no command behind them
		CategoryAnalysisMapper mapper = Mappers.getMapper(CategoryAnalysisMapper.class);

		// When: each entity is built anyway
		CategoryAnalysisEntity manual = mapper.toEntity(null, "GENERATED", CREATED_AT);
		CategoryAnalysisEntity standard = mapper.toStandardDeckEntity(null, "Title", "deck",
				"STANDARD", "GENERATED", CREATED_AT);

		// Then: those values still land rather than the mapper returning nothing
		assertThat(manual).isNotNull();
		assertThat(manual.getStatus()).isEqualTo("GENERATED");
		assertThat(manual.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(standard).isNotNull();
		assertThat(standard.getTitle()).isEqualTo("Title");
		assertThat(standard.getSlidesUrl()).isEqualTo("deck");
		assertThat(standard.getTemplateKind()).isEqualTo("STANDARD");
		assertThat(standard.getCategory()).isNull();
	}
}
