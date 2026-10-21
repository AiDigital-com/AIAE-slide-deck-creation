package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsight;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsightCardSlot;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
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
 * Clones the icon-and-insight cards, writing the static "KEY INSIGHT" label and the drafted
 * insight as two separately styled paragraphs, plus the drafted card title.
 */
@Service
@RequiredArgsConstructor
public class OnePagerInsightCardsModuleComposer implements OnePagerModuleComposer {

	private static final int LABEL_PARAGRAPH = 0;
	private static final int INSIGHT_PARAGRAPH = 1;

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.INSIGHT_CARDS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerInsight> insights = context.content().insightCards();
		if (insights == null || insights.isEmpty()) {
			return cursor;
		}
		int count = Math.min(insights.size(), OnePagerLayout.INSIGHT_CARD_SLOTS.size());
		XSLFSlide source = context.insightSource();
		long dy = cursor - shapes.topEmu(
				shapes.findShapeById(source, OnePagerLayout.INSIGHT_CARD_SLOTS.get(0).cardId()));
		long maxBottom = cursor;
		for (int index = 0; index < count; index++) {
			maxBottom = Math.max(maxBottom,
					applyCard(context, OnePagerLayout.INSIGHT_CARD_SLOTS.get(index), insights.get(index), dy));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Clones one card and writes its title and two-paragraph insight text.
	 *
	 * @param context canvas and content
	 * @param slot    master slot to clone
	 * @param insight drafted card content
	 * @param dy      vertical offset of the module in EMU
	 * @return the card background's bottom in EMU
	 */
	long applyCard(OnePagerAssemblyContext context, OnePagerInsightCardSlot slot, OnePagerInsight insight, long dy) {
		XSLFSlide source = context.insightSource();
		XSLFShape sourceText = shapes.findShapeById(source, slot.textId());
		OnePagerRunStyle labelStyle = textWriter.captureStyle(sourceText, LABEL_PARAGRAPH);
		OnePagerRunStyle insightStyle = textWriter.captureStyle(sourceText, INSIGHT_PARAGRAPH);
		XSLFShape background = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.cardId()), dy);
		XSLFShape text = shapes.cloneShifted(context.canvas(), sourceText, dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.footerBarId()), dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.iconBadgeId()), dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.iconInnerId()), dy);
		XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.iconPictureId()), dy);
		textWriter.setTwoParagraphs((XSLFTextShape) text, OnePagerLayout.INSIGHT_STATIC_LABEL, labelStyle,
				insight.insight(), insightStyle);
		textWriter.setSingleLine((XSLFTextShape) title, insight.title());
		return shapes.bottomEmu(background);
	}
}
