package com.aidigital.strategyplanning.web;

import com.aidigital.strategyplanning.service.categoryanalysis.models.GeneratedSlideImage;
import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageStore;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link SlideImagePublicController}.
 *
 * <p>Google Slides fetches this endpoint itself while {@code replaceImage} runs, so the response
 * has to be correct for a non-browser client: the declared content type, a content length, and a
 * cache window. An expired or unknown token must answer 404 rather than an error page, because
 * the store deliberately evicts entries after a few minutes.
 */
class SlideImagePublicControllerTest {

	private static final String TOKEN = "d41d8cd98f00b204e9800998ecf8427e";
	private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

	@Test
	void shouldServeStoredImageBytesWithItsDeclaredContentTypeTest() throws Exception {
		// Given: a token whose image is still held by the store
		SlideImageStore store = mock(SlideImageStore.class);
		when(store.get(TOKEN)).thenReturn(
				Optional.of(new GeneratedSlideImage(PNG_BYTES, "image/png", 1_700_000_000_000L)));
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new SlideImagePublicController(store)).build();

		// When: the public URL is fetched the way Google fetches it
		ResultActions response = mvc.perform(get("/public/slide-images/{token}", TOKEN));

		// Then: the raw bytes come back with the metadata an external fetcher needs
		response.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, PNG_BYTES.length))
				.andExpect(content().bytes(PNG_BYTES));
		verify(store).get(TOKEN);
	}

	@Test
	void shouldAllowShortLivedCachingOfAServedImageTest() throws Exception {
		// Given: a stored image
		SlideImageStore store = mock(SlideImageStore.class);
		when(store.get(TOKEN)).thenReturn(
				Optional.of(new GeneratedSlideImage(PNG_BYTES, "image/png", 1_700_000_000_000L)));
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new SlideImagePublicController(store)).build();

		// When: the image is fetched
		ResultActions response = mvc.perform(get("/public/slide-images/{token}", TOKEN));

		// Then: a bounded cache window is advertised, matching the store's short retention
		response.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300"));
	}

	@Test
	void shouldRespondNotFoundForAnUnknownOrExpiredTokenTest() throws Exception {
		// Given: a token the store no longer holds, which is what expiry looks like
		SlideImageStore store = mock(SlideImageStore.class);
		when(store.get("expired-token")).thenReturn(Optional.empty());
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new SlideImagePublicController(store)).build();

		// When: it is fetched
		ResultActions response = mvc.perform(get("/public/slide-images/{token}", "expired-token"));

		// Then: the fetcher gets a plain 404 instead of an error payload
		response.andExpect(status().isNotFound());
	}

	@Test
	void shouldServeWhateverContentTypeTheStoredImageDeclaresTest() throws Exception {
		// Given: an image stored as JPEG rather than the usual PNG
		SlideImageStore store = mock(SlideImageStore.class);
		byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
		when(store.get(TOKEN)).thenReturn(
				Optional.of(new GeneratedSlideImage(jpegBytes, "image/jpeg", 1_700_000_000_000L)));
		MockMvc mvc = MockMvcBuilders.standaloneSetup(new SlideImagePublicController(store)).build();

		// When: it is fetched
		ResultActions response = mvc.perform(get("/public/slide-images/{token}", TOKEN));

		// Then: the stored type is echoed rather than a hardcoded PNG
		response.andExpect(status().isOk())
				.andExpect(content().contentType("image/jpeg"));
	}
}
