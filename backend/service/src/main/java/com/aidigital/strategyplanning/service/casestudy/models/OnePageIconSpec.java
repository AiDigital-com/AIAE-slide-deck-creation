package com.aidigital.strategyplanning.service.casestudy.models;

/**
 * One icon of the brand icon library the One-Page Case Study can swap into its tagged slots.
 *
 * @param name        icon name (also its file name without extension)
 * @param description what the icon shows, used by the AI to choose and by the picker as a label
 */
public record OnePageIconSpec(String name, String description) {

}
