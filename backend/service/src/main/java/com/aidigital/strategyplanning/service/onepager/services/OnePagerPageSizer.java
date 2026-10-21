package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * Grows or shrinks the page to the assembled content and moves the footer to sit flush against
 * the new bottom edge.
 */
public interface OnePagerPageSizer {

	/**
	 * Sizes the page to {@code contentBottom} plus the footer's height, clamped to the allowed
	 * range, and moves every footer shape by the same delta. Footer shapes are the master's own
	 * shapes (ids below the clone range) at or below the footer background's original top.
	 *
	 * @param show          slide show whose page size is adjusted
	 * @param canvas        the shell slide holding the assembled content and footer
	 * @param shell         the shell the canvas was built from
	 * @param contentBottom bottom of the last assembled module, in EMU
	 */
	void fitPageToContent(XMLSlideShow show, OnePagerCanvas canvas, OnePagerShell shell, long contentBottom);
}
