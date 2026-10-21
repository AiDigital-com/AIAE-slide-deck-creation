package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftParser;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftPromptComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerOpenAiGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the drafting orchestration: compose, call, parse.
 */
class OpenAiOnePagerDraftServiceImplTest {

	@Test
	void shouldComposeCallAndParseInOrderTest() {
		// Given: a brief, a composer, a gateway that answers, and a parser
		OnePagerOpenAiGateway gateway = mock(OnePagerOpenAiGateway.class);
		OnePagerDraftPromptComposer composer = mock(OnePagerDraftPromptComposer.class);
		OnePagerDraftParser parser = mock(OnePagerDraftParser.class);
		OnePagerBrief brief = new OnePagerBrief("Automotive", "Acme", "Brief", List.of(OnePagerModuleKey.KPIS));
		OnePagerDraft expected = OnePagerFixtures.headerOnly();
		when(composer.buildDraftRequestBody(brief)).thenReturn("{\"model\":\"gpt-4o\"}");
		when(gateway.callChatCompletion("{\"model\":\"gpt-4o\"}")).thenReturn("{\"topicPill\":\"AUTO\"}");
		when(parser.parseDraft("{\"topicPill\":\"AUTO\"}", brief)).thenReturn(expected);

		// When: a draft is requested
		OnePagerDraft draft = new OpenAiOnePagerDraftServiceImpl(gateway, composer, parser).draft(brief);

		// Then: the parsed draft comes back and the gateway received the composed body
		assertThat(draft).isSameAs(expected);
		verify(gateway).callChatCompletion("{\"model\":\"gpt-4o\"}");
	}

	@Test
	void shouldReportTheGatewaysConnectionStateTest() {
		// Given: a connected gateway
		OnePagerOpenAiGateway gateway = mock(OnePagerOpenAiGateway.class);
		when(gateway.isConnected()).thenReturn(true);

		// When-Then: the service mirrors it
		assertThat(new OpenAiOnePagerDraftServiceImpl(gateway, mock(OnePagerDraftPromptComposer.class),
				mock(OnePagerDraftParser.class)).isConnected()).isTrue();
	}
}
