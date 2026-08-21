package com.aidigital.strategyplanning.service.categoryanalysis.models;

/**
 * Connection status for the Standard Category Analysis deck generator.
 *
 * @param aiConnected     true when the AI drafting engine (OpenAI API key) is configured
 * @param googleConnected true when the user's Google account grants Drive/Slides access via Clerk SSO
 * @param googleStatus    detailed Google connection state distinguishing "not signed in with Google"
 *                        from "signed in but missing Drive/Slides scopes"
 */
public record StandardConnections(
		boolean aiConnected,
		boolean googleConnected,
		GoogleConnectionStatus googleStatus
) {

}
