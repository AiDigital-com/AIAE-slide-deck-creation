package com.aidigital.strategyplanning.service.mappers;

import com.aidigital.strategyplanning.domain.rndrequest.entities.RndRequestEntity;
import com.aidigital.strategyplanning.service.mappers.rndrequest.RndRequestMapper;
import com.aidigital.strategyplanning.service.rndrequest.models.CreateRndRequestCommand;
import com.aidigital.strategyplanning.service.rndrequest.models.RndRequestRecord;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the RnD request entity/record mapping.
 *
 * <p>The buy amount decides whether a request escalates, so it has to survive mapping as an exact
 * decimal rather than being rounded or re-parsed through a double.
 */
class RndRequestMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 16, 5);

	@Test
	void shouldMapEveryEntityFieldOntoTheRecordTest() {
		// Given: a triaged request with a distinct value in every column
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);
		RndRequestEntity entity = new RndRequestEntity();
		entity.setId(41L);
		entity.setTitle("Programmatic audio buy");
		entity.setRequesterTeam("Growth");
		entity.setRequestDetails("Client wants audio inventory.");
		entity.setCapabilityNotes("We have a Spotify integration.");
		entity.setBuyAmount(new BigDecimal("100000.50"));
		entity.setDecision("ESCALATE_TO_RND");
		entity.setCapabilitySummary("Found 2 matching capabilities.");
		entity.setResponseDraft("RND ESCALATION");
		entity.setStatus("SUBMITTED");
		entity.setCreatedBy("user_1");
		entity.setCreatedAt(CREATED_AT);

		// When: it is converted for the API layer
		RndRequestRecord record = mapper.toRecord(entity);

		// Then: nothing is lost, and the buy amount keeps its exact scale because the
		// escalation threshold is compared against it
		assertThat(record.id()).isEqualTo(41L);
		assertThat(record.title()).isEqualTo("Programmatic audio buy");
		assertThat(record.requesterTeam()).isEqualTo("Growth");
		assertThat(record.requestDetails()).isEqualTo("Client wants audio inventory.");
		assertThat(record.capabilityNotes()).isEqualTo("We have a Spotify integration.");
		assertThat(record.buyAmount()).isEqualByComparingTo(new BigDecimal("100000.50"));
		assertThat(record.buyAmount().scale()).isEqualTo(2);
		assertThat(record.decision()).isEqualTo("ESCALATE_TO_RND");
		assertThat(record.capabilitySummary()).isEqualTo("Found 2 matching capabilities.");
		assertThat(record.responseDraft()).isEqualTo("RND ESCALATION");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		assertThat(record.createdBy()).isEqualTo("user_1");
		assertThat(record.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapANullEntityToNullTest() {
		// Given: a lookup that found nothing
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);

		// When-Then: the absence passes through rather than becoming an empty request
		assertThat(mapper.toRecord(null)).isNull();
		assertThat(mapper.toRecords(null)).isNull();
	}

	@Test
	void shouldMapAnEntityWithNoOptionalValuesSetTest() {
		// Given: a request saved before triage filled in the derived fields
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);
		RndRequestEntity entity = new RndRequestEntity();
		entity.setTitle("Untriaged");

		// When: it is converted
		RndRequestRecord record = mapper.toRecord(entity);

		// Then: absent columns stay absent instead of becoming empty strings
		assertThat(record.title()).isEqualTo("Untriaged");
		assertThat(record.buyAmount()).isNull();
		assertThat(record.decision()).isNull();
		assertThat(record.capabilitySummary()).isNull();
		assertThat(record.responseDraft()).isNull();
		assertThat(record.createdAt()).isNull();
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two triaged requests in list order
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);
		RndRequestEntity newer = new RndRequestEntity();
		newer.setTitle("Newer");
		RndRequestEntity older = new RndRequestEntity();
		older.setTitle("Older");

		// When: the list is converted
		List<RndRequestRecord> records = mapper.toRecords(List.of(newer, older));

		// Then: the query's order is the order the user sees
		assertThat(records).extracting(RndRequestRecord::title).containsExactly("Newer", "Older");
	}

	@Test
	void shouldBuildAnUnsavedEntityFromTheCommandAndTriageOutcomeTest() {
		// Given: the submitted request plus everything triage derived from it
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);
		CreateRndRequestCommand command = new CreateRndRequestCommand(
				"Programmatic audio buy", "Growth", "Client wants audio inventory.",
				"We have a Spotify integration.", new BigDecimal("100000.50"), "user_1");

		// When: the entity to persist is built
		RndRequestEntity entity = mapper.toEntity(command, "ESCALATE_TO_RND",
				"Found 2 matching capabilities.", "RND ESCALATION", "SUBMITTED", CREATED_AT);

		// Then: the submission and the triage outcome are stored together, id left to the
		// database
		assertThat(entity.getId()).isNull();
		assertThat(entity.getTitle()).isEqualTo("Programmatic audio buy");
		assertThat(entity.getRequesterTeam()).isEqualTo("Growth");
		assertThat(entity.getRequestDetails()).isEqualTo("Client wants audio inventory.");
		assertThat(entity.getCapabilityNotes()).isEqualTo("We have a Spotify integration.");
		assertThat(entity.getBuyAmount()).isEqualByComparingTo(new BigDecimal("100000.50"));
		assertThat(entity.getDecision()).isEqualTo("ESCALATE_TO_RND");
		assertThat(entity.getCapabilitySummary()).isEqualTo("Found 2 matching capabilities.");
		assertThat(entity.getResponseDraft()).isEqualTo("RND ESCALATION");
		assertThat(entity.getStatus()).isEqualTo("SUBMITTED");
		assertThat(entity.getCreatedBy()).isEqualTo("user_1");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldBuildAnEntityWhenTheCommandIsAbsentTest() {
		// Given: only the triage-side values
		RndRequestMapper mapper = Mappers.getMapper(RndRequestMapper.class);

		// When: the entity is built anyway
		RndRequestEntity entity = mapper.toEntity(null, "RELAY_WORKAROUND",
				"No capabilities found.", "Suggested workaround", "SUBMITTED", CREATED_AT);

		// Then: those values still land rather than the mapper returning nothing
		assertThat(entity).isNotNull();
		assertThat(entity.getTitle()).isNull();
		assertThat(entity.getBuyAmount()).isNull();
		assertThat(entity.getDecision()).isEqualTo("RELAY_WORKAROUND");
		assertThat(entity.getCapabilitySummary()).isEqualTo("No capabilities found.");
		assertThat(entity.getResponseDraft()).isEqualTo("Suggested workaround");
		assertThat(entity.getStatus()).isEqualTo("SUBMITTED");
		assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
	}
}
