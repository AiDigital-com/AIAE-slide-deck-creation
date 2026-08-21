package com.aidigital.strategyplanning.domain.casestudy.repositories;

import com.aidigital.strategyplanning.domain.casestudy.entities.CaseStudyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CaseStudyEntity}.
 */
public interface CaseStudyRepository extends JpaRepository<CaseStudyEntity, Long> {

	/**
	 * Finds all case studies created by a specific user, ordered by creation date descending.
	 *
	 * @param createdBy Clerk user ID of the creator
	 * @return list of case studies for that user
	 */
	List<CaseStudyEntity> findByCreatedByOrderByCreatedAtDesc(String createdBy);

	/**
	 * Finds a case study by ID and creator, enforcing ownership.
	 *
	 * @param id        case study identifier
	 * @param createdBy Clerk user ID of the creator
	 * @return matching case study if it exists and belongs to the user
	 */
	Optional<CaseStudyEntity> findByIdAndCreatedBy(Long id, String createdBy);
}
