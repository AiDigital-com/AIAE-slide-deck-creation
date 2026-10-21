package com.aidigital.strategyplanning.mappers;

import com.aidigital.strategyplanning.api.v1.model.CaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudySummaryV1;
import com.aidigital.strategyplanning.api.v1.model.CaseStudyV1;
import com.aidigital.strategyplanning.api.v1.model.CreateCaseStudyRequestV1;
import com.aidigital.strategyplanning.api.v1.model.CreateOnePageCaseStudyRequestV1;
import com.aidigital.strategyplanning.api.v1.model.GenerationStatusV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyDraftV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyFieldV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyFieldValueV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageCaseStudyIconChoiceV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageFieldSourceV1;
import com.aidigital.strategyplanning.api.v1.model.OnePageFitV1;
import com.aidigital.strategyplanning.mappers.casestudy.CaseStudyApiMapper;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.CaseStudyRecord;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.CreateOnePageCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconChoice;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyIcon;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSource;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconOption;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Arrays;
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

	@Test
	void shouldMapAOnePageDraftWithItsEnumsOntoTheResponseTest() {
		// Given: a measured draft with one field, one icon, and the library
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		OnePageCaseStudyDraft draft = new OnePageCaseStudyDraft(
				List.of(new OnePageCaseStudyField("challenge", "The challenge", "Challenge & campaign", "CPA rose.",
						OnePageFieldSource.FILES, 600, 5, 6, 2, OnePageFit.SHORT, "Add detail")),
				List.of(new OnePageCaseStudyIcon("plan_icon", "Game plan icon", null)),
				List.of(new OnePageIconOption("compass", "Compass", "/case-study-icons/compass.png")),
				"brief.pdf");

		// When: it is converted for the API
		OnePageCaseStudyDraftV1 response = mapper.toOnePageDraftV1(draft);

		// Then: every field, enum, and nullable comes through
		assertThat(response.getFields()).hasSize(1);
		assertThat(response.getFields().get(0).getKey()).isEqualTo("challenge");
		assertThat(response.getFields().get(0).getSection()).isEqualTo("Challenge & campaign");
		assertThat(response.getFields().get(0).getSource()).isEqualTo(OnePageFieldSourceV1.FILES);
		assertThat(response.getFields().get(0).getFit()).isEqualTo(OnePageFitV1.SHORT);
		assertThat(response.getFields().get(0).getMinLines()).isEqualTo(5);
		assertThat(response.getFields().get(0).getMaxLines()).isEqualTo(6);
		assertThat(response.getFields().get(0).getLines()).isEqualTo(2);
		assertThat(response.getFields().get(0).getMaxChars()).isEqualTo(600);
		assertThat(response.getFields().get(0).getNote()).isEqualTo("Add detail");
		assertThat(response.getIcons().get(0).getSlot()).isEqualTo("plan_icon");
		assertThat(response.getIcons().get(0).getIcon()).isNull();
		assertThat(response.getIconLibrary().get(0).getImageUrl()).isEqualTo("/case-study-icons/compass.png");
		assertThat(response.getSourceFileNames()).isEqualTo("brief.pdf");
	}

	@Test
	void shouldMapAOnePageCreateRequestOntoTheCommandTest() {
		// Given: a reviewed request with a field, a chosen icon, and a kept template icon
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CreateOnePageCaseStudyRequestV1 request = new CreateOnePageCaseStudyRequestV1(
				List.of(new OnePageCaseStudyFieldValueV1("title_line_1", "Scaling")),
				List.of(new OnePageCaseStudyIconChoiceV1("plan_icon").icon("compass"),
						new OnePageCaseStudyIconChoiceV1("stat_1_icon")))
				.sourceFileNames("brief.pdf");

		// When: it is converted to the service command
		CreateOnePageCaseStudyCommand command = mapper.toOnePageCommand(request, "user_1");

		// Then: values, icons (including the null), the file names, and the caller all carry over
		assertThat(command.fields()).hasSize(1);
		assertThat(command.fields().get(0).key()).isEqualTo("title_line_1");
		assertThat(command.fields().get(0).value()).isEqualTo("Scaling");
		assertThat(command.icons()).hasSize(2);
		assertThat(command.icons().get(0).icon()).isEqualTo("compass");
		assertThat(command.icons().get(1).icon()).isNull();
		assertThat(command.sourceFileNames()).isEqualTo("brief.pdf");
		assertThat(command.createdBy()).isEqualTo("user_1");
	}

	@Test
	void shouldMapEveryOnePageEnumValueAndTolerateNullElementsTest() {
		// Given: one field per source and fit value, plus null entries in every one-page list
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		List<OnePageCaseStudyField> fields = Arrays.asList(
				new OnePageCaseStudyField("a", "A", "S", "x", OnePageFieldSource.FILES, 10, 0, 0, 0,
						OnePageFit.FITS, null),
				new OnePageCaseStudyField("b", "B", "S", "y", OnePageFieldSource.WRITTEN, 10, 1, 2, 3,
						OnePageFit.OVER, null),
				new OnePageCaseStudyField("c", "C", "S", "", null, 10, 0, 0, 0, OnePageFit.EMPTY, null),
				new OnePageCaseStudyField("d", "D", "S", "z", OnePageFieldSource.FILES, 10, 1, 1, 0, null, null),
				null);
		OnePageCaseStudyDraft draft = new OnePageCaseStudyDraft(fields,
				Arrays.asList(new OnePageCaseStudyIcon("plan_icon", "Plan", "compass"), null),
				Arrays.asList(new OnePageIconOption("compass", "Compass", "/i.png"), null), "brief.pdf");

		// When: the draft and the field list are converted
		OnePageCaseStudyDraftV1 response = mapper.toOnePageDraftV1(draft);
		List<OnePageCaseStudyFieldV1> fieldsV1 = mapper.toOnePageFieldV1List(fields);

		// Then: each enum maps by name, absent enums stay null, and null elements pass through as null
		assertThat(fieldsV1).hasSize(5);
		assertThat(fieldsV1.get(0).getSource()).isEqualTo(OnePageFieldSourceV1.FILES);
		assertThat(fieldsV1.get(0).getFit()).isEqualTo(OnePageFitV1.FITS);
		assertThat(fieldsV1.get(1).getSource()).isEqualTo(OnePageFieldSourceV1.WRITTEN);
		assertThat(fieldsV1.get(1).getFit()).isEqualTo(OnePageFitV1.OVER);
		assertThat(fieldsV1.get(2).getSource()).isNull();
		assertThat(fieldsV1.get(2).getFit()).isEqualTo(OnePageFitV1.EMPTY);
		assertThat(fieldsV1.get(3).getFit()).isNull();
		assertThat(fieldsV1.get(4)).isNull();
		assertThat(response.getIcons()).hasSize(2);
		assertThat(response.getIcons().get(1)).isNull();
		assertThat(response.getIconLibrary()).hasSize(2);
		assertThat(response.getIconLibrary().get(1)).isNull();
		assertThat(mapper.toOnePageFieldV1List(List.of(new OnePageCaseStudyField("e", "E", "S", "w",
				OnePageFieldSource.FILES, 10, 1, 1, 1, OnePageFit.SHORT, "note"))).get(0).getFit())
				.isEqualTo(OnePageFitV1.SHORT);
	}

	@Test
	void shouldReturnNullForAbsentOnePageInputsTest() {
		// Given: absent inputs at every one-page boundary
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);

		// When-Then: MapStruct's null contract holds, so a missing body never becomes an NPE
		assertThat(mapper.toOnePageDraftV1(null)).isNull();
		assertThat(mapper.toOnePageFieldV1List(null)).isNull();
		assertThat(mapper.toOnePageFieldValues(null)).isNull();
		assertThat(mapper.toOnePageCommand(null, null)).isNull();
		assertThat(mapper.toOnePageDraftV1(new OnePageCaseStudyDraft(null, null, null, "x")).getFields()).isNull();
	}

	@Test
	void shouldMapAOnePageCommandWithOnlyTheCallerAndWithNullListEntriesTest() {
		// Given: a request-less call and a request with null entries in both lists
		CaseStudyApiMapper mapper = Mappers.getMapper(CaseStudyApiMapper.class);
		CreateOnePageCaseStudyRequestV1 request = new CreateOnePageCaseStudyRequestV1(
				Arrays.asList(new OnePageCaseStudyFieldValueV1("k", "v"), null),
				Arrays.asList(new OnePageCaseStudyIconChoiceV1("s"), null));

		// When: both are converted
		CreateOnePageCaseStudyCommand callerOnly = mapper.toOnePageCommand(null, "user_1");
		CreateOnePageCaseStudyCommand command = mapper.toOnePageCommand(request, "user_1");
		List<OnePageFieldValue> values = mapper.toOnePageFieldValues(request.getFields());

		// Then: the caller survives a missing request; null entries pass through as null
		assertThat(callerOnly.createdBy()).isEqualTo("user_1");
		assertThat(callerOnly.fields()).isNull();
		assertThat(command.fields()).containsExactly(new OnePageFieldValue("k", "v"), null);
		assertThat(command.icons()).containsExactly(new OnePageIconChoice("s", null), null);
		assertThat(command.sourceFileNames()).isNull();
		assertThat(values).hasSize(2);
	}
}
