package com.aidigital.strategyplanning.service.rfpoutline;

import com.aidigital.strategyplanning.service.rfpoutline.services.impl.RfpOutlineDocRequestFactoryImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Docs request factory: one insert carrying the whole text, then a style
 * request per heading and body paragraph whose ranges line up with the inserted text.
 */
class RfpOutlineDocRequestFactoryImplTest {

	@Test
	void shouldInsertTitleAndHeadedSectionsInOrderAndSkipBlankOnesTest() {
		// Given: an outline with a blank industry and no outcome
		Map<String, String> sections = new LinkedHashMap<>();
		sections.put("client_name", "Acme");
		sections.put("industry", "  ");
		sections.put("challenge", "Costs were rising");
		sections.put("deck_outline", "1. Intro\n2. Approach");

		// When: the requests are built
		ArrayNode requests = new RfpOutlineDocRequestFactoryImpl(new ObjectMapper())
				.buildDocumentRequests("Acme RFP", sections);

		// Then: the first request inserts the full text at the body start, blank sections are absent
		JsonNode insert = requests.get(0).path("insertText");
		assertThat(insert.path("location").path("index").asInt()).isEqualTo(1);
		assertThat(insert.path("text").asText()).isEqualTo("Acme RFP\n"
				+ "\nClient\nAcme\n"
				+ "\nChallenge\nCosts were rising\n"
				+ "\nResponse Deck Outline\n1. Intro\n2. Approach\n");
		assertThat(insert.path("text").asText()).doesNotContain("Industry", "Outcome");
	}

	@Test
	void shouldStyleEveryParagraphOverTheRangeItOccupiesTest() {
		// Given: a title and one section
		Map<String, String> sections = Map.of("solution", "Rebuild the funnel");

		// When: the requests are built
		ArrayNode requests = new RfpOutlineDocRequestFactoryImpl(new ObjectMapper())
				.buildDocumentRequests("Acme RFP", sections);

		// Then: title, heading and body are styled over consecutive ranges that match the text
		// "Acme RFP\n" occupies [1,10); "\n" gap [10,11); "Solution\n" [11,20); body [20,39)
		assertThat(requests).hasSize(4);
		JsonNode title = requests.get(1).path("updateParagraphStyle");
		assertThat(title.path("paragraphStyle").path("namedStyleType").asText()).isEqualTo("TITLE");
		assertThat(title.path("range").path("startIndex").asInt()).isEqualTo(1);
		assertThat(title.path("range").path("endIndex").asInt()).isEqualTo(10);
		JsonNode heading = requests.get(2).path("updateParagraphStyle");
		assertThat(heading.path("paragraphStyle").path("namedStyleType").asText()).isEqualTo("HEADING_2");
		assertThat(heading.path("range").path("startIndex").asInt()).isEqualTo(11);
		assertThat(heading.path("range").path("endIndex").asInt()).isEqualTo(20);
		JsonNode body = requests.get(3).path("updateParagraphStyle");
		assertThat(body.path("paragraphStyle").path("namedStyleType").asText()).isEqualTo("NORMAL_TEXT");
		assertThat(body.path("range").path("startIndex").asInt()).isEqualTo(20);
		assertThat(body.path("range").path("endIndex").asInt()).isEqualTo(39);
		assertThat(body.path("fields").asText()).isEqualTo("namedStyleType");
	}
}
