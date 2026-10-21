package com.aidigital.strategyplanning.service.onepager.models;

/**
 * One KPI tile on the master's KPI slide: a filled box, a large value text, and a label text —
 * three flat top-level shapes, safe to clone and reposition individually.
 *
 * @param boxId   shape id of the tile's background box
 * @param valueId shape id of the tile's large value text
 * @param labelId shape id of the tile's label text underneath
 */
public record OnePagerKpiSlot(int boxId, int valueId, int labelId) {

}
