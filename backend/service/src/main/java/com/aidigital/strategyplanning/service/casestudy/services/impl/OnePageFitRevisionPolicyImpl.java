package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyOpenAiGateway;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftParser;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFieldMeasurer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFitRevisionPolicy;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTextFitter;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link OnePageFitRevisionPolicy}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnePageFitRevisionPolicyImpl implements OnePageFitRevisionPolicy {

	/** Revision rounds for values that do not fit their box. */
	public static final int MAX_REVISION_ROUNDS = 3;

	private static final String MISFIT_SEPARATOR = ", ";

	private final CaseStudyOpenAiGateway openAiGateway;
	private final OnePageDraftPromptComposer promptComposer;
	private final OnePageDraftParser draftParser;
	private final OnePageFieldMeasurer measurer;
	private final OnePageTextFitter textFitter;
	private final OnePageTemplateRegistry templateRegistry;

	@Override
	public void reviseToFit(Map<String, String> values, List<SourceDocument> documents, List<SourceImage> images) {
		for (int round = 1; round <= MAX_REVISION_ROUNDS; round++) {
			List<OnePageFieldSpec> misfits = misfitsForRound(values, round);
			if (misfits.isEmpty()) {
				log.info("One-page case study: every field fits its box after {} revision round(s)", round - 1);
				break;
			}
			log.info("One-page case study revision round {}: {}", round, describe(misfits, values));
			Map<String, String> rewrites;
			try {
				rewrites = draftParser.parseRevisions(openAiGateway.callChatCompletion(
						promptComposer.buildRevisionRequestBody(misfits, values, documents, images)));
			} catch (RuntimeException e) {
				log.warn("One-page case study revision round {} failed, keeping current values: {}", round,
						e.getMessage());
				break;
			}
			int improved = applyRewrites(misfits, rewrites, values);
			log.info("One-page case study revision round {}: {} of {} field(s) improved", round, improved,
					misfits.size());
		}
		trimOverflow(values);
	}

	/**
	 * Lists the fields to send back this round. Facts-only fields that are short have usually run
	 * out of material, so they get one retry only.
	 *
	 * @param values current values keyed by field
	 * @param round  1-based round number
	 * @return fields to rewrite this round
	 */
	List<OnePageFieldSpec> misfitsForRound(Map<String, String> values, int round) {
		List<OnePageFieldSpec> misfits = measurer.misfits(values);
		if (round == 1) {
			return misfits;
		}
		return misfits.stream()
				.filter(spec -> !(templateRegistry.isFactsOnly(spec.key())
						&& measurer.measure(spec, values).fit() == OnePageFit.SHORT))
				.toList();
	}

	/**
	 * Applies the model's rewrites, keeping each one only when it measures closer to its box. An
	 * overshoot is usually one sentence too long, so whole trailing sentences are dropped first;
	 * an overshoot that whole sentences cannot fix goes back to the model next round.
	 *
	 * @param misfits  fields that were sent for rewriting
	 * @param rewrites raw rewritten values keyed by field
	 * @param values   current values keyed by field, updated in place
	 * @return how many fields improved
	 */
	int applyRewrites(List<OnePageFieldSpec> misfits, Map<String, String> rewrites, Map<String, String> values) {
		int improved = 0;
		for (OnePageFieldSpec spec : misfits) {
			String raw = rewrites.get(spec.key());
			if (raw == null) {
				continue;
			}
			String candidate = templateRegistry.formatValue(spec, raw);
			String prefix = measurer.prefix(spec, values);
			if (textFitter.measure(spec, candidate, prefix).fit() == OnePageFit.OVER) {
				candidate = textFitter.trimSentencesToFit(spec, candidate, prefix);
			}
			if (measurer.distance(spec, candidate, values) < measurer.distance(spec, values.get(spec.key()), values)) {
				values.put(spec.key(), candidate);
				improved++;
			}
		}
		return improved;
	}

	/**
	 * Guarantees nothing overflows the slide: any value still over its box is cut at a word
	 * boundary.
	 *
	 * @param values current values keyed by field, updated in place
	 */
	void trimOverflow(Map<String, String> values) {
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			if (spec.isMeasured() && measurer.measure(spec, values).fit() == OnePageFit.OVER) {
				String trimmed = textFitter.trimToFit(spec, values.get(spec.key()), measurer.prefix(spec, values));
				log.info("One-page case study: trimmed {} to fit its box", spec.key());
				values.put(spec.key(), trimmed);
			}
		}
	}

	/**
	 * Summarizes misfits for the log.
	 *
	 * @param misfits fields that need a rewrite
	 * @param values  current values
	 * @return one-line summary
	 */
	String describe(List<OnePageFieldSpec> misfits, Map<String, String> values) {
		return misfits.stream().map(spec -> {
			OnePageTextMeasurement m = measurer.measure(spec, values);
			return spec.key() + " " + m.fit() + " " + m.lines() + "/" + m.minLines() + "-" + m.maxLines();
		}).collect(Collectors.joining(MISFIT_SEPARATOR));
	}
}
