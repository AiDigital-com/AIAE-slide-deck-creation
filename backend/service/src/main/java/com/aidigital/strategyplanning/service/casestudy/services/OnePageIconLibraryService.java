package com.aidigital.strategyplanning.service.casestudy.services;

import com.aidigital.strategyplanning.service.casestudy.models.OnePageIconOption;

import java.util.List;

/**
 * Service contract for the brand icon library the One-Page Case Study swaps into its tagged
 * icon slots.
 */
public interface OnePageIconLibraryService {

	/**
	 * Lists every library icon with the app-relative URL of its image, for the review form.
	 *
	 * @return every icon option, in library order
	 */
	List<OnePageIconOption> options();

	/**
	 * Reports whether a name is a library icon.
	 *
	 * @param name candidate icon name, may be null
	 * @return true when the library has an icon with that name
	 */
	boolean contains(String name);

	/**
	 * Returns the app-relative URL path of an icon image.
	 *
	 * @param name library icon name
	 * @return image URL path, e.g. {@code /case-study-icons/target.png}
	 */
	String imagePath(String name);

	/**
	 * Returns the absolute URL Google fetches an icon image from.
	 *
	 * @param publicBaseUrl externally reachable base URL of this app, with or without a trailing slash
	 * @param name          library icon name
	 * @return absolute image URL
	 */
	String imageUrl(String publicBaseUrl, String name);
}
