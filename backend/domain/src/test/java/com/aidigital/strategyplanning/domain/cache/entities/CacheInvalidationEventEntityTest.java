package com.aidigital.strategyplanning.domain.cache.entities;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the cache-invalidation outbox row.
 *
 * <p>The poller reads {@code trackedClass} to decide which cache regions to evict and orders by
 * the generated id, so both fields have to survive a round trip intact.
 */
class CacheInvalidationEventEntityTest {

	@Test
	void shouldCarryTheTrackedClassAndTimestampFromTheConstructorTest() {
		// Given: an invalidation announced for one tracked entity class
		Instant createdAt = Instant.parse("2026-08-23T10:15:30Z");

		// When: the outbox row is built
		CacheInvalidationEventEntity event =
				new CacheInvalidationEventEntity("com.example.CountryEntity", createdAt);

		// Then: the poller can read both the region key and when it changed
		assertThat(event.getTrackedClass()).isEqualTo("com.example.CountryEntity");
		assertThat(event.getCreatedAt()).isEqualTo(createdAt);
	}

	@Test
	void shouldAllowJpaToRehydrateTheRowThroughSettersTest() {
		// Given: a row whose fields are populated after construction, the way a load does
		CacheInvalidationEventEntity event = new CacheInvalidationEventEntity(null, null);
		Instant createdAt = Instant.parse("2026-08-23T11:00:00Z");

		// When: the row is populated
		event.setId(5L);
		event.setTrackedClass("com.example.CurrencyEntity");
		event.setCreatedAt(createdAt);

		// Then: every field reads back, id included — it is the poller's ordering cursor
		assertThat(event.getId()).isEqualTo(5L);
		assertThat(event.getTrackedClass()).isEqualTo("com.example.CurrencyEntity");
		assertThat(event.getCreatedAt()).isEqualTo(createdAt);
	}

	@Test
	void shouldInheritIdOnlyEqualityTest() {
		// Given: two loads of the same outbox row carrying different field values
		CacheInvalidationEventEntity first =
				new CacheInvalidationEventEntity("A", Instant.parse("2026-08-23T10:00:00Z"));
		first.setId(3L);
		CacheInvalidationEventEntity second =
				new CacheInvalidationEventEntity("B", Instant.parse("2026-08-23T12:00:00Z"));
		second.setId(3L);

		// When-Then: identity is the row id, so a re-read does not duplicate the event
		assertThat(first).isEqualTo(second);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}
}
