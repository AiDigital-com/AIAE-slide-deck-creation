package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * One selectable slide in the In-Depth master template's "check to include" preview.
 *
 * @param slideNumber    1-based position in the master template (matches the deck's slide order;
 *                       not renumbered after slides are excluded)
 * @param title          short human-readable label for the checklist UI
 * @param defaultChecked whether this slide is checked by default when the picker first loads
 */
public record InDepthSlideSpec(
		int slideNumber,
		String title,
		boolean defaultChecked
) {

}
