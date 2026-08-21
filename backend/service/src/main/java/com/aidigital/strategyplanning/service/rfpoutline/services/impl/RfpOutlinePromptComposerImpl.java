package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.rfpoutline.config.RfpOutlineProperties;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlinePromptComposer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Default implementation of {@link RfpOutlinePromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class RfpOutlinePromptComposerImpl implements RfpOutlinePromptComposer {

	private final RfpOutlineProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String buildRequestBody(List<SourceDocument> documents, String notes) {
		StringBuilder prompt = new StringBuilder();
		appendStrategistFraming(prompt);
		appendSourceMaterial(prompt, documents, notes);
		appendDraftingInstructions(prompt);
		return buildChatPayload(prompt.toString());
	}

	/**
	 * Appends the opening framing that tells the model what document it is drafting and
	 * which parts of an RFP to ignore.
	 *
	 * @param prompt prompt buffer to append to
	 */
	void appendStrategistFraming(StringBuilder prompt) {
		prompt.append("You are a senior strategist at AI Digital, a digital media agency, preparing ")
				.append("an INTERNAL Strategy & Planning alignment document for how to respond to a ")
				.append("client's RFP — not client-facing copy, and not a finished pitch deck. Its job ")
				.append("is to align Strategy and Planning on the approach before deck-build begins. ")
				.append("Draft strictly from the source documents below (and the supplementary notes, ")
				.append("if present) — never invent clients, figures, or claims.\n\n")
				.append("IMPORTANT: RFP documents are often mostly procurement/legal/compliance ")
				.append("boilerplate (submission logistics, indemnification, evaluation criteria, ")
				.append("legal terms, contract provisions). Ignore that boilerplate entirely and ")
				.append("extract only what is strategically relevant: the client's business objectives, ")
				.append("budget, target audience, timeline, key challenges, selection/qualification ")
				.append("criteria, and measurement/KPI expectations.\n\n");
	}

	/**
	 * Appends the extracted source-document text and the strategist's optional notes.
	 *
	 * @param prompt    prompt buffer to append to
	 * @param documents parsed source documents (filename + extracted text)
	 * @param notes     optional free-text supplementary context, may be null or blank
	 */
	void appendSourceMaterial(StringBuilder prompt, List<SourceDocument> documents, String notes) {
		for (SourceDocument document : documents) {
			prompt.append("=== Document: ").append(document.fileName()).append(" ===\n")
					.append(document.text()).append("\n\n");
		}
		if (StringUtils.hasText(notes)) {
			prompt.append("=== Supplementary notes from the strategist ===\n")
					.append(notes).append("\n\n");
		}
	}

	/**
	 * Appends the drafting instructions and the required JSON response shape.
	 *
	 * @param prompt prompt buffer to append to
	 */
	void appendDraftingInstructions(StringBuilder prompt) {
		prompt.append("Draft two things:\n\n")
				.append("1. A \"Core Story\" POV — challenge, opportunity, solution, and outcome. Each ")
				.append("should be dense and specific — cite the actual figures, constraints, gates, ")
				.append("and language found in the source documents (budget structure, audience count, ")
				.append("geography, deadlines, qualification/selection criteria), not generic statements. ")
				.append("1-4 sentences each is fine if the specificity earns the length; never pad with ")
				.append("filler just to sound complete.\n\n")
				.append("2. An internal response-outline document (the \"deckOutline\" field). This is a ")
				.append("comprehensive internal PLANNING GUIDE, not a minimalist slide outline — think ")
				.append("several hundred to a thousand+ words per major section, not two or three lines. ")
				.append("Every distinct audience, goal, channel, or geography named in the source documents ")
				.append("should get its own breakout (its own bullet, row, or named sub-heading) rather ")
				.append("than being compressed into one representative example — if the RFP lists nine ")
				.append("audience segments or four campaign goals, draft all nine and all four, not a ")
				.append("sample. Within each major section, use 2-5 NAMED SUB-HEADINGS to organize distinct ")
				.append("chunks of content (e.g. a framework name/premise, a goal→solution table, a ")
				.append("campaign-parameters table, a layered data/targeting architecture, a geography ")
				.append("breakdown, named audience segments, a KPI table, a reporting/dashboard note, an ")
				.append("optimization-methodology list) rather than one flat bullet dump per section — real ")
				.append("examples of this internal-document style break every major section into several ")
				.append("clearly labeled parts. Err heavily toward MORE detail, MORE sub-headings, and MORE ")
				.append("rows per table; \"don't pad with filler\" means don't invent facts or write vague ")
				.append("boilerplate — it does NOT mean keep sections short. A section with only 2-3 lines ")
				.append("of content is too thin unless the source material genuinely has nothing more to ")
				.append("extract for it. Structure it as PLAIN TEXT (no markdown ** or ## syntax — this ")
				.append("text is inserted verbatim into a Google Doc). Do not repeat the Core Story fields ")
				.append("above inside deckOutline — it starts directly after them. Structure:\n\n")
				.append("A) A one-line \"FOR INTERNAL USE ONLY — NOT CLIENT-FACING\" banner, then a brief ")
				.append("metadata block (one line per field, or a couple of pipe-separated lines — whatever ")
				.append("reads cleanest) covering only fields the source documents actually support (omit ")
				.append("any not found — never fabricate): Client, Submitting Agency (if the client is ")
				.append("submitting through an agency/rep rather than direct), Due Date, Submission ")
				.append("Channel/Deliverables required, Total Budget, Flight/Timeline, and internal contacts ")
				.append("if named (submitter, account owner, etc.).\n\n")
				.append("B) Numbered sections following this house narrative arc, adapting which sections ")
				.append("appear to what this specific RFP actually calls for (skip a section entirely if ")
				.append("it doesn't apply — but every section you DO include should be fully fleshed out ")
				.append("with the named sub-headings described below, not a short summary):\n")
				.append("  1. Title & Agenda — a working title (plus an alternative), and a full proposed ")
				.append("agenda (one line per deck section it will contain).\n")
				.append("  2. Executive Summary / What We Heard — a named sub-heading restating EVERY ")
				.append("distinct goal, requirement, and constraint from the brief back in the client's own ")
				.append("language as its own bullet, proving it was read closely; a second named ")
				.append("sub-heading giving AI Digital's top-line strategic response; a third, if the brief ")
				.append("supports it, ranking the client's key priorities.\n")
				.append("  3. Credibility / Why AI Digital — every distinct credibility argument tailored to ")
				.append("THIS brief as its own named sub-point (data/technology differentiators, category ")
				.append("experience, named competitor/incumbent comparison if known, measurement/reporting ")
				.append("differentiation), plus every gap in proof points worth flagging (missing case ")
				.append("studies, unknown incumbent, etc.).\n")
				.append("  4. Strategic Approach — name the framework as its own line, a full paragraph on ")
				.append("its organizing premise, THEN a \"Goal → Solution\" table (2-3 columns as fits: ")
				.append("client goal, our solution, and an optional differentiator column) with one row per ")
				.append("distinct objective/goal found in the brief — not just one or two rows — THEN a ")
				.append("\"Campaign Parameters\" table or bullet list covering every confirmed parameter ")
				.append("(budget, flight, primary tactic, added value, brands/products in scope, data ")
				.append("foundation, measurement) with the actual figures cited.\n")
				.append("  5. Audience Strategy — a named sub-heading per distinct audience/segment in the ")
				.append("brief (Who / Motivation / Strategic approach for each), not a merged summary — if ")
				.append("segments share overlapping geography or traits, still list each one and call out ")
				.append("the overlap as its own strategic observation; if the brief supports it, add a ")
				.append("named \"Data/Targeting Architecture\" sub-heading (layered data sourcing, e.g. ")
				.append("verified/proprietary data → 3rd-party enrichment → behavioral/device layer) and a ")
				.append("named \"Geography\" sub-heading with the actual geography list.\n")
				.append("  6. Channel Strategy / Media Plan — a named \"Primary Channel(s)\" sub-heading, a ")
				.append("funnel-stage table with one row per stage the client itself defines (using the ")
				.append("client's own stage names and metrics when given), named sub-headings for any ")
				.append("distinct strategic posture options the brief implies (e.g. reach-extension vs. ")
				.append("frequency-amplification, or channel-by-channel logic), a \"Flighting/Seasonality\" ")
				.append("sub-heading, and an \"Added Value\" sub-heading — all at a level that outlines logic ")
				.append("without prescribing Planning's tactical execution.\n")
				.append("  7. Measurement & Optimization — a KPI table (metric, definition, how it's ")
				.append("measured, why it matters — as many rows as the brief's stated success metrics ")
				.append("support), a named sub-heading for any AI Digital asset explicitly requested or ")
				.append("relevant (ELEVATE reporting/optimization platform, Brand Study, Audience Engine), a ")
				.append("\"Reporting & Dashboard\" sub-heading covering cadence/format requirements, and an ")
				.append("\"Optimization Methodology\" sub-heading (bullets on how performance will be ")
				.append("monitored and acted on).\n")
				.append("  8. Partnership Model — only if the RFP's process/relationship structure is worth ")
				.append("calling out, but when included use named sub-headings for the partnership ")
				.append("philosophy/positioning, team & workflow, any data-sharing dependencies from the ")
				.append("client, and the long-term account-growth angle.\n")
				.append("  9. Next Steps — client-facing next steps as bullets, PLUS a named \"Internal ")
				.append("Deadlines\" table or list (milestone → timing) covering every concrete internal ")
				.append("dependency needed to hit the deadline.\n")
				.append("  10. Appendix — a bulleted list of backup content that would strengthen the deck ")
				.append("without cluttering the core narrative (case studies, methodology deep-dives, ")
				.append("reference material) — a list of what to pull, not the content itself.\n\n")
				.append("For EACH section above, also weave in internal callouts wherever they are ")
				.append("genuinely earned by the source material (never generic filler just to fill a ")
				.append("slot) — use whichever mix of style reads best for that spot: a one-sentence \"Why ")
				.append("this section matters\" note; grouped bullets under a heading like \"Questions this ")
				.append("section must answer\" or \"Key open questions for this section\"; and/or short ")
				.append("inline bracket-tagged callouts placed at the exact point they're relevant, using ")
				.append("tags like [OPEN Q], [RISK], [ASSUMPTION], or [CONFIRM WITH CLIENT] — mix grouped ")
				.append("and inline callouts as the content naturally calls for, matching how a real ")
				.append("strategist annotates their own working document.\n\n")
				.append("C) Close with three lists, each entry concrete and grounded in the actual source ")
				.append("documents, with as many entries as the source material genuinely supports (omit a ")
				.append("list entirely only if nothing real applies — a rich RFP usually supports many ")
				.append("entries per list, not just one):\n")
				.append("  - OPEN QUESTIONS — every distinct piece of information Strategy/Planning must ")
				.append("resolve before deck-build begins\n")
				.append("  - RISKS / SENSITIVITIES — real risks visible from the RFP (timeline, ambiguity, ")
				.append("compliance, process constraints, competitive/incumbent dynamics)\n")
				.append("  - STRATEGIC BETS — forward-looking positioning choices being proposed and why ")
				.append("they should win; write each as a full paragraph when the point deserves the room ")
				.append("(the reasoning is often as important as the bet itself), not just a one-liner\n\n")
				.append("Rules:\n")
				.append("- Use ONLY facts present in the documents/notes; never invent numbers or claims.\n")
				.append("- Every callout, open question, risk, or bet must be real and specific — cut it ")
				.append("entirely rather than write a vague or filler one. This is about truthfulness, not ")
				.append("brevity — a real RFP should still yield many of these, in full detail.\n")
				.append("- Set any Core Story field to null when the input does not support a value.\n\n")
				.append("Return ONE JSON object with exactly these keys (string or null): ")
				.append("title, clientName, industry, challenge, opportunity, solution, outcome, ")
				.append("deckOutline. Return only the JSON object.");
	}

	/**
	 * Wraps a finished prompt in the OpenAI chat completions payload.
	 *
	 * @param prompt the complete user prompt
	 * @return serialized JSON request body
	 */
	String buildChatPayload(String prompt) {
		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt);
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_object");
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put("temperature", 0.3);
		// A fully detailed deckOutline (per-section why/questions/flags, full audience/goal
		// breakouts, tables, closing lists) runs long — give the model ample room so it isn't
		// cut short and forced to compress detail to fit.
		payload.put("max_tokens", 8000);
		return payload.toString();
	}
}
