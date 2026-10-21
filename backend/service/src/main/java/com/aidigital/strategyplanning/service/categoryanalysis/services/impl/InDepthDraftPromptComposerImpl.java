package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthDraftField;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.models.InDepthTokenSpec;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthDraftPromptComposer;
import com.aidigital.strategyplanning.service.categoryanalysis.services.InDepthTemplateTokenRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link InDepthDraftPromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class InDepthDraftPromptComposerImpl implements InDepthDraftPromptComposer {

	/**
	 * Output ceiling per call. Generous relative to what any single pass needs (a full-deck
	 * knowledge draft or one topic's fact-check are both modest), kept as a safety margin.
	 */
	public static final int MAX_OUTPUT_TOKENS = 16000;

	private static final String MAX_COMPLETION_TOKENS_KEY = "max_completion_tokens";
	private static final String WEB_SEARCH_TOOL = "web_search";
	private static final String JSON_OBJECT_FORMAT = "json_object";
	private static final String ROLE_USER = "user";
	private static final int ABBREVIATION_LIMIT = 60;
	private static final String ABBREVIATION_SUFFIX = "...";

	private final CategoryAnalysisProperties properties;
	private final ObjectMapper objectMapper;
	private final InDepthTemplateTokenRegistry tokenRegistry;

	@Override
	public String buildAllFieldsRequestBody(String category, String clientName, String guidanceNotes,
	                                        String clientWebsite, String websiteText,
	                                        List<InDepthTokenSpec> tokens) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior media strategist at a digital marketing agency, drafting the checked ")
				.append("slides of an In-Depth Category Analysis deck from your own knowledge. A separate ")
				.append("fact-checking pass will verify statistics against live web sources afterward, so give ")
				.append("your best current, plausible estimate for every stat rather than leaving anything ")
				.append("blank or vague.\n\n")
				.append("Category: ").append(category).append('\n')
				.append("Client: ").append(clientName).append('\n')
				.append("Client website: ").append(clientWebsite).append('\n');
		if (StringUtils.hasText(websiteText)) {
			prompt.append("Website content excerpt (use it to keep the wording relevant to what the client ")
					.append("actually sells):\n\"\"\"\n").append(websiteText).append("\n\"\"\"\n");
		} else {
			prompt.append("(The client website could not be retrieved; write generically for the category ")
					.append("and client name.)\n");
		}
		if (StringUtils.hasText(guidanceNotes)) {
			prompt.append("Focus notes from the strategist: ").append(guidanceNotes).append('\n');
		}
		appendNarrativeArc(prompt);
		prompt.append("\nRules:\n")
				.append("- Stats must be plausible and current, drawn from research published in 2025 or ")
				.append("later, with a value like \"54%\" or \"11.9M\" and a label completing the sentence.\n")
				.append("- Sources fields start with \"Source: \" and cite plausible real research bodies or ")
				.append("publications from 2025 or later — they will be checked in the next pass.\n");
		appendFieldRules(prompt);
		prompt.append("- Keep each text under 45 words; bullets and driver/highlight texts 20-35 words.\n")
				.append("- Content must be specific to the category and client, ready for an internal review.\n")
				.append("- Fields belonging to more than one slide (shared by layout variants of the same ")
				.append("topic) are drafted once and reused verbatim across those slides.\n")
				.append("- NEVER answer with a placeholder like \"N/A\", \"-\", \"unknown\", \"TBD\", or \"data ")
				.append("not available\" for any field. If you are unsure of an exact figure, give your single ")
				.append("best specific, plausible estimate instead — every field must contain real, ")
				.append("presentation-ready content.\n\n")
				.append("Return ONE JSON object with exactly these keys:\n")
				.append("\"fields\": an object with ALL of the following string keys:\n");
		appendTokenList(prompt, tokens);
		prompt.append("Return only the JSON object.");

		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", ROLE_USER);
		message.put("content", prompt.toString());
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", JSON_OBJECT_FORMAT);
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put(MAX_COMPLETION_TOKENS_KEY, MAX_OUTPUT_TOKENS);
		return payload.toString();
	}

	@Override
	public String buildVerifyRequestBody(String category, List<InDepthTokenSpec> group,
	                                     Map<String, InDepthDraftField> draftedByKey) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are fact-checking statistics drafted for a media strategy deck about the category ")
				.append("below. Use the web_search tool to verify each drafted value against current, real ")
				.append("sources.\n\n")
				.append("Category: ").append(category).append("\n\n")
				.append("Drafted values to verify:\n");
		for (InDepthTokenSpec spec : group) {
			InDepthDraftField field = draftedByKey.get(spec.key());
			String current = field != null ? field.value() : "";
			prompt.append("  - ").append(spec.key()).append(" (").append(spec.label()).append("): \"")
					.append(current).append("\" — max ").append(tokenRegistry.maxCharsFor(spec))
					.append(" characters\n");
		}
		prompt.append("\nFor each key: if the drafted value is accurate, or a reasonable estimate you cannot ")
				.append("disprove, return it unchanged. If it is inaccurate, or you find a better current ")
				.append("figure or citation, return the corrected value instead.\n")
				.append("Only rely on research published in 2025 or later: if a drafted stat or source comes ")
				.append("from an older publication, replace it with the newest 2025-or-later figure and source ")
				.append("you can find. (Chart data points may describe earlier years, but their source must ")
				.append("still be 2025 or newer.)\n")
				.append("NEVER reply with a placeholder like \"N/A\", \"-\", \"unknown\", or \"unable to ")
				.append("verify\" for any key — if your search is inconclusive, return the ORIGINAL drafted ")
				.append("value unchanged rather than a non-answer.\n")
				.append("Return ONE JSON object with exactly the keys listed above, each mapped to its ")
				.append("(possibly corrected) string value — include every key even when unchanged. No ")
				.append("markdown code fences, no commentary before or after it.");
		return buildWebSearchPayload(prompt.toString());
	}

	@Override
	public String buildAlignmentRequestBody(String category, String clientName, String clientWebsite,
	                                        String websiteText) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a media strategist checking whether a proposed category analysis topic fits a ")
				.append("client's actual business.\n\n")
				.append("Category being analysed: ").append(category).append('\n')
				.append("Client: ").append(clientName).append('\n')
				.append("Client website: ").append(clientWebsite).append('\n');
		if (StringUtils.hasText(websiteText)) {
			prompt.append("Website content excerpt:\n\"\"\"\n").append(websiteText).append("\n\"\"\"\n");
		} else {
			prompt.append("(The client website could not be retrieved; infer their likely business focus ")
					.append("from the client name and category.)\n");
		}
		prompt.append("\nReturn a JSON object with exactly these keys:\n")
				.append("\"clientBusinessFocus\": one short sentence summarising the client's core business, ")
				.append("based on the website excerpt above.\n")
				.append("\"alignmentMatches\": boolean — true if the Category clearly fits that business focus, ")
				.append("false if the topic seems unrelated to what the client actually does.\n")
				.append("\"alignmentMessage\": one sentence confirming the deck aligns with the client's business ")
				.append("focus, or warning that the chosen category may not match what the client does.\n");
		return buildJsonModePayload(prompt.toString());
	}

	@Override
	public String buildSlideRequestBody(String category, String clientName, String guidanceNotes,
	                                    int slideNumber, String slideNote,
	                                    List<InDepthTokenSpec> slideTokens,
	                                    List<InDepthFieldValue> currentFields) {
		Map<String, String> currentByKey = new LinkedHashMap<>();
		if (currentFields != null) {
			for (InDepthFieldValue field : currentFields) {
				if (field != null && field.key() != null) {
					currentByKey.put(field.key(), field.value() == null ? "" : field.value());
				}
			}
		}
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior media strategist at a digital marketing agency. You are revising an ")
				.append("existing In-Depth Category Analysis deck. Redraft ONLY the fields of slide ")
				.append(slideNumber).append("; keep the deck coherent with the other checked slides, which ")
				.append("are not changing. Use the web_search tool to ground any stats or sources in current, ")
				.append("real research — do not invent them.\n\n")
				.append("Category: ").append(category).append('\n')
				.append("Client: ").append(clientName).append('\n');
		if (StringUtils.hasText(guidanceNotes)) {
			prompt.append("Original brief focus notes: ").append(guidanceNotes).append('\n');
		}
		if (StringUtils.hasText(slideNote)) {
			prompt.append("Guidance for THIS redraft of slide ").append(slideNumber).append(": ")
					.append(slideNote).append('\n');
		}
		prompt.append("\nKeep this redraft aligned with the deck's narrative arc (see below): if this slide ")
				.append("covers market/competitive pressure, consumer-behavior gaps, funnel/journey stages, or ")
				.append("paired insight-and-implication content, frame it to build toward programmatic media as ")
				.append("the strategic answer, consistent with the other checked slides shown below.\n");
		if (!currentByKey.isEmpty()) {
			prompt.append("\nCurrent content of the checked deck (for context — do not change fields outside ")
					.append("slide ").append(slideNumber).append("):\n");
			currentByKey.forEach((key, value) ->
					prompt.append("  - ").append(key).append(": ").append(abbreviate(value)).append('\n'));
		}
		prompt.append("\nRules:\n")
				.append("- Stats must be real and current — search for the latest available figures, from ")
				.append("research published in 2025 or later — with a value like \"54%\" or \"11.9M\" and a ")
				.append("label completing the sentence.\n")
				.append("- Sources fields start with \"Source: \" and cite the real research bodies (published ")
				.append("2025 or later) your search actually found.\n");
		appendFieldRules(prompt);
		prompt.append("- Keep each text under 45 words; bullets and driver/highlight texts 20-35 words.\n")
				.append("- Produce genuinely fresh wording — do not simply repeat the current values.\n")
				.append("- NEVER answer with a placeholder like \"N/A\", \"-\", \"unknown\", or \"data not ")
				.append("available\" for any field — give your single best specific, plausible estimate instead.\n")
				.append("- You MUST include every single key listed below in your final answer — never omit, ")
				.append("truncate, or stop early. If you are running low on space, shorten individual values ")
				.append("rather than leaving any key out.\n\n")
				.append("Return ONE JSON object with exactly this shape: {\"fields\": {..}} where \"fields\" ")
				.append("is an object with EXACTLY these string keys (slide ").append(slideNumber)
				.append(" only):\n");
		appendTokenList(prompt, slideTokens);
		prompt.append("Return only the JSON object — no markdown code fences, no commentary before or after it.");
		return buildWebSearchPayload(prompt.toString());
	}

	/**
	 * Appends the standing narrative-arc instruction every Category Analysis deck follows: the
	 * checked slides build one deliberate case for programmatic media rather than reading as
	 * disconnected facts. Framed by slide type, not slide number, since the checked set varies.
	 *
	 * @param prompt prompt being built
	 */
	void appendNarrativeArc(StringBuilder prompt) {
		prompt.append("\nNarrative arc — weave everything above into one deliberate argument across the checked ")
				.append("slides, not a set of disconnected facts, using whichever of these slide types are ")
				.append("checked:\n")
				.append("- Market/competitive/industry-pressure content (market size, competitive intensity, ")
				.append("spend trends): frame it to establish urgency — why waiting or relying on the status ")
				.append("quo carries real cost.\n")
				.append("- Consumer-behavior, demand-driver, or discovery-research content: identify a specific ")
				.append("gap in how today's audience actually discovers or decides in this category, versus how ")
				.append("it is typically marketed (e.g. search-only, broadcast-only, one-size-fits-all) — this ")
				.append("gap is the problem the rest of the deck solves.\n")
				.append("- Any recommendation, solution, or channel-opportunity content: explicitly position ")
				.append("programmatic media — CTV/streaming video, streaming/digital audio, addressable and ")
				.append("geofenced targeting, cross-device retargeting, dynamic creative — as the strategic ")
				.append("answer to that specific gap. Name real, current programmatic tactics, not a vague ")
				.append("\"digital marketing\" gesture.\n")
				.append("- Any funnel, journey, or stage-based content: tie each stage explicitly to a specific ")
				.append("programmatic tactic that operates at that stage — don't just describe audience behavior ")
				.append("in the abstract. When a stage has multiple bullet fields, at least one bullet per stage ")
				.append("must name the specific programmatic tactic or channel (e.g. CTV, streaming/digital ")
				.append("audio, geofencing, cross-device retargeting, dynamic creative, click-to-call) that reaches ")
				.append("or moves the audience at that exact stage — not merely what the audience does or needs, ")
				.append("but how programmatic media meets them there. Where a stage has its own drafted title ")
				.append("field, feel free to title it in language that fits this deck's narrative rather than a ")
				.append("generic stage name.\n")
				.append("- Any paired insight-and-so-what content (e.g. highlights + implications, drivers + ")
				.append("recommendations): write each pair as one specific insight followed by the specific ")
				.append("programmatic tactic it justifies, not two independently written lists.\n")
				.append("This is a narrative throughline, not license to invent facts: every stat and source ")
				.append("must still be real, current, and specific to the category — tell the story through how ")
				.append("you frame and connect real findings, never by fabricating ones that fit the plot.\n");
	}

	/**
	 * Appends the field-level drafting rules shared by the full draft and single-slide redrafts:
	 * casing, and how the paired or structured fields of specific slides relate to each other.
	 *
	 * @param prompt prompt being built
	 */
	void appendFieldRules(StringBuilder prompt) {
		prompt.append("- Any field whose example is ALL CAPS (headlines, section titles, highlight and ")
				.append("implication titles, channel names, funnel stage labels) is written in ALL CAPS; ")
				.append("title-case for driver and stage titles.\n")
				.append("- Highlights & implications (hi_ fields) are four rows. Row N's implication ")
				.append("(hi_implication_N_title + hi_implication_N_text) must be the direct strategic ")
				.append("consequence of row N's highlight (hi_highlight_N_title + hi_highlight_N_text): the ")
				.append("highlight states the market insight, and the implication on the same row answers ")
				.append("\"so what for the client's media\" by naming the programmatic tactic that insight ")
				.append("justifies. Never pair an implication with a different row's highlight.\n")
				.append("- Channel opportunity (channel_ fields) covers two programmatic channels. channel_N_name ")
				.append("is a short channel name (e.g. CTV, AUDIO, DISPLAY, DOOH, SOCIAL, max 12 characters); ")
				.append("channel_N_stat_value + channel_N_stat_label is a real stat about the target audience's ")
				.append("use of THAT channel; channel_N_insight says what that channel does for the client.\n")
				.append("- Donut chart (donut_ fields): four segments of ONE whole for the category (e.g. ")
				.append("trip types, purchase channels, age groups). donut_N_label is a short segment name ")
				.append("(max 24 characters); donut_N_value is that segment's share as a percentage like ")
				.append("\"38%\", and the four shares must add up to 100%.\n")
				.append("- Category stat chart (stat_N_value and stat_chart_bar_N_value) values are always ")
				.append("percentages like \"45%\".\n")
				.append("- Funnel stage labels (feed_stage_N_label) are 1-3 word purchase-funnel stage names, ")
				.append("in order awareness → consideration → purchase → trade-up → loyalty, phrased for this ")
				.append("category (e.g. \"SEASONAL AWARENESS\", \"CONSIDERATION\").\n");
	}

	/**
	 * Appends the "key (slide N, label, e.g. sample, max N characters)" listing shared by every
	 * prompt that lists token specs, plus the warning that samples show format only.
	 *
	 * @param prompt prompt being built
	 * @param tokens token specs to list
	 */
	void appendTokenList(StringBuilder prompt, List<InDepthTokenSpec> tokens) {
		for (InDepthTokenSpec spec : tokens) {
			prompt.append("  - ").append(spec.key())
					.append(" (slide").append(spec.slideNumbers().size() > 1 ? "s " : " ")
					.append(spec.slideNumbers()).append(", ").append(spec.label())
					.append(", e.g. \"").append(abbreviate(spec.sampleText())).append("\"")
					.append(", max ").append(tokenRegistry.maxCharsFor(spec)).append(" characters)\n");
		}
		prompt.append("The \"e.g.\" example after each field above shows its FORMAT ONLY (length, tone, ")
				.append("structure). Every example is from unrelated template decks (Medicare Advantage, ")
				.append("regional travel, pet food, tablets, etc.) — never reuse their wording, numbers, ")
				.append("or subject matter. This applies especially to short group/category labels (e.g. a ")
				.append("field whose example is a 2-3 word all-caps tag like \"TRIP TYPES\"): write a fresh ")
				.append("label naming what YOUR tags/items actually are for the category above, not the example's.\n");
	}

	/**
	 * Wraps a prompt into a Responses API body with the {@code web_search} tool enabled. The
	 * output format stays plain text because the tool cannot be combined with the API's JSON
	 * output modes, so the JSON contract is enforced through the prompt and tolerated by the
	 * parser instead.
	 *
	 * @param promptText fully built prompt text
	 * @return serialized JSON request body
	 */
	String buildWebSearchPayload(String promptText) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.put("input", promptText);
		payload.putArray("tools").addObject().put("type", WEB_SEARCH_TOOL);
		payload.put("max_output_tokens", MAX_OUTPUT_TOKENS);
		return payload.toString();
	}

	/**
	 * Wraps a prompt into a Responses API body with the API's JSON-object output mode and no
	 * tools, for calls that do not need {@code web_search}.
	 *
	 * @param promptText fully built prompt text
	 * @return serialized JSON request body
	 */
	String buildJsonModePayload(String promptText) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.put("input", promptText);
		payload.putObject("text").putObject("format").put("type", JSON_OBJECT_FORMAT);
		return payload.toString();
	}

	/**
	 * Shortens a sample or current value for inclusion in the prompt.
	 *
	 * @param text text to shorten
	 * @return text truncated to 60 characters
	 */
	String abbreviate(String text) {
		return text.length() <= ABBREVIATION_LIMIT
				? text
				: text.substring(0, ABBREVIATION_LIMIT - ABBREVIATION_SUFFIX.length()) + ABBREVIATION_SUFFIX;
	}
}
