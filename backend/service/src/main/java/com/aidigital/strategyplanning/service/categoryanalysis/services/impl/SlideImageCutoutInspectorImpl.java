package com.aidigital.strategyplanning.service.categoryanalysis.services.impl;

import com.aidigital.strategyplanning.service.categoryanalysis.services.SlideImageCutoutInspector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link SlideImageCutoutInspector}.
 */
@Slf4j
@Service
public class SlideImageCutoutInspectorImpl implements SlideImageCutoutInspector {

	private static final int SUBJECT_ALPHA_THRESHOLD = 16;
	private static final double REQUIRED_TRANSPARENT_FRACTION = 0.05;
	private static final double REQUIRED_TOP_MARGIN_FRACTION = 0.02;

	@Override
	public boolean hasClearTopMargin(byte[] pngBytes) {
		try {
			java.awt.image.BufferedImage image =
					javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pngBytes));
			if (image == null || image.getHeight() == 0) {
				return true;
			}
			int marginRows = Math.max(1, (int) Math.round(image.getHeight() * REQUIRED_TOP_MARGIN_FRACTION));
			for (int y = 0; y < marginRows && y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int alpha = (image.getRGB(x, y) >>> 24);
					if (alpha > SUBJECT_ALPHA_THRESHOLD) {
						return false;
					}
				}
			}
			return true;
		} catch (java.io.IOException e) {
			log.warn("Could not inspect generated image for top margin: {}", e.getMessage());
			return true;
		}
	}

	/**
	 * Checks whether a transparent-background cutout actually carries transparency — the
	 * deterministic guard against the model returning an opaque (e.g. white) background that
	 * renders as a boxed rectangle on the slide. At least
	 * {@link #REQUIRED_TRANSPARENT_FRACTION} of all pixels must have an alpha at or below
	 * {@link #SUBJECT_ALPHA_THRESHOLD}. Images that cannot be decoded pass the check so an
	 * unreadable-but-valid PNG is never rejected.
	 *
	 * @param pngBytes decoded PNG bytes of the generated image
	 * @return true when the image has a transparent background (or cannot be inspected)
	 */
	@Override
	public boolean hasTransparentBackground(byte[] pngBytes) {
		try {
			java.awt.image.BufferedImage image =
					javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pngBytes));
			if (image == null || image.getHeight() == 0 || image.getWidth() == 0) {
				return true;
			}
			long transparent = 0;
			long total = (long) image.getWidth() * image.getHeight();
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int alpha = (image.getRGB(x, y) >>> 24);
					if (alpha <= SUBJECT_ALPHA_THRESHOLD) {
						transparent++;
					}
				}
			}
			return transparent >= total * REQUIRED_TRANSPARENT_FRACTION;
		} catch (java.io.IOException e) {
			log.warn("Could not inspect generated image for transparency: {}", e.getMessage());
			return true;
		}
	}
}
