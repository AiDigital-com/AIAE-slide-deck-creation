package com.aidigital.strategyplanning.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the warm-up marker interface.
 *
 * <p>Generic types are erased at runtime, so a repository that opts into warm-up must state its
 * entity class itself. The default has to fail loudly rather than warm up the wrong region.
 */
class ToWarmUpTest {

	@Test
	void shouldRejectAWarmUpRepositoryThatDoesNotDeclareItsEntityClassTest() {
		// Given: a repository that opted into warm-up but left getClazz() inherited
		ToWarmUp<String> incomplete = List::of;

		// When-Then: startup fails with an actionable message instead of warming nothing
		assertThatThrownBy(incomplete::getClazz)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("getClazz() must be overridden by the warm-up repository.");
	}

	@Test
	void shouldUseTheDeclaredEntityClassWhenOverriddenTest() {
		// Given: a repository that declares its entity class properly
		ToWarmUp<String> complete = new ToWarmUp<>() {
			@Override
			public List<String> findAll() {
				return List.of("row");
			}

			@Override
			public Class<String> getClazz() {
				return String.class;
			}
		};

		// When-Then: the override is what the warm-up logging reports
		assertThat(complete.getClazz()).isEqualTo(String.class);
		assertThat(complete.findAll()).containsExactly("row");
	}
}
