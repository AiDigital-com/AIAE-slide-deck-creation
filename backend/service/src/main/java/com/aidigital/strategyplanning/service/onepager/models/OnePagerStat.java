package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One impact stat in the One Page Generator body.
 *
 * @param value       large headline value, e.g. "+45%"
 * @param description short descriptor underneath the value
 */
public record OnePagerStat(String value, String description) {

}
