package com.aidigital.strategyplanning.service.rfpoutline.services.impl;

import com.aidigital.strategyplanning.domain.rfpoutline.entities.RfpOutlineEntity;
import com.aidigital.strategyplanning.domain.rfpoutline.repositories.RfpOutlineRepository;
import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.time.CurrentTimeImpl;
import com.aidigital.strategyplanning.service.mappers.rfpoutline.RfpOutlineMapper;
import com.aidigital.strategyplanning.service.rfpoutline.models.CreateRfpOutlineCommand;
import com.aidigital.strategyplanning.service.rfpoutline.models.RfpOutlineRecord;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDocService;
import com.aidigital.strategyplanning.service.rfpoutline.services.RfpOutlineDraftService;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the RFP outline aggregate service.
 *
 * <p>The Google Doc is exported before the row is saved, so a saved outline always has a document
 * behind its link.
 */
class RfpOutlineServiceImplTest {

	/**
	 * Builds a create command with the given title.
	 *
	 * @param title outline title
	 * @return validated create command
	 */
	private CreateRfpOutlineCommand command(String title) {
		return new CreateRfpOutlineCommand(title, "Globex", "Logistics",
				"Fragmented tracking.", "Unified visibility.", "One control tower.",
				"On-time delivery up.", "1. Context", null, null, "user_1");
	}

	@Test
	void shouldExportTheDocThenSaveTheRowTest() {
		// Given: a reviewed outline and a Google flow that exports a document
		RfpOutlineRepository repository = mock(RfpOutlineRepository.class);
		RfpOutlineDraftService draftService = mock(RfpOutlineDraftService.class);
		RfpOutlineDocService docService = mock(RfpOutlineDocService.class);
		CreateRfpOutlineCommand command = command("Globex RFP");
		Map<String, String> tokens = Map.of("scope", "Discovery");
		when(draftService.buildTemplateTokenValues(command)).thenReturn(tokens);
		when(docService.createDoc("user_1", "Globex RFP", tokens))
				.thenReturn("https://docs.google.com/document/d/doc_1/edit");
		when(repository.save(org.mockito.ArgumentMatchers.<RfpOutlineEntity>any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		// When: the outline is created
		RfpOutlineRecord record = new RfpOutlineServiceImpl(repository,
				Mappers.getMapper(RfpOutlineMapper.class), draftService, docService,
				new CurrentTimeImpl()).create(command);

		// Then: the exported doc URL is persisted with the row
		assertThat(record.docUrl()).isEqualTo("https://docs.google.com/document/d/doc_1/edit");
		assertThat(record.status()).isEqualTo("GENERATED");
		assertThat(record.createdAt()).isNotNull();
	}

	@Test
	void shouldNotSaveARowWhenTheDocCouldNotBeExportedTest() {
		// Given: a Google flow that rejects the export
		RfpOutlineRepository repository = mock(RfpOutlineRepository.class);
		RfpOutlineDraftService draftService = mock(RfpOutlineDraftService.class);
		RfpOutlineDocService docService = mock(RfpOutlineDocService.class);
		CreateRfpOutlineCommand command = command("Globex RFP");
		Map<String, String> tokens = Map.of("scope", "Discovery");
		when(draftService.buildTemplateTokenValues(command)).thenReturn(tokens);
		when(docService.createDoc("user_1", "Globex RFP", tokens))
				.thenThrow(new IllegalStateException("Google refused"));

		// When-Then: nothing is persisted, so no outline links to a document that never existed
		assertThatThrownBy(() -> new RfpOutlineServiceImpl(repository,
				Mappers.getMapper(RfpOutlineMapper.class), draftService, docService,
				new CurrentTimeImpl()).create(command))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(repository);
	}

	@Test
	void shouldListOnlyTheCallersOwnOutlinesNewestFirstTest() {
		// Given: the caller's saved outlines as the repository orders them
		RfpOutlineRepository repository = mock(RfpOutlineRepository.class);
		RfpOutlineEntity newer = new RfpOutlineEntity();
		newer.setTitle("Newer");
		RfpOutlineEntity older = new RfpOutlineEntity();
		older.setTitle("Older");
		when(repository.findByCreatedByOrderByCreatedAtDesc("user_1"))
				.thenReturn(List.of(newer, older));

		// When: the list is requested
		List<RfpOutlineRecord> records = new RfpOutlineServiceImpl(repository,
				Mappers.getMapper(RfpOutlineMapper.class), mock(RfpOutlineDraftService.class),
				mock(RfpOutlineDocService.class), new CurrentTimeImpl()).listByUser("user_1");

		// Then: the query is scoped to the caller and the order is preserved
		assertThat(records).extracting(RfpOutlineRecord::title).containsExactly("Newer", "Older");
		verify(repository).findByCreatedByOrderByCreatedAtDesc("user_1");
	}

	@Test
	void shouldReturnAnOutlineOwnedByTheCallerTest() {
		// Given: an outline that belongs to the caller
		RfpOutlineRepository repository = mock(RfpOutlineRepository.class);
		RfpOutlineEntity entity = new RfpOutlineEntity();
		entity.setId(9L);
		entity.setTitle("Globex RFP");
		when(repository.findByIdAndCreatedBy(9L, "user_1")).thenReturn(Optional.of(entity));

		// When: it is fetched by id
		RfpOutlineRecord record = new RfpOutlineServiceImpl(repository,
				Mappers.getMapper(RfpOutlineMapper.class), mock(RfpOutlineDraftService.class),
				mock(RfpOutlineDocService.class), new CurrentTimeImpl()).getById(9L, "user_1");

		// Then: the caller gets their own row
		assertThat(record.id()).isEqualTo(9L);
		assertThat(record.title()).isEqualTo("Globex RFP");
	}

	@Test
	void shouldNotRevealAnotherUsersOutlineTest() {
		// Given: an id that exists but belongs to someone else
		RfpOutlineRepository repository = mock(RfpOutlineRepository.class);
		when(repository.findByIdAndCreatedBy(9L, "user_2")).thenReturn(Optional.empty());

		// When-Then: "not found" rather than "forbidden", which would confirm the row exists
		assertThatThrownBy(() -> new RfpOutlineServiceImpl(repository,
				Mappers.getMapper(RfpOutlineMapper.class), mock(RfpOutlineDraftService.class),
				mock(RfpOutlineDocService.class), new CurrentTimeImpl()).getById(9L, "user_2"))
				.isInstanceOf(AppException.class)
				.satisfies(e -> assertThat(((AppException) e).getValidationMessage().getCode())
						.isEqualTo("C001"));
	}
}
