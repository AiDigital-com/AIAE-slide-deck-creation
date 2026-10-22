package com.aidigital.strategyplanning.service.casestudy.templates;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconSpec;

import java.util.List;

/**
 * The brand icon library from the Category Analysis master template's Visual Elements Library
 * (slide 24). The PNGs ship with the frontend under {@code /case-study-icons/{name}.png}, where
 * Google Slides fetches them when a One-Page Case Study deck swaps a template icon.
 */
public final class OnePageIconLibrary {

	/** App-relative path the icon images are served from. */
	public static final String IMAGE_PATH = "/case-study-icons/";

	/** File extension of every icon image. */
	public static final String IMAGE_EXTENSION = ".png";

	/** Every icon in the library. */
	public static final List<OnePageIconSpec> ICONS = List.of(
			new OnePageIconSpec("heart", "Heart — loyalty, brand love, customer affinity"),
			new OnePageIconSpec("chart-sparkle-pink", "Bar chart with sparkle — standout results, reporting"),
			new OnePageIconSpec("ad", "Ad badge — advertising, ad formats, placements"),
			new OnePageIconSpec("lightning", "Lightning bolt — speed, quick wins, energy"),
			new OnePageIconSpec("sparkles-lime", "Sparkles — new ideas, creative, optimization"),
			new OnePageIconSpec("chat-heart", "Chat bubble with heart — social, engagement, sentiment"),
			new OnePageIconSpec("sparkle-outline", "Sparkle outline — highlight, innovation"),
			new OnePageIconSpec("eye-off", "Crossed-out eye — wasted impressions, blind spots, viewability"),
			new OnePageIconSpec("newspaper", "Newspaper — content, publishers, editorial"),
			new OnePageIconSpec("compass", "Compass — strategy, direction, planning"),
			new OnePageIconSpec("cursor", "Pointer cursor — clicks, conversions, actions"),
			new OnePageIconSpec("checkbox", "Checked box — goals met, completed tasks"),
			new OnePageIconSpec("chart-sparkle-small", "Small bar chart — metrics, data"),
			new OnePageIconSpec("sliders", "Sliders — tuning, settings, optimization"),
			new OnePageIconSpec("unlock", "Open padlock — unlocked growth, access"),
			new OnePageIconSpec("swap-arrows", "Up and down arrows — reallocation, budget shifts, A/B swaps"),
			new OnePageIconSpec("paperclip", "Paperclip — attachments, linked assets"),
			new OnePageIconSpec("lock", "Padlock — security, brand safety, privacy"),
			new OnePageIconSpec("check-circle", "Check in a circle — success, verified results"),
			new OnePageIconSpec("archive", "Archive box — inventory, organization, storage"),
			new OnePageIconSpec("stop-square", "Stop square — paused or cut spend"),
			new OnePageIconSpec("trend-up", "Upward trend chart — growth, rising performance"),
			new OnePageIconSpec("dollar", "Dollar coin — cost, budget, revenue, CPA"),
			new OnePageIconSpec("brain", "Brain — insights, intelligence, AI"),
			new OnePageIconSpec("chart-sparkle-lime", "Bar chart with sparkle — performance gains"),
			new OnePageIconSpec("sparkle-tile", "Sparkle tile — premium, featured, standout creative"),
			new OnePageIconSpec("arrow-up-right", "Arrow up and to the right — growth, scaling, lift"),
			new OnePageIconSpec("record", "Record button — video, recording, live"),
			new OnePageIconSpec("shrink-arrows", "Arrows pointing inward — consolidation, efficiency, focus"),
			new OnePageIconSpec("link", "Chain link — connections, attribution, integrations"),
			new OnePageIconSpec("tv", "Television — CTV, streaming, TV"),
			new OnePageIconSpec("target", "Crosshair target — targeting, audiences, precision"),
			new OnePageIconSpec("puzzle", "Puzzle piece — integration, fit, partnerships"),
			new OnePageIconSpec("user-check", "Person with check — audience, verified users, customers"),
			new OnePageIconSpec("video-play", "Video play button — YouTube, online video"),
			new OnePageIconSpec("search-sparkle", "Magnifier with sparkle — search, discovery, research"));

	private OnePageIconLibrary() {
	}
}
