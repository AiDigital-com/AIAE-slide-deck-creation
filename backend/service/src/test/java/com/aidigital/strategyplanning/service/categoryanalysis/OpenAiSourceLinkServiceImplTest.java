package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.OpenAiSourceLinkServiceImpl;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenAiSourceLinkServiceImpl} request building and response parsing.
 */
class OpenAiSourceLinkServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private CategoryAnalysisProperties properties;
	private OpenAiSourceLinkServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new CategoryAnalysisProperties();
		service = new OpenAiSourceLinkServiceImpl(properties, objectMapper, mock(OpenAiChatClient.class));
	}

	@Test
	void resolveSourceLinksReturnsEmptyWithoutApiKey() {
		assertThat(service.resolveSourceLinks(Map.of("trends_sources", "Source: KFF"))).isEmpty();
		assertThat(service.resolveSourceLinks(Map.of())).isEmpty();
		assertThat(service.resolveSourceLinks(null)).isEmpty();
	}

	@Test
	void buildRequestBodyListsEveryLineAndAsksForExactSubstrings() throws Exception {
		properties.setOpenaiModel("gpt-4o");
		Map<String, String> sources = new LinkedHashMap<>();
		sources.put("trends_sources", "Source: KFF, 2025");
		sources.put("drivers_sources", "Source: Mintel");
		JsonNode body = objectMapper.readTree(service.buildRequestBody(sources));

		assertThat(body.path("model").asText()).isEqualTo("gpt-4o");
		assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");
		String prompt = body.path("messages").path(0).path("content").asText();
		assertThat(prompt).contains("trends_sources").contains("Source: KFF, 2025");
		assertThat(prompt).contains("drivers_sources").contains("Source: Mintel");
		assertThat(prompt).contains("EXACT substring");
		assertThat(prompt).contains("never ").contains("deep links");
	}

	@Test
	void parseLinksKeepsOnlyValidatedLinks() {
		Map<String, String> sources = Map.of(
				"trends_sources", "Source: KFF, Medicare Advantage in 2025; CMS resources.");
		String content = """
				{"trends_sources":[
				  {"text":"KFF","url":"https://www.kff.org"},
				  {"text":"CMS","url":"ftp://bad.example"},
				  {"text":"Nielsen","url":"https://www.nielsen.com"},
				  {"text":"","url":"https://www.empty.org"},
				  {"text":"CMS","url":"https://www.cms.gov"}
				]}""";
		Map<String, List<SourceLink>> links = service.parseLinks(content, sources);
		assertThat(links).containsOnlyKeys("trends_sources");
		assertThat(links.get("trends_sources")).containsExactly(
				new SourceLink("KFF", "https://www.kff.org"),
				new SourceLink("CMS", "https://www.cms.gov"));
	}

	@Test
	void parseLinksOmitsKeysWithNoValidLinks() {
		Map<String, List<SourceLink>> links = service.parseLinks(
				"{\"trends_sources\":[]}", Map.of("trends_sources", "Source: KFF"));
		assertThat(links).isEmpty();
	}

	@Test
	void parseLinksRejectsInvalidJson() {
		assertThatThrownBy(() -> service.parseLinks("not json", Map.of("k", "v")))
				.isInstanceOf(AppException.class);
	}

	@Test
	void isValidLinkRequiresSubstringMatchAndHttpUrl() {
		assertThat(service.isValidLink("KFF", "https://www.kff.org", "Source: KFF")).isTrue();
		assertThat(service.isValidLink("KFF", "https://www.kff.org", "Source: Mintel")).isFalse();
		assertThat(service.isValidLink("KFF", "www.kff.org", "Source: KFF")).isFalse();
		assertThat(service.isValidLink(" ", "https://www.kff.org", "Source: KFF")).isFalse();
		assertThat(service.isValidLink("KFF", "https://www.kff.org", null)).isFalse();
	}
}
