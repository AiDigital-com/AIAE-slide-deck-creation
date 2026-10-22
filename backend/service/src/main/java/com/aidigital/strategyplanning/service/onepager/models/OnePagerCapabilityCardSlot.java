package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * One capability-card slot on the master's KPI slide: background card, inner checklist box, a
 * small pill badge above the title, title, description, a checklist text box, the checklist's
 * bullet icons, and a footer subtext.
 *
 * @param cardId        shape id of the card's background container
 * @param innerBoxId    shape id of the inner box behind the checklist
 * @param headerPillId  shape id of the small pill badge above the title
 * @param titleId       shape id of the card's title text
 * @param descriptionId shape id of the card's intro description text
 * @param bulletsId     shape id of the checklist text box (one paragraph per bullet)
 * @param bulletIconIds shape ids of the checklist's bullet icons, cloned as-is
 * @param footerId      shape id of the card's footer subtext
 */
public record OnePagerCapabilityCardSlot(int cardId, int innerBoxId, int headerPillId, int titleId,
										 int descriptionId, int bulletsId, List<Integer> bulletIconIds,
										 int footerId) {

}
