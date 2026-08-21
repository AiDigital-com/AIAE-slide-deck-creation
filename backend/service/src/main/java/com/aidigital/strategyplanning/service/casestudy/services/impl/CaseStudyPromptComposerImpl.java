package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.CreateCaseStudyCommand;
import com.aidigital.strategyplanning.service.casestudy.services.CaseStudyPromptComposer;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of {@link CaseStudyPromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class CaseStudyPromptComposerImpl implements CaseStudyPromptComposer {

	private final CaseStudyProperties properties;
	private final ObjectMapper objectMapper;

	@Override
	public String buildRequestBody(List<SourceDocument> documents) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior strategist at a digital marketing agency. ")
				.append("Draft the fields of a client case study strictly from the source documents ")
				.append("below. Spreadsheet documents are rendered as rows with \" | \" between cells — ")
				.append("interpret them as tabular data and pull real figures from them.\n\n");
		for (SourceDocument document : documents) {
			prompt.append("=== Document: ").append(document.fileName()).append(" ===\n")
					.append(document.text()).append("\n\n");
		}
		prompt.append("Rules:\n")
				.append("- Use ONLY facts present in the documents; never invent clients, numbers, or quotes.\n")
				.append("- keyMetrics must quote concrete figures from the documents (especially spreadsheet ")
				.append("data) as a short comma-separated list, e.g. \"40% cost reduction, 2x pipeline\".\n")
				.append("- testimonial must be a verbatim or near-verbatim quote found in the documents; ")
				.append("null when none exists.\n")
				.append("- Keep challenge, solution, and results to 2-4 sentences each.\n")
				.append("- Set any field to null when the documents do not support a value.\n\n")
				.append("Return ONE JSON object with exactly these keys (string or null): ")
				.append("title, clientName, industry, challenge, solution, results, keyMetrics, ")
				.append("timeline, testimonial. Return only the JSON object.");

		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt.toString());
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_object");
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put("temperature", 0.3);
		return payload.toString();
	}

	@Override
	public String buildTokenMappingRequestBody(CreateCaseStudyCommand command) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior strategist preparing a client case study slide deck. ")
				.append("Rewrite the case study fields below into the deck's placeholder structure. ")
				.append("Use ONLY facts from the fields; never invent numbers or claims.\n\n")
				.append("Case study fields:\n")
				.append("Title: ").append(nullToEmpty(command.title())).append('\n')
				.append("Client: ").append(nullToEmpty(command.clientName())).append('\n')
				.append("Industry: ").append(nullToEmpty(command.industry())).append('\n')
				.append("Challenge: ").append(nullToEmpty(command.challenge())).append('\n')
				.append("Solution: ").append(nullToEmpty(command.solution())).append('\n')
				.append("Results: ").append(nullToEmpty(command.results())).append('\n')
				.append("Key metrics: ").append(nullToEmpty(command.keyMetrics())).append('\n')
				.append("Timeline: ").append(nullToEmpty(command.timeline())).append('\n')
				.append("Testimonial: ").append(nullToEmpty(command.testimonial())).append("\n\n")
				.append("Return ONE JSON object with exactly these string keys (use \"\" when the ")
				.append("fields do not support a value):\n")
				.append("- client_vertical: the client's industry or vertical, short (2-5 words)\n")
				.append("- client_challenge: the challenge, 2-3 sentences\n")
				.append("- priority_1, priority_2, priority_3: the client's top priorities or goals, ")
				.append("one short phrase each\n")
				.append("- solution_body: the solution, 2-4 sentences\n")
				.append("- results_intro: one-sentence summary of the results\n")
				.append("- result_detail_1, result_detail_2, result_detail_3: one result highlight ")
				.append("each, one sentence\n")
				.append("- metric_1_value, metric_2_value, metric_3_value: the three strongest ")
				.append("figures (e.g. \"40%\", \"2x\", \"$1.2M\")\n")
				.append("- metric_1_label, metric_2_label, metric_3_label: short label describing ")
				.append("each figure\n")
				.append("Return only the JSON object.");

		ObjectNode message = objectMapper.createObjectNode();
		message.put("role", "user");
		message.put("content", prompt.toString());
		ObjectNode responseFormat = objectMapper.createObjectNode();
		responseFormat.put("type", "json_object");
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("model", properties.getOpenaiModel());
		payload.set("messages", objectMapper.createArrayNode().add(message));
		payload.set("response_format", responseFormat);
		payload.put("temperature", 0.3);
		return payload.toString();
	}

	/**
	 * Converts null to an empty string.
	 *
	 * @param value possibly-null string
	 * @return the value, or empty string when null
	 */
	String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
