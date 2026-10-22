package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * One spotlight-card slot on the master's body slide: background card, a decorative visual whose
 * shapes differ per slot (an image placeholder, a cluster of KPI-chip pills, or 3 connected
 * stage-name pills), a title, a description, the checklist's bullet icons, and the checklist text.
 *
 * @param cardId        shape id of the card's background container
 * @param visualIds     shape ids of the slot's decorative visual, cloned in order
 * @param titleId       shape id of the card's title text
 * @param descriptionId shape id of the card's intro description text
 * @param bulletIconIds shape ids of the checklist's bullet icons, cloned as-is
 * @param bulletsId     shape id of the checklist text box (one paragraph per bullet)
 */
public record OnePagerSpotlightCardSlot(int cardId, List<Integer> visualIds, int titleId, int descriptionId,
										List<Integer> bulletIconIds, int bulletsId) {

}
