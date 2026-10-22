package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerBrief;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerFieldBudget;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDraftPromptComposer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link OnePagerDraftPromptComposer}: a single user message in strict
 * JSON mode, no tools. The field keys match {@link OnePagerDraftParserImpl}.
 */
@Service
@RequiredArgsConstructor
public class OnePagerDraftPromptComposerImpl implements OnePagerDraftPromptComposer {

	/**
	 * Completion token ceiling for a page with every module. It also covers the reasoning tokens
	 * the model spends before answering, so it is larger than the visible output needs.
	 */
	public static final int MAX_COMPLETION_TOKENS = 8000;

	/** Fixed process-tracker step count drafted whenever the tracker is included. */
	public static final int TRACKER_STEP_COUNT = 5;

	/** Fixed core-solution card count, matching the master's 5-slot layout. */
	public static final int SOLUTION_CARD_COUNT = 5;

	/** Fixed KPI tile count drafted whenever the KPIs module is included. */
	public static final int KPI_TILE_COUNT = 4;

	/** Fixed card count of the three-card modules and the impact stats. */
	public static final int TRIPLE_COUNT = 3;

	/** Fixed KPI chip count of spotlight card index 1. */
	public static final int SPOTLIGHT_CHIP_COUNT = 7;

	private static final String MAX_COMPLETION_TOKENS_KEY = "max_completion_tokens";
	private static final String RESPONSE_FORMAT_JSON = "json_object";
	private static final String ROLE_USER = "user";
	private static final String FIELD = "- ";
	private static final String SUBFIELD = "    - ";
	private static final String CHARS = " characters)\n";
	private static final String MAX = " (max ";

	private final OnePagerProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String buildDraftRequestBody(OnePagerBrief brief) {
		ObjectNode body = objectMapper.createObjectNode();
		body.put("model", properties.getOpenaiModel());
		body.put(MAX_COMPLETION_TOKENS_KEY, MAX_COMPLETION_TOKENS);
		body.putObject("response_format").put("type", RESPONSE_FORMAT_JSON);
		ArrayNode messages = body.putArray("messages");
		messages.addObject().put("role", ROLE_USER).put("content", buildPrompt(brief));
		return body.toString();
	}

