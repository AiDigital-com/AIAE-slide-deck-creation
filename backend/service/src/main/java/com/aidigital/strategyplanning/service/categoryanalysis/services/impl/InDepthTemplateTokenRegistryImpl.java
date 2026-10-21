package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthSlideSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.InDepthTemplateTokens;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link InDepthTemplateTokenRegistry}.
 */
@Service
public class InDepthTemplateTokenRegistryImpl implements InDepthTemplateTokenRegistry {

	/**
	 * Fixed budget for the slide-21 stage titles: 2.5x the length of "Understand", set by the
	 * template owner after resizing those boxes to the journey column width.
	 */
	public static final int JOURNEY_STAGE_TITLE_MAX_CHARS = 25;

	/**
	 * Absolute headroom added to a sample's length so short fields (stat values, tags) still get
	 * a comfortable budget.
	 */
	public static final int SAMPLE_HEADROOM_CHARS = 20;

	/**
	 * Proportional headroom applied to a sample's length so long fields (bullets, paragraphs)
	 * scale with the box they fill.
	 */
	public static final double SAMPLE_HEADROOM_FACTOR = 1.6;

	/**
	 * Fixed budgets for short label boxes whose sample is too short for the generic formula to
	 * give a sensible limit (it would allow 23 characters into the 3-letter "CTV" tag).
	 */
	private static final Map<String, Integer> FIXED_MAX_CHARS = Map.ofEntries(
			Map.entry("channel_1_name", 12), Map.entry("channel_2_name", 12),
			Map.entry("donut_1_label", 24), Map.entry("donut_2_label", 24),
			Map.entry("donut_3_label", 24), Map.entry("donut_4_label", 24),
			Map.entry("feed_stage_1_label", 20), Map.entry("feed_stage_2_label", 20),
			Map.entry("feed_stage_3_label", 20), Map.entry("feed_stage_4_label", 20),
			Map.entry("feed_stage_5_label", 20));

	@Override
	public List<InDepthSlideSpec> slides() {
		return InDepthTemplateTokens.SLIDES;
	}

	@Override
	public List<InDepthTokenSpec> tokensForSlide(int slideNumber) {
		List<InDepthTokenSpec> result = new ArrayList<>();
		for (InDepthTokenSpec spec : InDepthTemplateTokens.TOKENS) {
			if (spec.slideNumbers().contains(slideNumber)) {
				result.add(spec);
			}
		}
		return result;
	}

	@Override
	public List<InDepthTokenSpec> tokensForSlides(Collection<Integer> selectedSlides) {
		Set<Integer> selected = selectedSlides == null ? Set.of() : new LinkedHashSet<>(selectedSlides);
		List<InDepthTokenSpec> result = new ArrayList<>();
		for (InDepthTokenSpec spec : InDepthTemplateTokens.TOKENS) {
			for (Integer slideNumber : spec.slideNumbers()) {
				if (selected.contains(slideNumber)) {
					result.add(spec);
					break;
				}
			}
		}
		return result;
	}

	@Override
	public int maxCharsFor(InDepthTokenSpec spec) {
		if (InDepthTemplateTokens.JOURNEY_STAGE_TITLE_KEYS.contains(spec.key())) {
			return JOURNEY_STAGE_TITLE_MAX_CHARS;
		}
		Integer fixed = FIXED_MAX_CHARS.get(spec.key());
		if (fixed != null) {
			return fixed;
		}
		int sampleLength = spec.sampleText().length();
		return Math.max(sampleLength + SAMPLE_HEADROOM_CHARS,
				(int) Math.round(sampleLength * SAMPLE_HEADROOM_FACTOR));
	}
}
