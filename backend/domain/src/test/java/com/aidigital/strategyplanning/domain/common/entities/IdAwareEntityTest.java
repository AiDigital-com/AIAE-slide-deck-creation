package com.aidigital.strategyplanning.domain.common.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the ID-only equality contract every entity inherits.
 *
 * <p>These semantics are what keeps a {@code Set} of entities correct across a persist: two
 * unsaved rows must never collapse into one, and a saved row must stay findable.
 */
class IdAwareEntityTest {

	@Test
	void shouldNotEqualTransientEntitiesWithNullIdTest() {
		// Given: two entities that have not been persisted yet
		SampleEntity left = new SampleEntity();
		SampleEntity right = new SampleEntity();

		// When-Then: they stay distinct, so a Set of new rows does not silently lose one
		assertThat(left).isNotEqualTo(right);
		assertThat(left.hashCode()).isZero();
	}

	@Test
	void shouldEqualPersistedEntitiesWithSameIdTest() {
		// Given: two instances of the same persisted row
		SampleEntity left = new SampleEntity();
		left.setId(42L);
		SampleEntity right = new SampleEntity();
		right.setId(42L);

		// When-Then: they are the same entity, hash included
		assertThat(left).isEqualTo(right);
		assertThat(left.hashCode()).isEqualTo(right.hashCode());
	}

	@Test
	void shouldEqualItselfTest() {
		// Given: one unsaved entity
		SampleEntity entity = new SampleEntity();

		// When-Then: identity wins before the id is even assigned
		assertThat(entity).isEqualTo(entity);
	}

	@Test
	void shouldNotEqualNullOrAnotherEntityTypeTest() {
		// Given: two entity types that happen to share an id
		SampleEntity sample = new SampleEntity();
		sample.setId(1L);
		OtherEntity other = new OtherEntity();
		other.setId(1L);

		// When-Then: a shared id across tables is not a shared identity
		assertThat(sample).isNotEqualTo(null);
		assertThat(sample).isNotEqualTo(other);
	}

	@Test
	void shouldNotEqualWhenOnlyOneSideIsPersistedTest() {
		// Given: one saved row and one still-unsaved instance
		SampleEntity saved = new SampleEntity();
		saved.setId(7L);
		SampleEntity unsaved = new SampleEntity();

		// When-Then: neither direction claims equality
		assertThat(saved).isNotEqualTo(unsaved);
		assertThat(unsaved).isNotEqualTo(saved);
	}

	@Test
	void shouldExposeTheGeneratedIdTest() {
		// Given: an entity assigned a generated identity
		SampleEntity entity = new SampleEntity();
		entity.setId(99L);

		// When-Then: the id reads back for callers that need the cursor value
		assertThat(entity.getId()).isEqualTo(99L);
	}

	private static final class SampleEntity extends IdAwareEntity {

	}

	private static final class OtherEntity extends IdAwareEntity {

	}
}
