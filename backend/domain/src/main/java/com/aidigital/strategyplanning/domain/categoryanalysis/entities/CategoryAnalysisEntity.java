package com.aidigital.strategyplanning.domain.categoryanalysis.entities;

import com.aidigital.strategyplanning.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistence model for a category analysis created via the Category Analysis Builder tool.
 */
@Entity
@Table(name = "category_analyses")
@Getter
@Setter
@NoArgsConstructor
public class CategoryAnalysisEntity extends IdAwareEntity {

	@Column(nullable = false)
	private String title;

	@Column
	private String category;

	@Column(name = "market_overview", columnDefinition = "TEXT")
	private String marketOverview;

	@Column(name = "key_players", columnDefinition = "TEXT")
	private String keyPlayers;

	@Column(columnDefinition = "TEXT")
	private String trends;

	@Column(columnDefinition = "TEXT")
	private String opportunities;

	@Column(columnDefinition = "TEXT")
	private String recommendations;

	@Column(name = "source_file_names", columnDefinition = "TEXT")
	private String sourceFileNames;

	@Column(name = "slides_url", columnDefinition = "TEXT")
	private String slidesUrl;

	@Column(name = "template_kind", columnDefinition = "TEXT")
	private String templateKind;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
