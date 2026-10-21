package com.aidigital.strategyplanning.service.casestudy.services.impl;

import com.aidigital.strategyplanning.service.casestudy.services.OnePageDeckRequestAssembler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Default implementation of {@link OnePageDeckRequestAssembler}.
 */
@Service
@RequiredArgsConstructor
public class OnePageDeckRequestAssemblerImpl implements OnePageDeckRequestAssembler {

	private static final String TOKEN_OPEN = "{{";
	private static final String TOKEN_CLOSE = "}}";
	private static final String SLIDES_MEMBER = "slides";
	private static final String PAGE_ELEMENTS_MEMBER = "pageElements";
	private static final String DESCRIPTION_MEMBER = "description";
	private static final String IMAGE_MEMBER = "image";
	private static final String OBJECT_ID_MEMBER = "objectId";
	private static final String ELEMENT_GROUP_MEMBER = "elementGroup";
	private static final String CHILDREN_MEMBER = "children";
	private static final String REPLACE_IMAGE_REQUEST = "replaceImage";
	private static final String IMAGE_OBJECT_ID_FIELD = "imageObjectId";
	private static final String IMAGE_REPLACE_METHOD_FIELD = "imageReplaceMethod";
	private static final String IMAGE_REPLACE_METHOD_CENTER_INSIDE = "CENTER_INSIDE";
	private static final String URL_FIELD = "url";

	private final ObjectMapper objectMapper;

	@Override
	public ArrayNode buildReplaceImageRequests(JsonNode presentation, Map<String, String> imageUrlsBySlot) {
		ArrayNode requests = objectMapper.createArrayNode();
		for (JsonNode slide : presentation.path(SLIDES_MEMBER)) {
			addReplaceImageRequests(slide.path(PAGE_ELEMENTS_MEMBER), imageUrlsBySlot, requests);
		}
		return requests;
	}

	/**
	 * Walks page elements (recursing into groups) and adds a replaceImage request for each
	 * tagged image that has a chosen icon.
	 *
	 * @param elements        page elements to walk
	 * @param imageUrlsBySlot public image URLs keyed by slot
	 * @param requests        request array to add to
	 */
	void addReplaceImageRequests(JsonNode elements, Map<String, String> imageUrlsBySlot, ArrayNode requests) {
		for (JsonNode element : elements) {
			String altText = element.path(DESCRIPTION_MEMBER).asText("").trim();
			if (element.has(IMAGE_MEMBER) && altText.startsWith(TOKEN_OPEN) && altText.endsWith(TOKEN_CLOSE)) {
				String url = imageUrlsBySlot.get(altText.substring(TOKEN_OPEN.length(),
						altText.length() - TOKEN_CLOSE.length()));
				if (StringUtils.hasText(url)) {
					requests.add(replaceImageRequest(element.path(OBJECT_ID_MEMBER).asText(), url));
				}
			}
			addReplaceImageRequests(element.path(ELEMENT_GROUP_MEMBER).path(CHILDREN_MEMBER), imageUrlsBySlot,
					requests);
		}
	}

	/**
	 * Builds one replaceImage request that keeps the image's frame and centres the new image
	 * inside it.
	 *
	 * @param imageObjectId object id of the template image
	 * @param url           public URL of the replacement image
	 * @return request node
	 */
	ObjectNode replaceImageRequest(String imageObjectId, String url) {
		ObjectNode request = objectMapper.createObjectNode();
		ObjectNode inner = request.putObject(REPLACE_IMAGE_REQUEST);
		inner.put(IMAGE_OBJECT_ID_FIELD, imageObjectId);
		inner.put(IMAGE_REPLACE_METHOD_FIELD, IMAGE_REPLACE_METHOD_CENTER_INSIDE);
		inner.put(URL_FIELD, url);
		return request;
	}
}
