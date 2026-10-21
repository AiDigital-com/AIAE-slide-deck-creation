package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageDraftPromptComposerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageFieldMeasurerImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTemplateRegistryImpl;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageTextFitterImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageIconLibrary;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageType;
import com.aidigital.strategyplanning.service.common.files.impl.SourceImageServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One-Page drafting prompts, wired to the real registry and fitter so the box
 * sizes in the prompt are the ones the model is held to.
 */
class OnePageDraftPromptComposerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private OnePageDraftPromptComposerImpl composer() {
		CaseStudyProperties properties = new CaseStudyProperties();
		properties.setOpenaiModel("gpt-4o");
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		OnePageTextFitterImpl fitter = new OnePageTextFitterImpl();
		return new OnePageDraftPromptComposerImpl(properties, objectMapper, new SourceImageServiceImpl(), registry,
				fitter, new OnePageFieldMeasurerImpl(registry, fitter));
	}

	private String promptText(String body) throws Exception {
		return objectMapper.readTree(body).path("messages").path(0).path("content").path(0).path("text").asText();
	}

	@Test
	void shouldListEveryFieldAndIconAndAttachImagesTest() throws Exception {
		// Given: one document and one screenshot
		String body = composer().buildDraftRequestBody(List.of(new SourceDocument("report.pdf", "CPA fell 67%")),
				List.of(new SourceImage("dashboard.png", SourceImageType.PNG, new byte[]{1, 2, 3})));

		// When: the request is inspected
		JsonNode root = objectMapper.readTree(body);
		JsonNode content = root.path("messages").path(0).path("content");
		String prompt = content.path(0).path("text").asText();

		// Then: every field key, every icon, both uploads, and JSON mode are in the request
		new OnePageTemplateRegistryImpl().fields().forEach(spec -> assertThat(prompt).contains(spec.key()));
		OnePageIconLibrary.ICONS.forEach(icon -> assertThat(prompt).contains(icon.name()));
		assertThat(prompt).contains("dashboard.png").contains("CPA fell 67%").contains("5-6 lines");
		assertThat(content.path(1).path("type").asText()).isEqualTo("image_url");
		assertThat(content.path(1).path("image_url").path("url").asText()).startsWith("data:image/png;base64,");
		assertThat(root.path("model").asText()).isEqualTo("gpt-4o");
		assertThat(root.path("response_format").path("type").asText()).isEqualTo("json_object");
		assertThat(root.path("temperature").asDouble()).isEqualTo(0.2);
	}

	@Test
	void shouldNotMentionScreenshotsWhenNoneAreAttachedTest() throws Exception {
		// Given: documents only
		String body = composer().buildDraftRequestBody(List.of(new SourceDocument("report.pdf", "Body")), List.of());

		// When-Then: the prompt is one text part with no screenshot sentence
		JsonNode content = objectMapper.readTree(body).path("messages").path(0).path("content");
		assertThat(content).hasSize(1);
		assertThat(promptText(body)).doesNotContain("screenshots");
	}

	@Test
	void shouldGiveExactLineFeedbackForEachMisfitTest() throws Exception {
		// Given: a short challenge and a short headline, plus the source material
		OnePageDraftPromptComposerImpl composer = composer();
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();
		Map<String, String> values = new HashMap<>();
		values.put("challenge", "In May, CPA soared to $564.14 due to generic ads and unfocused spend.");
		values.put("headline_stat", "DRIVING 67%");
		values.put("client", "A home improvement company");
		List<OnePageFieldSpec> misfits = List.of(registry.field("challenge").orElseThrow(),
				registry.field("headline_stat").orElseThrow(), registry.field("client").orElseThrow());

		// When: the revision request is built
		String prompt = promptText(composer.buildRevisionRequestBody(misfits, values,
				List.of(new SourceDocument("report.pdf", "May CPA was $564.14")), List.of()));

		// Then: each misfit gets its box size and an ADD instruction; facts-only fields are marked
		assertThat(prompt).contains("challenge").contains("5-6 lines").contains("ADD about")
				.contains("headline_stat").contains("4-5 lines").contains("May CPA was $564.14")
				.contains("FACTS ONLY");
	}

	@Test
	void shouldAskToCutWhenAValueOverflowsTest() throws Exception {
		// Given: a headline lead that runs past its single line
		OnePageDraftPromptComposerImpl composer = composer();
		OnePageFieldSpec lead = new OnePageTemplateRegistryImpl().field("headline_lead").orElseThrow();
		Map<String, String> values = Map.of("headline_lead",
				"HOW SMART RESTRUCTURING WON THE QUARTER FOR EVERY SINGLE REGIONAL MARKET WE TOUCHED");

		// When: the revision request is built
		String prompt = promptText(composer.buildRevisionRequestBody(List.of(lead), values, List.of(), List.of()));

		// Then: the model is told to cut
		assertThat(prompt).contains("CUT about").contains("up to 1 line(s)");
	}

	@Test
	void shouldDescribeBoxSizesForEveryKindOfFieldTest() {
		// Given: a character-limited stat, a fixed two-line driver, a 5-6 line challenge, a one-line title
		OnePageDraftPromptComposerImpl composer = composer();
		OnePageTemplateRegistryImpl registry = new OnePageTemplateRegistryImpl();

		// When-Then: each description names the right unit
		assertThat(composer.boxSize(registry.field("stat_1_value").orElseThrow())).isEqualTo("max 7 characters");
		assertThat(composer.boxSize(registry.field("driver_1").orElseThrow())).startsWith("exactly 2 line(s)");
		assertThat(composer.boxSize(registry.field("challenge").orElseThrow())).startsWith("5-6 lines, about ");
		assertThat(composer.boxSize(registry.field("title_line_1").orElseThrow())).startsWith("up to 1 line(s)");
	}
}
