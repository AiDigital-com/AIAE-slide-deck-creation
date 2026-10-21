package com.aidigital.strategyplanning.service.onepager;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCapabilityCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerDraft;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerInsight;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerKpi;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerMilestone;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerModuleKey;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSolution;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerSpotlightCard;
import com.aidigital.strategyplanning.service.onepager.models.OnePagerStat;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerAssemblyServiceImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerCapabilityCardsModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerHeaderComposerImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerImageTextModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerImpactStatsModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerInsightCardsModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerKeyTakeawayModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerKpiModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerMilestoneModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerPageSizerImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerPhotoModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerShapeEditorImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerSolutionsModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerSpotlightCardsModuleComposer;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerTextFitterImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerTextWriterImpl;
import com.aidigital.strategyplanning.service.onepager.services.impl.OnePagerTrackerModuleComposer;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Immutable test inputs shared by the One Page Generator tests: the bundled master template and
 * a complete drafted page that exercises every module.
 */
public final class OnePagerFixtures {

	/** Classpath location of the master template, exported from the real Slides master. */
	public static final String MASTER_RESOURCE = "/onepager/one-pager-master.pptx";

	/** Every module key, in a stacking order that pairs the solutions visual with its cards. */
	public static final List<OnePagerModuleKey> ALL_MODULES = List.of(
			OnePagerModuleKey.TRACKER, OnePagerModuleKey.SOLUTIONS, OnePagerModuleKey.SOLUTIONS_VISUAL,
			OnePagerModuleKey.KPIS, OnePagerModuleKey.IMAGE_TEXT, OnePagerModuleKey.KEY_TAKEAWAY,
			OnePagerModuleKey.IMPACT_STATS, OnePagerModuleKey.CAPABILITY_CARDS, OnePagerModuleKey.SPOTLIGHT_CARDS,
			OnePagerModuleKey.PHOTO, OnePagerModuleKey.INSIGHT_CARDS, OnePagerModuleKey.MILESTONE_BANNER);

	private OnePagerFixtures() {
	}

