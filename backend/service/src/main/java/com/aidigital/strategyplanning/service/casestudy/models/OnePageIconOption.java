package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One library icon offered to the reviewer, with the app-relative URL of its image.
 *
 * @param name        icon name
 * @param description what the icon shows
 * @param imageUrl    app-relative URL of the icon image
 */
public record OnePageIconOption(String name, String description, String imageUrl) {

}
