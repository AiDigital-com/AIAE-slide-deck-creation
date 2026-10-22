package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsight;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestone;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolution;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStat;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftParser;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerJsonFieldReader;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link OnePagerDraftParser}. Field names match the keys the prompt
 * composer asks the model for.
 */
@Service
@RequiredArgsConstructor
public class OnePagerDraftParserImpl implements OnePagerDraftParser {

	/** Top-level JSON members of the draft reply. */
	public static final String TOPIC_PILL = "topicPill";
	public static final String MAIN_TITLE = "mainTitle";
	public static final String SUPPORTING_COPY = "supportingCopy";
	public static final String PROCESS_SECTION_LABEL = "processSectionLabel";
	public static final String PROCESS_STEPS = "processSteps";
	public static final String SOLUTIONS_SECTION_LABEL = "solutionsSectionLabel";
	public static final String SOLUTIONS = "solutions";
	public static final String SOLUTIONS_VISUAL_LABEL = "solutionsVisualLabel";
	public static final String KPI_SECTION_LABEL = "kpiSectionLabel";
	public static final String KPIS = "kpis";
	public static final String KPI_DISCLAIMER = "kpiDisclaimer";
	public static final String IMAGE_TEXT_SECTION_LABEL = "imageTextSectionLabel";
	public static final String IMAGE_TEXT_BODY = "imageTextBody";
	public static final String KEY_TAKEAWAY = "keyTakeaway";
	public static final String IMPACT_STATS = "impactStats";
	public static final String CAPABILITY_CARDS = "capabilityCards";
	public static final String SPOTLIGHT_CARDS = "spotlightCards";
	public static final String INSIGHT_CARDS = "insightCards";
	public static final String MILESTONE_HEADLINE = "milestoneHeadline";
	public static final String MILESTONES = "milestones";

	/** Nested JSON members of the card objects. */
	public static final String TITLE = "title";
	public static final String VALUE_LINE = "valueLine";
	public static final String BODY = "body";
	public static final String VALUE = "value";
	public static final String LABEL = "label";
	public static final String DESCRIPTION = "description";
	public static final String BULLETS = "bullets";
	public static final String FOOTER = "footer";
	public static final String KPI_CHIPS = "kpiChips";
	public static final String STAGE_NAMES = "stageNames";
	public static final String INSIGHT = "insight";
	public static final String SUBTEXT = "subtext";

	private static final String NO_PREFIX = "";
	private static final String INCOMPLETE_MESSAGE = "AI draft incomplete — missing fields: ";

	private final OnePagerJsonFieldReader reader;

