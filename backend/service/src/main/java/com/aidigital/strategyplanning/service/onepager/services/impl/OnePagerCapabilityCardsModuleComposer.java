package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCardSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Clones the capability cards (background, inner box, header pill, title, description, bullets,
 * footer, and one checkmark icon per drafted bullet).
 */
@Service
@RequiredArgsConstructor
public class OnePagerCapabilityCardsModuleComposer implements OnePagerModuleComposer {

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.CAPABILITY_CARDS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerCapabilityCard> cards = context.content().capabilityCards();
		if (cards == null || cards.isEmpty()) {
			return cursor;
		}
		int count = Math.min(cards.size(), OnePagerLayout.CAPABILITY_CARD_SLOTS.size());
		XSLFSlide source = context.kpiSource();
		long dy = cursor - shapes.topEmu(
				shapes.findShapeById(source, OnePagerLayout.CAPABILITY_CARD_SLOTS.get(0).cardId()));
		long maxBottom = cursor;
		for (int index = 0; index < count; index++) {
			maxBottom = Math.max(maxBottom,
					applyCard(context, OnePagerLayout.CAPABILITY_CARD_SLOTS.get(index), cards.get(index), dy));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Clones one card and writes its text; bullet icons are cloned one per drafted bullet line.
	 *
	 * @param context canvas and content
	 * @param slot    master slot to clone
	 * @param card    drafted card content
	 * @param dy      vertical offset of the module in EMU
	 * @return the card background's bottom in EMU
	 */
	long applyCard(OnePagerAssemblyContext context, OnePagerCapabilityCardSlot slot, OnePagerCapabilityCard card,
			long dy) {
		XSLFSlide source = context.kpiSource();
		XSLFShape background = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.cardId()), dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.innerBoxId()), dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.headerPillId()), dy);
		XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
		XSLFShape description = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, slot.descriptionId()), dy);
		XSLFShape bullets = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.bulletsId()), dy);
		XSLFShape footer = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.footerId()), dy);
		textWriter.setSingleLine((XSLFTextShape) title, card.title());
		textWriter.setSingleLine((XSLFTextShape) description, card.description());
		textWriter.setLines((XSLFTextShape) bullets, card.bullets());
		textWriter.setSingleLine((XSLFTextShape) footer, card.footer());
		int iconCount = Math.min(card.bullets() == null ? 0 : card.bullets().size(), slot.bulletIconIds().size());
		for (int index = 0; index < iconCount; index++) {
			shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.bulletIconIds().get(index)), dy);
		}
		return shapes.bottomEmu(background);
	}
}
