package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One drafted text field of the One-Page Case Study, with how it fits its box on the slide.
 *
 * @param key      template token key
 * @param label    human-readable label
 * @param section  template section the field belongs to
 * @param value    drafted value; empty when the documents do not support one
 * @param source   where the content may come from
 * @param maxChars hard character ceiling
 * @param minLines lines that fill the box (0 = no fill target)
 * @param maxLines lines that fit the box (0 = not measured in lines)
 * @param lines    lines the value wraps to in its box (0 when empty or not measured)
 * @param fit      how the value sits in its box; a field without a measured box is FITS or EMPTY
 * @param note     explanation for the reviewer when the value could not fill its box, or null
 */
public record OnePageCaseStudyField(String key, String label, String section, String value,
									OnePageFieldSource source, int maxChars, int minLines, int maxLines,
									int lines, OnePageFit fit, String note) {

}