	@Override
	public OnePagerDraft parseDraft(String content, OnePagerBrief brief) {
		JsonNode root = reader.parseJson(content);
		List<String> missing = new ArrayList<>();
		boolean tracker = brief.includes(OnePagerModuleKey.TRACKER);
		boolean solutions = brief.includes(OnePagerModuleKey.SOLUTIONS);
		boolean kpis = brief.includes(OnePagerModuleKey.KPIS);
		boolean imageText = brief.includes(OnePagerModuleKey.IMAGE_TEXT);
		boolean milestones = brief.includes(OnePagerModuleKey.MILESTONE_BANNER);
		OnePagerDraft draft = new OnePagerDraft(
				text(root, TOPIC_PILL, OnePagerFieldBudget.TOPIC_PILL, missing),
				reader.requireStringArray(root, MAIN_TITLE, OnePagerFieldBudget.TITLE_LINE, missing, NO_PREFIX),
				text(root, SUPPORTING_COPY, OnePagerFieldBudget.SUPPORTING_COPY, missing),
				brief.moduleOrder(),
				tracker ? text(root, PROCESS_SECTION_LABEL, OnePagerFieldBudget.SECTION_LABEL, missing) : null,
				tracker ? reader.requireStringArray(root, PROCESS_STEPS, OnePagerFieldBudget.TRACKER_STEP, missing,
						NO_PREFIX) : List.of(),
				solutions ? text(root, SOLUTIONS_SECTION_LABEL, OnePagerFieldBudget.SECTION_LABEL, missing) : null,
				solutions ? parseSolutions(root.path(SOLUTIONS), missing) : List.of(),
				solutions && brief.includes(OnePagerModuleKey.SOLUTIONS_VISUAL)
						? text(root, SOLUTIONS_VISUAL_LABEL, OnePagerFieldBudget.SOLUTIONS_VISUAL_LABEL, missing)
						: null,
				kpis ? text(root, KPI_SECTION_LABEL, OnePagerFieldBudget.SECTION_LABEL, missing) : null,
				kpis ? parseKpis(root.path(KPIS), KPIS, missing) : List.of(),
				kpis ? text(root, KPI_DISCLAIMER, OnePagerFieldBudget.KPI_DISCLAIMER, missing) : null,
				imageText ? text(root, IMAGE_TEXT_SECTION_LABEL, OnePagerFieldBudget.IMAGE_TEXT_LABEL, missing)
						: null,
				imageText ? text(root, IMAGE_TEXT_BODY, OnePagerFieldBudget.IMAGE_TEXT_BODY, missing) : null,
				brief.includes(OnePagerModuleKey.KEY_TAKEAWAY)
						? text(root, KEY_TAKEAWAY, OnePagerFieldBudget.KEY_TAKEAWAY, missing) : null,
				brief.includes(OnePagerModuleKey.IMPACT_STATS) ? parseStats(root.path(IMPACT_STATS), missing)
						: List.of(),
				brief.includes(OnePagerModuleKey.CAPABILITY_CARDS)
						? parseCapabilityCards(root.path(CAPABILITY_CARDS), missing) : List.of(),
				brief.includes(OnePagerModuleKey.SPOTLIGHT_CARDS)
						? parseSpotlightCards(root.path(SPOTLIGHT_CARDS), missing) : List.of(),
				brief.includes(OnePagerModuleKey.INSIGHT_CARDS) ? parseInsights(root.path(INSIGHT_CARDS), missing)
						: List.of(),
				milestones ? text(root, MILESTONE_HEADLINE, OnePagerFieldBudget.MILESTONE_HEADLINE, missing) : null,
				milestones ? parseMilestones(root.path(MILESTONES), missing) : List.of());
		if (!missing.isEmpty()) {
			throw new AppException(ErrorReason.C003, INCOMPLETE_MESSAGE + missing);
		}
		return draft;
	}

	/**
	 * Reads a required top-level text field.
	 *
	 * @param root    JSON object containing the field
	 * @param field   field name
	 * @param budget  character budget
	 * @param missing accumulator of missing field paths
	 * @return the capped value, or null when missing
	 */
	String text(JsonNode root, String field, OnePagerFieldBudget budget, List<String> missing) {
		return reader.requireText(root, field, budget, missing, NO_PREFIX);
	}

	/**
	 * Builds the path prefix of the n-th entry of an array field, e.g. {@code "solutions[0]."}.
	 *
	 * @param field array field name
	 * @param index entry index
	 * @return the prefix
	 */
	String prefix(String field, int index) {
		return field + "[" + index + "].";
	}

