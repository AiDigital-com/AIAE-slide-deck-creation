package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolution;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCard;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the module composers' content-driven branches against the real master: an
 * empty module leaves the cursor alone, and the optional pieces (tracker line, solutions visual,
 * KPI disclaimer, spotlight chips) appear only when drafted.
 */
class OnePagerModuleComposersTest {

	private static final long CURSOR = 3_000_000L;

	private List<OnePagerModuleComposer> composers(OnePagerShapeEditorImpl shapes) {
		OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(new OnePagerTextFitterImpl());
		return List.of(
				new OnePagerTrackerModuleComposer(shapes, writer),
				new OnePagerSolutionsModuleComposer(shapes, writer),
				new OnePagerKpiModuleComposer(shapes, writer),
				new OnePagerImageTextModuleComposer(shapes, writer),
				new OnePagerKeyTakeawayModuleComposer(shapes, writer),
				new OnePagerImpactStatsModuleComposer(shapes, writer),
				new OnePagerCapabilityCardsModuleComposer(shapes, writer),
				new OnePagerSpotlightCardsModuleComposer(shapes, writer),
				new OnePagerInsightCardsModuleComposer(shapes, writer),
				new OnePagerMilestoneModuleComposer(shapes, writer));
	}

	private OnePagerAssemblyContext context(XMLSlideShow show, OnePagerDraft content) {
		List<org.apache.poi.xslf.usermodel.XSLFSlide> slides = show.getSlides();
		return new OnePagerAssemblyContext(new OnePagerCanvas(slides.get(OnePagerShell.BLUE_HEADER.getSlideIndex())),
				slides.get(OnePagerLayout.BODY_SLIDE_INDEX), slides.get(OnePagerLayout.KPI_SLIDE_INDEX),
				slides.get(OnePagerLayout.INSIGHT_SLIDE_INDEX), content);
	}

	private List<String> texts(OnePagerAssemblyContext context) {
		return context.canvas().slide().getShapes().stream()
				.filter(shape -> shape.getShapeId() >= OnePagerCanvas.CLONE_ID_START)
				.filter(XSLFTextShape.class::isInstance)
				.map(shape -> ((XSLFTextShape) shape).getText())
				.toList();
	}

