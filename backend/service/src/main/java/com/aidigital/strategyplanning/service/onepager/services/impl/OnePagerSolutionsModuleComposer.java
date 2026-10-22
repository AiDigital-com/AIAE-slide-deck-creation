package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolution;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolutionSlot;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Clones the core-solution cards (number, title, value line + body) stacked at a fixed card
 * height, plus the optional companion visual beside them when a visual label was drafted.
 */
@Service
@RequiredArgsConstructor
public class OnePagerSolutionsModuleComposer implements OnePagerModuleComposer {

	/** Fixed height in EMU of each core-solution card, regardless of how many are used. */
	public static final long CARD_HEIGHT_EMU = 1_150_000L;

	/** Vertical gap in EMU between core-solution cards. */
	public static final long CARD_GAP_EMU = 150_000L;

	private static final String NUMBER_FORMAT = "%02d";

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.SOLUTIONS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerSolution> solutions = context.content().solutions();
		if (solutions == null || solutions.isEmpty()) {
			return cursor;
		}
		int count = Math.min(solutions.size(), OnePagerLayout.SOLUTION_SLOTS.size());
		XSLFSlide source = context.bodySource();
		XSLFShape sourceLabel = shapes.findShapeById(source, OnePagerLayout.SOLUTIONS_SECTION_LABEL_ID);
		long labelOffset = shapes.topEmu(shapes.findShapeById(source, OnePagerLayout.SOLUTION_SLOTS.get(0).cardId()))
				- shapes.topEmu(sourceLabel);
		XSLFShape label = shapes.cloneShifted(context.canvas(), sourceLabel,
				cursor - labelOffset - shapes.topEmu(sourceLabel));
		textWriter.setSingleLine((XSLFTextShape) label, context.content().solutionsSectionLabel());
		OnePagerRunStyle valueLineStyle = textWriter.captureStyle(
				shapes.findShapeById(source, OnePagerLayout.SECTION_LABEL_ID));

		long cardTop = cursor;
		for (int index = 0; index < count; index++) {
			applyCard(context, OnePagerLayout.SOLUTION_SLOTS.get(index), solutions.get(index), index, cardTop,
					valueLineStyle);
			cardTop += CARD_HEIGHT_EMU + CARD_GAP_EMU;
		}
		long cardsBottom = cardTop - CARD_GAP_EMU;
		long visualBottom = StringUtils.hasText(context.content().solutionsVisualLabel())
				? applyVisual(context, cursor) : cardsBottom;
		return Math.max(cardsBottom, visualBottom) + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Clones one card at {@code cardTop}, stretching the background and body box to the fixed card
	 * height, and writes the number, title, value line, and body.
	 *
	 * @param context        canvas and content
	 * @param slot           master slot to clone
	 * @param solution       drafted card content
	 * @param index          zero-based card index, shown as "01", "02", ...
	 * @param cardTop        top of the card in EMU
	 * @param valueLineStyle run style of the bold value line
	 */
	void applyCard(OnePagerAssemblyContext context, OnePagerSolutionSlot slot, OnePagerSolution solution, int index,
			long cardTop, OnePagerRunStyle valueLineStyle) {
		XSLFSlide source = context.bodySource();
		XSLFShape sourceCard = shapes.findShapeById(source, slot.cardId());
		XSLFShape sourceBody = shapes.findShapeById(source, slot.bodyId());
		long dy = cardTop - shapes.topEmu(sourceCard);

		XSLFShape card = shapes.cloneOnto(context.canvas(), sourceCard);
		XSLFShape number = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.numberId()), dy);
		XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
		XSLFShape body = shapes.cloneShifted(context.canvas(), sourceBody, dy);

		Rectangle2D cardAnchor = card.getAnchor();
		shapes.setAnchor(card, new Rectangle2D.Double(cardAnchor.getX(), shapes.toPoints(cardTop),
				cardAnchor.getWidth(), shapes.toPoints(CARD_HEIGHT_EMU)));
		Rectangle2D bodyAnchor = body.getAnchor();
		long extraHeight = CARD_HEIGHT_EMU - shapes.heightEmu(sourceCard);
		shapes.setAnchor(body, new Rectangle2D.Double(bodyAnchor.getX(), bodyAnchor.getY(),
				bodyAnchor.getWidth(), bodyAnchor.getHeight() + shapes.toPoints(extraHeight)));

		textWriter.setSingleLine((XSLFTextShape) number, NUMBER_FORMAT.formatted(index + 1));
		textWriter.setSingleLine((XSLFTextShape) title, solution.title());
		textWriter.setTwoParagraphs((XSLFTextShape) body, solution.valueLine(), valueLineStyle, solution.body(),
				textWriter.captureStyle(sourceBody));
	}

	/**
	 * Clones the companion visual (a label over two stacked image placeholders in the master's
	 * right column) starting at the same row as the first card.
	 *
	 * @param context canvas and content
	 * @param cursor  top of the first card in EMU
	 * @return the bottom in EMU of the lowest cloned image
	 */
	long applyVisual(OnePagerAssemblyContext context, long cursor) {
		XSLFSlide source = context.bodySource();
		XSLFShape sourceFirstImage = shapes.findShapeById(source, OnePagerLayout.SOLUTIONS_VISUAL_IMAGE_IDS.get(0));
		long dy = cursor - shapes.topEmu(sourceFirstImage);
		XSLFShape sourceLabel = shapes.findShapeById(source, OnePagerLayout.SOLUTIONS_VISUAL_LABEL_ID);
		XSLFShape label = shapes.cloneShifted(context.canvas(), sourceLabel, dy);
		textWriter.setSingleLine((XSLFTextShape) label, context.content().solutionsVisualLabel());
		long maxBottom = cursor;
		for (int imageId : OnePagerLayout.SOLUTIONS_VISUAL_IMAGE_IDS) {
			XSLFShape image = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, imageId), dy);
			maxBottom = Math.max(maxBottom, shapes.bottomEmu(image));
		}
		return maxBottom;
	}
}
