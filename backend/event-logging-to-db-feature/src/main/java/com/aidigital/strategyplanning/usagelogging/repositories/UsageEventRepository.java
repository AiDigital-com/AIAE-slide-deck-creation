package com.aidigital.strategyplanning.usagelogging.repositories;

import com.aidigital.strategyplanning.usagelogging.entities.UsageEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for persisted usage logging events.
 */
public interface UsageEventRepository extends JpaRepository<UsageEventEntity, Long> {

}
