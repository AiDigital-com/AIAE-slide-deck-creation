package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestone;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestoneSlot;
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
 * Clones the milestone banner (photo, overlay, headline, and numbered badges with title and
 * subtext).
 */
@Service
@RequiredArgsConstructor
public class OnePagerMilestoneModuleComposer implements OnePagerModuleComposer {

	private static final String NUMBER_FORMAT = "%02d";

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.MILESTONE_BANNER;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerMilestone> milestones = context.content().milestones();
		if (milestones == null || milestones.isEmpty()) {
			return cursor;
		}
		int count = Math.min(milestones.size(), OnePagerLayout.MILESTONE_SLOTS.size());
		XSLFSlide source = context.insightSource();
		XSLFShape sourcePhoto = shapes.findShapeById(source, OnePagerLayout.MILESTONE_PHOTO_ID);
		long dy = cursor - shapes.topEmu(sourcePhoto);
		XSLFShape photo = shapes.cloneShifted(context.canvas(), sourcePhoto, dy);
		shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, OnePagerLayout.MILESTONE_OVERLAY_ID), dy);
		XSLFShape headline = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.MILESTONE_HEADLINE_ID), dy);
		textWriter.setSingleLine((XSLFTextShape) headline, context.content().milestoneHeadline());
		long maxBottom = shapes.bottomEmu(photo);
		for (int index = 0; index < count; index++) {
			OnePagerMilestoneSlot slot = OnePagerLayout.MILESTONE_SLOTS.get(index);
			OnePagerMilestone milestone = milestones.get(index);
			shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.badgeId()), dy);
			XSLFShape number = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.numberId()), dy);
			XSLFShape title = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.titleId()), dy);
			XSLFShape subtext = shapes.cloneShifted(context.canvas(),
					shapes.findShapeById(source, slot.subtextId()), dy);
			textWriter.setSingleLine((XSLFTextShape) number, NUMBER_FORMAT.formatted(index + 1));
			textWriter.setSingleLine((XSLFTextShape) title, milestone.title());
			textWriter.setSingleLine((XSLFTextShape) subtext, milestone.subtext());
			maxBottom = Math.max(maxBottom, shapes.bottomEmu(subtext));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}
}
