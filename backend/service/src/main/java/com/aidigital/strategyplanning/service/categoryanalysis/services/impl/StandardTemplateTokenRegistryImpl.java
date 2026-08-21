package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.StandardTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link StandardTemplateTokenRegistry}.
 */
@Service
public class StandardTemplateTokenRegistryImpl implements StandardTemplateTokenRegistry {

	@Override
	public List<StandardTemplateTokens.TokenSpec> tokensForSlide(int slideNumber) {
		List<StandardTemplateTokens.TokenSpec> result = new ArrayList<>();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			if (spec.slideNumber() == slideNumber) {
				result.add(spec);
			}
		}
		return result;
	}
}
