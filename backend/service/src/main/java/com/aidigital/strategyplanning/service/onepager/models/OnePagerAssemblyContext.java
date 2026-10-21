package com.aidigital.strategyplanning.service.onepager.models;

import org.apache.poi.xslf.usermodel.XSLFSlide;

/**
 * Everything a module composer needs to clone one body module onto the page: the canvas being
 * assembled, the master's three body-module source slides, and the drafted content.
 *
 * @param canvas        the shell slide receiving the modules
 * @param bodySource    master slide holding the tracker, solution cards, and spotlight cards
 * @param kpiSource     master slide holding the KPI tiles, image + text, takeaway, stats, and
 *                      capability cards
 * @param insightSource master slide holding the photos, insight cards, and milestone banner
 * @param content       drafted page content
 */
public record OnePagerAssemblyContext(OnePagerCanvas canvas, XSLFSlide bodySource, XSLFSlide kpiSource,
									  XSLFSlide insightSource, OnePagerDraft content) {

}
