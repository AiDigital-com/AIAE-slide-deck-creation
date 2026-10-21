package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.common.error.AppException;
import com.aidigital.strategyplanning.service.common.error.ErrorReason;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerAssemblyContext;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerShell;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerAssemblyService;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerHeaderComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerPageSizer;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import com.aidigital.strategyplanning.service.onepager.templates.OnePagerLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Apache POI implementation of {@link OnePagerAssemblyService}: opens the exported master, picks
 * the shell, fills its header, clones every drafted module in stacking order, sizes the page, and
 * drops the master's other slides so exactly one finished page remains.
 */
@Service
public class OnePagerAssemblyServiceImpl implements OnePagerAssemblyService {

	/** Stacking order used when the draft carries none. */
	public static final List<OnePagerModuleKey> DEFAULT_MODULE_ORDER = List.of(
			OnePagerModuleKey.TRACKER, OnePagerModuleKey.SOLUTIONS, OnePagerModuleKey.KPIS);

	private static final String ASSEMBLY_FAILURE = "Could not assemble the one-pager: ";

	private final OnePagerShapeEditor shapes;
	private final OnePagerHeaderComposer headerComposer;
	private final OnePagerPageSizer pageSizer;
	private final Map<OnePagerModuleKey, OnePagerModuleComposer> composers = new EnumMap<>(OnePagerModuleKey.class);

	/**
	 * Wires the assembly with its collaborators, indexing one composer per module key.
	 *
	 * @param shapes          shape editor
	 * @param headerComposer  header composer
	 * @param pageSizer       page sizer
	 * @param moduleComposers one composer per body module
	 */
	public OnePagerAssemblyServiceImpl(OnePagerShapeEditor shapes, OnePagerHeaderComposer headerComposer,
			OnePagerPageSizer pageSizer, List<OnePagerModuleComposer> moduleComposers) {
		this.shapes = shapes;
		this.headerComposer = headerComposer;
		this.pageSizer = pageSizer;
		moduleComposers.forEach(composer -> composers.put(composer.key(), composer));
	}

	@Override
	public byte[] assemble(byte[] masterPptxBytes, OnePagerShell shell, OnePagerDraft content) {
		try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(masterPptxBytes))) {
			List<XSLFSlide> slides = show.getSlides();
			OnePagerCanvas canvas = new OnePagerCanvas(slides.get(shell.getSlideIndex()));
			for (int removeId : shell.getRemoveOnUse()) {
				shapes.removeShape(shapes.findShapeById(canvas.slide(), removeId));
			}
			OnePagerAssemblyContext context = new OnePagerAssemblyContext(canvas,
					slides.get(OnePagerLayout.BODY_SLIDE_INDEX), slides.get(OnePagerLayout.KPI_SLIDE_INDEX),
					slides.get(OnePagerLayout.INSIGHT_SLIDE_INDEX), content);
			long cursor = headerComposer.apply(canvas, shell, content);
			for (OnePagerModuleKey key : moduleOrder(content)) {
				cursor = applyModule(context, key, cursor);
			}
			pageSizer.fitPageToContent(show, canvas, shell, cursor);
			shapes.removeAllSlidesExcept(show, canvas.slide());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			show.write(out);
			return out.toByteArray();
		} catch (IOException | RuntimeException e) {
			// POI reports a non-OOXML master and a missing shape as runtime failures; both mean the
			// master is not the template this feature was built against, which the user must hear.
			throw new AppException(ErrorReason.C003, e, ASSEMBLY_FAILURE + e.getMessage());
		}
	}

	/**
	 * Returns the stacking order to assemble in: the draft's own, or the default when it has none.
	 *
	 * @param content drafted page content
	 * @return module keys in stacking order
	 */
	List<OnePagerModuleKey> moduleOrder(OnePagerDraft content) {
		return content.moduleOrder() == null || content.moduleOrder().isEmpty()
				? DEFAULT_MODULE_ORDER : content.moduleOrder();
	}

	/**
	 * Clones one module at the cursor. Keys without a composer (the solutions visual, which the
	 * solutions composer renders beside its cards) leave the cursor unchanged.
	 *
	 * @param context canvas, source slides, and content
	 * @param key     module to clone
	 * @param cursor  y position in EMU where the module starts
	 * @return the y position in EMU for the next module
	 */
	long applyModule(OnePagerAssemblyContext context, OnePagerModuleKey key, long cursor) {
		OnePagerModuleComposer composer = composers.get(key);
		return composer == null ? cursor : composer.apply(context, cursor);
	}
}
