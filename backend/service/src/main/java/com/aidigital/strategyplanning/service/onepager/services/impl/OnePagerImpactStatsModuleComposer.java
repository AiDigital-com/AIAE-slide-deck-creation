package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStat;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStatSlot;
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
 * Clones the impact-stats row (top divider, value/description pairs, and the vertical dividers
 * between them).
 */
@Service
@RequiredArgsConstructor
public class OnePagerImpactStatsModuleComposer implements OnePagerModuleComposer {

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.IMPACT_STATS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerStat> stats = context.content().impactStats();
		if (stats == null || stats.isEmpty()) {
			return cursor;
		}
		int count = Math.min(stats.size(), OnePagerLayout.STAT_SLOTS.size());
		XSLFSlide source = context.kpiSource();
		XSLFShape sourceDivider = shapes.findShapeById(source, OnePagerLayout.STATS_TOP_DIVIDER_ID);
		long dy = cursor - shapes.topEmu(sourceDivider);
		long maxBottom = shapes.bottomEmu(shapes.cloneShifted(context.canvas(), sourceDivider, dy));
		for (int index = 0; index < count; index++) {
			OnePagerStatSlot slot = OnePagerLayout.STAT_SLOTS.get(index);
			OnePagerStat stat = stats.get(index);
			XSLFShape value = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.valueId()), dy);
			XSLFShape desc = shapes.cloneShifted(context.canvas(), shapes.findShapeById(source, slot.descId()), dy);
			textWriter.setSingleLine((XSLFTextShape) value, stat.value());
			textWriter.setSingleLine((XSLFTextShape) desc, stat.description());
			maxBottom = Math.max(maxBottom, shapes.bottomEmu(desc));
		}
		for (int index = 0; index < count - 1; index++) {
			XSLFShape divider = shapes.cloneShifted(context.canvas(),
					shapes.findShapeById(source, OnePagerLayout.STAT_DIVIDER_IDS.get(index)), dy);
			maxBottom = Math.max(maxBottom, shapes.bottomEmu(divider));
		}
		return maxBottom + OnePagerLayout.SECTION_GAP_EMU;
	}
}
