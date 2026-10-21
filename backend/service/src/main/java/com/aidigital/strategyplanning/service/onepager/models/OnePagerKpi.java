package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One performance-signal KPI tile, or one spotlight KPI chip, in the One Page Generator body.
 *
 * @param value large headline value, e.g. "30%" or "2X"
 * @param label short descriptor underneath, e.g. "INCREASED SCHEDULED APPOINTMENTS"
 */
public record OnePagerKpi(String value, String label) {

}
