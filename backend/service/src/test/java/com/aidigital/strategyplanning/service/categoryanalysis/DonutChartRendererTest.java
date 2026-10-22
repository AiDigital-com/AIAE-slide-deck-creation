package com.aidigital.strategyplanning.service.categoryanalysis;

import com.aidigital.strategyplanning.service.categoryanalysis.services.DonutChartRenderer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Unit tests for {@link DonutChartRenderer}.
 */
class DonutChartRendererTest {

	private static final Color PURPLE = new Color(0x43, 0x00, 0xDE);
	private static final Color PINK = new Color(0xFF, 0x5C, 0xFB);
	private static final Color BLUE = new Color(0x55, 0x9F, 0xFF);
	private static final Color GREEN = new Color(0x99, 0xFD, 0x00);

	private final DonutChartRenderer renderer = new DonutChartRenderer();

	@Test
	void shouldStartTheFirstSliceAtTwelveOClockAndRunClockwiseTest() throws IOException {
		// Given: 50/25/15/10 shares — slice 1 covers 12→6 o'clock on the right, slice 2 the next quarter
		BufferedImage image = decode(renderer.render(List.of(50.0, 25.0, 15.0, 10.0),
				List.of(PURPLE, PINK, BLUE, GREEN), 1.0));
		double ringRadius = image.getWidth() / 2.0 * (1 + DonutChartRenderer.HOLE_RATIO) / 2;

		// When-Then: sampling the ring at clock angles finds each slice's colour
		assertThat(colorAt(image, 90, ringRadius)).isEqualTo(PURPLE.getRGB());
		assertThat(colorAt(image, 225, ringRadius)).isEqualTo(PINK.getRGB());
		assertThat(colorAt(image, 290, ringRadius)).isEqualTo(BLUE.getRGB());
		assertThat(colorAt(image, 345, ringRadius)).isEqualTo(GREEN.getRGB());
	}

	@Test
	void shouldLeaveTheHoleAndCornersTransparentTest() throws IOException {
		// Given: four equal slices
		BufferedImage image = decode(renderer.render(List.of(1.0, 1.0, 1.0, 1.0),
				List.of(PURPLE, PINK, BLUE, GREEN), 1.0));
		int center = image.getWidth() / 2;

		// When-Then: the centre and the corners have zero alpha
		assertThat(image.getRGB(center, center) >>> 24).isZero();
		assertThat(image.getRGB(2, 2) >>> 24).isZero();
	}

	@Test
	void shouldMatchTheTargetBoxAspectTest() throws IOException {
		// Given: a 2:1 box
		BufferedImage wide = decode(renderer.render(List.of(1.0, 2.0, 3.0, 4.0),
				List.of(PURPLE, PINK, BLUE, GREEN), 2.0));
		BufferedImage tall = decode(renderer.render(List.of(1.0, 2.0), List.of(PURPLE, PINK), 0.5));

		// When-Then: the image is sized to the box, with the long side at the configured length
		assertThat((double) wide.getWidth() / wide.getHeight()).isCloseTo(2.0, offset(0.01));
		assertThat(wide.getWidth()).isEqualTo(DonutChartRenderer.LONG_SIDE_PX);
		assertThat(tall.getHeight()).isEqualTo(DonutChartRenderer.LONG_SIDE_PX);
	}

	@Test
	void shouldRenderAnEmptyImageWhenThereAreNoValuesTest() throws IOException {
		// Given: no slices
		BufferedImage image = decode(renderer.render(List.of(), List.of(), 1.0));
		double ringRadius = image.getWidth() / 2.0 * (1 + DonutChartRenderer.HOLE_RATIO) / 2;

		// When-Then: the ring is not painted
		assertThat(colorAt(image, 90, ringRadius) >>> 24).isZero();
	}

	@Test
	void shouldShareTheRingEvenlyWhenEveryValueIsZeroTest() throws IOException {
		// Given: all-zero values
		BufferedImage image = decode(renderer.render(List.of(0.0, 0.0), List.of(PURPLE, PINK), 1.0));
		double ringRadius = image.getWidth() / 2.0 * (1 + DonutChartRenderer.HOLE_RATIO) / 2;

		// When-Then: each slice takes half the ring
		assertThat(colorAt(image, 90, ringRadius)).isEqualTo(PURPLE.getRGB());
		assertThat(colorAt(image, 270, ringRadius)).isEqualTo(PINK.getRGB());
	}

	private int colorAt(BufferedImage image, double clockDegrees, double radius) {
		double radians = Math.toRadians(clockDegrees);
		int x = (int) Math.round(image.getWidth() / 2.0 + radius * Math.sin(radians));
		int y = (int) Math.round(image.getHeight() / 2.0 - radius * Math.cos(radians));
		return image.getRGB(x, y);
	}

	private BufferedImage decode(byte[] png) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(png));
	}
}