	/**
	 * Writes the whole prompt: the brief, the fixed-box rule, one line per field of the picked
	 * modules with its budget, and the no-placeholder closing rule.
	 *
	 * @param brief the brief and the modules to draft
	 * @return the prompt text
	 */
	String buildPrompt(OnePagerBrief brief) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are drafting the copy for a single-page vertical one-pager (an AI Digital sales ")
				.append("collateral piece) for the \"").append(brief.category()).append("\" category, prepared for ")
				.append(brief.clientName()).append(".\n\n");
		prompt.append("BRIEF (this is your ONLY source of facts, figures, product names, and claims — ")
				.append("never invent, embellish, or fact-check beyond it; you may only tighten wording for ")
				.append("length and clarity while preserving the meaning):\n").append(brief.guidanceNotes())
				.append("\n\n");
		prompt.append("Every field below is going into a FIXED-SIZE box in a pre-built layout — it will NOT ")
				.append("resize or shrink to fit. Stay under the character limit given for each field; write ")
				.append("tight, concrete copy rather than padding to the limit.\n\n");
		prompt.append("Return ONLY a single minified JSON object (no markdown fences, no commentary) with ")
				.append("exactly these keys:\n");
		appendHeaderFields(prompt);
		if (brief.includes(OnePagerModuleKey.TRACKER)) {
			appendTrackerFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.SOLUTIONS)) {
			appendSolutionFields(prompt, brief.includes(OnePagerModuleKey.SOLUTIONS_VISUAL));
		}
		if (brief.includes(OnePagerModuleKey.KPIS)) {
			appendKpiFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.IMAGE_TEXT)) {
			appendImageTextFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.KEY_TAKEAWAY)) {
			appendField(prompt, OnePagerDraftParserImpl.KEY_TAKEAWAY,
					"one callout sentence summarizing the key takeaway", OnePagerFieldBudget.KEY_TAKEAWAY);
		}
		if (brief.includes(OnePagerModuleKey.IMPACT_STATS)) {
			appendImpactStatFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.CAPABILITY_CARDS)) {
			appendCapabilityCardFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.SPOTLIGHT_CARDS)) {
			appendSpotlightCardFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.INSIGHT_CARDS)) {
			appendInsightCardFields(prompt);
		}
		if (brief.includes(OnePagerModuleKey.MILESTONE_BANNER)) {
			appendMilestoneFields(prompt);
		}
		prompt.append("\nNEVER answer any field with a placeholder like \"N/A\", \"-\", \"TBD\", or \"data not ")
				.append("available\" — every field must have a real, specific value drawn from the brief. ")
				.append("Use confident, concise, non-generic language. Section headers should read in ")
				.append("uppercase. Do not add statistics, capabilities, partnerships, or claims that are not in ")
				.append("the brief.");
		return prompt.toString();
	}

	/**
	 * Appends one top-level field line with its budget.
	 *
	 * @param prompt      prompt being built
	 * @param key         JSON key
	 * @param description what the field holds
	 * @param budget      character budget
	 */
	void appendField(StringBuilder prompt, String key, String description, OnePagerFieldBudget budget) {
		prompt.append(FIELD).append(key).append(": ").append(description).append(MAX).append(budget.getMaxChars())
				.append(CHARS);
	}

	/**
	 * Appends one nested field line (inside an array object) with its budget.
	 *
	 * @param prompt      prompt being built
	 * @param key         JSON key
	 * @param description what the field holds
	 * @param budget      character budget
	 */
	void appendSubField(StringBuilder prompt, String key, String description, OnePagerFieldBudget budget) {
		prompt.append(SUBFIELD).append(key).append(": ").append(description).append(MAX)
				.append(budget.getMaxChars()).append(CHARS);
	}

	/**
	 * Appends the opening line of an array-of-objects field.
	 *
	 * @param prompt prompt being built
	 * @param key    JSON key
	 * @param count  exact number of objects expected
	 * @param note   optional clause after the count, or empty
	 */
	void appendArrayField(StringBuilder prompt, String key, int count, String note) {
		prompt.append(FIELD).append(key).append(": an array of exactly ").append(count).append(" objects")
				.append(note).append(", each with:\n");
	}

	/**
	 * Appends the header fields (always drafted).
	 *
	 * @param prompt prompt being built
	 */
	void appendHeaderFields(StringBuilder prompt) {
		appendField(prompt, OnePagerDraftParserImpl.TOPIC_PILL, "header pill label", OnePagerFieldBudget.TOPIC_PILL);
		prompt.append(FIELD).append(OnePagerDraftParserImpl.MAIN_TITLE)
				.append(": an array of 1-3 short strings, one per line, forming the page's headline (each line max ")
				.append(OnePagerFieldBudget.TITLE_LINE.getMaxChars()).append(CHARS);
		appendField(prompt, OnePagerDraftParserImpl.SUPPORTING_COPY, "one supporting sentence",
				OnePagerFieldBudget.SUPPORTING_COPY);
	}

	/**
	 * Appends the process-tracker fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendTrackerFields(StringBuilder prompt) {
		appendField(prompt, OnePagerDraftParserImpl.PROCESS_SECTION_LABEL,
				"a short label above the process tracker, e.g. \"HOW IT WORKS\"", OnePagerFieldBudget.SECTION_LABEL);
		prompt.append(FIELD).append(OnePagerDraftParserImpl.PROCESS_STEPS).append(": an array of exactly ")
				.append(TRACKER_STEP_COUNT).append(" short step names, in order (each max ")
				.append(OnePagerFieldBudget.TRACKER_STEP.getMaxChars()).append(" characters, 1-2 words)\n");
	}

	/**
	 * Appends the core-solution fields, plus the companion visual's label when picked.
	 *
	 * @param prompt     prompt being built
	 * @param withVisual whether the companion visual was picked
	 */
	void appendSolutionFields(StringBuilder prompt, boolean withVisual) {
		appendField(prompt, OnePagerDraftParserImpl.SOLUTIONS_SECTION_LABEL, "a short section title",
				OnePagerFieldBudget.SECTION_LABEL);
		appendArrayField(prompt, OnePagerDraftParserImpl.SOLUTIONS, SOLUTION_CARD_COUNT, "");
		appendSubField(prompt, OnePagerDraftParserImpl.TITLE, "short solution name",
				OnePagerFieldBudget.SOLUTION_TITLE);
		appendSubField(prompt, OnePagerDraftParserImpl.VALUE_LINE, "one short bold value-oriented subhead",
				OnePagerFieldBudget.SOLUTION_VALUE_LINE);
		appendSubField(prompt, OnePagerDraftParserImpl.BODY, "explanatory copy", OnePagerFieldBudget.SOLUTION_BODY);
		if (withVisual) {
			appendField(prompt, OnePagerDraftParserImpl.SOLUTIONS_VISUAL_LABEL,
					"a short section title for the companion visual beside the solution cards",
					OnePagerFieldBudget.SOLUTIONS_VISUAL_LABEL);
		}
	}

	/**
	 * Appends the performance-signal KPI fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendKpiFields(StringBuilder prompt) {
		appendField(prompt, OnePagerDraftParserImpl.KPI_SECTION_LABEL, "a short section title",
				OnePagerFieldBudget.SECTION_LABEL);
		appendArrayField(prompt, OnePagerDraftParserImpl.KPIS, KPI_TILE_COUNT, "");
		prompt.append(SUBFIELD).append(OnePagerDraftParserImpl.VALUE)
				.append(": the headline value from the brief — a short number/percent/multiplier only (\"30%\", ")
				.append("\"2X\"), max ").append(OnePagerFieldBudget.KPI_VALUE.getMaxChars())
				.append(" characters, NEVER a phrase or sentence. This box never shrinks or wraps, so if the ")
				.append("brief's exact figure is longer than that (e.g. \"4,400\" or \"31,400+\"), round and ")
				.append("abbreviate it to fit (\"4.4K\", \"31K\") rather than giving the full number — never ")
				.append("truncate a number mid-digit\n");
		appendSubField(prompt, OnePagerDraftParserImpl.LABEL, "a short descriptor underneath, in caps",
				OnePagerFieldBudget.KPI_LABEL);
		appendField(prompt, OnePagerDraftParserImpl.KPI_DISCLAIMER,
				"the disclaimer sentence for the KPI values, taken from the brief", OnePagerFieldBudget.KPI_DISCLAIMER);
	}

	/**
	 * Appends the image-and-text fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendImageTextFields(StringBuilder prompt) {
		appendField(prompt, OnePagerDraftParserImpl.IMAGE_TEXT_SECTION_LABEL, "a short section title",
				OnePagerFieldBudget.IMAGE_TEXT_LABEL);
		appendField(prompt, OnePagerDraftParserImpl.IMAGE_TEXT_BODY, "a short paragraph",
				OnePagerFieldBudget.IMAGE_TEXT_BODY);
	}

	/**
	 * Appends the impact-stat fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendImpactStatFields(StringBuilder prompt) {
		appendArrayField(prompt, OnePagerDraftParserImpl.IMPACT_STATS, TRIPLE_COUNT, "");
		appendSubField(prompt, OnePagerDraftParserImpl.VALUE,
				"a short number/percent/multiplier from the brief, NEVER a phrase or sentence",
				OnePagerFieldBudget.STAT_VALUE);
		appendSubField(prompt, OnePagerDraftParserImpl.DESCRIPTION, "a short descriptor underneath",
				OnePagerFieldBudget.STAT_DESCRIPTION);
	}

	/**
	 * Appends the capability-card fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendCapabilityCardFields(StringBuilder prompt) {
		appendArrayField(prompt, OnePagerDraftParserImpl.CAPABILITY_CARDS, TRIPLE_COUNT, "");
		appendSubField(prompt, OnePagerDraftParserImpl.TITLE, "short card heading",
				OnePagerFieldBudget.CAPABILITY_TITLE);
		appendSubField(prompt, OnePagerDraftParserImpl.DESCRIPTION, "short intro sentence",
				OnePagerFieldBudget.CAPABILITY_DESCRIPTION);
		appendSubField(prompt, OnePagerDraftParserImpl.BULLETS,
				"an array of exactly " + TRIPLE_COUNT + " short checklist lines (each max",
				OnePagerFieldBudget.CAPABILITY_BULLET);
		appendSubField(prompt, OnePagerDraftParserImpl.FOOTER, "a small subtext line",
				OnePagerFieldBudget.CAPABILITY_FOOTER);
	}

	/**
	 * Appends the spotlight-card fields, including the KPI chips and stage names that only the
	 * second and third card carry.
	 *
	 * @param prompt prompt being built
	 */
	void appendSpotlightCardFields(StringBuilder prompt) {
		appendArrayField(prompt, OnePagerDraftParserImpl.SPOTLIGHT_CARDS, TRIPLE_COUNT,
				" — index 0, 1, and 2 are 3 DIFFERENT card designs (an image card, a KPI-chip-cluster card, and a "
						+ "stage-banner card), not 3 repeats of the same card. Every object has");
		appendSubField(prompt, OnePagerDraftParserImpl.TITLE, "short card heading",
				OnePagerFieldBudget.SPOTLIGHT_TITLE);
		appendSubField(prompt, OnePagerDraftParserImpl.DESCRIPTION, "short intro sentence",
				OnePagerFieldBudget.SPOTLIGHT_DESCRIPTION);
		appendSubField(prompt, OnePagerDraftParserImpl.BULLETS,
				"an array of exactly " + TRIPLE_COUNT + " short checklist lines (each max",
				OnePagerFieldBudget.SPOTLIGHT_BULLET);
		prompt.append(SUBFIELD).append(OnePagerDraftParserImpl.KPI_CHIPS).append(": ONLY for index 1 — an array of ")
				.append("exactly ").append(SPOTLIGHT_CHIP_COUNT).append(" objects, each with {value, label} in the ")
				.append("same style as the kpis field (value: a short number/percent/multiplier, max ")
				.append(OnePagerFieldBudget.KPI_VALUE.getMaxChars()).append(" characters; label: a short descriptor ")
				.append("in caps, max ").append(OnePagerFieldBudget.KPI_LABEL.getMaxChars())
				.append(" characters). Empty array ([]) for index 0 and 2.\n");
		prompt.append(SUBFIELD).append(OnePagerDraftParserImpl.STAGE_NAMES).append(": ONLY for index 2 — an array ")
				.append("of exactly ").append(TRIPLE_COUNT).append(" short stage/step names (e.g. \"DISCOVERY\", ")
				.append("\"LAUNCH\"), each max ").append(OnePagerFieldBudget.SPOTLIGHT_STAGE_NAME.getMaxChars())
				.append(" characters. Empty array ([]) for index 0 and 1.\n");
	}

	/**
	 * Appends the insight-card fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendInsightCardFields(StringBuilder prompt) {
		appendArrayField(prompt, OnePagerDraftParserImpl.INSIGHT_CARDS, TRIPLE_COUNT, "");
		appendSubField(prompt, OnePagerDraftParserImpl.TITLE, "short card title", OnePagerFieldBudget.INSIGHT_TITLE);
		appendSubField(prompt, OnePagerDraftParserImpl.INSIGHT, "a short supporting sentence",
				OnePagerFieldBudget.INSIGHT_TEXT);
	}

	/**
	 * Appends the milestone-banner fields.
	 *
	 * @param prompt prompt being built
	 */
	void appendMilestoneFields(StringBuilder prompt) {
		appendField(prompt, OnePagerDraftParserImpl.MILESTONE_HEADLINE, "a short banner headline",
				OnePagerFieldBudget.MILESTONE_HEADLINE);
		appendArrayField(prompt, OnePagerDraftParserImpl.MILESTONES, TRIPLE_COUNT, "");
		appendSubField(prompt, OnePagerDraftParserImpl.TITLE, "short step title", OnePagerFieldBudget.MILESTONE_TITLE);
		appendSubField(prompt, OnePagerDraftParserImpl.SUBTEXT, "a short supporting subtext",
				OnePagerFieldBudget.MILESTONE_SUBTEXT);
	}
}
