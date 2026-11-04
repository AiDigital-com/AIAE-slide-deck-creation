package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Ungrouping against the real master: grouped children become top-level shapes at the position
 * the group displayed them, and the catalog's ids all resolve afterwards.
 */
class OnePagerGroupFlattenerImplTest {

	/** Left edge of the KPI-tile group and of its first tile; the group has no offset or scale. */
	private static final long KPI_TILE_LEFT_EMU = 5_567_994L;

	/** Left edge the capability-card group is placed at on the slide. */
	private static final long CAPABILITY_GROUP_LEFT_EMU = 296_335L;

	/** First capability card's width after the group's horizontal scale is applied. */
	private static final long CAPABILITY_CARD_WIDTH_EMU = 2326989L;

	private static final long TOLERANCE_EMU = 1L;

	@Test
	void shouldPlaceUngroupedChildrenWhereTheGroupShowedThemTest() throws IOException {
		// Given: the KPI slide of the master, whose tiles and capability cards sit in groups
		OnePagerGroupFlattenerImpl flattener = new OnePagerGroupFlattenerImpl();
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			XSLFSlide slide = show.getSlides().get(OnePagerLayout.KPI_SLIDE_INDEX);

			// When: the wrapper groups are flattened
			flattener.flatten(slide, OnePagerLayout.KPI_WRAPPER_GROUP_IDS);

			// Then: a tile from the unscaled group keeps its position, a card from the scaled group
			// moves to the group's origin and shrinks with it, and the groups themselves are gone
			XSLFShape tile = shapes.findShapeById(slide, OnePagerLayout.KPI_SLOTS.get(0).boxId());
			assertThat(shapes.leftEmu(tile)).isCloseTo(KPI_TILE_LEFT_EMU, within(TOLERANCE_EMU));
			XSLFShape card = shapes.findShapeById(slide, OnePagerLayout.CAPABILITY_CARD_SLOTS.get(0).cardId());
			assertThat(shapes.leftEmu(card)).isCloseTo(CAPABILITY_GROUP_LEFT_EMU, within(TOLERANCE_EMU));
			assertThat(shapes.widthEmu(card)).isCloseTo(CAPABILITY_CARD_WIDTH_EMU, within(TOLERANCE_EMU));
			assertThat(slide.getShapes().stream().map(XSLFShape::getShapeId))
					.doesNotContainAnyElementsOf(OnePagerLayout.KPI_WRAPPER_GROUP_IDS);
		}
	}

	@Test
	void shouldLeaveEveryCatalogIdResolvableOnTheModuleSlidesTest() throws IOException {
		// Given: the three module slides
		OnePagerGroupFlattenerImpl flattener = new OnePagerGroupFlattenerImpl();
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			XSLFSlide body = show.getSlides().get(OnePagerLayout.BODY_SLIDE_INDEX);
			XSLFSlide kpi = show.getSlides().get(OnePagerLayout.KPI_SLIDE_INDEX);
			XSLFSlide insight = show.getSlides().get(OnePagerLayout.INSIGHT_SLIDE_INDEX);

			// When: their wrapper groups are flattened
			flattener.flatten(body, OnePagerLayout.BODY_WRAPPER_GROUP_IDS);
			flattener.flatten(kpi, OnePagerLayout.KPI_WRAPPER_GROUP_IDS);
			flattener.flatten(insight, OnePagerLayout.INSIGHT_WRAPPER_GROUP_IDS);

			// Then: every id the catalog names for each slide is a top-level shape there
			List<Integer> bodyIds = List.of(OnePagerLayout.SPOTLIGHT_CARD_SLOTS.get(1).cardId(),
					OnePagerLayout.SPOTLIGHT_KPI_CHIP_GROUP_IDS.get(0), OnePagerLayout.SPOTLIGHT_STAGE_PILL_IDS.get(2));
			List<Integer> kpiIds = List.of(OnePagerLayout.IMAGE_TEXT_IMAGE_ID, OnePagerLayout.KPI_SLOTS.get(3).labelId(),
					OnePagerLayout.CAPABILITY_CARD_SLOTS.get(2).footerId(), OnePagerLayout.STAT_SLOTS.get(2).descId());
			List<Integer> insightIds = List.of(OnePagerLayout.INSIGHT_CARD_SLOTS.get(0).iconInnerId(),
					OnePagerLayout.INSIGHT_CARD_SLOTS.get(0).iconPictureId(), OnePagerLayout.MILESTONE_SLOTS.get(2).subtextId());
			bodyIds.forEach(id -> assertThat(shapes.findShapeById(body, id).getShapeId()).isEqualTo(id));
			kpiIds.forEach(id -> assertThat(shapes.findShapeById(kpi, id).getShapeId()).isEqualTo(id));
			insightIds.forEach(id -> assertThat(shapes.findShapeById(insight, id).getShapeId()).isEqualTo(id));
		}
	}

	@Test
	void shouldRejectAGroupIdThatIsNotOnTheSlideTest() throws IOException {
		// Given: a shell slide, which carries no wrapper group
		OnePagerGroupFlattenerImpl flattener = new OnePagerGroupFlattenerImpl();
		try (XMLSlideShow show = OnePagerFixtures.open(OnePagerFixtures.master())) {
			XSLFSlide shell = show.getSlides().get(0);

			// When-Then: asking for a group that is not there is reported, not ignored
			assertThatThrownBy(() -> flattener.flatten(shell, OnePagerLayout.BODY_WRAPPER_GROUP_IDS))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Group 226 not found on slide");
		}
	}
}
