package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.external.openai.OpenAiChatClient;
import com.aidigital.strategyplanning.external.openai.OpenAiExternalException;
import com.aidigital.strategyplanning.external.openai.model.OpenAiChatCall;
import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SourceLinkService;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-backed implementation of {@link SourceLinkService}.
 * Sends the final reviewed sources lines to the chat completions API and asks it to
 * identify each cited research body together with the official website URL it is
 * confident is real. Resolution is best-effort: any failure returns an empty map so
 * deck creation always proceeds without hyperlinks rather than failing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiSourceLinkServiceImpl implements SourceLinkService {

	private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(60);

	private final CategoryAnalysisProperties properties;
	private final ObjectMapper objectMapper;
	private final OpenAiChatClient openAiChatClient;

	@Override
	public Map<String, List<SourceLink>> resolveSourceLinks(Map<String, String> sourcesByKey) {
		if (!StringUtils.hasText(properties.getOpenaiApiKey())
				|| sourcesByKey == null || sourcesByKey.isEmpty()) {
			return Map.of();
		}
		try {
			String content = callChatCompletion(buildRequestBody(sourcesByKey));
			return parseLinks(content, sourcesByKey);
		} catch (RuntimeException e) {
			log.warn("Could not resolve source hyperlinks: {}", e.getMessage());
			return Map.of();
		}
	}

	/**
	 * Builds the chat completions request body asking for the cited organizations and
	 * their official website URLs for every sources line.
	 *
	 * @param sourcesByKey final reviewed sources text keyed by template token key
	 * @return serialized JSON request body
	 */
	public String buildRequestBody(Map<String, String> sourcesByKey) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You resolve research citations to official websites.\n")
				.append("For each citation line below, identify every research body, publisher, or ")
				.append("organization cited, and give the official public website URL you are confident ")
				.append("really exists (the organization's homepage or main research section — never ")
				.append("guess deep links to specific reports or PDFs).\n\nCitation lines:\n");
		for (Map.Entry<String, String> entry : sourcesByKey.entrySet()) {
			prompt.append("  \"").append(entry.getKey()).append("\": \"")
					.append(entry.getValue()).append("\"\n");
		}
		prompt.append("\nReturn ONE JSON object with exactly the same keys. Each value is an array of ")
				.append("objects with two string fields:\n")
				.append("  - \"text\": the EXACT substring of that citation line naming the organization, ")
				.append("copied character-for-character (so it can be located in the line)\n")
				.append("  - \"url\": the organization's official https website URL\n")
				.append("Skip any organization whose website you are not confident about. ")
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
		payload.put("temperature", 0.2);
		return payload.toString();
	}

	/**
	 * Sends a prepared chat completions request body to OpenAI and returns the message content.
	 *
	 * @param requestBody serialized JSON chat completions request body
	 * @return the model's message content string
	 * @throws AppException with C003 reason when the call fails or returns no content
	 */
	public String callChatCompletion(String requestBody) {
		try {
			return openAiChatClient.complete(new OpenAiChatCall(
					properties.getOpenaiBaseUrl(),
					properties.getOpenaiApiKey(),
					requestBody,
					CHAT_TIMEOUT));
		} catch (OpenAiExternalException e) {
			throw sourceLinkCallFailure(e);
		}
	}

	/**
	 * Translates a provider failure into this feature's application error, preserving the wording
	 * the UI already surfaces for each case.
	 *
	 * @param failure provider failure raised by the OpenAI client
	 * @return the application exception to throw
	 */
	AppException sourceLinkCallFailure(OpenAiExternalException failure) {
		return switch (failure.getFailure()) {
			case HTTP_STATUS -> new AppException(ErrorReason.C003,
					"Source link call failed with HTTP " + failure.getStatusCode());
			case EMPTY_CONTENT -> new AppException(ErrorReason.C003,
					"Source link call returned no content");
			case INTERRUPTED -> new AppException(ErrorReason.C003, failure,
					"Source link call interrupted");
			case TRANSPORT -> new AppException(ErrorReason.C003, failure,
					"Source link call failed: " + rootMessage(failure));
		};
	}

	/**
	 * Returns the message of the failure's underlying cause, which is what the previous direct
	 * HTTP call reported to the user.
	 *
	 * @param failure provider failure
	 * @return cause message, or the failure's own message when it has no cause
	 */
	String rootMessage(OpenAiExternalException failure) {
		return failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage();
	}

	/**
	 * Parses the model response into validated links. A link is kept only when its text is
	 * a non-blank exact substring of the corresponding sources line and its URL is an
	 * http(s) address — anything else is dropped silently.
	 *
	 * @param content      model message content (JSON object keyed by token key)
	 * @param sourcesByKey the sources lines the links must belong to
	 * @return validated links per token key, keys without valid links omitted
	 * @throws AppException with C003 reason when the content is not valid JSON
	 */
	public Map<String, List<SourceLink>> parseLinks(String content, Map<String, String> sourcesByKey) {
		JsonNode root;
		try {
			root = objectMapper.readTree(content);
		} catch (JsonProcessingException e) {
			throw new AppException(ErrorReason.C003, e, "Source link response was not valid JSON");
		}
		Map<String, List<SourceLink>> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : sourcesByKey.entrySet()) {
			List<SourceLink> links = new ArrayList<>();
			for (JsonNode linkNode : root.path(entry.getKey())) {
				String text = linkNode.path("text").asText("");
				String url = linkNode.path("url").asText("");
				if (isValidLink(text, url, entry.getValue())) {
					links.add(new SourceLink(text, url));
				}
			}
			if (!links.isEmpty()) {
				result.put(entry.getKey(), links);
			}
		}
		return result;
	}

	/**
	 * Validates a single candidate link against the sources line it must belong to.
	 *
	 * @param text       candidate link text
	 * @param url        candidate URL
	 * @param sourceLine the sources line the text must appear in
	 * @return true when the text is a non-blank substring of the line and the URL is http(s)
	 */
	public boolean isValidLink(String text, String url, String sourceLine) {
		return StringUtils.hasText(text)
				&& StringUtils.hasText(url)
				&& sourceLine != null && sourceLine.contains(text)
				&& (url.startsWith("https://") || url.startsWith("http://"));
	}
}
