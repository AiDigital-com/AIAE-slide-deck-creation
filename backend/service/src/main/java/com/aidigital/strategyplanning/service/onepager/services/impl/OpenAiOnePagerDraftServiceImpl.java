package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftParser;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftPromptComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerOpenAiGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * OpenAI (chat completions, strict JSON mode, no tools) implementation of
 * {@link OnePagerDraftService}: one call drafts every field of the picked modules.
 */
@Service
@RequiredArgsConstructor
public class OpenAiOnePagerDraftServiceImpl implements OnePagerDraftService {

	private final OnePagerOpenAiGateway openAiGateway;
	private final OnePagerDraftPromptComposer promptComposer;
	private final OnePagerDraftParser draftParser;

	@Override
	public boolean isConnected() {
		return openAiGateway.isConnected();
	}

	@Override
	public OnePagerDraft draft(OnePagerBrief brief) {
		String content = openAiGateway.callChatCompletion(promptComposer.buildDraftRequestBody(brief));
		return draftParser.parseDraft(content, brief);
	}
}
