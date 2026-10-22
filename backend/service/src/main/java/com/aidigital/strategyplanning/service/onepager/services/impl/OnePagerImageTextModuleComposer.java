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
 * Clones the image + text module (primary image, section label, body, secondary image).
 */
@Service
@RequiredArgsConstructor
public class OnePagerImageTextModuleComposer implements OnePagerModuleComposer {

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.IMAGE_TEXT;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		if (!StringUtils.hasText(context.content().imageTextBody())) {
			return cursor;
		}
		XSLFSlide source = context.kpiSource();
		XSLFShape sourceImage = shapes.findShapeById(source, OnePagerLayout.IMAGE_TEXT_IMAGE_ID);
		long dy = cursor - shapes.topEmu(sourceImage);
		shapes.cloneShifted(context.canvas(), sourceImage, dy);
		XSLFShape label = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.IMAGE_TEXT_LABEL_ID), dy);
		XSLFShape body = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.IMAGE_TEXT_BODY_ID), dy);
		XSLFShape secondaryImage = shapes.cloneShifted(context.canvas(),
				shapes.findShapeById(source, OnePagerLayout.IMAGE_TEXT_SECONDARY_IMAGE_ID), dy);
		textWriter.setSingleLine((XSLFTextShape) label, context.content().imageTextSectionLabel());
		textWriter.setSingleLine((XSLFTextShape) body, context.content().imageTextBody());
		return shapes.bottomEmu(secondaryImage) + OnePagerLayout.SECTION_GAP_EMU;
	}
}
