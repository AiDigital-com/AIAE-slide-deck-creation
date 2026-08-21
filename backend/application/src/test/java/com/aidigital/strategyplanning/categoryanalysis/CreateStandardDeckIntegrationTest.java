package com.aidigital.strategyplanning.categoryanalysis;

import com.aidigital.strategyplanning.domain.categoryanalysis.entities.CategoryAnalysisEntity;
import com.aidigital.strategyplanning.domain.categoryanalysis.repositories.CategoryAnalysisRepository;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CategoryAnalysisRecord;
import com.aidigital.strategyplanning.service.categoryanalysis.models.CreateStandardDeckCommand;
import com.aidigital.strategyplanning.service.categoryanalysis.models.StandardFieldValue;
import com.aidigital.strategyplanning.service.categoryanalysis.services.CategoryAnalysisService;
import com.aidigital.strategyplanning.service.categoryanalysis.services.GoogleDeckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test for the Standard deck creation flow.
 *
 * <p>Exercises the real {@link CategoryAnalysisService} against a real JPA
 * repository (H2, test profile), mocking only the external Google Slides call.
 * Confirms that {@code createStandardDeck} persists the returned {@code slidesUrl},
 * {@code templateKind} and {@code status} — the fields the "Create Google Slides
 * deck" button depends on.
 */
@SpringBootTest
@ActiveProfiles("test")
class CreateStandardDeckIntegrationTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private GoogleDeckService googleDeckService;

	@Autowired
	private CategoryAnalysisService categoryAnalysisService;

	@Autowired
	private CategoryAnalysisRepository categoryAnalysisRepository;

	@Test
	void shouldPersistSlidesUrlTemplateKindAndStatusTest() {
		String expectedUrl = "https://docs.google.com/presentation/d/deck123/edit";
		when(googleDeckService.createDeck(eq("user_123"), anyString(), any(), any(), any()))
				.thenReturn(expectedUrl);

		CreateStandardDeckCommand command = new CreateStandardDeckCommand(
				"Beverages",
				"Acme",
				List.of(new StandardFieldValue("trends_headline", "Growth is accelerating")),
				"user_123");

		CategoryAnalysisRecord record = categoryAnalysisService.createStandardDeck(command, "https://example.com");

		assertThat(record.id()).isNotNull();
		assertThat(record.slidesUrl()).isEqualTo(expectedUrl);
		assertThat(record.templateKind()).isEqualTo("STANDARD");
		assertThat(record.status()).isEqualTo("SUBMITTED");
		assertThat(record.title()).isEqualTo("Category Analysis — Beverages — Acme");

		CategoryAnalysisEntity persisted =
				categoryAnalysisRepository.findByIdAndCreatedBy(record.id(), "user_123").orElseThrow();
		assertThat(persisted.getSlidesUrl()).isEqualTo(expectedUrl);
		assertThat(persisted.getTemplateKind()).isEqualTo("STANDARD");
		assertThat(persisted.getStatus()).isEqualTo("SUBMITTED");
		assertThat(persisted.getCategory()).isEqualTo("Beverages");
		assertThat(persisted.getCreatedBy()).isEqualTo("user_123");
		assertThat(persisted.getCreatedAt()).isNotNull();
	}
}
