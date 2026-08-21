package com.aidigital.strategyplanning.domain.rndrequest.repositories;

import com.aidigital.strategyplanning.domain.rndrequest.entities.RndRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RndRequestEntity}.
 */
public interface RndRequestRepository extends JpaRepository<RndRequestEntity, Long> {

	/**
	 * Finds all RnD requests created by a specific user, ordered by creation date descending.
	 *
	 * @param createdBy Clerk user ID of the creator
	 * @return list of RnD requests for that user
	 */
	List<RndRequestEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

	/**
	 * Finds an RnD request by ID and creator, enforcing ownership.
	 *
	 * @param id        RnD request identifier
	 * @param createdBy Clerk user ID of the creator
	 * @return matching RnD request if it exists and belongs to the user
	 */
	Optional<RndRequestEntity> findByIdAndCreatedBy(Long id, String createdBy);
}
