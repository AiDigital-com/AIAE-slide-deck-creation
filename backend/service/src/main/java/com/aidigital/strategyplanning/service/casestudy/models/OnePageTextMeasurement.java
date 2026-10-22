package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * A One-Page Case Study field's measured fit in its box.
 *
 * @param fit       verdict against the box's line targets
 * @param lines     lines at the narrow (overflow-safe) width
 * @param wideLines lines at the wide (fill-safe) width
 * @param minLines  box fill target
 * @param maxLines  box line limit
 */
public record OnePageTextMeasurement(OnePageFit fit, int lines, int wideLines, int minLines, int maxLines) {

}
