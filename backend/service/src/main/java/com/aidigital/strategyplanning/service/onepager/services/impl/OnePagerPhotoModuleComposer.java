package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

/**
 * Clones the full-bleed photo placeholders as a standalone module, independent of the insight
 * cards that sit below them on the same master slide. The module has no drafted content, so it
 * renders whenever it is in the stacking order.
 */
@Service
@RequiredArgsConstructor
public class OnePagerPhotoModuleComposer implements OnePagerModuleComposer {

	private final OnePagerShapeEditor shapes;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.PHOTO;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		XSLFSlide source = context.insightSource();
		long dy = cursor - shapes.topEmu(shapes.findShapeById(source, OnePagerLayout.PHOTO_IDS.get(0)));
		long maxBottom = cursor;
		for (int photoId : OnePagerLayout.PHOTO_IDS) {
			XSLFShape photo = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, photoId), dy);
			maxBottom = Math.max(maxBottom, shapes.bottomEmu(photo));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}
}
