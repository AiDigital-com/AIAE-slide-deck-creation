package com.aidigital.strategyplanning.domain.rfpoutline.repositories;

import com.aidigital.strategyplanning.domain.rfpoutline.entities.RfpOutlineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RfpOutlineEntity}.
 */
public interface RfpOutlineRepository extends JpaRepository<RfpOutlineEntity, Long> {

	/**
	 * Finds all RFP outlines created by a specific user, ordered by creation date descending.
	 *
	 * @param createdBy Clerk user ID of the creator
	 * @return list of RFP outlines for that user
	 */
	List<RfpOutlineEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

	/**
	 * Finds a single RFP outline owned by a specific user.
	 *
	 * @param id        RFP outline identifier
	 * @param createdBy Clerk user ID of the creator
	 * @return the outline when it exists and belongs to that user, otherwise empty
	 */
	Optional<RfpOutlineEntity> findByIdAndCreatedBy(Long id, String createdBy);
}
