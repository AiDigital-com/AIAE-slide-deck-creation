package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * One spotlight card in the One Page Generator body. The 3 cards are visually distinct from each
 * other, so most of each slot's decorative visual is cloned from the master as-is; the exceptions
 * are the KPI-chip cluster of card index 1 and the stage-name pills of card index 2, which would
 * otherwise show the master's placeholder copy verbatim.
 *
 * @param title       card heading
 * @param description short intro sentence under the heading
 * @param bullets     up to 3 short checklist lines
 * @param kpiChips    up to 7 small value+label chips, card index 1 only; empty otherwise
 * @param stageNames  up to 3 short stage names, card index 2 only; empty otherwise
 */
public record OnePagerSpotlightCard(String title, String description, List<String> bullets,
									List<OnePagerKpi> kpiChips, List<String> stageNames) {

}
