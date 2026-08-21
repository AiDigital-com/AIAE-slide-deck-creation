package com.aidigital.strategyplanning.service.mappers;

import com.aidigital.strategyplanning.domain.rfpoutline.entities.RfpOutlineEntity;
import com.aidigital.strategyplanning.service.mappers.rfpoutline.RfpOutlineMapper;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the RFP outline entity/record mapping.
 *
 * <p>Every field carries a distinct value so a dropped mapping fails the test rather than
 * matching by coincidence. A dropped field means a saved outline loses a section of the POV.
 */
class RfpOutlineMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 9, 30);

	@Test
	void shouldMapEveryEntityFieldOntoTheRecordTest() {
		// Given: a persisted outline with a distinct value in every column
		RfpOutlineMapper mapper = Mappers.getMapper(RfpOutlineMapper.class);
		RfpOutlineEntity entity = new RfpOutlineEntity();
		entity.setId(21L);
		entity.setTitle("Globex RFP");
		entity.setClientName("Globex");
		entity.setIndustry("Logistics");
		entity.setChallenge("Fragmented tracking.");
		entity.setOpportunity("Unified visibility.");
		entity.setSolution("One control tower.");
		entity.setOutcome("On-time delivery up.");
		entity.setDeckOutline("1. Context 2. Approach");
		entity.setSupplementaryNotes("Ask about incumbent.");
		entity.setSourceFileName("globex-rfp.pdf");
		entity.setDocUrl("https://docs.google.com/document/d/doc_1/edit");
		entity.setStatus("GENERATED");
		entity.setCreatedBy("user_1");
		entity.setCreatedAt(CREATED_AT);

		// When: it is converted for the API layer
		RfpOutlineRecord record = mapper.toRecord(entity);

		// Then: every section survives the trip
		assertThat(record.id()).isEqualTo(21L);
		assertThat(record.title()).isEqualTo("Globex RFP");
		assertThat(record.clientName()).isEqualTo("Globex");
		assertThat(record.industry()).isEqualTo("Logistics");
		assertThat(record.challenge()).isEqualTo("Fragmented tracking.");
		assertThat(record.opportunity()).isEqualTo("Unified visibility.");
		assertThat(record.solution()).isEqualTo("One control tower.");
		assertThat(record.outcome()).isEqualTo("On-time delivery up.");
		assertThat(record.deckOutline()).isEqualTo("1. Context 2. Approach");
		assertThat(record.supplementaryNotes()).isEqualTo("Ask about incumbent.");
		assertThat(record.sourceFileName()).isEqualTo("globex-rfp.pdf");
		assertThat(record.docUrl()).isEqualTo("https://docs.google.com/document/d/doc_1/edit");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapANullEntityToNullTest() {
		// Given: a lookup that found nothing
		RfpOutlineMapper mapper = Mappers.getMapper(RfpOutlineMapper.class);

		// When-Then: the absence passes through rather than becoming an empty outline
		assertThat(mapper.toRecord(null)).isNull();
		assertThat(mapper.toRecords(null)).isNull();
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two saved outlines in list order
		RfpOutlineMapper mapper = Mappers.getMapper(RfpOutlineMapper.class);
		RfpOutlineEntity first = new RfpOutlineEntity();
		first.setTitle("First");
		RfpOutlineEntity second = new RfpOutlineEntity();
		second.setTitle("Second");

		// When: the list is converted
		List<RfpOutlineRecord> records = mapper.toRecords(List.of(first, second));

		// Then: the query's order is the order the user sees
		assertThat(records).extracting(RfpOutlineRecord::title)
				.containsExactly("First", "Second");
	}

	@Test
	void shouldBuildAnUnsavedEntityFromTheCommandAndDocUrlTest() {
		// Given: a reviewed command plus the exported Google Doc
		RfpOutlineMapper mapper = Mappers.getMapper(RfpOutlineMapper.class);
		CreateRfpOutlineCommand command = new CreateRfpOutlineCommand(
				"Globex RFP", "Globex", "Logistics",
				"Fragmented tracking.", "Unified visibility.", "One control tower.",
				"On-time delivery up.", "1. Context 2. Approach", "Ask about incumbent.",
				"globex-rfp.pdf", "user_1");

		// When: the entity to persist is built
		RfpOutlineEntity entity = mapper.toEntity(command,
				"https://docs.google.com/document/d/doc_1/edit", "GENERATED", CREATED_AT);

		// Then: the command's fields and the doc URL are both persisted, id left to the database
		assertThat(entity.getId()).isNull();
		assertThat(entity.getTitle()).isEqualTo("Globex RFP");
		assertThat(entity.getClientName()).isEqualTo("Globex");
		assertThat(entity.getIndustry()).isEqualTo("Logistics");
		assertThat(entity.getChallenge()).isEqualTo("Fragmented tracking.");
		assertThat(entity.getOpportunity()).isEqualTo("Unified visibility.");
		assertThat(entity.getSolution()).isEqualTo("One control tower.");
		assertThat(entity.getOutcome()).isEqualTo("On-time delivery up.");
		assertThat(entity.getDeckOutline()).isEqualTo("1. Context 2. Approach");
		assertThat(entity.getSupplementaryNotes()).isEqualTo("Ask about incumbent.");
		assertThat(entity.getSourceFileName()).isEqualTo("globex-rfp.pdf");
		assertThat(entity.getDocUrl()).isEqualTo("https://docs.google.com/document/d/doc_1/edit");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedBy()).isEqualTo("user_1");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldBuildAnEntityWhenTheCommandIsAbsentTest() {
		// Given: only the export-side values
		RfpOutlineMapper mapper = Mappers.getMapper(RfpOutlineMapper.class);

		// When: the entity is built anyway
		RfpOutlineEntity entity = mapper.toEntity(null, "doc", "GENERATED", CREATED_AT);

		// Then: those values still land rather than the mapper returning nothing
		assertThat(entity).isNotNull();
		assertThat(entity.getTitle()).isNull();
		assertThat(entity.getDocUrl()).isEqualTo("doc");
		assertThat(entity.getStatus()).isEqualTo("GENERATED");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}
}
