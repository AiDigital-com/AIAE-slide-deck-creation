package com.aidigital.strategyplanning.service.categoryanalysis.services;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Draws the In-Depth template's slide-8 donut as a transparent PNG matching the master's style:
 * flat-ended slices with no gaps, a hole 58% of the outer diameter, the first slice starting at
 * 12 o'clock and the rest following clockwise. Google Slides cannot resize the arc of a native
 * shape, so the deck builder swaps this image in for the master's donut.
 */
@Component
public class DonutChartRenderer {

	/**
	 * Hole diameter as a fraction of the outer diameter, measured from the master's donut.
	 */
	public static final double HOLE_RATIO = 0.58;

	/**
	 * Pixel length of the image's longer side — high enough to stay crisp at any slide zoom.
	 */
	public static final int LONG_SIDE_PX = 1600;

	/**
	 * Slices overlap the next one by this many degrees so anti-aliasing never leaves a hairline
	 * seam between them; each later slice is painted over the overlap.
	 */
	public static final double SEAM_OVERLAP_DEGREES = 0.4;

	private static final String PNG_FORMAT = "png";
	private static final double TWELVE_OCLOCK_DEGREES = 90;
	private static final double FULL_CIRCLE_DEGREES = 360;
	private static final int EDGE_MARGIN_PX = 2;

	/**
	 * Renders the donut.
	 *
	 * @param values slice values in drawing order (slice 1 first); any non-negative scale
	 * @param colors slice colours, same order and size as {@code values}
	 * @param aspect width divided by height of the box the image will fill
	 * @return PNG bytes
	 */
	public byte[] render(List<Double> values, List<Color> colors, double aspect) {
		int width = aspect >= 1 ? LONG_SIDE_PX : (int) Math.round(LONG_SIDE_PX * aspect);
		int height = aspect >= 1 ? (int) Math.round(LONG_SIDE_PX / aspect) : LONG_SIDE_PX;
		BufferedImage image = new BufferedImage(Math.max(width, 1), Math.max(height, 1),
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			double diameter = Math.min(image.getWidth(), image.getHeight()) - EDGE_MARGIN_PX;
			double left = (image.getWidth() - diameter) / 2;
			double top = (image.getHeight() - diameter) / 2;
			double holeDiameter = diameter * HOLE_RATIO;
			Area hole = new Area(new Ellipse2D.Double(left + (diameter - holeDiameter) / 2,
					top + (diameter - holeDiameter) / 2, holeDiameter, holeDiameter));
			if (values.isEmpty()) {
				return encode(image);
			}
			// Paint the whole ring in slice 1's colour first, so the seam where the last slice meets
			// slice 1 at 12 o'clock lands on solid colour instead of the background.
			Area ring = new Area(new Ellipse2D.Double(left, top, diameter, diameter));
			ring.subtract(hole);
			g.setColor(colors.get(0));
			g.fill(ring);

			double total = values.stream().mapToDouble(v -> Math.max(v, 0)).sum();
			double startDegrees = TWELVE_OCLOCK_DEGREES;
			for (int i = 0; i < values.size(); i++) {
				double share = total > 0 ? Math.max(values.get(i), 0) / total : 1.0 / values.size();
				double sweep = share * FULL_CIRCLE_DEGREES;
				if (i > 0 && sweep > 0) {
					boolean last = i == values.size() - 1;
					double drawn = Math.min(FULL_CIRCLE_DEGREES, sweep + (last ? 0 : SEAM_OVERLAP_DEGREES));
					Area slice = new Area(new Arc2D.Double(left, top, diameter, diameter,
							startDegrees, -drawn, Arc2D.PIE));
					slice.subtract(hole);
					g.setColor(colors.get(i));
					g.fill(slice);
				}
				startDegrees -= sweep;
			}
		} finally {
			g.dispose();
		}
		return encode(image);
	}

	/**
	 * Encodes a rendered image as PNG.
	 *
	 * @param image rendered donut
	 * @return PNG bytes
	 */
	byte[] encode(BufferedImage image) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, PNG_FORMAT, out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not encode donut chart PNG", e);
		}
	}
}
