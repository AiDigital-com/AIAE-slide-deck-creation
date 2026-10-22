package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One icon-and-insight card in the One Page Generator body.
 *
 * @param title   short card title
 * @param insight short supporting sentence, shown under a static "KEY INSIGHT" label
 */
public record OnePagerInsight(String title, String insight) {

}
