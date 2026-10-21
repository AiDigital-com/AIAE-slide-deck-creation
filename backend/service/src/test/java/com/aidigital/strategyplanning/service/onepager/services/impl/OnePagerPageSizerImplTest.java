package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for sizing the page to its content and moving the footer with it.
 */
class OnePagerPageSizerImplTest {

	@Test
	void shouldGrowThePageAndMoveTheFooterDownTest() throws IOException {
		// Given: shell 1 and content that ends well below the master's footer
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerCanvas canvas = new OnePagerCanvas(show.getSlides().get(OnePagerShell.BLUE_HEADER.getSlideIndex()));
			XSLFShape footer = shapes.findShapeById(canvas.slide(), OnePagerShell.BLUE_HEADER.getFooterBackgroundId());
			long footerTop = shapes.topEmu(footer);
			long footerHeight = shapes.heightEmu(footer);
			long contentBottom = OnePagerLayout.MASTER_PAGE_HEIGHT_EMU + 2_000_000L;

			// When: the page is fitted
			new OnePagerPageSizerImpl(shapes).fitPageToContent(show, canvas, OnePagerShell.BLUE_HEADER, contentBottom);

			// Then: the page is content plus footer tall and the footer sits flush at the bottom
			long pageHeight = Units.toEMU(show.getPageSize().getHeight());
			assertThat(pageHeight).isCloseTo(contentBottom + footerHeight, org.assertj.core.data.Offset.offset(12_700L));
			assertThat(shapes.topEmu(footer)).isCloseTo(pageHeight - footerHeight,
					org.assertj.core.data.Offset.offset(12_700L));
			assertThat(shapes.topEmu(footer)).isGreaterThan(footerTop);
			assertThat((long) Units.toEMU(show.getPageSize().getWidth())).isCloseTo(OnePagerLayout.PAGE_WIDTH_EMU,
					org.assertj.core.data.Offset.offset(12_700L));
		}
	}

	@Test
	void shouldLeaveThePageAloneWhenTheFooterAlreadySitsAtTheBottomTest() throws IOException {
		// Given: shell 3, whose master footer already sits flush at the page bottom
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerCanvas canvas = new OnePagerCanvas(show.getSlides().get(OnePagerShell.TOP_BAR.getSlideIndex()));
			XSLFShape footer = shapes.findShapeById(canvas.slide(), OnePagerShell.TOP_BAR.getFooterBackgroundId());
			long footerTop = shapes.topEmu(footer);
			long heightBefore = Units.toEMU(show.getPageSize().getHeight());

			// When: the page is fitted to content that ends exactly at the footer's top
			new OnePagerPageSizerImpl(shapes).fitPageToContent(show, canvas, OnePagerShell.TOP_BAR, footerTop);

			// Then: nothing moved
			assertThat(shapes.topEmu(footer)).isEqualTo(footerTop);
			assertThat((long) Units.toEMU(show.getPageSize().getHeight())).isEqualTo(heightBefore);
		}
	}

	@Test
	void shouldClampTheHeightToTheAllowedRangeTest() {
		// Given: a sizer
		OnePagerPageSizerImpl sizer = new OnePagerPageSizerImpl(new OnePagerShapeEditorImpl());

		// When-Then: tiny content hits the floor, huge content the ceiling, the rest passes through
		assertThat(sizer.clampPageHeight(1)).isEqualTo(OnePagerLayout.MIN_PAGE_HEIGHT_EMU);
		assertThat(sizer.clampPageHeight(Long.MAX_VALUE / 2)).isEqualTo(OnePagerLayout.MAX_PAGE_HEIGHT_EMU);
		assertThat(sizer.clampPageHeight(OnePagerLayout.MASTER_PAGE_HEIGHT_EMU))
				.isEqualTo(OnePagerLayout.MASTER_PAGE_HEIGHT_EMU);
	}
}
