package com.aidigital.strategyplanning.service.mappers;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.mappers.casestudy.CaseStudyMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the case study entity/record mapping.
 *
 * <p>Every field is given a distinct value so a mapping that is silently dropped shows up as a
 * failure rather than passing on a coincidental match. A dropped field here means a saved case
 * study comes back to the user with an empty section.
 */
class CaseStudyMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 10, 15);

	@Test
	void shouldMapEveryEntityFieldOntoTheRecordTest() {
		// Given: a persisted case study with a distinct value in every column
		CaseStudyMapper mapper = Mappers.getMapper(CaseStudyMapper.class);
		CaseStudyEntity entity = new CaseStudyEntity();
		entity.setId(11L);
		entity.setTitle("Acme Turnaround");
		entity.setClientName("Acme");
		entity.setIndustry("Retail");
		entity.setChallenge("Sales were declining.");
		entity.setSolution("We rebuilt the funnel.");
		entity.setResults("Revenue grew.");
		entity.setKeyMetrics("40% cost reduction");
		entity.setTimeline("6 months");
		entity.setTestimonial("Best decision we made.");
		entity.setSourceFileName("acme-report.pdf");
		entity.setTemplateUrl("https://docs.google.com/presentation/d/template/edit");
		entity.setSlidesUrl("https://docs.google.com/presentation/d/deck_1/edit");
		entity.setStatus("GENERATED");
		entity.setCreatedBy("user_1");
		entity.setCreatedAt(CREATED_AT);

		// When: it is converted for the API layer
		CaseStudyRecord record = mapper.toRecord(entity);

		// Then: nothing is lost on the way out
		assertThat(record.id()).isEqualTo(11L);
		assertThat(record.title()).isEqualTo("Acme Turnaround");
		assertThat(record.clientName()).isEqualTo("Acme");
		assertThat(record.industry()).isEqualTo("Retail");
		assertThat(record.challenge()).isEqualTo("Sales were declining.");
		assertThat(record.solution()).isEqualTo("We rebuilt the funnel.");
		assertThat(record.results()).isEqualTo("Revenue grew.");
		assertThat(record.keyMetrics()).isEqualTo("40% cost reduction");
		assertThat(record.timeline()).isEqualTo("6 months");
		assertThat(record.testimonial()).isEqualTo("Best decision we made.");
		assertThat(record.sourceFileName()).isEqualTo("acme-report.pdf");
		assertThat(record.templateUrl())
				.isEqualTo("https://docs.google.com/presentation/d/template/edit");
		assertThat(record.slidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapANullEntityToNullTest() {
		// Given: a lookup that found nothing
		CaseStudyMapper mapper = Mappers.getMapper(CaseStudyMapper.class);

		// When-Then: the absence passes through instead of becoming an empty record
		assertThat(mapper.toRecord(null)).isNull();
		assertThat(mapper.toRecords(null)).isNull();
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two saved case studies in list order
		CaseStudyMapper mapper = Mappers.getMapper(CaseStudyMapper.class);
		CaseStudyEntity first = new CaseStudyEntity();
		first.setTitle("First");
		CaseStudyEntity second = new CaseStudyEntity();
		second.setTitle("Second");

		// When: the list is converted
		List<CaseStudyRecord> records = mapper.toRecords(List.of(first, second));

		// Then: the order the query returned is the order the user sees
		assertThat(records).extracting(CaseStudyRecord::title)
				.containsExactly("First", "Second");
	}

	@Test
	void shouldBuildAnUnsavedEntityFromTheCommandAndDeckValuesTest() {
		// Given: a reviewed create command plus the values produced while generating the deck
		CaseStudyMapper mapper = Mappers.getMapper(CaseStudyMapper.class);
		CreateCaseStudyCommand command = new CreateCaseStudyCommand(
				"Acme Turnaround", "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.", "Revenue grew.",
				"40% cost reduction", "6 months", "Best decision we made.",
				"acme-report.pdf", "user_1");

		// When: the entity to persist is built
		CaseStudyEntity entity = mapper.toEntity(command,
				"https://docs.google.com/presentation/d/template/edit",
				"https://docs.google.com/presentation/d/deck_1/edit",
				"GENERATED", CREATED_AT);

		// Then: the command's fields and the deck's own values are both persisted, and the id
		// is left for the database to assign
		assertThat(entity.getId()).isNull();
		assertThat(entity.getTitle()).isEqualTo("Acme Turnaround");
		assertThat(entity.getClientName()).isEqualTo("Acme");
		assertThat(entity.getIndustry()).isEqualTo("Retail");
		assertThat(entity.getChallenge()).isEqualTo("Sales were declining.");
		assertThat(entity.getSolution()).isEqualTo("We rebuilt the funnel.");
		assertThat(entity.getResults()).isEqualTo("Revenue grew.");
		assertThat(entity.getKeyMetrics()).isEqualTo("40% cost reduction");
		assertThat(entity.getTimeline()).isEqualTo("6 months");
		assertThat(entity.getTestimonial()).isEqualTo("Best decision we made.");
		assertThat(entity.getSourceFileName()).isEqualTo("acme-report.pdf");
		assertThat(entity.getTemplateUrl())
				.isEqualTo("https://docs.google.com/presentation/d/template/edit");
		assertThat(entity.getSlidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedBy()).isEqualTo("user_1");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldBuildAnEntityWhenTheCommandIsAbsentTest() {
		// Given: only the deck-side values, with no command behind them
		CaseStudyMapper mapper = Mappers.getMapper(CaseStudyMapper.class);

		// When: the entity is built anyway
		CaseStudyEntity entity =
				mapper.toEntity(null, "template", "deck", "GENERATED", CREATED_AT);

		// Then: the deck values still land rather than the mapper returning nothing
		assertThat(entity).isNotNull();
		assertThat(entity.getTitle()).isNull();
		assertThat(entity.getTemplateUrl()).isEqualTo("template");
		assertThat(entity.getSlidesUrl()).isEqualTo("deck");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}
}
