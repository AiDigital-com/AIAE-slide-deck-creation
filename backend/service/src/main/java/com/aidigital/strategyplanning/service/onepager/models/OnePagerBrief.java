package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * The user's drafting brief for one One Page Generator page.
 *
 * @param category      market category or vertical the page targets, e.g. "Automotive"
 * @param clientName    label used in the generated file's title
 * @param guidanceNotes the brief: the only source of facts, figures, and claims the drafter uses
 * @param moduleOrder   the body modules to draft, in stacking order; a module is included solely
 *                      by its presence here
 */
public record OnePagerBrief(String category, String clientName, String guidanceNotes,
							List<OnePagerModuleKey> moduleOrder) {

	/**
	 * Reports whether a module was picked for the page.
	 *
	 * @param key module to look for
	 * @return true when the module is in the stacking order
	 */
	public boolean includes(OnePagerModuleKey key) {
		return moduleOrder != null && moduleOrder.contains(key);
	}
}
