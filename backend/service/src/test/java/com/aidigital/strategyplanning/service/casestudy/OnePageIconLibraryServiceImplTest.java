package com.aidigital.strategyplanning.service.casestudy;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconOption;
import com.aidigital.strategyplanning.service.casestudy.services.impl.OnePageIconLibraryServiceImpl;
import com.aidigital.strategyplanning.service.casestudy.templates.OnePageIconLibrary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the One-Page icon library service.
 */
class OnePageIconLibraryServiceImplTest {

	@Test
	void shouldOfferEveryLibraryIconWithItsImagePathTest() {
		// Given: the library service
		OnePageIconLibraryServiceImpl library = new OnePageIconLibraryServiceImpl();

		// When: the options are listed
		List<OnePageIconOption> options = library.options();

		// Then: every library icon is offered, in order, with the path the frontend serves it from
		assertThat(options).hasSize(OnePageIconLibrary.ICONS.size());
		assertThat(options.get(0).name()).isEqualTo("heart");
		assertThat(options.get(0).description()).isEqualTo("Heart — loyalty, brand love, customer affinity");
		assertThat(options.get(0).imageUrl()).isEqualTo("/case-study-icons/heart.png");
	}

	@Test
	void shouldRecogniseOnlyLibraryNamesTest() {
		// Given: the library service
		OnePageIconLibraryServiceImpl library = new OnePageIconLibraryServiceImpl();

		// When-Then: library names pass, anything else (including null) does not
		assertThat(library.contains("target")).isTrue();
		assertThat(library.contains("not-a-real-icon")).isFalse();
		assertThat(library.contains(null)).isFalse();
	}

	@Test
	void shouldBuildAnAbsoluteImageUrlWithoutDoubledSlashesTest() {
		// Given: base URLs with and without a trailing slash
		OnePageIconLibraryServiceImpl library = new OnePageIconLibraryServiceImpl();

		// When-Then: both produce the same clean URL Google can fetch
		assertThat(library.imageUrl("https://app.example.com/", "target"))
				.isEqualTo("https://app.example.com/case-study-icons/target.png");
		assertThat(library.imageUrl("https://app.example.com", "target"))
				.isEqualTo("https://app.example.com/case-study-icons/target.png");
	}
}
