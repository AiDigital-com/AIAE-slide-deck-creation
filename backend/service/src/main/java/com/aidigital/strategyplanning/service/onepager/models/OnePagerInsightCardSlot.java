package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One icon-and-insight card slot on the master's insight slide: background card, a two-paragraph
 * text box (a static "KEY INSIGHT" label, then the insight), a colored footer bar, the icon's
 * badge background and inner shape, a title, and the icon picture itself (cloned as-is).
 *
 * @param cardId        shape id of the card's background container
 * @param textId        shape id of the two-paragraph text box
 * @param footerBarId   shape id of the card's colored footer bar
 * @param iconBadgeId   shape id of the icon's badge background
 * @param iconInnerId   shape id of the icon badge's inner shape
 * @param titleId       shape id of the card's title text
 * @param iconPictureId shape id of the icon picture
 */
public record OnePagerInsightCardSlot(int cardId, int textId, int footerBarId, int iconBadgeId, int iconInnerId,
									  int titleId, int iconPictureId) {

}
