package com.aidigital.strategyplanning.service.onepager.models;

/**
 * The One Page Generator page created in the user's Google Drive and recorded as a
 * category-analysis row with the {@code ONE_PAGER} template kind.
 *
 * @param id        identifier of the recorded row
 * @param title     file name the page was saved under
 * @param slidesUrl edit URL of the new Google Slides file
 */
public record OnePagerDeck(Long id, String title, String slidesUrl) {

}
