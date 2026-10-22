package com.aidigital.strategyplanning.service.onepager.models;

import java.util.List;

/**
 * Complete drafted (or reviewed/edited) content for one One Page Generator page.
 *
 * <p>Every field maps onto a fixed shape of the template (header, footer, and the body modules).
 * A body module renders when its content is non-empty: an empty {@code processSteps} list omits
 * the tracker, a null {@code keyTakeaway} omits that bar, and so on. {@code moduleOrder} only
 * decides where a module is placed, never whether it renders.
 *
 * @param topicPill             short header pill label, e.g. "AUTOMOTIVE"
 * @param mainTitle             header title, one string per line (up to 3)
 * @param supportingCopy        header supporting sentence
 * @param moduleOrder           stacking order of the body modules, top to bottom
 * @param processSectionLabel   label above the process tracker, null when there are no steps
 * @param processSteps          up to 5 short step words
 * @param solutionsSectionLabel label above the core-solution cards, null when there are none
 * @param solutions             up to 5 core-solution cards
 * @param solutionsVisualLabel  label of the companion visual beside the solution cards, or null
 * @param kpiSectionLabel       label above the KPI tiles, null when there are none
 * @param kpis                  up to 4 KPI tiles
 * @param kpiDisclaimer         disclaimer under the KPI tiles, or null
 * @param imageTextSectionLabel title of the image-and-text block, null when the block is omitted
 * @param imageTextBody         paragraph of the image-and-text block, or null to omit it
 * @param keyTakeaway           callout sentence of the key-takeaway bar, or null to omit it
 * @param impactStats           0 or 3 impact stats
 * @param capabilityCards       0 or 3 capability cards
 * @param spotlightCards        0 or 3 spotlight cards
 * @param insightCards          0 or 3 icon-and-insight cards
 * @param milestoneHeadline     headline of the milestone banner, null when there are no milestones
 * @param milestones            0 or 3 numbered milestone steps
 */
public record OnePagerDraft(
		String topicPill,
		List<String> mainTitle,
		String supportingCopy,
		List<OnePagerModuleKey> moduleOrder,
		String processSectionLabel,
		List<String> processSteps,
		String solutionsSectionLabel,
		List<OnePagerSolution> solutions,
		String solutionsVisualLabel,
		String kpiSectionLabel,
		List<OnePagerKpi> kpis,
		String kpiDisclaimer,
		String imageTextSectionLabel,
		String imageTextBody,
		String keyTakeaway,
		List<OnePagerStat> impactStats,
		List<OnePagerCapabilityCard> capabilityCards,
		List<OnePagerSpotlightCard> spotlightCards,
		List<OnePagerInsight> insightCards,
		String milestoneHeadline,
		List<OnePagerMilestone> milestones) {

}
