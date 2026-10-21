package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFieldMeasurer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTextFitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link OnePageFieldMeasurer}.
 */
@Service
@RequiredArgsConstructor
public class OnePageFieldMeasurerImpl implements OnePageFieldMeasurer {

	/** Fit reported for a field without a measured box: its character ceiling already cut it. */
	private static final int UNMEASURED = 0;
	private static final String SHORT_NOTE_FORMAT = "Your files don't have enough detail to fill this box — it fills "
			+ "%d of %d lines. Add detail here or upload more material.";

	private final OnePageTemplateRegistry templateRegistry;
	private final OnePageTextFitter textFitter;

	@Override
	public List<OnePageCaseStudyField> measureFields(Map<String, String> values) {
		List<OnePageCaseStudyField> fields = new ArrayList<>();
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			String value = templateRegistry.formatValue(spec, values.get(spec.key()));
			if (!spec.isMeasured()) {
				fields.add(new OnePageCaseStudyField(spec.key(), spec.label(), spec.section(), value, spec.source(),
						spec.maxChars(), UNMEASURED, UNMEASURED, UNMEASURED,
						value.isEmpty() ? OnePageFit.EMPTY : OnePageFit.FITS, null));
				continue;
			}
			OnePageTextMeasurement measurement = measureCandidate(spec, value, values);
			String note = measurement.fit() == OnePageFit.SHORT
					? SHORT_NOTE_FORMAT.formatted(measurement.lines(), measurement.minLines())
					: null;
			fields.add(new OnePageCaseStudyField(spec.key(), spec.label(), spec.section(), value, spec.source(),
					spec.maxChars(), measurement.minLines(), measurement.maxLines(), measurement.lines(),
					measurement.fit(), note));
		}
		return fields;
	}

	@Override
	public OnePageTextMeasurement measure(OnePageFieldSpec spec, Map<String, String> values) {
		return measureCandidate(spec, values.getOrDefault(spec.key(), ""), values);
	}

	@Override
	public OnePageTextMeasurement measureCandidate(OnePageFieldSpec spec, String candidate,
			Map<String, String> values) {
		return textFitter.measure(spec, candidate, prefix(spec, values));
	}

	@Override
	public String prefix(OnePageFieldSpec spec, Map<String, String> values) {
		return spec.box().prefixKey() == null ? null : values.get(spec.box().prefixKey());
	}

	@Override
	public int distance(OnePageFieldSpec spec, String candidate, Map<String, String> values) {
		OnePageTextMeasurement m = measureCandidate(spec, candidate, values);
		return switch (m.fit()) {
			case FITS -> 0;
			case SHORT -> m.minLines() - m.wideLines();
			case OVER -> m.lines() - m.maxLines();
			case EMPTY -> Integer.MAX_VALUE;
		};
	}

	@Override
	public List<OnePageFieldSpec> misfits(Map<String, String> values) {
		return templateRegistry.fields().stream()
				.filter(OnePageFieldSpec::isMeasured)
				.filter(spec -> {
					OnePageFit fit = measure(spec, values).fit();
					return fit == OnePageFit.SHORT || fit == OnePageFit.OVER;
				})
				.toList();
	}
}
