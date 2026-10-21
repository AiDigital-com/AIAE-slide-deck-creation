package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;

/**
 * Fills the shell's header (topic pill, title, supporting copy) and reports where the body starts.
 */
public interface OnePagerHeaderComposer {

	/**
	 * Fills the header. The title box is authored for two lines and is scaled to the number of
	 * drafted lines; supporting copy stacked below the title moves down by the same growth.
	 *
	 * @param canvas  canvas being assembled
	 * @param shell   the shell the canvas was built from
	 * @param content drafted page content
	 * @return the y position in EMU at which the first body module may start
	 */
	long apply(OnePagerCanvas canvas, OnePagerShell shell, OnePagerDraft content);
}
