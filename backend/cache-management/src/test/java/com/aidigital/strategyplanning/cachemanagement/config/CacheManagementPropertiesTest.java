package com.aidigital.strategyplanning.cachemanagement.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the invalidation-poller configuration.
 *
 * <p>Each setter rejects a value that would break the poller rather than accepting it and
 * failing later: a zero batch size makes every poll fetch nothing and the cursor never advances,
 * a zero page count means the poller never runs, and a non-positive retention would delete rows
 * a restarting node still needs to replay.
 */
class CacheManagementPropertiesTest {

	@Test
	void shouldStartFromSafeDefaultsTest() {
		// Given: properties with nothing configured
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then: the defaults are usable without any configuration at all
		assertThat(properties.getBatchSize()).isEqualTo(500);
		assertThat(properties.getMaxBatchesPerPoll()).isEqualTo(20);
		assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(7));
		assertThat(properties.isVerifyRegistry()).isFalse();
		assertThat(properties.isCleanupEnabled()).isFalse();
	}

	@Test
	void shouldAcceptConfiguredValuesTest() {
		// Given: properties bound from configuration
		CacheManagementProperties properties = new CacheManagementProperties();

		// When: every value is set
		properties.setBatchSize(100);
		properties.setMaxBatchesPerPoll(5);
		properties.setRetention(Duration.ofHours(12));
		properties.setVerifyRegistry(true);
		properties.setCleanupEnabled(true);

		// Then: each one reads back
		assertThat(properties.getBatchSize()).isEqualTo(100);
		assertThat(properties.getMaxBatchesPerPoll()).isEqualTo(5);
		assertThat(properties.getRetention()).isEqualTo(Duration.ofHours(12));
		assertThat(properties.isVerifyRegistry()).isTrue();
		assertThat(properties.isCleanupEnabled()).isTrue();
	}

	@Test
	void shouldRejectANonPositiveBatchSizeTest() {
		// Given: properties about to be bound from bad configuration
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then: a zero or negative page size would stall the cursor forever
		assertThatThrownBy(() -> properties.setBatchSize(0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("batch-size must be positive");
		assertThatThrownBy(() -> properties.setBatchSize(-1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("batch-size must be positive");
		assertThat(properties.getBatchSize()).isEqualTo(500);
	}

	@Test
	void shouldRejectANonPositiveBatchCountTest() {
		// Given: properties about to be bound from bad configuration
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then: zero pages per poll means the poller silently never evicts anything
		assertThatThrownBy(() -> properties.setMaxBatchesPerPoll(0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("max-batches-per-poll must be positive");
		assertThatThrownBy(() -> properties.setMaxBatchesPerPoll(-5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("max-batches-per-poll must be positive");
		assertThat(properties.getMaxBatchesPerPoll()).isEqualTo(20);
	}

	@Test
	void shouldRejectAbsentOrNonPositiveRetentionTest() {
		// Given: properties about to be bound from bad configuration
		CacheManagementProperties properties = new CacheManagementProperties();

		// When-Then: retention must leave a replay window for a restarting node
		assertThatThrownBy(() -> properties.setRetention(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("retention must be positive");
		assertThatThrownBy(() -> properties.setRetention(Duration.ZERO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("retention must be positive");
		assertThatThrownBy(() -> properties.setRetention(Duration.ofDays(-1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("retention must be positive");
		assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(7));
	}
}
