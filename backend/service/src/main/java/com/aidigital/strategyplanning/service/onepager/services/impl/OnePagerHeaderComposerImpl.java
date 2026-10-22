package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerRunStyle;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerHeaderComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextFitter;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerTextWriter;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Service;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Default implementation of {@link OnePagerHeaderComposer}.
 *
 * <p>Title lines are measured against the box this shell actually has, in the master's title
 * font: a flat character cap cannot work because shell 3's title box is under half the width of
 * shell 1's, and a real page overflowed its title into the module below it exactly this way.
 */
@Service
@RequiredArgsConstructor
public class OnePagerHeaderComposerImpl implements OnePagerHeaderComposer {

	/** The title box is authored to fit this many lines; its height scales for 1 or 3. */
	public static final int TITLE_DESIGN_LINES = 2;

	/** Tolerance in EMU deciding whether supporting copy sits below the title rather than beside it. */
	public static final long STACKED_LAYOUT_TOLERANCE_EMU = 60_000L;

	/** Fallback used only if the title run carries no explicit font size. */
	public static final double DEFAULT_TITLE_FONT_SIZE = 41.0;

	private final OnePagerShapeEditor shapes;
	private final OnePagerTextWriter textWriter;
	private final OnePagerTextFitter textFitter;

	@Override
	public long apply(OnePagerCanvas canvas, OnePagerShell shell, OnePagerDraft content) {
		textWriter.setSingleLine((XSLFTextShape) shapes.findShapeById(canvas.slide(), shell.getTopicPillTextId()),
				content.topicPill());
		XSLFTextShape title = (XSLFTextShape) shapes.findShapeById(canvas.slide(), shell.getTitleId());
		XSLFTextShape copy = (XSLFTextShape) shapes.findShapeById(canvas.slide(), shell.getSupportingCopyId());
		Rectangle2D titleAnchor = title.getAnchor();
		long originalTitleBottom = shapes.bottomEmu(title);
		boolean copyStackedBelowTitle = shapes.topEmu(copy) >= originalTitleBottom - STACKED_LAYOUT_TOLERANCE_EMU;

		List<String> safeTitle = fitTitleLines(title, content.mainTitle());
		textWriter.setLines(title, safeTitle);
		int lines = Math.max(1, safeTitle.size());
		double newTitleHeight = titleAnchor.getHeight() * lines / (double) TITLE_DESIGN_LINES;
		shapes.setAnchor(title, new Rectangle2D.Double(titleAnchor.getX(), titleAnchor.getY(),
				titleAnchor.getWidth(), newTitleHeight));
		if (copyStackedBelowTitle) {
			shapes.moveBy(copy, 0, shapes.bottomEmu(title) - originalTitleBottom);
		}
		textWriter.setSingleLine(copy, content.supportingCopy());

		XSLFShape headerBackground = shapes.findShapeById(canvas.slide(), shell.getHeaderBackgroundBottomId());
		return Math.max(Math.max(shapes.bottomEmu(title), shapes.bottomEmu(copy)), shapes.bottomEmu(headerBackground))
				+ OnePagerLayout.HEADER_GAP_EMU;
	}

	/**
	 * Fits every title line to the title box's usable width in the box's own font.
	 *
	 * @param title the shell's title shape
	 * @param lines drafted title lines, may be null
	 * @return the fitted lines, or a single empty line when none were drafted
	 */
	List<String> fitTitleLines(XSLFTextShape title, List<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return List.of("");
		}
		OnePagerRunStyle style = textWriter.captureStyle(title);
		double fontSize = style.fontSize() != null ? style.fontSize() : DEFAULT_TITLE_FONT_SIZE;
		double usableWidth = title.getAnchor().getWidth() - title.getLeftInset() - title.getRightInset();
		return lines.stream()
				.map(line -> textFitter.fitToWidth(line, usableWidth, style.fontFamily(), fontSize))
				.toList();
	}
}
