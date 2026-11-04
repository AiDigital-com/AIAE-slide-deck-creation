package com.aidigital.strategyplanning.service.onepager.services;

import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.util.List;

/**
 * Ungroups wrapper groups on a master module slide so their children become top-level shapes
 * that assembly can look up by id and clone one by one.
 *
 * <p>The master's designer groups a module's shapes to move them as one, but assembly clones
 * individual shapes and reads their anchors in slide coordinates. A grouped child's anchor is
 * expressed in its group's child space instead, so the group's transform is baked into each child
 * while ungrouping.
 */
public interface OnePagerGroupFlattener {

	/**
	 * Replaces each listed group on the slide with its children, positioned as the group showed
	 * them. Must run before the slide's shapes are first read, because POI caches that list.
	 *
	 * @param slide    master module slide, edited in place
	 * @param groupIds shape ids of the top-level groups to ungroup
	 * @throws IllegalStateException when a listed group is not on the slide
	 */
	void flatten(XSLFSlide slide, List<Integer> groupIds);
}
