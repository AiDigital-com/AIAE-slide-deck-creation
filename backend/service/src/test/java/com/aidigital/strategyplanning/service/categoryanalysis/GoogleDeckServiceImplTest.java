package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.config.CategoryAnalysisProperties;
import com.aidigital.strategyplanning.service.categoryanalysis.models.GoogleConnectionStatus;
import com.aidigital.strategyplanning.service.categoryanalysis.models.SourceLink;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SourceLinkService;
import com.aidigital.strategyplanning.external.clerk.ClerkOAuthClient;
import com.aidigital.strategyplanning.external.google.GoogleWorkspaceClient;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.DeckImagePlacementServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.DeckSourceHyperlinkServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.GoogleDeckServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.GoogleSlidesGatewayImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlidesPresentationInspectorImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.SlidesRequestFactoryImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.services.impl.StandardDeckTemplateAdjusterImpl;
import com.aidigital.strategyplanning.service.common.google.impl.GoogleGrantServiceImpl;
import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link GoogleDeckServiceImpl} request building.
 */
class GoogleDeckServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private GoogleDeckServiceImpl service;
	private SlidesPresentationInspectorImpl inspector;
	private SlidesRequestFactoryImpl requestFactory;

	@BeforeEach
	void setUp() {
		SlideImageService imageService = new SlideImageService() {
			@Override
			public boolean isConnected() {
				return false;
			}

			@Override
			public Map<Integer, String> generateSlideImageUrls(String category, String publicBaseUrl) {
				return Map.of();
			}
		};
		SourceLinkService linkService = sources -> Map.of();
		CategoryAnalysisProperties properties = new CategoryAnalysisProperties();
		GoogleWorkspaceClient workspaceClient = mock(GoogleWorkspaceClient.class);
		inspector = new SlidesPresentationInspectorImpl();
		requestFactory = new SlidesRequestFactoryImpl(objectMapper, inspector);
		GoogleGrantServiceImpl grantService = new GoogleGrantServiceImpl(mock(ClerkOAuthClient.class));
		GoogleSlidesGatewayImpl gateway =
				new GoogleSlidesGatewayImpl(properties, objectMapper, workspaceClient);
		service = new GoogleDeckServiceImpl(properties, inspector, requestFactory,
				new StandardDeckTemplateAdjusterImpl(objectMapper, inspector), grantService, gateway,
				new DeckSourceHyperlinkServiceImpl(linkService, requestFactory, gateway),
				new DeckImagePlacementServiceImpl(objectMapper, imageService, inspector,
						requestFactory, gateway));
	}

	@Test
	void checkGoogleConnectionIsNotConnectedWithoutClerkKey() {
		assertThat(service.checkGoogleConnection("user_1"))
				.isEqualTo(GoogleConnectionStatus.NOT_CONNECTED);
	}

	@Test
	void buildReplaceTextRequestsScopesEachTokenToItsSlide() {
		List<String> slideIds = List.of("s1", "s2", "s3", "s4", "s5");
		ArrayNode requests = requestFactory.buildReplaceTextRequests(
				Map.of("category_name", "PET FOOD", "channel_headline", "CTV WINS"), slideIds);

		JsonNode categoryToken = findRequest(requests, "{{category_name}}");
		assertThat(categoryToken.path("replaceText").asText()).isEqualTo("PET FOOD");
		assertThat(categoryToken.path("pageObjectIds").path(0).asText()).isEqualTo("s1");
		JsonNode categorySample = findRequest(requests, "ONLINE CASINO/GAMING INDUSTRY");
		assertThat(categorySample.path("replaceText").asText()).isEqualTo("PET FOOD");
		assertThat(categorySample.path("pageObjectIds").path(0).asText()).isEqualTo("s1");
		JsonNode channelToken = findRequest(requests, "{{channel_headline}}");
		assertThat(channelToken.path("replaceText").asText()).isEqualTo("CTV WINS");
		assertThat(channelToken.path("pageObjectIds").path(0).asText()).isEqualTo("s4");
	}

	@Test
	void buildReplaceTextRequestsReplacesBlankValuesWithEmptyStringSoNoTokenIsLeft() {
		ArrayNode requests = requestFactory.buildReplaceTextRequests(
				Map.of("category_name", " "), List.of("s1", "s2", "s3", "s4", "s5"));

		JsonNode tokenForm = findRequest(requests, "{{category_name}}");
		assertThat(tokenForm.path("replaceText").asText()).isEmpty();
		JsonNode sampleForm = findRequest(requests, "ONLINE CASINO/GAMING INDUSTRY");
		assertThat(sampleForm.path("replaceText").asText()).isEmpty();
	}

	@Test
	void buildReplaceTextRequestsCoversEveryTokenLeavingNoPlaceholders() {
		ArrayNode requests = requestFactory.buildReplaceTextRequests(
				Map.of(), List.of("s1", "s2", "s3", "s4", "s5"));

		assertThat(requests).hasSize(StandardTemplateTokens.TOKENS.size() * 2);
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			assertThat(findRequest(requests, "{{" + spec.key() + "}}")).isNotNull();
		}
	}

	private JsonNode findRequest(ArrayNode requests, String findText) {
		for (JsonNode request : requests) {
			JsonNode inner = request.path("replaceAllText");
			if (findText.equals(inner.path("containsText").path("text").asText())) {
				return inner;
			}
		}
		return null;
	}

	@Test
	void duplicateSampleLiteralsAreDisambiguatedBySlideScoping() {
		long distinctSamples = StandardTemplateTokens.TOKENS.stream()
				.map(StandardTemplateTokens.TokenSpec::sampleText).distinct().count();
		assertThat(distinctSamples).isLessThan(StandardTemplateTokens.TOKENS.size());

		List<String> slideIds = List.of("s1", "s2", "s3", "s4", "s5");
		String shared = "Source: KFF, Medicare Advantage in 2025; CMS, Medicare Open Enrollment Partner Resources.";
		ArrayNode requests = requestFactory.buildReplaceTextRequests(
				Map.of("trends_sources", "Source: A", "channel_sources", "Source: B"), slideIds);
		for (JsonNode request : requests) {
			JsonNode inner = request.path("replaceAllText");
			if (shared.equals(inner.path("containsText").path("text").asText())) {
				String page = inner.path("pageObjectIds").path(0).asText();
				String replacement = inner.path("replaceText").asText();
				if ("s2".equals(page)) {
					assertThat(replacement).isEqualTo("Source: A");
				} else {
					assertThat(page).isEqualTo("s4");
					assertThat(replacement).isEqualTo("Source: B");
				}
			}
		}
	}

	@Test
	void findTargetImageElementPicksLargestImageOnSlide() throws Exception {
		String json = """
				{"slides":[
				  {"objectId":"s1","pageElements":[]},
				  {"objectId":"s2","pageElements":[
				    {"objectId":"imgSmall","image":{},"size":{"width":{"magnitude":100},"height":{"magnitude":100}}},
				    {"objectId":"imgBig","image":{},"size":{"width":{"magnitude":500},"height":{"magnitude":400}}},
				    {"objectId":"shape1","shape":{}}
				  ]}
				]}""";
		JsonNode presentation = objectMapper.readTree(json);
		assertThat(inspector.findTargetImageElement(presentation, 1, false).path("objectId").asText())
				.isEqualTo("imgBig");
		assertThat(inspector.findTargetImageElement(presentation, 0, false)).isNull();
		assertThat(inspector.findTargetImageElement(presentation, 5, false)).isNull();
	}

	@Test
	void findTargetImageElementPrefersSquareImageForCircularSlide() throws Exception {
		String json = """
				{"slides":[
				  {"objectId":"s3","pageElements":[
				    {"objectId":"imgWideBig","image":{},"size":{"width":{"magnitude":3079177},"height":{"magnitude":2852416}}},
				    {"objectId":"imgCircle","image":{},"size":{"width":{"magnitude":1435050},"height":{"magnitude":1435050}}},
				    {"objectId":"imgLogo","image":{},"size":{"width":{"magnitude":182121},"height":{"magnitude":137612}}}
				  ]}
				]}""";
		JsonNode presentation = objectMapper.readTree(json);
		assertThat(inspector.findTargetImageElement(presentation, 0, true).path("objectId").asText())
				.isEqualTo("imgCircle");
		assertThat(inspector.findTargetImageElement(presentation, 0, false).path("objectId").asText())
				.isEqualTo("imgWideBig");
	}

	@Test
	void findTargetImageElementFallsBackToLargestWhenNoSquareImageExists() throws Exception {
		String json = """
				{"slides":[
				  {"objectId":"s3","pageElements":[
				    {"objectId":"imgWide","image":{},"size":{"width":{"magnitude":500},"height":{"magnitude":300}}}
				  ]}
				]}""";
		JsonNode presentation = objectMapper.readTree(json);
		assertThat(inspector.findTargetImageElement(presentation, 0, true).path("objectId").asText())
				.isEqualTo("imgWide");
	}

	@Test
	void buildAbsolutePlacementRequestScalesToTargetSizeAtFixedPosition() throws Exception {
		String json = """
				{"objectId":"img4",
				 "size":{"width":{"magnitude":1000},"height":{"magnitude":2000}},
				 "transform":{"scaleX":2.0,"scaleY":1.0,"translateX":100.0,"translateY":200.0,"unit":"EMU"}}""";
		JsonNode element = objectMapper.readTree(json);
		JsonNode inner = requestFactory.buildAbsolutePlacementRequest(element, 4000, 500, 600)
				.path("updatePageElementTransform");
		assertThat(inner.path("objectId").asText()).isEqualTo("img4");
		assertThat(inner.path("applyMode").asText()).isEqualTo("ABSOLUTE");
		JsonNode t = inner.path("transform");
		assertThat(t.path("scaleX").asDouble()).isEqualTo(4.0);
		assertThat(t.path("scaleY").asDouble()).isEqualTo(2.0);
		assertThat(t.path("translateX").asDouble()).isEqualTo(500.0);
		assertThat(t.path("translateY").asDouble()).isEqualTo(600.0);
		assertThat(t.path("unit").asText()).isEqualTo("EMU");
	}

	@Test
	void findShapeIdByTextLocatesShapeContainingLiteral() throws Exception {
		String json = """
				{"slides":[
				  {"objectId":"s1","pageElements":[
				    {"objectId":"img1","image":{}},
				    {"objectId":"box1","shape":{"text":{"textElements":[
				      {"textRun":{"content":"CATEGORY "}},{"textRun":{"content":"ANALYSIS\\n"}}
				    ]}}}
				  ]},
				  {"objectId":"s2","pageElements":[
				    {"objectId":"chartTitle","shape":{"text":{"textElements":[
				      {"textRun":{"content":"MEDICARE ADVANTAGE SHARE OF ELIGIBLE MEDICARE BENEFICIARIES"}}
				    ]}}}
				  ]},
				  {"objectId":"s3","pageElements":[
				    {"objectId":"sectionLabel","shape":{"text":{"textElements":[
				      {"textRun":{"content":"TRIP TYPES\\n"}}
				    ]}}}
				  ]}
				]}""";
		JsonNode presentation = objectMapper.readTree(json);
		assertThat(inspector.findShapeIdByText(presentation, 0, "CATEGORY ANALYSIS")).isEqualTo("box1");
		assertThat(inspector.findShapeIdByText(presentation, 1,
				com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens.CHART_TITLE_SAMPLE))
				.isEqualTo("chartTitle");
		assertThat(inspector.findShapeIdByText(presentation, 2,
				com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens.DRIVERS_SECTION_LABEL_SAMPLE))
				.isEqualTo("sectionLabel");
		assertThat(inspector.findShapeIdByText(presentation, 0, "NOT PRESENT")).isNull();
		assertThat(inspector.findShapeIdByText(presentation, 9, "CATEGORY ANALYSIS")).isNull();
	}

	@Test
	void buildCreateImageRequestPlacesCutoutInFixedSlide2Box() {
		JsonNode inner = requestFactory.buildCreateImageRequest("slide2", "https://img/cutout.png")
				.path("createImage");
		assertThat(inner.path("url").asText()).isEqualTo("https://img/cutout.png");
		JsonNode props = inner.path("elementProperties");
		assertThat(props.path("pageObjectId").asText()).isEqualTo("slide2");
		assertThat(props.path("size").path("width").path("magnitude").asDouble())
				.isEqualTo(SlidesRequestFactoryImpl.FIT_IMAGE_SIZE_EMU);
		assertThat(props.path("size").path("height").path("magnitude").asDouble())
				.isEqualTo(SlidesRequestFactoryImpl.FIT_IMAGE_SIZE_EMU);
		JsonNode t = props.path("transform");
		assertThat(t.path("translateX").asDouble()).isEqualTo(SlidesRequestFactoryImpl.FIT_IMAGE_TRANSLATE_X);
		assertThat(t.path("translateY").asDouble()).isEqualTo(SlidesRequestFactoryImpl.FIT_IMAGE_TRANSLATE_Y);
		assertThat(t.path("unit").asText()).isEqualTo("EMU");
	}

	@Test
	void extractSlideIdsPreservesOrder() throws Exception {
		JsonNode presentation = objectMapper.readTree(
				"{\"slides\":[{\"objectId\":\"a\"},{\"objectId\":\"b\"}]}");
		assertThat(inspector.extractSlideIds(presentation)).containsExactly("a", "b");
	}

	@Test
	void buildSourceLinkRequestsLinksCitationKeepingColorAndNoUnderline() throws Exception {
		String sources = "Source: KFF, Medicare Advantage in 2025; CMS resources.";
		String json = """
				{"slides":[
				  {"objectId":"s1","pageElements":[]},
				  {"objectId":"s2","pageElements":[
				    {"objectId":"other","shape":{"text":{"textElements":[
				      {"startIndex":0,"endIndex":8,"textRun":{"content":"headline"}}]}}},
				    {"objectId":"srcBox","shape":{"text":{"textElements":[
				      {"startIndex":0,"endIndex":55,"textRun":{
				        "content":"Source: KFF, Medicare Advantage in 2025; CMS resources.",
				        "style":{"foregroundColor":{"opaqueColor":{"rgbColor":{"red":0.5,"green":0.5,"blue":0.5}}}}}}]}}}
				  ]}
				]}""";
		JsonNode presentation = objectMapper.readTree(json);
		ArrayNode requests = requestFactory.buildSourceLinkRequests(presentation,
				Map.of("trends_sources", sources),
				Map.of("trends_sources", List.of(
						new SourceLink("KFF", "https://www.kff.org"),
						new SourceLink("CMS", "https://www.cms.gov"))));

		assertThat(requests).hasSize(2);
		JsonNode first = requests.get(0).path("updateTextStyle");
		assertThat(first.path("objectId").asText()).isEqualTo("srcBox");
		assertThat(first.path("textRange").path("type").asText()).isEqualTo("FIXED_RANGE");
		assertThat(first.path("textRange").path("startIndex").asInt()).isEqualTo(8);
		assertThat(first.path("textRange").path("endIndex").asInt()).isEqualTo(11);
		assertThat(first.path("style").path("link").path("url").asText())
				.isEqualTo("https://www.kff.org");
		assertThat(first.path("style").path("underline").asBoolean()).isFalse();
		assertThat(first.path("style").path("foregroundColor")
				.path("opaqueColor").path("rgbColor").path("red").asDouble()).isEqualTo(0.5);
		assertThat(first.path("fields").asText()).isEqualTo("link,underline,foregroundColor");
	}

	@Test
	void buildSourceLinkRequestsSkipsCitationsNotFoundInAnyShape() throws Exception {
		JsonNode presentation = objectMapper.readTree(
				"{\"slides\":[{\"objectId\":\"s1\"},{\"objectId\":\"s2\",\"pageElements\":[]}]}");
		ArrayNode requests = requestFactory.buildSourceLinkRequests(presentation,
				Map.of("trends_sources", "Source: KFF"),
				Map.of("trends_sources", List.of(new SourceLink("KFF", "https://www.kff.org"))));
		assertThat(requests).isEmpty();
	}

	@Test
	void updateTextLinkRequestOmitsColorValueButStillMasksItWhenInherited() {
		JsonNode request = requestFactory.updateTextLinkRequest("box", 0, 3, "https://a.org", null);
		JsonNode inner = request.path("updateTextStyle");
		assertThat(inner.path("style").has("foregroundColor")).isFalse();
		assertThat(inner.path("style").path("underline").asBoolean()).isFalse();
		assertThat(inner.path("fields").asText()).isEqualTo("link,underline,foregroundColor");
	}
}
