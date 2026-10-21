package com.aidigital.strategyplanning.service.onepager.models;

import java.awt.Color;

/**
 * The run style captured from a master shape's first run, re-applied to the runs written in its
 * place so drafted text keeps the template's font, size, weight, and color.
 *
 * @param fontFamily font family, or null to inherit
 * @param fontSize   font size in points, or null to inherit
 * @param bold       whether the run is bold
 * @param italic     whether the run is italic
 * @param color      solid font color, or null when the master uses a theme color
 */
public record OnePagerRunStyle(String fontFamily, Double fontSize, boolean bold, boolean italic, Color color) {

	/** A style that inherits everything from the shape. */
	public static final OnePagerRunStyle INHERITED = new OnePagerRunStyle(null, null, false, false, null);
}