	/**
	 * Parses the drafted solution cards.
	 *
	 * @param array   JSON array of solution objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed cards
	 */
	List<OnePagerSolution> parseSolutions(JsonNode array, List<String> missing) {
		List<OnePagerSolution> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(SOLUTIONS, index++);
			result.add(new OnePagerSolution(
					reader.requireText(node, TITLE, OnePagerFieldBudget.SOLUTION_TITLE, missing, prefix),
					reader.requireText(node, VALUE_LINE, OnePagerFieldBudget.SOLUTION_VALUE_LINE, missing, prefix),
					reader.requireText(node, BODY, OnePagerFieldBudget.SOLUTION_BODY, missing, prefix)));
		}
		return result;
	}

	/**
	 * Parses drafted KPI value/label pairs, for the KPI tiles and the spotlight KPI chips alike.
	 *
	 * @param array   JSON array of KPI objects; a missing or non-array node yields no entries
	 * @param field   name of the array field, for the missing-field path
	 * @param missing accumulator of missing field paths
	 * @return the parsed KPIs
	 */
	List<OnePagerKpi> parseKpis(JsonNode array, String field, List<String> missing) {
		List<OnePagerKpi> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(field, index++);
			result.add(new OnePagerKpi(
					reader.requireText(node, VALUE, OnePagerFieldBudget.KPI_VALUE, missing, prefix),
					reader.requireText(node, LABEL, OnePagerFieldBudget.KPI_LABEL, missing, prefix)));
		}
		return result;
	}

	/**
	 * Parses the drafted impact stats.
	 *
	 * @param array   JSON array of stat objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed stats
	 */
	List<OnePagerStat> parseStats(JsonNode array, List<String> missing) {
		List<OnePagerStat> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(IMPACT_STATS, index++);
			result.add(new OnePagerStat(
					reader.requireText(node, VALUE, OnePagerFieldBudget.STAT_VALUE, missing, prefix),
					reader.requireText(node, DESCRIPTION, OnePagerFieldBudget.STAT_DESCRIPTION, missing, prefix)));
		}
		return result;
	}

	/**
	 * Parses the drafted capability cards.
	 *
	 * @param array   JSON array of capability card objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed cards
	 */
	List<OnePagerCapabilityCard> parseCapabilityCards(JsonNode array, List<String> missing) {
		List<OnePagerCapabilityCard> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(CAPABILITY_CARDS, index++);
			result.add(new OnePagerCapabilityCard(
					reader.requireText(node, TITLE, OnePagerFieldBudget.CAPABILITY_TITLE, missing, prefix),
					reader.requireText(node, DESCRIPTION, OnePagerFieldBudget.CAPABILITY_DESCRIPTION, missing, prefix),
					reader.requireStringArray(node, BULLETS, OnePagerFieldBudget.CAPABILITY_BULLET, missing, prefix),
					reader.requireText(node, FOOTER, OnePagerFieldBudget.CAPABILITY_FOOTER, missing, prefix)));
		}
		return result;
	}

	/**
	 * Parses the drafted spotlight cards. KPI chips and stage names are read leniently: they are
	 * legitimately empty on two of the three cards, so an empty array is the expected state.
	 *
	 * @param array   JSON array of spotlight card objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed cards
	 */
	List<OnePagerSpotlightCard> parseSpotlightCards(JsonNode array, List<String> missing) {
		List<OnePagerSpotlightCard> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(SPOTLIGHT_CARDS, index++);
			result.add(new OnePagerSpotlightCard(
					reader.requireText(node, TITLE, OnePagerFieldBudget.SPOTLIGHT_TITLE, missing, prefix),
					reader.requireText(node, DESCRIPTION, OnePagerFieldBudget.SPOTLIGHT_DESCRIPTION, missing, prefix),
					reader.requireStringArray(node, BULLETS, OnePagerFieldBudget.SPOTLIGHT_BULLET, missing, prefix),
					parseKpis(node.path(KPI_CHIPS), prefix + KPI_CHIPS, missing),
					reader.optionalStringArray(node, STAGE_NAMES, OnePagerFieldBudget.SPOTLIGHT_STAGE_NAME)));
		}
		return result;
	}

	/**
	 * Parses the drafted insight cards.
	 *
	 * @param array   JSON array of insight objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed cards
	 */
	List<OnePagerInsight> parseInsights(JsonNode array, List<String> missing) {
		List<OnePagerInsight> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(INSIGHT_CARDS, index++);
			result.add(new OnePagerInsight(
					reader.requireText(node, TITLE, OnePagerFieldBudget.INSIGHT_TITLE, missing, prefix),
					reader.requireText(node, INSIGHT, OnePagerFieldBudget.INSIGHT_TEXT, missing, prefix)));
		}
		return result;
	}

	/**
	 * Parses the drafted milestones.
	 *
	 * @param array   JSON array of milestone objects
	 * @param missing accumulator of missing field paths
	 * @return the parsed milestones
	 */
	List<OnePagerMilestone> parseMilestones(JsonNode array, List<String> missing) {
		List<OnePagerMilestone> result = new ArrayList<>();
		int index = 0;
		for (JsonNode node : array) {
			String prefix = prefix(MILESTONES, index++);
			result.add(new OnePagerMilestone(
					reader.requireText(node, TITLE, OnePagerFieldBudget.MILESTONE_TITLE, missing, prefix),
					reader.requireText(node, SUBTEXT, OnePagerFieldBudget.MILESTONE_SUBTEXT, missing, prefix)));
		}
		return result;
	}
}
