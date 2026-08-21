package com.aidigital.strategyplanning.service.cache;

import com.aidigital.strategyplanning.cachemanagement.registry.CacheNamesByClassRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Application-owned mapping from mutation sources to every affected Hibernate
 * L2, query-cache, and Spring cache region.
 *
 * <p>Empty until a read path is measured as a cache candidate. Add an entry when
 * caching is introduced; region names must match {@code ehcache.xml} exactly, and
 * the owning service must publish an invalidation event in the same transaction
 * as the mutation.
 */
@Component
public class ApplicationCacheNamesByClassRegistry implements CacheNamesByClassRegistry {

	@Override
	public Map<Class<?>, List<String>> cacheNamesByClassMap() {
		return Map.of();
	}
}
