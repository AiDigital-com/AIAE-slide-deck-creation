package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCardSlot;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clones the spotlight cards. The 3 slots are intentionally different designs (an image
 * placeholder, a KPI-chip cluster, 3 connected stage-name pills), so each slot's visual shapes
 * are cloned as-is; only the chip cluster's 7 chips and the stage banner's 3 pills receive
 * drafted text, since left untouched they would show the master's placeholder copy.
 */
@Service
@RequiredArgsConstructor
public class OnePagerSpotlightCardsModuleComposer implements OnePagerModuleComposer {

	/** Fallback font sizes used only if a chip or pill run carries no explicit size. */
	public static final double DEFAULT_CHIP_VALUE_FONT_SIZE = 6.7;
	public static final double DEFAULT_CHIP_LABEL_FONT_SIZE = 6.1;
	public static final double DEFAULT_STAGE_NAME_FONT_SIZE = 6.7;

	/** Fixed child order of a KPI chip group: background pill, value badge, label. */
	public static final int CHIP_VALUE_CHILD = 1;
	public static final int CHIP_LABEL_CHILD = 2;
	private static final int CHIP_CHILD_COUNT = 3;

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.SPOTLIGHT_CARDS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerSpotlightCard> cards = context.content().spotlightCards();
		if (cards == null || cards.isEmpty()) {
			return cursor;
		}
		int count = Math.min(cards.size(), OnePagerLayout.SPOTLIGHT_CARD_SLOTS.size());
		XSLFSlide source = context.bodySource();
		long dy = cursor - shapes.topEmu(
				shapes.findShapeById(source, OnePagerLayout.SPOTLIGHT_CARD_SLOTS.get(0).cardId()));
		long maxBottom = cursor;
		for (int index = 0; index < count; index++) {
			maxBottom = Math.max(maxBottom,
					applyCard(context, OnePagerLayout.SPOTLIGHT_CARD_SLOTS.get(index), cards.get(index), dy));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Clones one card with its visual, title, description, bullets, and one icon per bullet.
	 *
	 * @param context canvas and content
	 * @param slot    master slot to clone
	 * @param card    drafted card content
	 * @param dy      vertical offset of the module in EMU
	 * @return the card background's bottom in EMU
	 */
	long applyCard(OnePagerAssemblyContext context, OnePagerSpotlightCardSlot slot, OnePagerSpotlightCard card,
			long dy) {
		XSLFSlide source = context.bodySource();
		XSLFShape background = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.cardId()), dy);
		Map<Integer, XSLFShape> visuals = new HashMap<>();
		for (int visualId : slot.visualIds()) {
			visuals.put(visualId, shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, visualId), dy));
		}
		if (card.kpiChips() != null && !card.kpiChips().isEmpty()) {
			applyKpiChips(visuals, card.kpiChips());
		}
		if (card.stageNames() != null && !card.stageNames().isEmpty()) {
			applyStageNames(visuals, card.stageNames());
		}
		XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
		XSLFShape description = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, slot.descriptionId()), dy);
		XSLFShape bullets = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.bulletsId()), dy);
		textWriter.setSingleLine((XSLFTextShape) title, card.title());
		textWriter.setSingleLine((XSLFTextShape) description, card.description());
		textWriter.setLines((XSLFTextShape) bullets, card.bullets());
		int iconCount = Math.min(card.bullets() == null ? 0 : card.bullets().size(), slot.bulletIconIds().size());
		for (int index = 0; index < iconCount; index++) {
			shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.bulletIconIds().get(index)), dy);
		}
		return shapes.bottomEmu(background);
	}

	/**
	 * Fills the KPI chips' value and label. Each chip is a 3-child group cloned as one unit, so
	 * the children are addressed by position inside the cloned group.
	 *
	 * @param visuals cloned visual shapes of the card, keyed by master shape id
	 * @param chips   drafted chip values and labels
	 */
	void applyKpiChips(Map<Integer, XSLFShape> visuals, List<OnePagerKpi> chips) {
		int count = Math.min(chips.size(), OnePagerLayout.SPOTLIGHT_KPI_CHIP_GROUP_IDS.size());
		for (int index = 0; index < count; index++) {
			XSLFShape cloned = visuals.get(OnePagerLayout.SPOTLIGHT_KPI_CHIP_GROUP_IDS.get(index));
			if (!(cloned instanceof XSLFGroupShape group) || group.getShapes().size() < CHIP_CHILD_COUNT) {
				continue;
			}
			OnePagerKpi chip = chips.get(index);
			textWriter.setFittedText(group.getShapes().get(CHIP_VALUE_CHILD), chip.value(),
					DEFAULT_CHIP_VALUE_FONT_SIZE);
			textWriter.setFittedText(group.getShapes().get(CHIP_LABEL_CHILD), chip.label(),
					DEFAULT_CHIP_LABEL_FONT_SIZE);
		}
	}

	/**
	 * Fills the stage-name pills; each pill is its own plain text shape.
	 *
	 * @param visuals    cloned visual shapes of the card, keyed by master shape id
	 * @param stageNames drafted stage names
	 */
	void applyStageNames(Map<Integer, XSLFShape> visuals, List<String> stageNames) {
		int count = Math.min(stageNames.size(), OnePagerLayout.SPOTLIGHT_STAGE_PILL_IDS.size());
		for (int index = 0; index < count; index++) {
			XSLFShape cloned = visuals.get(OnePagerLayout.SPOTLIGHT_STAGE_PILL_IDS.get(index));
			if (cloned != null) {
				textWriter.setFittedText(cloned, stageNames.get(index), DEFAULT_STAGE_NAME_FONT_SIZE);
			}
		}
	}
}