	@Test
	void shouldLeaveTheCursorAloneWhenAModuleHasNoContentTest() throws IOException {
		// Given: a header-only draft
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerAssemblyContext context = context(show, OnePagerFixtures.headerOnly());
			int shapesBefore = context.canvas().slide().getShapes().size();

			// When-Then: every content-driven composer reports its key and returns the cursor unchanged
			for (OnePagerModuleComposer composer : composers(shapes)) {
				assertThat(composer.key()).isNotEqualTo(OnePagerModuleKey.PHOTO);
				assertThat(composer.apply(context, CURSOR)).as(composer.key().name()).isEqualTo(CURSOR);
			}
			assertThat(context.canvas().slide().getShapes()).hasSize(shapesBefore);
		}
	}

	@Test
	void shouldAlwaysRenderThePhotoModuleTest() throws IOException {
		// Given: a header-only draft, which has no photo content to speak of
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerAssemblyContext context = context(show, OnePagerFixtures.headerOnly());
			OnePagerPhotoModuleComposer composer = new OnePagerPhotoModuleComposer(shapes);

			// When: the photo module is applied
			long next = composer.apply(context, CURSOR);

			// Then: both placeholders are cloned at the cursor and the cursor advances past them
			assertThat(composer.key()).isEqualTo(OnePagerModuleKey.PHOTO);
			List<XSLFShape> clones = context.canvas().slide().getShapes().stream()
					.filter(shape -> shape.getShapeId() >= OnePagerCanvas.CLONE_ID_START).toList();
			assertThat(clones).hasSize(OnePagerLayout.PHOTO_IDS.size());
			assertThat(shapes.topEmu(clones.get(0))).isEqualTo(CURSOR);
			assertThat(next).isEqualTo(clones.stream().mapToLong(shapes::bottomEmu).max().orElseThrow()
					+ OnePagerLayout.SECTION_GAP_EMU);
		}
	}

	@Test
	void shouldDropTheTrackerLineForASingleStepTest() throws IOException {
		// Given: a draft with one tracker step
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerDraft full = OnePagerFixtures.automotiveExample();
		OnePagerDraft oneStep = new OnePagerDraft(full.topicPill(), full.mainTitle(), full.supportingCopy(),
				full.moduleOrder(), "HOW", List.of("UNDERSTAND"), null, List.of(), null, null, List.of(), null, null,
				null, null, List.of(), List.of(), List.of(), List.of(), null, List.of());
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerAssemblyContext context = context(show, oneStep);

			// When: the tracker is applied
			long next = new OnePagerTrackerModuleComposer(shapes,
					new OnePagerTextWriterImpl(new OnePagerTextFitterImpl())).apply(context, CURSOR);

			// Then: the step and label are written, no connector was kept, and the cursor advanced
			assertThat(texts(context)).contains("HOW", "UNDERSTAND");
			assertThat(context.canvas().slide().getShapes().stream()
					.filter(shape -> shape.getShapeId() >= OnePagerCanvas.CLONE_ID_START)
					.noneMatch(XSLFConnectorShape.class::isInstance)).isTrue();
			assertThat(next).isGreaterThan(CURSOR);
		}
	}

	@Test
	void shouldSkipTheSolutionsVisualAndKpiDisclaimerWhenNotDraftedTest() throws IOException {
		// Given: solutions without a visual label and KPIs without a disclaimer
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerDraft full = OnePagerFixtures.automotiveExample();
		OnePagerDraft trimmed = new OnePagerDraft(full.topicPill(), full.mainTitle(), full.supportingCopy(),
				full.moduleOrder(), null, List.of(), "SOLUTIONS",
				List.of(new OnePagerSolution("ONE", "Value", "Body")), null, "SIGNALS",
				List.of(new OnePagerKpi("30%", "LIFT"), new OnePagerKpi("2X", "SALES")), null, null, null, null,
				List.of(), List.of(), List.of(), List.of(), null, List.of());
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerAssemblyContext context = context(show, trimmed);
			OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(new OnePagerTextFitterImpl());

			// When: both modules are applied
			long afterSolutions = new OnePagerSolutionsModuleComposer(shapes, writer).apply(context, CURSOR);
			long afterKpis = new OnePagerKpiModuleComposer(shapes, writer).apply(context, afterSolutions);

			// Then: one card and two tiles are written, with neither the visual label nor a disclaimer
			List<String> texts = texts(context);
			assertThat(texts).contains("SOLUTIONS", "01", "ONE", "Value\nBody", "SIGNALS", "30%", "LIFT", "2X", "SALES");
			assertThat(texts).doesNotContain("SUPPORTING VISUALS", "02");
			assertThat(texts.stream().filter(text -> text.startsWith("Based on"))).isEmpty();
			assertThat(afterSolutions).isEqualTo(CURSOR + OnePagerSolutionsModuleComposer.CARD_HEIGHT_EMU
					+ OnePagerLayout.SECTION_GAP_EMU);
			assertThat(afterKpis).isGreaterThan(afterSolutions);
		}
	}

	@Test
	void shouldOnlyFillChipsAndPillsOnTheCardsThatCarryThemTest() throws IOException {
		// Given: spotlight cards where the chips are (wrongly) on the image card and pills on the chip card
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerDraft full = OnePagerFixtures.automotiveExample();
		OnePagerDraft misplaced = new OnePagerDraft(full.topicPill(), full.mainTitle(), full.supportingCopy(),
				full.moduleOrder(), null, List.of(), null, List.of(), null, null, List.of(), null, null, null, null,
				List.of(), List.of(), List.of(
						new OnePagerSpotlightCard("IMAGE", "Desc", List.of("One"),
								List.of(new OnePagerKpi("99%", "MISPLACED")), List.of("NOWHERE")),
						new OnePagerSpotlightCard("CHIPS", "Desc", List.of("One", "Two"),
								List.of(new OnePagerKpi("30%", "LIFT")), List.of()),
						new OnePagerSpotlightCard("STAGES", "Desc", List.of(), List.of(),
								List.of("CONNECT", "LAUNCH"))),
				List.of(), null, List.of());
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerAssemblyContext context = context(show, misplaced);

			// When: the spotlight cards are applied
			long next = new OnePagerSpotlightCardsModuleComposer(shapes,
					new OnePagerTextWriterImpl(new OnePagerTextFitterImpl())).apply(context, CURSOR);

			// Then: the misplaced chips and pills are ignored, the real ones are written
			List<String> texts = texts(context);
			assertThat(texts).contains("IMAGE", "CHIPS", "STAGES", "CONNECT", "LAUNCH", "One\nTwo");
			assertThat(texts).doesNotContain("MISPLACED", "NOWHERE", "99%");
			assertThat(context.canvas().slide().getShapes().stream()
					.filter(org.apache.poi.xslf.usermodel.XSLFGroupShape.class::isInstance)
					.flatMap(group -> ((org.apache.poi.xslf.usermodel.XSLFGroupShape) group).getShapes().stream())
					.filter(XSLFTextShape.class::isInstance)
					.map(shape -> ((XSLFTextShape) shape).getText()))
					.contains("30%", "LIFT");
			assertThat(next).isGreaterThan(CURSOR);
		}
	}
}
