package com.aidigital.strategyplanning.service.onepager.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * One header/footer shell of the master template (slides 1-3), with the shape ids assembly needs
 * on that slide. The header's title and supporting copy sit below a background block, so the
 * blank body starts at the lowest of that block's bottom and the text boxes' bottoms.
 *
 * <p>Shell 1's {@code headerBackgroundBottomId} points at the visible blue header block (shape 89)
 * rather than the nearly invisible off-white band stacked beneath it (shape 100): using the band
 * reserved an extra blank strip below the header with no visible purpose.
 *
 * <p>All shape ids were read from the master's OOXML and are stable only while the master keeps
 * its structure; re-derive them if the master is edited.
 */
@Getter
@RequiredArgsConstructor
public enum OnePagerShell {

	/** Shell 1: blue header block. */
	BLUE_HEADER(0, 94, 91, 92, 89, 95, Set.of()),

	/**
	 * Shell 2: large hero graphic, which leaves the least body room. Ships with a generic 3-box
	 * row that duplicates the insight-cards module in spirit, deleted before modules are added.
	 */
	HERO_GRAPHIC(1, 111, 108, 109, 106, 112, Set.of(117, 121, 122, 123, 124, 125, 126, 127, 128, 129)),

	/** Shell 3: thin top bar. */
	TOP_BAR(2, 142, 137, 138, 136, 139, Set.of());

	/** 0-based slide index of this shell in the master. */
	private final int slideIndex;

	/** Shape id of the topic pill's text. */
	private final int topicPillTextId;

	/** Shape id of the main title text (up to 3 lines). */
	private final int titleId;

	/** Shape id of the supporting-copy text. */
	private final int supportingCopyId;

	/** Shape id of the lowest header-background block, the floor for where the body may start. */
	private final int headerBackgroundBottomId;

	/** Shape id of the footer background, moved to sit flush against the fitted page bottom. */
	private final int footerBackgroundId;

	/** Shape ids of baked-in decorative content deleted from this shell before adding modules. */
	private final Set<Integer> removeOnUse;
}
