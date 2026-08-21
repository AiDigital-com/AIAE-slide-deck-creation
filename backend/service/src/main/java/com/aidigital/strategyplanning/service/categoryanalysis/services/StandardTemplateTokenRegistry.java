package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;

import java.util.List;

/**
 * Answers which template tokens belong to which slide of the Standard deck.
 *
 * <p>Both the prompt composer and the reply parser need this view of the token registry — one to
 * ask the model for a slide's fields, the other to check the reply carries them — so the lookup
 * lives in one place instead of being duplicated on either side.
 */
public interface StandardTemplateTokenRegistry {

	/**
	 * Returns the tokens that belong to one slide.
	 *
	 * @param slideNumber slide number
	 * @return token specs of that slide, empty when the slide does not exist
	 */
	List<StandardTemplateTokens.TokenSpec> tokensForSlide(int slideNumber);
}
