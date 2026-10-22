package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageDeckRequestAssemblerImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One-Page icon swap requests.
 */
class OnePageDeckRequestAssemblerImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private ObjectNode presentationWithTaggedIcons() {
		ObjectNode presentation = objectMapper.createObjectNode();
		ArrayNode elements = presentation.putArray("slides").addObject().putArray("pageElements");
		elements.addObject().put("objectId", "photo").put("description", "Wavy lime ribbon").putObject("image");
		ObjectNode group = elements.addObject().put("objectId", "group");
		ObjectNode tagged = group.putObject("elementGroup").putArray("children").addObject();
		tagged.put("objectId", "icon1").put("description", "{{tactic_1_icon}}").putObject("image");
		elements.addObject().put("objectId", "icon2").put("description", "{{driver_1_icon}}").putObject("image");
		elements.addObject().put("objectId", "shape").put("description", "{{tactic_1_icon}}").putObject("shape");
		return presentation;
	}

	@Test
	void shouldTargetTaggedImagesInsideGroupsOnlyTest() {
		// Given: a deck with a tagged icon inside a group, another on the slide, a photo, and a tagged shape
		OnePageDeckRequestAssemblerImpl assembler = new OnePageDeckRequestAssemblerImpl(objectMapper);

		// When: requests are built for one chosen slot
		ArrayNode requests = assembler.buildReplaceImageRequests(presentationWithTaggedIcons(),
				Map.of("tactic_1_icon", "https://app.example.com/case-study-icons/target.png"));

		// Then: only the grouped image tagged with that slot is replaced, in place
		assertThat(requests).hasSize(1);
		JsonNode replace = requests.get(0).path("replaceImage");
		assertThat(replace.path("imageObjectId").asText()).isEqualTo("icon1");
		assertThat(replace.path("imageReplaceMethod").asText()).isEqualTo("CENTER_INSIDE");
		assertThat(replace.path("url").asText()).endsWith("/case-study-icons/target.png");
	}

	@Test
	void shouldSkipSlotsWithoutAChosenIconTest() {
		// Given: the same deck and a slot map that leaves both tagged slots blank
		OnePageDeckRequestAssemblerImpl assembler = new OnePageDeckRequestAssemblerImpl(objectMapper);

		// When: requests are built
		ArrayNode requests = assembler.buildReplaceImageRequests(presentationWithTaggedIcons(),
				Map.of("plan_icon", "https://app.example.com/case-study-icons/compass.png"));

		// Then: nothing is sent, so the deck keeps the template's icons
		assertThat(requests).isEmpty();
		assertThat(assembler.buildReplaceImageRequests(objectMapper.createObjectNode(), Map.of())).isEmpty();
	}
}