	/**
	 * Reads the bundled master template.
	 *
	 * @return raw PPTX bytes
	 */
	public static byte[] master() {
		try (InputStream in = OnePagerFixtures.class.getResourceAsStream(MASTER_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing fixture " + MASTER_RESOURCE);
			}
			return in.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Opens PPTX bytes as a slide show.
	 *
	 * @param bytes PPTX bytes
	 * @return the opened show; the caller closes it
	 */
	public static XMLSlideShow open(byte[] bytes) {
		try {
			return new XMLSlideShow(new ByteArrayInputStream(bytes));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Wires the real assembly with all of its collaborators.
	 *
	 * @return a ready assembly service
	 */
	public static OnePagerAssemblyServiceImpl assembly() {
		OnePagerShapeEditorImpl shapes = new OnePagerShapeEditorImpl();
		OnePagerTextFitterImpl fitter = new OnePagerTextFitterImpl();
		OnePagerTextWriterImpl writer = new OnePagerTextWriterImpl(fitter);
		return new OnePagerAssemblyServiceImpl(shapes, new OnePagerHeaderComposerImpl(shapes, writer, fitter),
				new OnePagerPageSizerImpl(shapes), List.of(
						new OnePagerTrackerModuleComposer(shapes, writer),
						new OnePagerSolutionsModuleComposer(shapes, writer),
						new OnePagerKpiModuleComposer(shapes, writer),
						new OnePagerImageTextModuleComposer(shapes, writer),
						new OnePagerKeyTakeawayModuleComposer(shapes, writer),
						new OnePagerImpactStatsModuleComposer(shapes, writer),
						new OnePagerCapabilityCardsModuleComposer(shapes, writer),
						new OnePagerSpotlightCardsModuleComposer(shapes, writer),
						new OnePagerInsightCardsModuleComposer(shapes, writer),
						new OnePagerPhotoModuleComposer(shapes),
						new OnePagerMilestoneModuleComposer(shapes, writer)));
	}

	/**
	 * A drafted page with content in every module, in {@link #ALL_MODULES} order.
	 *
	 * @return the draft
	 */
	public static OnePagerDraft automotiveExample() {
		return automotiveExample(ALL_MODULES);
	}

	/**
	 * A drafted page with content in every module, stacked in the given order.
	 *
	 * @param moduleOrder stacking order
	 * @return the draft
	 */
	public static OnePagerDraft automotiveExample(List<OnePagerModuleKey> moduleOrder) {
		return new OnePagerDraft(
				"AUTOMOTIVE",
				List.of("SOLVING AUTOMOTIVE'S", "LOCAL MEDIA FRAGMENTATION"),
				"WITH PRECISION, SPEED, AND SALES ACCOUNTABILITY",
				moduleOrder,
				"HOW IT WORKS",
				List.of("UNDERSTAND", "PERSONALIZE", "ACTIVATE", "OPTIMIZE", "PROVE"),
				"CORE AUTOMOTIVE SOLUTIONS",
				List.of(
						new OnePagerSolution("VIN-LEVEL DYNAMIC CREATIVE OPTIMIZATION",
								"Real-time creative built around live inventory and local demand",
								"Personalize ads using available vehicles, nearest dealership, audience signals, "
										+ "and real-time inventory feeds."),
						new OnePagerSolution("PREMIUM LOCALIZED INVENTORY",
								"Reach local auto shoppers across every screen and daily moment",
								"Activate premium geo-targeted media across the digital ecosystem to drive "
										+ "awareness, consideration, and dealership demand."),
						new OnePagerSolution("PROPRIETARY STORE INTELLIGENCE",
								"Smarter market planning based on location-level opportunity",
								"Use store intelligence and competitive-density mapping to identify growth "
										+ "markets and prioritize investment by location."),
						new OnePagerSolution("VEHICLE SALES LIFT & ATTRIBUTION",
								"Connect media exposure to real automotive outcomes",
								"Measure beyond impressions and clicks through dealership visitation and "
										+ "vehicle-sales attribution.")),
				"SUPPORTING VISUALS",
				"CONNECTS PERFORMANCE SIGNALS THAT MATTER FOR AUTOMOTIVE",
				List.of(
						new OnePagerKpi("30%", "INCREASED SCHEDULED APPOINTMENTS"),
						new OnePagerKpi("50%", "LIFT IN DEALERSHIP VISITS"),
						new OnePagerKpi("2X", "LIFT IN VEHICLE SALES IMPACT"),
						new OnePagerKpi("3X", "HIGHER MEDIA ENGAGEMENT")),
				"Based on AI Digital first-party performance data and third-party automotive industry "
						+ "benchmarks. Results may vary by campaign, market, and data availability.",
				"WHY IT WORKS FOR AUTOMOTIVE",
				"AI Digital combines first-party inventory data with real-time media activation to close "
						+ "the gap between local demand and dealership supply.",
				"Dealers using this approach saw appointment volume climb within the first 30 days.",
				List.of(
						new OnePagerStat("+45%", "FASTER LEAD RESPONSE"),
						new OnePagerStat("3.2X", "RETURN ON AD SPEND"),
						new OnePagerStat("+28%", "SERVICE APPOINTMENT VOLUME")),
				List.of(
						new OnePagerCapabilityCard("REAL-TIME INVENTORY SYNC",
								"Ads always reflect what's actually on the lot.",
								List.of("Live feed integration", "Automatic ad refresh", "Zero manual uploads"),
								"Refreshes every 15 minutes"),
						new OnePagerCapabilityCard("LOCATION-AWARE TARGETING",
								"Media spend follows verified local demand signals.",
								List.of("Store-level intelligence", "Competitive density mapping",
										"Radius-based bidding"),
								"Covers every active market"),
						new OnePagerCapabilityCard("CLOSED-LOOP ATTRIBUTION",
								"Every impression traces to a dealership outcome.",
								List.of("Visitation matching", "Sales lift studies", "Cross-channel reporting"),
								"Verified by third-party audit")),
				List.of(
						new OnePagerSpotlightCard("DEALER-READY CREATIVE",
								"Every ad reflects what's actually in stock, automatically.",
								List.of("Live inventory feed", "Auto-refreshed creative", "Zero manual uploads"),
								List.of(), List.of()),
						new OnePagerSpotlightCard("PERFORMANCE AT A GLANCE",
								"The signals that matter for automotive, in one view.",
								List.of("Appointment lift", "Dealership visits", "Sales attribution"),
								List.of(
										new OnePagerKpi("30%", "APPOINTMENTS"),
										new OnePagerKpi("50%", "DEALER VISITS"),
										new OnePagerKpi("2X", "VEHICLE SALES"),
										new OnePagerKpi("3X", "ENGAGEMENT"),
										new OnePagerKpi("15M", "FEED SYNC"),
										new OnePagerKpi("24/7", "LIVE DATA"),
										new OnePagerKpi("98%", "ACCURACY")),
								List.of()),
						new OnePagerSpotlightCard("LAUNCH IN THREE STAGES",
								"From inventory sync to measured sales lift.",
								List.of("Connect inventory", "Launch campaigns", "Measure lift"),
								List.of(), List.of("CONNECT", "LAUNCH", "MEASURE"))),
				List.of(
						new OnePagerInsight("SPEED TO MARKET",
								"New inventory goes live in ads within hours, not days."),
						new OnePagerInsight("PRECISION REACH",
								"Creative adapts automatically to nearest dealership."),
						new OnePagerInsight("PROVEN LIFT",
								"Sales attribution ties spend directly to showroom visits.")),
				"THREE MILESTONES TO LAUNCH",
				List.of(
						new OnePagerMilestone("CONNECT INVENTORY", "Sync live feeds in under a week"),
						new OnePagerMilestone("LAUNCH CAMPAIGNS", "Go live across every priority channel"),
						new OnePagerMilestone("MEASURE LIFT", "Track dealership outcomes from day one")));
	}

	/**
	 * A header-only draft: no module has content, so the page carries only the shell.
	 *
	 * @return the draft
	 */
	public static OnePagerDraft headerOnly() {
		return new OnePagerDraft("RETAIL", List.of("ONE LINE"), "Supporting copy", List.of(), null, List.of(),
				null, List.of(), null, null, List.of(), null, null, null, null, List.of(), List.of(), List.of(),
				List.of(), null, List.of());
	}
}
