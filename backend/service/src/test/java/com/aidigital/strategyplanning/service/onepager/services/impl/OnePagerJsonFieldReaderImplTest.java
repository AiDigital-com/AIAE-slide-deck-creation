package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for reading drafted values out of the model's JSON.
 */
class OnePagerJsonFieldReaderImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void shouldParseFencedOrCommentedJsonAndRejectTheRestTest() {
		// Given: a reader
		OnePagerJsonFieldReaderImpl reader = new OnePagerJsonFieldReaderImpl(objectMapper);

		// When-Then: fences and commentary are stripped, anything else is the feature's error
		assertThat(reader.parseJson("```json\n{\"a\":1}\n```").path("a").asInt()).isEqualTo(1);
		assertThat(reader.parseJson("Sure! {\"a\":2} Hope this helps.").path("a").asInt()).isEqualTo(2);
		assertThat(reader.extractJsonObject(null)).isEmpty();
		assertThat(reader.extractJsonObject("```\n{\"a\":3}")).isEqualTo("{\"a\":3}");
		assertThatThrownBy(() -> reader.parseJson("not json"))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("not valid JSON");
	}

	@Test
	void shouldRecordMissingAndPlaceholderTextAndCapTheRestTest() {
		// Given: a reply with a value, a placeholder, a blank, and an over-length value
		OnePagerJsonFieldReaderImpl reader = new OnePagerJsonFieldReaderImpl(objectMapper);
		JsonNode node = reader.parseJson("{\"ok\":\" 30% \",\"ph\":\"N/A.\",\"blank\":\"  \","
				+ "\"long\":\"SOLVING AUTOMOTIVE'S LOCAL MEDIA FRAGMENTATION\"}");
		List<String> missing = new ArrayList<>();

		// When: each is read
		String ok = reader.requireText(node, "ok", OnePagerFieldBudget.KPI_VALUE, missing, "");
		String placeholder = reader.requireText(node, "ph", OnePagerFieldBudget.KPI_VALUE, missing, "kpis[0].");
		String blank = reader.requireText(node, "blank", OnePagerFieldBudget.KPI_VALUE, missing, "");
		String absent = reader.requireText(node, "absent", OnePagerFieldBudget.KPI_VALUE, missing, "");
		String capped = reader.requireText(node, "long", OnePagerFieldBudget.TITLE_LINE, missing, "");

		// Then: the value is trimmed, the rest are recorded with their path, and the long one is
		// cut at a word boundary inside its budget
		assertThat(ok).isEqualTo("30%");
		assertThat(placeholder).isNull();
		assertThat(blank).isNull();
		assertThat(absent).isNull();
		assertThat(missing).containsExactly("kpis[0].ph", "blank", "absent");
		assertThat(capped).isEqualTo("SOLVING AUTOMOTIVE'S LOCAL");
		assertThat(capped.length()).isLessThanOrEqualTo(OnePagerFieldBudget.TITLE_LINE.getMaxChars());
	}

	@Test
	void shouldReadRequiredAndOptionalStringArraysTest() {
		// Given: a reply with a good array, one with holes, a non-array, and no array at all
		OnePagerJsonFieldReaderImpl reader = new OnePagerJsonFieldReaderImpl(objectMapper);
		JsonNode node = reader.parseJson("{\"good\":[\"A\",\"B\"],\"holes\":[\"A\",\"\",\"TBD\",\"C\"],"
				+ "\"scalar\":\"x\",\"empty\":[]}");
		List<String> missing = new ArrayList<>();

		// When: each is read
		List<String> good = reader.requireStringArray(node, "good", OnePagerFieldBudget.TRACKER_STEP, missing, "");
		List<String> holes = reader.requireStringArray(node, "holes", OnePagerFieldBudget.TRACKER_STEP, missing, "");
		List<String> scalar = reader.requireStringArray(node, "scalar", OnePagerFieldBudget.TRACKER_STEP, missing,
				"card.");
		List<String> empty = reader.requireStringArray(node, "empty", OnePagerFieldBudget.TRACKER_STEP, missing, "");
		List<String> optional = reader.optionalStringArray(node, "holes", OnePagerFieldBudget.TRACKER_STEP);
		List<String> optionalScalar = reader.optionalStringArray(node, "scalar", OnePagerFieldBudget.TRACKER_STEP);

		// Then: holes are skipped and recorded once per hole, non-arrays are missing, and the
		// optional reader never records anything
		assertThat(good).containsExactly("A", "B");
		assertThat(holes).containsExactly("A", "C");
		assertThat(scalar).isEmpty();
		assertThat(empty).isEmpty();
		assertThat(missing).containsExactly("holes[]", "holes[]", "card.scalar", "empty");
		assertThat(optional).containsExactly("A", "C");
		assertThat(optionalScalar).isEmpty();
	}

	@Test
	void shouldDetectPlaceholdersAndCapAtWordBoundariesTest() {
		// Given: a reader
		OnePagerJsonFieldReaderImpl reader = new OnePagerJsonFieldReaderImpl(objectMapper);

		// When-Then: placeholders are matched loosely, caps keep whole words or cut a single word
		assertThat(reader.isPlaceholder("N/A")).isTrue();
		assertThat(reader.isPlaceholder(" tbd! ")).isTrue();
		assertThat(reader.isPlaceholder("—")).isTrue();
		assertThat(reader.isPlaceholder("30%")).isFalse();
		assertThat(reader.capToFit(null, OnePagerFieldBudget.KPI_VALUE)).isNull();
		assertThat(reader.capToFit("2X", OnePagerFieldBudget.KPI_VALUE)).isEqualTo("2X");
		assertThat(reader.capToFit("31,400+ units", OnePagerFieldBudget.KPI_VALUE)).isEqualTo("31,400");
		assertThat(reader.capToFit("ABCDEFGHIJ", OnePagerFieldBudget.KPI_VALUE)).isEqualTo("ABCDEF");
	}
}
