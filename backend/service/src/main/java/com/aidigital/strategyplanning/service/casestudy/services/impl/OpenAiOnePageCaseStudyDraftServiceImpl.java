package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyDraft;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageCaseStudyField;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldValue;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyOpenAiGateway;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageCaseStudyDraftService;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftParser;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFieldMeasurer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFitRevisionPolicy;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenAI-backed implementation of {@link OnePageCaseStudyDraftService}.
 *
 * <p>Drafting runs in three steps: one chat completions call (JSON mode) reads every document
 * and screenshot and drafts all fields plus an icon per slot; {@link OnePageFitRevisionPolicy}
 * measures every value in its real box and sends misfits back for rewriting; the final values are
 * measured once more so the review form shows each field's fit.
 */
@Service
@RequiredArgsConstructor
public class OpenAiOnePageCaseStudyDraftServiceImpl implements OnePageCaseStudyDraftService {

	private static final String FILE_NAME_SEPARATOR = ", ";

	private final CaseStudyOpenAiGateway openAiGateway;
	private final OnePageDraftPromptComposer promptComposer;
	private final OnePageDraftParser draftParser;
	private final OnePageFieldMeasurer measurer;
	private final OnePageFitRevisionPolicy revisionPolicy;

	@Override
	public OnePageCaseStudyDraft draft(List<SourceDocument> documents, List<SourceImage> images) {
		String content = openAiGateway.callChatCompletion(promptComposer.buildDraftRequestBody(documents, images));
		String sourceFileNames = Stream.concat(
						documents.stream().map(SourceDocument::fileName),
						images.stream().map(SourceImage::fileName))
				.collect(Collectors.joining(FILE_NAME_SEPARATOR));
		OnePageCaseStudyDraft first = draftParser.parseDraft(content, sourceFileNames);
		Map<String, String> values = new LinkedHashMap<>();
		first.fields().forEach(field -> values.put(field.key(), field.value()));
		revisionPolicy.reviseToFit(values, documents, images);
		return new OnePageCaseStudyDraft(measurer.measureFields(values), first.icons(), first.iconLibrary(),
				sourceFileNames);
	}

	@Override
	public List<OnePageCaseStudyField> measureFields(List<OnePageFieldValue> values) {
		Map<String, String> byKey = new LinkedHashMap<>();
		for (OnePageFieldValue value : values) {
			byKey.put(value.key(), value.value());
		}
		return measurer.measureFields(byKey);
	}
}
