package com.aidigital.strategyplanning.service.onepager.models;

/**
 * The One Page Generator page created in the user's Google Drive. Nothing is recorded
 * server-side: the page lives only in the user's own Drive.
 *
 * @param title     file name the page was saved under
 * @param slidesUrl edit URL of the new Google Slides file
 */
public record OnePagerDeck(String title, String slidesUrl) {

}
