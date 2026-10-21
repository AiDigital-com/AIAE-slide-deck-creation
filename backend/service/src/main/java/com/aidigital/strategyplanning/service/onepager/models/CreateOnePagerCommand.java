package com.aidigital.strategyplanning.service.onepager.models;

/**
 * Reviewed content and creator for a One Page Generator page-creation request.
 *
 * @param category     market category or vertical the page targets; part of the file title
 * @param clientName   label used in the generated file's title
 * @param shellVariant which header/footer shell to build onto
 * @param content      final reviewed page content
 * @param createdBy    Clerk user ID of the requester
 */
public record CreateOnePagerCommand(String category, String clientName, OnePagerShell shellVariant,
									OnePagerDraft content, String createdBy) {

}
