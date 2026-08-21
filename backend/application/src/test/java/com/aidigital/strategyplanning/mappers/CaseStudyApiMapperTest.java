package com.aidigital.strategyplanning.mappers;

import com.aidigital.strategyplanning.api.v1.model.CaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudySummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudyV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCaseStudyRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.mappers.casestudy.CaseStudyApiMapper;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the case study API boundary mapping.
 *
 * <p>This is the contract the frontend's generated client consumes, so every field gets a
 * distinct value: a dropped mapping here shows up as a blank section in the UI, and the summary
 * DTO deliberately carries less than the full one.
 */
class CaseStudyApiMapperTest {

	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 23, 10, 15);

	@Test
	void shouldMapEveryRecordFieldOntoTheResponseTest() {
		// Given: a saved case study with a distinct value in every field
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CaseStudyRecord record = new CaseStudyRecord(11L, "Acme Turnaround", "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.", "Revenue grew.",
				"40% cost reduction", "6 months", "Best decision we made.", "acme-report.pdf",
				"https://docs.google.com/presentation/d/template/edit",
				"https://docs.google.com/presentation/d/deck_1/edit", "GENERATED", "user_1",
				CREATED_AT);

		// When: it is converted for the API
		CaseStudyV1 response = mapper.toV1(record);

		// Then: nothing is lost on the way to the client
		assertThat(response.getId()).isEqualTo(11L);
		assertThat(response.getTitle()).isEqualTo("Acme Turnaround");
		assertThat(response.getClientName()).isEqualTo("Acme");
		assertThat(response.getIndustry()).isEqualTo("Retail");
		assertThat(response.getChallenge()).isEqualTo("Sales were declining.");
		assertThat(response.getSolution()).isEqualTo("We rebuilt the funnel.");
		assertThat(response.getResults()).isEqualTo("Revenue grew.");
		assertThat(response.getKeyMetrics()).isEqualTo("40% cost reduction");
		assertThat(response.getTimeline()).isEqualTo("6 months");
		assertThat(response.getTestimonial()).isEqualTo("Best decision we made.");
		assertThat(response.getSourceFileName()).isEqualTo("acme-report.pdf");
		assertThat(response.getTemplateUrl())
				.isEqualTo("https://docs.google.com/presentation/d/template/edit");
		assertThat(response.getSlidesUrl())
				.isEqualTo("https://docs.google.com/presentation/d/deck_1/edit");
		assertThat(response.getStatus()).isEqualTo(GenerationStatusV1.GENERATED);
		assertThat(response.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldKeepTheSummaryToTheListColumnsOnlyTest() {
		// Given: the same saved case study
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CaseStudyRecord record = new CaseStudyRecord(11L, "Acme Turnaround", "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.", "Revenue grew.",
				"40% cost reduction", "6 months", "Best decision we made.", "acme-report.pdf",
				"template", "deck", "GENERATED", "user_1", CREATED_AT);

		// When: it is converted for the list endpoint
		CaseStudySummaryV1 summary = mapper.toSummaryV1(record);

		// Then: the list carries only what the list shows, so a page of case studies does not
		// ship every full body over the wire
		assertThat(summary.getId()).isEqualTo(11L);
		assertThat(summary.getTitle()).isEqualTo("Acme Turnaround");
		assertThat(summary.getClientName()).isEqualTo("Acme");
		assertThat(summary.getIndustry()).isEqualTo("Retail");
		assertThat(summary.getStatus()).isEqualTo(GenerationStatusV1.GENERATED);
		assertThat(summary.getCreatedAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void shouldMapAListPreservingOrderTest() {
		// Given: two saved case studies in newest-first order
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CaseStudyRecord newer = new CaseStudyRecord(2L, "Newer", null, null, null, null, null,
				null, null, null, null, null, null, "GENERATED", "user_1", CREATED_AT);
		CaseStudyRecord older = new CaseStudyRecord(1L, "Older", null, null, null, null, null,
				null, null, null, null, null, null, "SUBMITTED", "user_1", CREATED_AT);

		// When: the list is converted
		List<CaseStudySummaryV1> summaries = mapper.toSummaryV1List(List.of(newer, older));

		// Then: the service's order is the order the client renders
		assertThat(summaries).extracting(CaseStudySummaryV1::getTitle)
				.containsExactly("Newer", "Older");
		assertThat(summaries).extracting(CaseStudySummaryV1::getStatus)
				.containsExactly(GenerationStatusV1.GENERATED, GenerationStatusV1.SUBMITTED);
	}

	@Test
	void shouldMapNullRecordsAndListsToNullTest() {
		// Given: nothing to convert
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);

		// When-Then: the absence passes through rather than becoming an empty payload
		assertThat(mapper.toV1(null)).isNull();
		assertThat(mapper.toSummaryV1(null)).isNull();
		assertThat(mapper.toSummaryV1List(null)).isNull();
		assertThat(mapper.toDraftV1(null)).isNull();
		assertThat(mapper.toCommand(null, null)).isNull();
	}

	@Test
	void shouldMapEachLifecycleStatusOntoItsApiEnumTest() {
		// Given: every status the service can persist
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);

		// When-Then: each maps to its API enum, so the client's switch stays exhaustive
		assertThat(mapper.mapStatus("DRAFT")).isEqualTo(GenerationStatusV1.DRAFT);
		assertThat(mapper.mapStatus("SUBMITTED")).isEqualTo(GenerationStatusV1.SUBMITTED);
		assertThat(mapper.mapStatus("GENERATED")).isEqualTo(GenerationStatusV1.GENERATED);
	}

	@Test
	void shouldRefuseAStatusThatIsNotInTheContractTest() {
		// Given: a status string the OpenAPI enum does not declare
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);

		// When-Then: it fails loudly rather than serialising a value the generated client
		// cannot parse
		assertThatThrownBy(() -> mapper.mapStatus("ARCHIVED"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldMapTheAiDraftOntoItsResponseTest() {
		// Given: an AI-generated draft awaiting review
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CaseStudyDraft draft = new CaseStudyDraft("Acme Turnaround", "Acme", "Retail",
				"Sales were declining.", "We rebuilt the funnel.", "Revenue grew.",
				"40% cost reduction", "6 months", "Best decision we made.",
				"acme-report.pdf,notes.docx");

		// When: it is converted for the review form
		CaseStudyDraftV1 response = mapper.toDraftV1(draft);

		// Then: every drafted section reaches the form the user edits
		assertThat(response.getTitle()).isEqualTo("Acme Turnaround");
		assertThat(response.getClientName()).isEqualTo("Acme");
		assertThat(response.getIndustry()).isEqualTo("Retail");
		assertThat(response.getChallenge()).isEqualTo("Sales were declining.");
		assertThat(response.getSolution()).isEqualTo("We rebuilt the funnel.");
		assertThat(response.getResults()).isEqualTo("Revenue grew.");
		assertThat(response.getKeyMetrics()).isEqualTo("40% cost reduction");
		assertThat(response.getTimeline()).isEqualTo("6 months");
		assertThat(response.getTestimonial()).isEqualTo("Best decision we made.");
		assertThat(response.getSourceFileNames()).isEqualTo("acme-report.pdf,notes.docx");
	}

	@Test
	void shouldTakeTheCreatorFromTheSessionRatherThanTheRequestTest() {
		// Given: a create request from the browser, which carries no user identity
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CreateCaseStudyRequestV1 request = new CreateCaseStudyRequestV1();
		request.setTitle("Acme Turnaround");
		request.setClientName("Acme");
		request.setIndustry("Retail");
		request.setChallenge("Sales were declining.");
		request.setSolution("We rebuilt the funnel.");
		request.setResults("Revenue grew.");
		request.setKeyMetrics("40% cost reduction");
		request.setTimeline("6 months");
		request.setTestimonial("Best decision we made.");
		request.setSourceFileName("acme-report.pdf");

		// When: the command is built with the authenticated user id
		CreateCaseStudyCommand command = mapper.toCommand(request, "user_1");

		// Then: the request's fields land, and ownership comes from the verified session so a
		// caller cannot create a case study as someone else
		assertThat(command.title()).isEqualTo("Acme Turnaround");
		assertThat(command.clientName()).isEqualTo("Acme");
		assertThat(command.industry()).isEqualTo("Retail");
		assertThat(command.challenge()).isEqualTo("Sales were declining.");
		assertThat(command.solution()).isEqualTo("We rebuilt the funnel.");
		assertThat(command.results()).isEqualTo("Revenue grew.");
		assertThat(command.keyMetrics()).isEqualTo("40% cost reduction");
		assertThat(command.timeline()).isEqualTo("6 months");
		assertThat(command.testimonial()).isEqualTo("Best decision we made.");
		assertThat(command.sourceFileName()).isEqualTo("acme-report.pdf");
		assertThat(command.createdBy()).isEqualTo("user_1");
	}
}
