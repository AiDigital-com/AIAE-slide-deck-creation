package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.config.CaseStudyProperties;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFieldSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageFit;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSlot;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSpec;
import com.aidigital.strategyplanning.service.casestudy.models.OnePageTextMeasurement;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageDraftPromptComposer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageFieldMeasurer;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTemplateRegistry;
import com.aidigital.strategyplanning.service.casestudy.services.OnePageTextFitter;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageIconLibrary;
import com.aidigital.strategyplanning.service.common.files.SourceDocument;
import com.aidigital.strategyplanning.service.common.files.SourceImage;
import com.aidigital.strategyplanning.service.common.files.SourceImageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link OnePageDraftPromptComposer}.
 */
@Service
@RequiredArgsConstructor
public class OnePageDraftPromptComposerImpl implements OnePageDraftPromptComposer {

	/** Sampling temperature: low, so figures are copied rather than paraphrased. */
	public static final double TEMPERATURE = 0.2;

	private static final String ROLE_KEY = "role";
	private static final String ROLE_USER = "user";
	private static final String CONTENT_KEY = "content";
	private static final String MODEL_KEY = "model";
	private static final String MESSAGES_KEY = "messages";
	private static final String RESPONSE_FORMAT_KEY = "response_format";
	private static final String RESPONSE_FORMAT_TYPE_KEY = "type";
	private static final String RESPONSE_FORMAT_JSON = "json_object";
	private static final String TEMPERATURE_KEY = "temperature";
	private static final String PART_TYPE_KEY = "type";
	private static final String PART_TYPE_TEXT = "text";
	private static final String PART_TYPE_IMAGE_URL = "image_url";
	private static final String IMAGE_URL_KEY = "url";
	private static final String IMAGE_DETAIL_KEY = "detail";
	private static final String IMAGE_DETAIL_HIGH = "high";
	private static final String FILE_NAME_SEPARATOR = ", ";
	private static final double HALF_LINE = 0.5;

	private final CaseStudyProperties properties;
	private final ObjectMapper objectMapper;
	private final SourceImageService sourceImageService;
	private final OnePageTemplateRegistry templateRegistry;
	private final OnePageTextFitter textFitter;
	private final OnePageFieldMeasurer measurer;

	@Override
	public String buildDraftRequestBody(List<SourceDocument> documents, List<SourceImage> images) {
		return payload(buildDraftPrompt(documents, images), images);
	}

