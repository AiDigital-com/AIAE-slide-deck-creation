package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end assembly against the real master template: every shell, every module, and the
 * page sizing the finished file must carry.
 */
class OnePagerAssemblyServiceImplTest {

	private static final int MIN_SHAPES_FULL_PAGE = 40;

	private List<String> texts(XSLFSlide slide) {
		return slide.getShapes().stream()
				.filter(XSLFTextShape.class::isInstance)
				.map(shape -> ((XSLFTextShape) shape).getText())
				.toList();
	}

	@Test
	void shouldAssembleEveryModuleOntoShellThreeAsOneSlideTest() throws IOException {
		// Given: the master and a page with content in every module
		OnePagerAssemblyServiceImpl assembly = OnePagerFixtures.assembly();

		// When: the page is assembled onto the thin-top-bar shell
		byte[] result = assembly.assemble(OnePagerFixtures.master(), OnePagerShell.TOP_BAR,
				OnePagerFixtures.automotiveExample());

		// Then: exactly one slide remains, carrying the header text, every module, and a taller page
		try (XMLSlideShow show = OnePagerFixtures.open(result)) {
			assertThat(show.getSlides()).hasSize(1);
			XSLFSlide slide = show.getSlides().get(0);
			assertThat(slide.getShapes().size()).isGreaterThan(MIN_SHAPES_FULL_PAGE);
			List<String> texts = texts(slide);
			assertThat(texts).contains("AUTOMOTIVE", "WITH PRECISION, SPEED, AND SALES ACCOUNTABILITY", "HOW IT WORKS", "UNDERSTAND", "PROVE",
					"CORE AUTOMOTIVE SOLUTIONS", "01", "04", "VIN-LEVEL DYNAMIC CREATIVE OPTIMIZATION",
					"SUPPORTING VISUALS", "30%", "INCREASED SCHEDULED APPOINTMENTS", "WHY IT WORKS FOR AUTOMOTIVE",
					"Dealers using this approach saw appointment volume climb within the first 30 days.",
					"+45%", "REAL-TIME INVENTORY SYNC", "DEALER-READY CREATIVE", "SPEED TO MARKET",
					"KEY INSIGHT\nNew inventory goes live in ads within hours, not days.",
					"THREE MILESTONES TO LAUNCH", "CONNECT INVENTORY");
			assertThat(texts).anyMatch(text -> text.startsWith("Live feed integration\nAutomatic ad refresh"));
			// Shell 3's title box is under half the width of shell 1's: each line is cut to what fits.
			assertThat(texts).anyMatch(text -> text.startsWith("SOLVING") && text.contains("\nLOCAL"));
			assertThat((long) Units.toEMU(show.getPageSize().getHeight()))
					.isGreaterThan(OnePagerLayout.MASTER_PAGE_HEIGHT_EMU)
					.isLessThanOrEqualTo(OnePagerLayout.MAX_PAGE_HEIGHT_EMU);
			assertThat(slide.getShapes().stream().mapToInt(XSLFShape::getShapeId).max().orElse(0))
					.isGreaterThanOrEqualTo(OnePagerCanvas.CLONE_ID_START);
		}
	}

	@Test
	void shouldAssembleOntoShellOneAndTwoWithTheSameContentTest() throws IOException {
		// Given: the master and the full page
		OnePagerAssemblyServiceImpl assembly = OnePagerFixtures.assembly();
		OnePagerDraft content = OnePagerFixtures.automotiveExample();

		// When: the page is assembled onto the other two shells
		byte[] shellOne = assembly.assemble(OnePagerFixtures.master(), OnePagerShell.BLUE_HEADER, content);
		byte[] shellTwo = assembly.assemble(OnePagerFixtures.master(), OnePagerShell.HERO_GRAPHIC, content);

		// Then: both are single-slide pages, and shell 2's generic decorative row is gone
		try (XMLSlideShow one = OnePagerFixtures.open(shellOne); XMLSlideShow two = OnePagerFixtures.open(shellTwo)) {
			assertThat(one.getSlides()).hasSize(1);
			assertThat(two.getSlides()).hasSize(1);
			assertThat(one.getSlides().get(0).getShapes().size()).isGreaterThan(MIN_SHAPES_FULL_PAGE);
			assertThat(two.getSlides().get(0).getShapes().stream().map(XSLFShape::getShapeId))
					.doesNotContainAnyElementsOf(OnePagerShell.HERO_GRAPHIC.getRemoveOnUse());
			assertThat(texts(two.getSlides().get(0))).contains("AUTOMOTIVE", "HOW IT WORKS");
			// Shell 1's title box is wide enough for the first line in full.
			assertThat(texts(one.getSlides().get(0))).anyMatch(text -> text.startsWith("SOLVING AUTOMOTIVE'S\n"));
		}
	}

	@Test
	void shouldShrinkAHeaderOnlyPageToTheMinimumHeightTest() throws IOException {
		// Given: a draft with no module content
		OnePagerAssemblyServiceImpl assembly = OnePagerFixtures.assembly();

		// When: it is assembled
		byte[] result = assembly.assemble(OnePagerFixtures.master(), OnePagerShell.BLUE_HEADER,
				OnePagerFixtures.headerOnly());

		// Then: the page shrinks to the floor and only the shell's own shapes remain
		try (XMLSlideShow show = OnePagerFixtures.open(result)) {
			assertThat((long) Units.toEMU(show.getPageSize().getHeight()))
					.isCloseTo(OnePagerLayout.MIN_PAGE_HEIGHT_EMU, Offset.offset(12_700L));
			assertThat(show.getSlides().get(0).getShapes())
					.allMatch(shape -> shape.getShapeId() < OnePagerCanvas.CLONE_ID_START);
			assertThat(texts(show.getSlides().get(0))).contains("RETAIL", "ONE LINE");
		}
	}

	@Test
	void shouldFallBackToTheDefaultOrderWhenTheDraftHasNoneTest() {
		// Given: a draft whose module order is empty
		OnePagerAssemblyServiceImpl assembly = OnePagerFixtures.assembly();

		// When-Then: the default stacking order is used
		assertThat(assembly.moduleOrder(OnePagerFixtures.automotiveExample(List.of())))
				.isEqualTo(OnePagerAssemblyServiceImpl.DEFAULT_MODULE_ORDER);
		assertThat(assembly.moduleOrder(OnePagerFixtures.automotiveExample(List.of(OnePagerModuleKey.KPIS))))
				.containsExactly(OnePagerModuleKey.KPIS);
	}

	@Test
	void shouldRejectBytesThatAreNotAPresentationTest() {
		// Given: garbage instead of the master
		OnePagerAssemblyServiceImpl assembly = OnePagerFixtures.assembly();

		// When-Then: the failure is the feature's application error
		assertThatThrownBy(() -> assembly.assemble(new byte[]{1, 2, 3}, OnePagerShell.TOP_BAR,
				OnePagerFixtures.headerOnly()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Could not assemble the one-pager");
	}
}
