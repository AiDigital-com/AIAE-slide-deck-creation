package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
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
import org.springframework.util.StringUtils;

/**
 * Clones the key-takeaway bar and its callout text.
 */
@Service
@RequiredArgsConstructor
public class OnePagerKeyTakeawayModuleComposer implements OnePagerModuleComposer {

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.KEY_TAKEAWAY;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		if (!StringUtils.hasText(context.content().keyTakeaway())) {
			return cursor;
		}
		XSLFSlide source = context.kpiSource();
		XSLFShape sourceBar = shapes.findShapeById(source, OnePagerLayout.TAKEAWAY_BAR_ID);
		long dy = cursor - shapes.topEmu(sourceBar);
		XSLFShape bar = shapes.cloneShifted(context.canvas(), sourceBar, dy);
		XSLFShape text = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.TAKEAWAY_TEXT_ID), dy);
		textWriter.setSingleLine((XSLFTextShape) text, context.content().keyTakeaway());
		return shapes.bottomEmu(bar) + OnePagerLayout.SECTION_GAP_EMU;
	}
}
