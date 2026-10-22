package com.aidigital.strategyplanning.service.casestudy.models;

import java.util.List;

/**
 * One icon slot of the One-Page Case Study template: an image whose alt text is {@code {{slot}}}.
 *
 * @param slot      slot key (the alt text without braces)
 * @param label     human-readable label for the review form
 * @param fieldKeys fields the icon illustrates; when all are blank the template icon is kept
 */
public record OnePageIconSlot(String slot, String label, List<String> fieldKeys) {

}
