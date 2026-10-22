package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One core-solution card in the One Page Generator body.
 *
 * @param title     short solution name, e.g. "VIN-LEVEL DYNAMIC CREATIVE OPTIMIZATION"
 * @param valueLine short bold value-oriented subhead
 * @param body      explanatory copy, roughly 25-40 words
 */
public record OnePagerSolution(String title, String valueLine, String body) {

}
