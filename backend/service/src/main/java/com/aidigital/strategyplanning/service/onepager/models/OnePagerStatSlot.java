package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One impact-stat slot on the master's KPI slide: a large value text and a description beneath.
 *
 * @param valueId shape id of the stat's large value text
 * @param descId  shape id of the stat's description text
 */
public record OnePagerStatSlot(int valueId, int descId) {

}
