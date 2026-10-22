package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One process-tracker step slot on the master's body slide: a circle-number badge group (cloned
 * whole), a title shape, and a description shape.
 *
 * @param circleGroupId shape id of the circle/number badge group
 * @param titleId       shape id of the step's title text
 * @param descId        shape id of the step's description text
 */
public record OnePagerTrackerSlot(int circleGroupId, int titleId, int descId) {

}
