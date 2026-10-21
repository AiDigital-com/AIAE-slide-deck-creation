package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerTrackerSlot;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Clones the process tracker (section label, container, step circles, step titles, and the
 * connecting line), fitting the container and line to only the steps drafted.
 */
@Service
@RequiredArgsConstructor
public class OnePagerTrackerModuleComposer implements OnePagerModuleComposer {

	/** Share of the circle spacing kept as right padding after the last used circle. */
	public static final double RIGHT_PAD_SHARE = 0.42;

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.TRACKER;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<String> steps = context.content().processSteps();
		if (steps == null || steps.isEmpty()) {
			return cursor;
		}
		int count = Math.min(steps.size(), OnePagerLayout.TRACKER_SLOTS.size());
		XSLFSlide source = context.bodySource();
		XSLFShape sourceContainer = shapes.findShapeById(source, OnePagerLayout.TRACKER_CONTAINER_ID);
		long dy = cursor - shapes.topEmu(sourceContainer);

		XSLFShape container = shapes.cloneOnto(context.canvas(), sourceContainer);
		XSLFShape label = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.SECTION_LABEL_ID), dy);
		textWriter.setSingleLine((XSLFTextShape) label, context.content().processSectionLabel());
		shapes.setAnchor(container, new Rectangle2D.Double(sourceContainer.getAnchor().getX(),
				shapes.toPoints(cursor), shapes.toPoints(fittedContainerWidth(source, sourceContainer, count)),
				sourceContainer.getAnchor().getHeight()));

		XSLFShape firstCircle = null;
		XSLFShape lastCircle = null;
		for (int index = 0; index < count; index++) {
			OnePagerTrackerSlot slot = OnePagerLayout.TRACKER_SLOTS.get(index);
			XSLFShape circle = shapes.cloneShifted(context.canvas(),
					shapes.findShapeById(source, slot.circleGroupId()), dy);
			XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
			textWriter.setSingleLine((XSLFTextShape) title, steps.get(index));
			firstCircle = firstCircle == null ? circle : firstCircle;
			lastCircle = circle;
		}
		fitLine(context, source, dy, firstCircle, lastCircle);
		return shapes.bottomEmu(container) + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Computes the container width that covers only the used circles: the master authors it
	 * full-width for all 5 slots, which would leave a wide blank card after a shorter tracker.
	 *
	 * @param source          master body slide
	 * @param sourceContainer the master's container shape
	 * @param count           number of steps used
	 * @return the fitted width in EMU
	 */
	long fittedContainerWidth(XSLFSlide source, XSLFShape sourceContainer, int count) {
		XSLFShape firstCircle = shapes.findShapeById(source, OnePagerLayout.TRACKER_SLOTS.get(0).circleGroupId());
		long leftPad = shapes.leftEmu(firstCircle) - shapes.leftEmu(sourceContainer);
		long circleSpacing = count > 1
				? shapes.leftEmu(shapes.findShapeById(source, OnePagerLayout.TRACKER_SLOTS.get(1).circleGroupId()))
						- shapes.leftEmu(firstCircle)
				: 0;
		XSLFShape lastCircle = shapes.findShapeById(source,
				OnePagerLayout.TRACKER_SLOTS.get(count - 1).circleGroupId());
		long rightmost = shapes.leftEmu(lastCircle) + shapes.widthEmu(lastCircle)
				+ (long) (circleSpacing * RIGHT_PAD_SHARE);
		return rightmost + leftPad - shapes.leftEmu(sourceContainer);
	}

	/**
	 * Clones the connecting line and stretches it between the first and last circle; a single
	 * step leaves nothing to connect, so no line is cloned.
	 *
	 * @param context     canvas and content
	 * @param source      master body slide
	 * @param dy          vertical offset of the module in EMU
	 * @param firstCircle first cloned circle
	 * @param lastCircle  last cloned circle
	 */
	void fitLine(OnePagerAssemblyContext context, XSLFSlide source, long dy, XSLFShape firstCircle,
			XSLFShape lastCircle) {
		if (firstCircle == lastCircle) {
			return;
		}
		XSLFShape line = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.TRACKER_LINE_ID), dy);
		Rectangle2D lineAnchor = line.getAnchor();
		double startX = firstCircle.getAnchor().getCenterX();
		double endX = lastCircle.getAnchor().getCenterX();
		shapes.setAnchor(line,
				new Rectangle2D.Double(startX, lineAnchor.getY(), endX - startX, lineAnchor.getHeight()));
	}
}
