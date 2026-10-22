package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.common.google.GoogleGrantService;
import com.aidigital.strategyplanning.service.common.google.GoogleTemplateFileGateway;
import com.aidigital.strategyplanning.service.common.google.models.GoogleOAuthScope;
import com.aidigital.strategyplanning.service.onepager.OnePagerFixtures;
import com.aidigital.strategyplanning.service.onepager.config.OnePagerProperties;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerAssemblyService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the one-pager deck flow: grant, export, assemble, upload.
 */
class GoogleOnePagerDeckServiceImplTest {

	private static final String TOKEN = "ya29.token";
	private static final String EXPORT_ACTION = "read the one-pager master template";
	private static final String UPLOAD_ACTION = "save the one-pager to your Google Drive";

	private OnePagerProperties properties() {
		OnePagerProperties properties = new OnePagerProperties();
		properties.setClerkSecretKey("sk_test");
		properties.setTemplatePresentationId("master_1");
		return properties;
	}

	@Test
	void shouldExportAssembleUploadAndReturnTheEditUrlTest() {
		// Given: a granted user, a master export, an assembly, and a Drive that uploads
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES)))
				.thenReturn(TOKEN);
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);
		byte[] master = {1, 2, 3};
		byte[] page = {4, 5, 6};
		when(gateway.exportPresentation(TOKEN, "master_1", EXPORT_ACTION)).thenReturn(master);
		OnePagerAssemblyService assembly = mock(OnePagerAssemblyService.class);
		OnePagerDraft content = OnePagerFixtures.headerOnly();
		when(assembly.assemble(master, OnePagerShell.TOP_BAR, content)).thenReturn(page);
		when(gateway.uploadPresentation(TOKEN, "Page", page, UPLOAD_ACTION)).thenReturn("new_1");

		// When: the page is created
		String url = new GoogleOnePagerDeckServiceImpl(properties(), grants, gateway, assembly)
				.createDeck("user_1", "Page", OnePagerShell.TOP_BAR, content);

		// Then: the user gets an editable link to the uploaded file
		assertThat(url).isEqualTo("https://docs.google.com/presentation/d/new_1/edit");
		verify(gateway).uploadPresentation(TOKEN, "Page", page, UPLOAD_ACTION);
	}

	@Test
	void shouldNotTouchGoogleWhenTheUserHasNoGrantTest() {
		// Given: a grant lookup that rejects the user
		GoogleGrantService grants = mock(GoogleGrantService.class);
		when(grants.requireAccessToken("sk_test", "user_1", Set.of(GoogleOAuthScope.DRIVE, GoogleOAuthScope.SLIDES)))
				.thenThrow(new AppException(ErrorReason.C003, "Not signed in with Google"));
		GoogleTemplateFileGateway gateway = mock(GoogleTemplateFileGateway.class);
		OnePagerAssemblyService assembly = mock(OnePagerAssemblyService.class);

		// When-Then: the rejection surfaces and nothing is exported or assembled
		assertThatThrownBy(() -> new GoogleOnePagerDeckServiceImpl(properties(), grants, gateway, assembly)
				.createDeck("user_1", "Page", OnePagerShell.BLUE_HEADER, OnePagerFixtures.headerOnly()))
				.isInstanceOf(AppException.class)
				.hasMessageContaining("Not signed in with Google");
		verify(gateway, never()).exportPresentation(anyString(), anyString(), anyString());
		verifyNoInteractions(assembly);
	}
}
