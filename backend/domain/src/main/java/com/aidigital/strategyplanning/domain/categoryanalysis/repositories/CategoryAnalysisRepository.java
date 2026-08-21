package com.aidigital.strategyplanning.domain.categoryanalysis.repositories;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CategoryAnalysisEntity}.
 */
public interface CategoryAnalysisRepository extends JpaRepository<CategoryAnalysisEntity, Long> {

	/**
	 * Finds all category analyses created by a specific user, ordered by creation date descending.
	 *
	 * @param createdBy Clerk user ID of the creator
	 * @return list of category analyses for that user
	 */
	List<CategoryAnalysisEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

	/**
	 * Finds a category analysis by ID and creator, enforcing ownership.
	 *
	 * @param id        category analysis identifier
	 * @param createdBy Clerk user ID of the creator
	 * @return matching category analysis if it exists and belongs to the user
	 */
	Optional<CategoryAnalysisEntity> findByIdAndCreatedBy(Long id, String createdBy);
}
