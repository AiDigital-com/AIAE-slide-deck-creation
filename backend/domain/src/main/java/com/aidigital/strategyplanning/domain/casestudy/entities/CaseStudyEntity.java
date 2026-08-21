package com.aidigital.strategyplanning.domain.casestudy.entities;

import com.aidigital.strategyplanning.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistence model for a case study created via the Case Study Builder tool.
 */
@Entity
@Table(name = "case_studies")
@Getter
@Setter
@NoArgsConstructor
public class CaseStudyEntity extends IdAwareEntity {

	@Column(nullable = false)
	private String title;

	@Column(name = "client_name")
	private String clientName;

	@Column
	private String industry;

	@Column(columnDefinition = "TEXT")
	private String challenge;

	@Column(columnDefinition = "TEXT")
	private String solution;

	@Column(columnDefinition = "TEXT")
	private String results;

	@Column(name = "key_metrics", columnDefinition = "TEXT")
	private String keyMetrics;

	@Column
	private String timeline;

	@Column(columnDefinition = "TEXT")
	private String testimonial;

	@Column(name = "source_file_name")
	private String sourceFileName;

	@Column(name = "template_url")
	private String templateUrl;

	@Column(name = "slides_url")
	private String slidesUrl;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
