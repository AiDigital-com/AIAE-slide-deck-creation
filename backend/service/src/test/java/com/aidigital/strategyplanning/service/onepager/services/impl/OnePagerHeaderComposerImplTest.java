package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the header fill against the real shells.
 */
class OnePagerHeaderComposerImplTest {

	private OnePagerHeaderComposerImpl composer(OnePagerShapeEditorImpl shapes) {
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();
		return new OnePagerHeaderComposerImpl(shapes, new OnePagerTextWriterImpl(fitter), fitter);
	}

	@Test
	void shouldGrowTheTitleBoxForThreeLinesAndPushStackedCopyDownTest() throws IOException {
		// Given: shell 1 and a three-line title
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerDraft draft = OnePagerFixtures.automotiveExample();
		OnePagerDraft threeLines = new OnePagerDraft("PILL", List.of("ONE", "TWO", "THREE"), draft.supportingCopy(),
				draft.moduleOrder(), null, List.of(), null, List.of(), null, null, List.of(), null, null, null, null,
				List.of(), List.of(), List.of(), List.of(), null, List.of());
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerCanvas canvas = new OnePagerCanvas(show.getSlides().get(OnePagerShell.BLUE_HEADER.getSlideIndex()));
			XSLFTextShape title = (XSLFTextShape) shapes.findShapeById(canvas.slide(),
					OnePagerShell.BLUE_HEADER.getTitleId());
			XSLFTextShape copy = (XSLFTextShape) shapes.findShapeById(canvas.slide(),
					OnePagerShell.BLUE_HEADER.getSupportingCopyId());
			double originalTitleHeight = title.getAnchor().getHeight();
			long originalCopyTop = shapes.topEmu(copy);

			// When: the header is filled
			long cursor = composer(shapes).apply(canvas, OnePagerShell.BLUE_HEADER, threeLines);

			// Then: the title holds three lines in a box half again as tall, the copy moved down by
			// the growth, and the body starts below everything plus the header gap
			assertThat(title.getText()).isEqualTo("ONE\nTWO\nTHREE");
			assertThat(title.getAnchor().getHeight()).isCloseTo(originalTitleHeight * 1.5,
					org.assertj.core.data.Offset.offset(0.01));
			assertThat(shapes.topEmu(copy) - originalCopyTop)
					.isCloseTo(shapes.toEmu(originalTitleHeight * 0.5), org.assertj.core.data.Offset.offset(2L));
			assertThat(copy.getText()).isEqualTo(draft.supportingCopy());
			assertThat(((XSLFTextShape) shapes.findShapeById(canvas.slide(),
					OnePagerShell.BLUE_HEADER.getTopicPillTextId())).getText()).isEqualTo("PILL");
			assertThat(cursor).isEqualTo(Math.max(shapes.bottomEmu(title), Math.max(shapes.bottomEmu(copy),
					shapes.bottomEmu(shapes.findShapeById(canvas.slide(),
							OnePagerShell.BLUE_HEADER.getHeaderBackgroundBottomId()))))
					+ OnePagerLayout.HEADER_GAP_EMU);
		}
	}

	@Test
	void shouldWriteAnEmptyTitleWhenNoneWasDraftedTest() throws IOException {
		// Given: shell 3 and a draft without title lines
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerDraft noTitle = new OnePagerDraft("PILL", null, "Copy", List.of(), null, List.of(), null, List.of(),
				null, null, List.of(), null, null, null, null, List.of(), List.of(), List.of(), List.of(), null,
				List.of());
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			OnePagerCanvas canvas = new OnePagerCanvas(show.getSlides().get(OnePagerShell.TOP_BAR.getSlideIndex()));
			XSLFTextShape title = (XSLFTextShape) shapes.findShapeById(canvas.slide(),
					OnePagerShell.TOP_BAR.getTitleId());
			double originalTitleHeight = title.getAnchor().getHeight();

			// When: the header is filled
			long cursor = composer(shapes).apply(canvas, OnePagerShell.TOP_BAR, noTitle);

			// Then: the title is blank in a one-line box and the body still starts below the header
			assertThat(title.getText()).isEmpty();
			assertThat(title.getAnchor().getHeight()).isCloseTo(originalTitleHeight / 2,
					org.assertj.core.data.Offset.offset(0.01));
			assertThat(cursor).isGreaterThan(shapes.bottomEmu(title));
		}
	}
}
