package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One numbered-step slot of the milestone banner on the master's insight slide: a number badge
 * background, the number text ("01", "02", ...), a title, and a subtext.
 *
 * @param badgeId   shape id of the step number's badge background
 * @param numberId  shape id of the step number text
 * @param titleId   shape id of the step's title text
 * @param subtextId shape id of the step's subtext
 */
public record OnePagerMilestoneSlot(int badgeId, int numberId, int titleId, int subtextId) {

}
