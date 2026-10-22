package com.aidigital.strategyplanning.service.categoryanalysis.services;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;

import java.util.List;

/**
 * Fact-checks the quantitative fields of an In-Depth draft against live web search.
 *
 * <p>Checking runs one small {@code web_search} call per topic group, so a call's size never
 * depends on how many slides are checked overall. It is best-effort: a topic whose verification
 * fails keeps its drafted values rather than failing the whole draft.
 */
public interface InDepthFactChecker {

	/**
	 * Verifies the factual fields of a draft and applies any corrections the model returns.
	 *
	 * @param category market category being analysed
	 * @param tokens   every distinct token that was drafted
	 * @param drafted  the drafted fields to verify
	 * @return the same fields, with any verified corrections applied
	 */
	List<InDepthDraftField> verifyFactualFields(String category, List<InDepthTokenSpec> tokens,
			List<InDepthDraftField> drafted);
}
