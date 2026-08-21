package com.aidigital.strategyplanning.service.rndrequest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration properties for the RnD Request Triage tool.
 * Binds from {@code app.rnd-request.*} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.rnd-request")
public class RndRequestProperties {

	/**
	 * Deal size ("the buy") in USD at or above which a request is escalated to RnD.
	 */
	private BigDecimal escalationThreshold = new BigDecimal("100000");

	/**
	 * Slite API key used for capability search across the Slite knowledge base.
	 * Blank means the Slite integration is not connected.
	 */
	private String sliteApiKey = "";

	/**
	 * Asana access token used for capability search across Asana projects.
	 * Blank means the Asana integration is not connected.
	 */
	private String asanaAccessToken = "";

	/**
	 * Returns the escalation threshold in USD.
	 *
	 * @return threshold amount at or above which requests escalate to RnD
	 */
	public BigDecimal getEscalationThreshold() {
		return escalationThreshold;
	}

	/**
	 * Sets the escalation threshold in USD.
	 *
	 * @param escalationThreshold threshold amount to set
	 */
	public void setEscalationThreshold(BigDecimal escalationThreshold) {
		this.escalationThreshold = escalationThreshold;
	}

	/**
	 * Returns the Slite API key.
	 *
	 * @return Slite API key, blank when not connected
	 */
	public String getSliteApiKey() {
		return sliteApiKey;
	}

	/**
	 * Sets the Slite API key.
	 *
	 * @param sliteApiKey Slite API key to set
	 */
	public void setSliteApiKey(String sliteApiKey) {
		this.sliteApiKey = sliteApiKey;
	}

	/**
	 * Returns the Asana access token.
	 *
	 * @return Asana access token, blank when not connected
	 */
	public String getAsanaAccessToken() {
		return asanaAccessToken;
	}

	/**
	 * Sets the Asana access token.
	 *
	 * @param asanaAccessToken Asana access token to set
	 */
	public void setAsanaAccessToken(String asanaAccessToken) {
		this.asanaAccessToken = asanaAccessToken;
	}
}
