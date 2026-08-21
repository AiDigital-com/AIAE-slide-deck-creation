package com.aidigital.strategyplanning.cachemanagement.updater;

import com.aidigital.strategyplanning.cachemanagement.config.CacheManagementProperties;
import com.aidigital.strategyplanning.cachemanagement.event.CacheInvalidationEvent;
import com.aidigital.strategyplanning.cachemanagement.event.CacheInvalidationEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for monotonic, retry-safe cache invalidation polling.
 */
@ExtendWith(MockitoExtension.class)
class ScheduledCacheUpdaterTest {

	@Mock
	private CacheInvalidationEventService eventService;

	@Mock
	private CacheUpdaterService cacheUpdaterService;

	@Test
	void shouldProcessOrderedEventsAndAdvanceCursorTest() {
		CacheManagementProperties properties = new CacheManagementProperties();
		when(eventService.updatesAfter(0L, properties.getBatchSize())).thenReturn(List.of(
				new CacheInvalidationEvent(10L, "example.Team", Instant.now()),
				new CacheInvalidationEvent(11L, "example.Role", Instant.now())));
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		updater.pollAndEvict();

		InOrder order = inOrder(cacheUpdaterService);
		order.verify(cacheUpdaterService).clearCachesForClass("example.Team");
		order.verify(cacheUpdaterService).clearCachesForClass("example.Role");
		assertThat(updater.getLastProcessedSequence()).isEqualTo(11L);
	}

	@Test
	void shouldKeepCursorOnFailedEvictionForRetryTest() {
		CacheManagementProperties properties = new CacheManagementProperties();
		CacheInvalidationEvent event =
				new CacheInvalidationEvent(7L, "example.Team", Instant.now());
		when(eventService.updatesAfter(0L, properties.getBatchSize())).thenReturn(List.of(event));
		org.mockito.Mockito.doThrow(new IllegalStateException("cache unavailable"))
				.when(cacheUpdaterService).clearCachesForClass("example.Team");
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		assertThatThrownBy(updater::pollAndEvict)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cache unavailable");
		assertThat(updater.getLastProcessedSequence()).isZero();
	}

	@Test
	void shouldRejectNonIncreasingSequenceTest() {
		CacheManagementProperties properties = new CacheManagementProperties();
		when(eventService.updatesAfter(0L, properties.getBatchSize())).thenReturn(List.of(
				new CacheInvalidationEvent(0L, "example.Team", Instant.now())));
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		assertThatThrownBy(updater::pollAndEvict)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("strictly increasing");
		verifyNoInteractions(cacheUpdaterService);
	}

	@Test
	void shouldStopAfterABoundedNumberOfBatchesTest() {
		// Given: a backlog larger than one poll is allowed to drain, so every page comes
		// back full and the loop has to stop itself
		CacheManagementProperties properties = new CacheManagementProperties();
		properties.setBatchSize(1);
		properties.setMaxBatchesPerPoll(2);
		when(eventService.updatesAfter(0L, 1)).thenReturn(List.of(
				new CacheInvalidationEvent(1L, "example.Team", Instant.now())));
		when(eventService.updatesAfter(1L, 1)).thenReturn(List.of(
				new CacheInvalidationEvent(2L, "example.Role", Instant.now())));
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		// When: one poll runs
		updater.pollAndEvict();

		// Then: exactly the bounded number of pages was processed and the cursor advanced,
		// leaving the rest for the next firing rather than monopolizing the scheduler
		InOrder order = inOrder(cacheUpdaterService);
		order.verify(cacheUpdaterService).clearCachesForClass("example.Team");
		order.verify(cacheUpdaterService).clearCachesForClass("example.Role");
		order.verifyNoMoreInteractions();
		assertThat(updater.getLastProcessedSequence()).isEqualTo(2L);
	}

	@Test
	void shouldStopEarlyOnAPartialPageTest() {
		// Given: a page that comes back short, meaning the backlog is drained
		CacheManagementProperties properties = new CacheManagementProperties();
		properties.setBatchSize(2);
		properties.setMaxBatchesPerPoll(5);
		when(eventService.updatesAfter(0L, 2)).thenReturn(List.of(
				new CacheInvalidationEvent(4L, "example.Team", Instant.now())));
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		// When: one poll runs
		updater.pollAndEvict();

		// Then: no second query is issued for a backlog that is already empty
		verify(eventService).updatesAfter(0L, 2);
		verifyNoMoreInteractions(eventService);
		assertThat(updater.getLastProcessedSequence()).isEqualTo(4L);
	}

	@Test
	void shouldDoNothingWhenNoEventsTest() {
		CacheManagementProperties properties = new CacheManagementProperties();
		when(eventService.updatesAfter(0L, properties.getBatchSize())).thenReturn(List.of());
		ScheduledCacheUpdater updater =
				new ScheduledCacheUpdater(eventService, cacheUpdaterService, properties);

		updater.pollAndEvict();

		verifyNoInteractions(cacheUpdaterService);
		assertThat(updater.getLastProcessedSequence()).isZero();
	}
}
