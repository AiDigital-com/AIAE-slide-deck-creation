package com.aidigital.strategyplanning.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;

/**
 * Web-layer tests for {@link SpaFallbackController}.
 *
 * <p>What this controller actually decides is which paths belong to the React router and which
 * must be left to Spring. Getting that wrong is invisible until deployment: too greedy and the
 * API or Actuator starts returning HTML, too narrow and a browser refresh on a deep link 404s.
 * These tests pin both directions by asserting which handler method matched, independently of
 * whether a built SPA is on the classpath.
 */
class SpaFallbackControllerTest {

	private MockMvc mvc() {
		return MockMvcBuilders.standaloneSetup(new SpaFallbackController()).build();
	}

	@Test
	void shouldServeTheSpaShellForTheRootAndIndexPathsTest() throws Exception {
		// Given: the deployed SPA entry points
		MockMvc mvc = mvc();

		// When-Then: both are handled by the root mapping
		mvc.perform(get("/")).andExpect(handler().methodName("root"));
		mvc.perform(get("/index.html")).andExpect(handler().methodName("root"));
	}

	@Test
	void shouldServeTheSpaShellForSingleSegmentClientRoutesTest() throws Exception {
		// Given: a client-side route a user can bookmark or refresh
		MockMvc mvc = mvc();

		// When-Then: it reaches the single-segment fallback instead of 404ing at the server
		mvc.perform(get("/login")).andExpect(handler().methodName("spaSingleSegment"));
		mvc.perform(get("/category-analysis")).andExpect(handler().methodName("spaSingleSegment"));
	}

	@Test
	void shouldServeTheSpaShellForNestedClientRoutesTest() throws Exception {
		// Given: a nested client-side route
		MockMvc mvc = mvc();

		// When-Then: it reaches the nested fallback
		mvc.perform(get("/case-studies/42/edit")).andExpect(handler().methodName("spaNested"));
	}

	@Test
	void shouldNotCaptureApiActuatorOrDocumentationPathsTest() throws Exception {
		// Given: server-owned prefixes that must never be answered with the React shell
		MockMvc mvc = mvc();

		// When-Then: none of them is claimed by a fallback handler
		assertNoFallbackHandler(mvc, "/api/v1/case-studies");
		assertNoFallbackHandler(mvc, "/actuator/health");
		assertNoFallbackHandler(mvc, "/actuator/prometheus");
		assertNoFallbackHandler(mvc, "/swagger-ui/index.html");
		assertNoFallbackHandler(mvc, "/v3/api-docs");
	}

	@Test
	void shouldNotCaptureStaticAssetRequestsTest() throws Exception {
		// Given: paths the static resource handler owns, including the generated image endpoint
		MockMvc mvc = mvc();

		// When-Then: the fallback leaves them alone so real assets are not shadowed by HTML
		assertNoFallbackHandler(mvc, "/assets/index-a1b2c3.js");
		assertNoFallbackHandler(mvc, "/favicon.ico");
		assertNoFallbackHandler(mvc, "/public/slide-images/token123");
		assertNoFallbackHandler(mvc, "/logo.png");
	}

	/**
	 * Asserts that no SPA fallback handler claimed the path.
	 *
	 * @param mvc  standalone MockMvc holding only the fallback controller
	 * @param path request path that must stay unclaimed
	 * @throws Exception when the request cannot be performed
	 */
	private void assertNoFallbackHandler(MockMvc mvc, String path) throws Exception {
		MvcResult result = mvc.perform(get(path)).andReturn();
		assertThat(result.getHandler())
				.as("path %s must not be answered by the SPA fallback", path)
				.isNull();
	}
}
