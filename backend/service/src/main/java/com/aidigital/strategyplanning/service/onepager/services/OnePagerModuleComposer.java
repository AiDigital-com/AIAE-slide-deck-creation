package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;

/**
 * Clones one body module from the master's component slides onto the page at the current cursor.
 * One implementation per module; the assembly dispatches by {@link #key()}.
 */
public interface OnePagerModuleComposer {

	/**
	 * The module this composer renders.
	 *
	 * @return the module key
	 */
	OnePagerModuleKey key();

	/**
	 * Clones the module onto the canvas at {@code cursor} when its content is present.
	 *
	 * @param context canvas, source slides, and content
	 * @param cursor  y position in EMU where the module starts
	 * @return the y position in EMU for the next module, or {@code cursor} unchanged when the
	 * module has no content
	 */
	long apply(OnePagerAssemblyContext context, long cursor);
}
