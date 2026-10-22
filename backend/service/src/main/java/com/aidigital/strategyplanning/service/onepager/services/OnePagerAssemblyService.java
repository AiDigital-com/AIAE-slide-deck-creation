package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;

/**
 * Assembles a finished One Page Generator page from the master template's raw OOXML.
 */
public interface OnePagerAssemblyService {

	/**
	 * Builds a single-slide PPTX from the master template: starts from the chosen header/footer
	 * shell, clones the drafted body modules onto it in {@code content.moduleOrder()}, sizes the
	 * page to the content, and removes the master's other slides.
	 *
	 * @param masterPptxBytes raw PPTX bytes of the master template; never mutated
	 * @param shell           header/footer shell to build onto
	 * @param content         reviewed page content
	 * @return raw PPTX bytes of the finished one-slide page
	 * @throws com.aidigital.strategyplanning.service.common.error.AppException with C003 reason when the master cannot
	 * be read or the page cannot be written
	 */
	byte[] assemble(byte[] masterPptxBytes, OnePagerShell shell, OnePagerDraft content);
}
