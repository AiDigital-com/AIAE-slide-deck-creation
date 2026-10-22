package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;

import java.util.List;
import java.util.Optional;

/**
 * Answers which text fields and icon slots the One-Page Case Study template has, and applies the
 * template's casing and character budget to a value.
 *
 * <p>The prompt composer, the reply parser, the measurer, and the deck filler all need this view
 * of the template — one to ask the model for every field, one to normalize the reply, one to
 * measure each box, one to fill every token — so the lookup lives in one place.
 */
public interface OnePageTemplateRegistry {

	/**
	 * Lists every text field of the template, in slide order.
	 *
	 * @return every template text field
	 */
	List<OnePageFieldSpec> fields();

	/**
	 * Lists every tagged icon slot of the template.
	 *
	 * @return every icon slot
	 */
	List<OnePageIconSlot> iconSlots();

	/**
	 * Finds a field by its token key.
	 *
	 * @param key token key
	 * @return the field, or empty when the key is unknown
	 */
	Optional<OnePageFieldSpec> field(String key);

	/**
	 * Applies the template's casing and the field's character budget to a value.
	 *
	 * @param spec  field the value belongs to
	 * @param value raw value, may be null
	 * @return the trimmed value, capitalized when the template uses capitals, cut at a word
	 * boundary to fit the budget; empty for a null or blank value
	 */
	String formatValue(OnePageFieldSpec spec, String value);

	/**
	 * Reports whether a field states plain facts that must never be lengthened with
	 * interpretation.
	 *
	 * @param key token key
	 * @return true when the field is facts-only
	 */
	boolean isFactsOnly(String key);
}
