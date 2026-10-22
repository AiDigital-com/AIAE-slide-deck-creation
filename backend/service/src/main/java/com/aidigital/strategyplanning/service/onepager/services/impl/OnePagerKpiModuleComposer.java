package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpiSlot;
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

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Clones the performance-signal KPI tiles into a centered horizontal row (the master arranges
 * them as a column paired with a module this feature does not use), with a section label above
 * and an optional disclaimer below. Each value is measured against its tile's real width because
 * the tile never shrinks its font.
 */
@Service
@RequiredArgsConstructor
public class OnePagerKpiModuleComposer implements OnePagerModuleComposer {

	/** Horizontal gap in EMU between KPI tiles. */
	public static final long ROW_GAP_EMU = 160_000L;

	/** Gap in EMU between the section label and the tiles. */
	public static final long LABEL_GAP_EMU = 120_000L;

	/** Gap in EMU between the tiles and the disclaimer. */
	public static final long DISCLAIMER_GAP_EMU = 100_000L;

	/** Fallback used only if the value run carries no explicit font size. */
	public static final double DEFAULT_VALUE_FONT_SIZE = 33.0;

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;

	@Override
	public OnePagerModuleKey key() {
		return OnePagerModuleKey.KPIS;
	}

	@Override
	public long apply(OnePagerAssemblyContext context, long cursor) {
		List<OnePagerKpi> kpis = context.content().kpis();
		if (kpis == null || kpis.isEmpty()) {
			return cursor;
		}
		int count = Math.min(kpis.size(), OnePagerLayout.KPI_SLOTS.size());
		XSLFSlide kpiSource = context.kpiSource();

		XSLFShape label = shapes.cloneOnto(context.canvas(),
				shapes.findShapeById(context.bodySource(), OnePagerLayout.SECTION_LABEL_ID));
		textWriter.setSingleLine((XSLFTextShape) label, context.content().kpiSectionLabel());
		Rectangle2D labelAnchor = label.getAnchor();
		shapes.setAnchor(label, new Rectangle2D.Double(labelAnchor.getX(), shapes.toPoints(cursor),
				labelAnchor.getWidth(), labelAnchor.getHeight()));
		long tileTop = cursor + shapes.heightEmu(label) + LABEL_GAP_EMU;

		long tileWidth = shapes.widthEmu(shapes.findShapeById(kpiSource, OnePagerLayout.KPI_SLOTS.get(0).boxId()));
		long totalWidth = tileWidth * count + ROW_GAP_EMU * (count - 1);
		long startX = (OnePagerLayout.PAGE_WIDTH_EMU - totalWidth) / 2;
		long maxTileBottom = tileTop;
		for (int index = 0; index < count; index++) {
			long tileLeft = startX + index * (tileWidth + ROW_GAP_EMU);
			maxTileBottom = Math.max(maxTileBottom,
					applyTile(context, OnePagerLayout.KPI_SLOTS.get(index), kpis.get(index), tileLeft, tileTop));
		}
		long bottom = StringUtils.hasText(context.content().kpiDisclaimer())
				? applyDisclaimer(context, maxTileBottom) : maxTileBottom;
		return bottom + OnePagerLayout.SECTION_GAP_EMU;
	}

	/**
	 * Clones one tile (box, value, label) to the given position and writes its fitted value.
	 *
	 * @param context  canvas and content
	 * @param slot     master slot to clone
	 * @param kpi      drafted tile content
	 * @param tileLeft left edge in EMU
	 * @param tileTop  top edge in EMU
	 * @return the tile box's bottom in EMU
	 */
	long applyTile(OnePagerAssemblyContext context, OnePagerKpiSlot slot, OnePagerKpi kpi, long tileLeft,
			long tileTop) {
		XSLFSlide kpiSource = context.kpiSource();
		XSLFShape sourceBox = shapes.findShapeById(kpiSource, slot.boxId());
		long dx = tileLeft - shapes.leftEmu(sourceBox);
		long dy = tileTop - shapes.topEmu(sourceBox);
		XSLFShape box = shapes.cloneOnto(context.canvas(), sourceBox);
		XSLFShape value = shapes.cloneOnto(context.canvas(), shapes.findShapeById(kpiSource, slot.valueId()));
		XSLFShape valueLabel = shapes.cloneOnto(context.canvas(), shapes.findShapeById(kpiSource, slot.labelId()));
		shapes.moveBy(box, dx, dy);
		shapes.moveBy(value, dx, dy);
		shapes.moveBy(valueLabel, dx, dy);
		textWriter.setFittedText(value, kpi.value(), DEFAULT_VALUE_FONT_SIZE);
		textWriter.setSingleLine((XSLFTextShape) valueLabel, kpi.label());
		return shapes.bottomEmu(box);
	}

	/**
	 * Clones the disclaimer paragraph centered under the tiles.
	 *
	 * @param context     canvas and content
	 * @param tilesBottom bottom of the lowest tile in EMU
	 * @return the disclaimer's bottom in EMU
	 */
	long applyDisclaimer(OnePagerAssemblyContext context, long tilesBottom) {
		XSLFShape disclaimer = shapes.cloneOnto(context.canvas(),
				shapes.findShapeById(context.bodySource(), OnePagerLayout.DISCLAIMER_STYLE_SOURCE_ID));
		Rectangle2D anchor = disclaimer.getAnchor();
		long top = tilesBottom + DISCLAIMER_GAP_EMU;
		shapes.setAnchor(disclaimer, new Rectangle2D.Double(
				shapes.toPoints((OnePagerLayout.PAGE_WIDTH_EMU - shapes.toEmu(anchor.getWidth())) / 2),
				shapes.toPoints(top), anchor.getWidth(), anchor.getHeight()));
		textWriter.setSingleLine((XSLFTextShape) disclaimer, context.content().kpiDisclaimer());
		return shapes.bottomEmu(disclaimer);
	}
}
