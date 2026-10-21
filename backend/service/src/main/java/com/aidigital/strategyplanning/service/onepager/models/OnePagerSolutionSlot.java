package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One core-solution card slot on the master's body slide: background card, number badge, title,
 * and a body text box that receives two paragraphs (a bold value line, then the body copy).
 *
 * @param cardId   shape id of the card's background container
 * @param numberId shape id of the card's number badge text ("01", "02", ...)
 * @param titleId  shape id of the card's title text
 * @param bodyId   shape id of the card's body text box
 */
public record OnePagerSolutionSlot(int cardId, int numberId, int titleId, int bodyId) {

}
