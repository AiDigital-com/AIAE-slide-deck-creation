package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.templates.StandardTemplateTokens;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Standard template token registry is complete and consistent.
 */
class StandardTemplateTokensTest {

	@Test
	void tokensCoverAllFiveSlides() {
		Set<Integer> slides = new HashSet<>();
		StandardTemplateTokens.TOKENS.forEach(t -> slides.add(t.slideNumber()));
		assertThat(slides).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
	}

	@Test
	void tokenKeysAreUniqueAndNonBlank() {
		Set<String> keys = new HashSet<>();
		for (StandardTemplateTokens.TokenSpec spec : StandardTemplateTokens.TOKENS) {
			assertThat(spec.key()).isNotBlank();
			assertThat(spec.label()).isNotBlank();
			assertThat(spec.sampleText()).isNotBlank();
			assertThat(keys.add(spec.key())).as("duplicate key: " + spec.key()).isTrue();
		}
		assertThat(keys).hasSize(50);
	}
}
