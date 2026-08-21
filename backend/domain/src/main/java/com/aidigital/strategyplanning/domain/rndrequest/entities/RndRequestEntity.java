package com.aidigital.strategyplanning.domain.rndrequest.entities;

import com.aidigital.strategyplanning.domain.common.entities.IdAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence model for a triaged RnD request created via the RnD Request Triage tool.
 */
@Entity
@Table(name = "rnd_requests")
@Getter
@Setter
@NoArgsConstructor
public class RndRequestEntity extends IdAwareEntity {

	@Column(nullable = false)
	private String title;

	@Column(name = "requester_team")
	private String requesterTeam;

	@Column(name = "request_details", columnDefinition = "TEXT")
	private String requestDetails;

	@Column(name = "capability_notes", columnDefinition = "TEXT")
	private String capabilityNotes;

	@Column(name = "buy_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal buyAmount;

	@Column(nullable = false)
	private String decision;

	@Column(name = "capability_summary", columnDefinition = "TEXT")
	private String capabilitySummary;

	@Column(name = "response_draft", columnDefinition = "TEXT")
	private String responseDraft;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_by", nullable = false)
	private String createdBy;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
