package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.DeckImagePlacementService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleSlidesGateway;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesPresentationInspector;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlidesRequestFactory;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardDeckLayout;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Default implementation of {@link DeckImagePlacementService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeckImagePlacementServiceImpl implements DeckImagePlacementService {

	private final ObjectMapper objectMapper;
	private final SlideImageService slideImageService;
	private final SlidesPresentationInspector inspector;
	private final SlidesRequestFactory requestFactory;
	private final GoogleSlidesGateway slidesGateway;

	@Override
	public void replaceSlideImages(String accessToken, String presentationId,
	                               JsonNode presentation, String category, String publicBaseUrl) {
		Map<Integer, String> imageUrls = slideImageService.generateSlideImageUrls(category, publicBaseUrl);
		if (imageUrls.isEmpty()) {
			return;
		}
		ArrayNode imageRequests = objectMapper.createArrayNode();
		for (Map.Entry<Integer, String> entry : imageUrls.entrySet()) {
			int slideNumber = entry.getKey();
			JsonNode imageElement = inspector.findTargetImageElement(presentation, slideNumber - 1,
					slideNumber == StandardDeckLayout.CIRCULAR_IMAGE_SLIDE);
			if (slideNumber == StandardDeckLayout.FIT_INSIDE_SLIDE) {
				if (imageElement != null) {
					ObjectNode delete = objectMapper.createObjectNode();
					delete.putObject("deleteObject")
							.put("objectId", imageElement.path("objectId").asText());
					imageRequests.add(delete);
				}
				String slideObjectId = presentation.path("slides").path(slideNumber - 1)
						.path("objectId").asText("");
				if (!slideObjectId.isEmpty()) {
					imageRequests.add(requestFactory.buildCreateImageRequest(slideObjectId, entry.getValue()));
				}
				continue;
			}
			if (imageElement != null) {
				ObjectNode replaceImage = objectMapper.createObjectNode();
				ObjectNode inner = replaceImage.putObject("replaceImage");
				inner.put("imageObjectId", imageElement.path("objectId").asText());
				inner.put("imageReplaceMethod", "CENTER_CROP");
				inner.put("url", entry.getValue());
				imageRequests.add(replaceImage);
				if (slideNumber == StandardDeckLayout.SCALED_IMAGE_SLIDE) {
					imageRequests.add(requestFactory.buildAbsolutePlacementRequest(imageElement,
							StandardDeckLayout.SCALED_TARGET_SIZE_EMU,
							StandardDeckLayout.SCALED_TARGET_TRANSLATE_X,
							StandardDeckLayout.SCALED_TARGET_TRANSLATE_Y));
				}
			}
		}
		if (imageRequests.isEmpty()) {
			return;
		}
		try {
			slidesGateway.batchUpdate(accessToken, presentationId, imageRequests);
		} catch (RuntimeException e) {
			log.warn("Could not apply generated slide images to deck {}: {}", presentationId, e.getMessage());
		}
	}
}
