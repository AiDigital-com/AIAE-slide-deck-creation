package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * Everything needed to record a created One Page Generator page as a category-analysis row, once
 * the page already exists in the user's Drive.
 *
 * @param title     file title the page was saved under
 * @param category  market category or vertical the page targets
 * @param slidesUrl edit URL of the new Google Slides file
 * @param createdBy Clerk user ID of the requester
 */
public record RecordOnePagerCommand(String title, String category, String slidesUrl, String createdBy) {

}
