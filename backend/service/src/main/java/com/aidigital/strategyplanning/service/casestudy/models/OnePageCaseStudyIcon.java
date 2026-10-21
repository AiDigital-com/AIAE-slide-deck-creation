package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One icon slot of the One-Page Case Study and the library icon chosen for it.
 *
 * @param slot  slot key (the icon's alt text without braces)
 * @param label human-readable label
 * @param icon  chosen library icon name, or null to keep the template's icon
 */
public record OnePageCaseStudyIcon(String slot, String label, String icon) {

}
