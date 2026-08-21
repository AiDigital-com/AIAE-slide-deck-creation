package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;

import java.util.Map;

/**
 * Deterministic template-token mapping used whenever the AI drafting engine is unavailable.
 *
 * <p>This is the path the product takes when OpenAI is unreachable or unconfigured, so it decides
 * what actually lands on the slides in that case.
 */
public interface CaseStudyFallbackTokenResolver {

	/**
	 * Maps reviewed case study fields onto every template token without calling the model.
	 *
	 * @param command reviewed case study fields
	 * @return values for every template token key
	 */
	Map<String, String> resolve(CreateCaseStudyCommand command);
}
