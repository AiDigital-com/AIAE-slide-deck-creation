package com.aidigital.strategyplanning.service.categoryanalysis.services;

/**
 * Deterministic quality checks on a generated cutout before it reaches a slide.
 *
 * <p>The model sometimes returns an opaque rectangle instead of a cutout, or crops the subject's
 * head at the top edge. Both are checked from the pixels rather than trusted, and an image that
 * cannot be decoded passes so an unreadable-but-valid PNG is never rejected.
 */
public interface SlideImageCutoutInspector {

	/**
	 * Checks that the subject stays clear of the top edge.
	 *
	 * @param pngBytes decoded PNG bytes of the generated image
	 * @return true when the top margin is subject-free, or the image cannot be inspected
	 */
	boolean hasClearTopMargin(byte[] pngBytes);

	/**
	 * Checks that the image is a real cutout rather than an opaque rectangle.
	 *
	 * @param pngBytes decoded PNG bytes of the generated image
	 * @return true when enough of the image is transparent, or it cannot be inspected
	 */
	boolean hasTransparentBackground(byte[] pngBytes);
}
