package com.aidigital.strategyplanning.domain.rfpoutline.entities;

import com.aidigital.strategyplanning.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistence model for an RFP outline created via the RFP Outline Generator tool.
 */
@Entity
@Table(name = "rfp_outlines")
@Getter
@Setter
@NoArgsConstructor
public class RfpOutlineEntity extends IdAwareEntity {

	@Column(nullable = false)
	private String title;

	@Column(name = "client_name")
	private String clientName;

	@Column
	private String industry;

	@Column(columnDefinition = "TEXT")
	private String challenge;

	@Column(columnDefinition = "TEXT")
	private String opportunity;

	@Column(columnDefinition = "TEXT")
	private String solution;

	@Column(columnDefinition = "TEXT")
	private String outcome;

	@Column(name = "deck_outline", columnDefinition = "TEXT")
	private String deckOutline;

	@Column(name = "supplementary_notes", columnDefinition = "TEXT")
	private String supplementaryNotes;

	@Column(name = "source_file_name")
	private String sourceFileName;

	@Column(name = "doc_url")
	private String docUrl;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
