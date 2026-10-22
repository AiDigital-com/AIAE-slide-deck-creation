package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerPageSizer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.springframework.stereotype.Service;

import java.awt.Dimension;
import java.util.ArrayList;

/**
 * Default implementation of {@link OnePagerPageSizer}. The master's fixed 15in page otherwise
 * clipped a page with many modules at the bottom, or left a sparse page with a lot of blank
 * space above the footer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnePagerPageSizerImpl implements OnePagerPageSizer {

	private final OnePagerShapeEditor shapes;

	@Override
	public void fitPageToContent(XMLSlideShow show, OnePagerCanvas canvas, OnePagerShell shell, long contentBottom) {
		XSLFShape footerBackground = shapes.findShapeById(canvas.slide(), shell.getFooterBackgroundId());
		long footerTop = shapes.topEmu(footerBackground);
		long footerHeight = shapes.heightEmu(footerBackground);
		long newPageHeight = clampPageHeight(contentBottom + footerHeight);
		long delta = newPageHeight - footerHeight - footerTop;
		if (delta == 0) {
			return;
		}
		for (XSLFShape shape : new ArrayList<>(canvas.slide().getShapes())) {
			if (shape.getShapeId() < OnePagerCanvas.CLONE_ID_START && shapes.topEmu(shape) >= footerTop) {
				shapes.moveBy(shape, 0, delta);
			}
		}
		show.setPageSize(new Dimension(
				(int) Math.round(shapes.toPoints(OnePagerLayout.PAGE_WIDTH_EMU)),
				(int) Math.round(shapes.toPoints(newPageHeight))));
	}

	/**
	 * Clamps the natural page height to the allowed range, warning when content is cut off at
	 * the ceiling: the lowest module then runs past the visible page edge.
	 *
	 * @param naturalPageHeight content bottom plus footer height, in EMU
	 * @return the page height to use, in EMU
	 */
	long clampPageHeight(long naturalPageHeight) {
		if (naturalPageHeight > OnePagerLayout.MAX_PAGE_HEIGHT_EMU) {
			log.warn("One-pager content ({} EMU tall) exceeds the {} EMU page-height cap; the lowest module(s) "
					+ "will run past the visible page edge", naturalPageHeight, OnePagerLayout.MAX_PAGE_HEIGHT_EMU);
		}
		return Math.max(OnePagerLayout.MIN_PAGE_HEIGHT_EMU,
				Math.min(OnePagerLayout.MAX_PAGE_HEIGHT_EMU, naturalPageHeight));
	}
}