	@Override
	public String buildRevisionRequestBody(List<OnePageFieldSpec> misfits, Map<String, String> values,
			List<SourceDocument> documents, List<SourceImage> images) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are revising a one-page client case study slide. Each field below was measured in its ")
				.append("real text box on the slide and does not fit: some leave the box partly empty, some run ")
				.append("past it. Rewrite each one to the size given.\n")
				.append("- To lengthen, add specific detail from the source material (figures, context, how it was ")
				.append("done) and, where the facts run out, interpretation of why it mattered for the client. ")
				.append("Never add facts, figures, names, or dates that are not in the material.\n")
				.append("- To shorten, keep the strongest facts and cut the rest.\n")
				.append("- Keep each field's meaning and never name the client brand.\n\nFields to rewrite:\n");
		for (OnePageFieldSpec spec : misfits) {
			String value = values.get(spec.key());
			prompt.append("- ").append(spec.key()).append(" (").append(spec.label()).append("): box is ")
					.append(boxSize(spec)).append("; the current text is ").append(value.length())
					.append(" characters and ").append(describeChange(spec, values)).append(".");
			if (templateRegistry.isFactsOnly(spec.key())) {
				prompt.append(" FACTS ONLY: use only details stated in the material, no interpretation; if the ")
						.append("material has nothing more, return the current text unchanged.");
			}
			prompt.append(" Current: \"").append(value).append("\"\n");
		}
		appendSourceMaterial(prompt, documents, images);
		prompt.append("Return ONE JSON object: {\"fields\": {<each key above>: string}}. Return only the JSON.");
		return payload(prompt.toString(), images);
	}

	@Override
	public String boxSize(OnePageFieldSpec spec) {
		if (!spec.isMeasured()) {
			return "max " + spec.maxChars() + " characters";
		}
		int perLine = textFitter.charsPerLine(spec);
		int max = spec.box().maxLines() * perLine;
		if (spec.box().minLines() == 0) {
			return "up to " + spec.box().maxLines() + " line(s), at most about " + max + " characters";
		}
		int min = Math.max(1, (int) Math.round((spec.box().minLines() - HALF_LINE) * perLine));
		return spec.box().minLines() == spec.box().maxLines()
				? "exactly " + spec.box().minLines() + " line(s), about " + min + "-" + max + " characters"
				: spec.box().minLines() + "-" + spec.box().maxLines() + " lines, about " + min + "-" + max
						+ " characters";
	}

	/**
	 * Builds the drafting prompt: the role, the source material, the rules, every field with its
	 * box size and example, every icon slot, and the icon library.
	 *
	 * @param documents parsed text documents
	 * @param images    uploaded images (named so the model can cite them)
	 * @return prompt text
	 */
	String buildDraftPrompt(List<SourceDocument> documents, List<SourceImage> images) {
		StringBuilder prompt = new StringBuilder();
		prompt.append("You are a senior strategist at AI Digital, a programmatic media agency, writing a ")
				.append("ONE-PAGE client case study slide from the source material below.");
		appendSourceMaterial(prompt, documents, images);
		prompt.append("Rules:\n")
				.append("- Fields marked FILES are facts: figures, dates, channels, tactics, and results must ")
				.append("come ONLY from the source material — never invent, estimate, or round them. If the ")
				.append("material does not support a FILES field at all, return \"\" for it.\n")
				.append("- Fields marked WRITTEN are short titles and labels. Phrase them for a punchy, fluid ")
				.append("slide, based on what the material shows. A WRITTEN field that labels an empty FILES ")
				.append("field must also be \"\".\n")
				.append("- Never name the client brand. Describe the client generically, like \"A home ")
				.append("improvement company covering windows, roofing, and baths\".\n")
				.append("- Fill sections in order and never pad: if the material supports only 3 tactics, fill ")
				.append("tactics 1-3 and return \"\" for tactic 4. The same for plan steps, drivers, and stats.\n")
				.append("- FILL EACH BOX. Every field shows its box size in lines and roughly how many ")
				.append("characters that is. Write to that size: text that is too short leaves a visibly empty ")
				.append("box; text that is too long runs into the next element. Reach the size with specific ")
				.append("detail from the material (figures, context, how it was done) and, where the facts run ")
				.append("out, with interpretation of why it mattered for the client — but no new facts.\n")
				.append("- Match each example's format, never its content (examples come from an unrelated ")
				.append("home-services case study).\n")
				.append("- The three stats are the strongest results, value like \"-68%\" or \"+32%\" with a ")
				.append("short label. headline_stat opens with a verb and the biggest result, then says what ")
				.append("it delivered, like \"DRIVING 67% CPA REDUCTION WHILE SCALING FULL-FUNNEL YOUTUBE\".\n\n")
				.append("Fields (key [source, box size] label — e.g. example):\n");
		for (OnePageFieldSpec spec : templateRegistry.fields()) {
			prompt.append("- ").append(spec.key()).append(" [").append(spec.source()).append(", ")
					.append(boxSize(spec)).append("] ").append(spec.label()).append(" — e.g. \"")
					.append(spec.example()).append("\"\n");
		}
		prompt.append("\nIcons: for each icon slot, pick the library icon that best illustrates the content ")
				.append("it sits beside, or null when that content is empty. Use each icon at most once.\n")
				.append("Icon slots (slot — illustrates):\n");
		for (OnePageIconSlot slot : templateRegistry.iconSlots()) {
			prompt.append("- ").append(slot.slot()).append(" — ").append(String.join(FILE_NAME_SEPARATOR,
					slot.fieldKeys())).append('\n');
		}
		prompt.append("Icon library (name — shows):\n");
		for (OnePageIconSpec icon : OnePageIconLibrary.ICONS) {
			prompt.append("- ").append(icon.name()).append(" — ").append(icon.description()).append('\n');
		}
		prompt.append("\nReturn ONE JSON object: {\"fields\": {<every field key>: string}, \"icons\": ")
				.append("{<every icon slot>: icon name or null}}. Return only the JSON object.");
		return prompt.toString();
	}

	/**
	 * Tells the model how a misfit measures now and roughly how many characters to add or cut.
	 *
	 * @param spec   misfit field
	 * @param values current values keyed by field
	 * @return the change description
	 */
	String describeChange(OnePageFieldSpec spec, Map<String, String> values) {
		OnePageTextMeasurement m = measurer.measure(spec, values);
		int perLine = textFitter.charsPerLine(spec);
		return m.fit() == OnePageFit.SHORT
				? "fills " + m.wideLines() + " line(s) — ADD about " + (m.minLines() - m.wideLines()) * perLine
						+ " characters"
				: "runs to " + m.lines() + " lines — CUT about " + (m.lines() - m.maxLines()) * perLine
						+ " characters";
	}

	/**
	 * Appends every document's text (and names the attached screenshots) to a prompt.
	 *
	 * @param prompt    prompt being built
	 * @param documents parsed text documents
	 * @param images    uploaded images
	 */
	void appendSourceMaterial(StringBuilder prompt, List<SourceDocument> documents, List<SourceImage> images) {
		prompt.append(" Spreadsheets are rendered as rows with \" | \" between cells; read them as tables.");
		if (!images.isEmpty()) {
			prompt.append(" Images attached after this text are screenshots (")
					.append(images.stream().map(SourceImage::fileName).collect(Collectors.joining(FILE_NAME_SEPARATOR)))
					.append("); read figures from them too.");
		}
		prompt.append("\n\nSource material:\n");
		for (SourceDocument document : documents) {
			prompt.append("=== Document: ").append(document.fileName()).append(" ===\n")
					.append(document.text()).append("\n\n");
		}
	}

	/**
	 * Wraps a prompt and images into a JSON-mode chat completions request body.
	 *
	 * @param promptText prompt text
	 * @param images     uploaded images
	 * @return serialized JSON request body
	 */
	String payload(String promptText, List<SourceImage> images) {
		ObjectNode message = objectMapper.createObjectNode();
		message.put(ROLE_KEY, ROLE_USER);
		message.set(CONTENT_KEY, toContentParts(promptText, images));
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put(MODEL_KEY, properties.getOpenaiModel());
		payload.set(MESSAGES_KEY, objectMapper.createArrayNode().add(message));
		payload.putObject(RESPONSE_FORMAT_KEY).put(RESPONSE_FORMAT_TYPE_KEY, RESPONSE_FORMAT_JSON);
		payload.put(TEMPERATURE_KEY, TEMPERATURE);
		return payload.toString();
	}

	/**
	 * Builds the multi-part content of the user message: the prompt text first, then one
	 * {@code image_url} part per uploaded image carrying an inline data URL.
	 *
	 * @param promptText assembled prompt text
	 * @param images     uploaded images, may be empty
	 * @return chat message content parts
	 */
	ArrayNode toContentParts(String promptText, List<SourceImage> images) {
		ArrayNode parts = objectMapper.createArrayNode();
		parts.addObject().put(PART_TYPE_KEY, PART_TYPE_TEXT).put(PART_TYPE_TEXT, promptText);
		for (SourceImage image : images) {
			ObjectNode part = parts.addObject();
			part.put(PART_TYPE_KEY, PART_TYPE_IMAGE_URL);
			part.putObject(PART_TYPE_IMAGE_URL)
					.put(IMAGE_URL_KEY, sourceImageService.toDataUrl(image))
					.put(IMAGE_DETAIL_KEY, IMAGE_DETAIL_HIGH);
		}
		return parts;
	}
}
