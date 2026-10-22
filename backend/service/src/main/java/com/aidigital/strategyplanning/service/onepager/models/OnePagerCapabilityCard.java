package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * One capability card in the One Page Generator body.
 *
 * @param title       card heading
 * @param description short intro sentence under the heading
 * @param bullets     up to 3 short checklist lines
 * @param footer      small subtext line at the bottom of the card
 */
public record OnePagerCapabilityCard(String title, String description, List<String> bullets, String footer) {

}
