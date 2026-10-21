package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;

import java.util.Collection;
import java.util.List;

/**
 * Answers which In-Depth template tokens belong to which checked slides, and how much text each
 * of them can hold.
 *
 * <p>The prompt composer, the reply parser, and the deck filler all need this view of the token
 * registry — one to ask the model for a slide's fields, one to check the reply carries them, one
 * to write them to the right slides — so the lookup lives in one place.
 */
public interface InDepthTemplateTokenRegistry {

	/**
	 * Lists every selectable slide of the master template, in deck order.
	 *
	 * @return slide catalog for the "check to include" preview
	 */
	List<InDepthSlideSpec> slides();

	/**
	 * Returns the token specs whose value is written to one slide, preserving registry order.
	 *
	 * @param slideNumber slide to filter tokens for (1-24)
	 * @return token specs targeting the slide, empty when none match
	 */
	List<InDepthTokenSpec> tokensForSlide(int slideNumber);

	/**
	 * Returns the distinct token specs to draft for a set of checked slides. Each token appears
	 * once even when it targets several of the checked slides (e.g. a headline shared by two
	 * checked layout variants of the same topic), so the AI drafts it exactly once and the deck
	 * filler then writes it to every checked slide that uses it.
	 *
	 * @param selectedSlides slide numbers the user checked, may be null
	 * @return distinct token specs to draft, in registry order
	 */
	List<InDepthTokenSpec> tokensForSlides(Collection<Integer> selectedSlides);

	/**
	 * Returns the maximum characters a token's drafted value may hold, derived from the master
	 * template's own sample text for that field — the original author's evidence of how much a
	 * text box can hold — or a fixed budget for short label boxes.
	 *
	 * @param spec token spec whose sample text sets the budget
	 * @return maximum character length for this field's drafted value
	 */
	int maxCharsFor(InDepthTokenSpec spec);
}
