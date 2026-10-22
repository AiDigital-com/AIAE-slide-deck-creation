package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerAssemblyService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerDeckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Google Drive implementation of {@link OnePagerDeckService}: resolves the user's Google access
 * token through Clerk, exports the master as PPTX, assembles the page in memory, and uploads the
 * result as a new Slides file.
 */
@Service
@RequiredArgsConstructor
public class GoogleOnePagerDeckServiceImpl implements OnePagerDeckService {

	private static final Set<GoogleOAuthScope> REQUIRED_SCOPES =
			Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES);
	private static final String EXPORT_ACTION = "read the one-pager master template";
	private static final String UPLOAD_ACTION = "save the one-pager to your Google Drive";
	private static final String DECK_URL_PREFIX = "https://docs.google.com/presentation/d/";
	private static final String DECK_URL_SUFFIX = "/edit";

	private final OnePagerProperties properties;
	private final GoogleGrantService googleGrantService;
	private final GoogleTemplateFileGateway templateFileGateway;
	private final OnePagerAssemblyService assemblyService;

	@Override
	public String createDeck(String userId, String deckTitle, OnePagerShell shell, OnePagerDraft content) {
		String accessToken = googleGrantService.requireAccessToken(
				properties.getClerkSecretKey(), userId, REQUIRED_SCOPES);
		byte[] master = templateFileGateway.exportPresentation(accessToken,
				properties.getTemplatePresentationId(), EXPORT_ACTION);
		byte[] page = assemblyService.assemble(master, shell, content);
		String fileId = templateFileGateway.uploadPresentation(accessToken, deckTitle, page, UPLOAD_ACTION);
		return DECK_URL_PREFIX + fileId + DECK_URL_SUFFIX;
	}
}
