package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the raw shape operations, on small slide shows built in memory.
 */
class OnePagerShapeEditorImplTest {

	private static final Rectangle2D BOX = new Rectangle2D.Double(10, 20, 100, 50);

	private byte[] onePixelPng() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
		return out.toByteArray();
	}

	@Test
	void shouldFindShapesByIdAndRejectUnknownIdsTest() throws IOException {
		// Given: a slide with one text box
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFSlide slide = show.createSlide();
			XSLFTextBox box = slide.createTextBox();
			OnePagerShapeEditorImpl editor = new OnePagerShapeEditorImpl();

			// When-Then: the box is found by its id and an unknown id is rejected by id
			assertThat(editor.findShapeById(slide, box.getShapeId())).isSameAs(box);
			assertThatThrownBy(() -> editor.findShapeById(slide, 9_999))
					.isInstanceOf(IllegalStateException.class)
					.hasMessage("Shape 9999 not found on slide");
		}
	}

	@Test
	void shouldCloneTextGroupAndConnectorShapesWithFreshIdsTest() throws IOException {
		// Given: a source slide with a text box, a group, and a connector, and an empty canvas
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFSlide source = show.createSlide();
			XSLFTextBox box = source.createTextBox();
			box.setAnchor(BOX);
			box.setText("Hello");
			XSLFGroupShape group = source.createGroup();
			group.setAnchor(BOX);
			XSLFConnectorShape connector = source.createConnector();
			connector.setAnchor(BOX);
			OnePagerCanvas canvas = new OnePagerCanvas(show.createSlide());
			OnePagerShapeEditorImpl editor = new OnePagerShapeEditorImpl();

			// When: each is cloned onto the canvas, the text box shifted down
			XSLFShape clonedBox = editor.cloneShifted(canvas, box, editor.toEmu(5));
			XSLFShape clonedGroup = editor.cloneOnto(canvas, group);
			XSLFShape clonedConnector = editor.cloneOnto(canvas, connector);

			// Then: clones carry the text, consecutive ids from the clone range, and the shift
			assertThat(clonedBox).isInstanceOf(XSLFTextShape.class);
			assertThat(((XSLFTextShape) clonedBox).getText()).isEqualTo("Hello");
			assertThat(clonedBox.getShapeId()).isEqualTo(OnePagerCanvas.CLONE_ID_START);
			assertThat(clonedGroup).isInstanceOf(XSLFGroupShape.class);
			assertThat(clonedGroup.getShapeId()).isEqualTo(OnePagerCanvas.CLONE_ID_START + 1);
			assertThat(clonedConnector).isInstanceOf(XSLFConnectorShape.class);
			assertThat(clonedConnector.getShapeId()).isEqualTo(OnePagerCanvas.CLONE_ID_START + 2);
			assertThat(canvas.nextShapeId().get()).isEqualTo(OnePagerCanvas.CLONE_ID_START + 3);
			assertThat(clonedBox.getAnchor().getY()).isCloseTo(25, org.assertj.core.data.Offset.offset(0.01));
			assertThat(editor.topEmu(clonedBox)).isEqualTo(editor.toEmu(25));
			assertThat(editor.leftEmu(clonedBox)).isEqualTo(editor.toEmu(10));
			assertThat(editor.widthEmu(clonedBox)).isEqualTo(editor.toEmu(100));
			assertThat(editor.heightEmu(clonedBox)).isEqualTo(editor.toEmu(50));
			assertThat(editor.bottomEmu(clonedBox)).isEqualTo(editor.toEmu(75));
			assertThat(source.getShapes()).hasSize(3);
		}
	}

	@Test
	void shouldClonePicturesWithTheirOwnImageRelationshipTest() throws IOException {
		// Given: a source slide with a picture and an empty canvas
		try (XMLSlideShow show = new XMLSlideShow()) {
			XSLFPictureData data = show.addPicture(onePixelPng(), PictureData.PictureType.PNG);
			XSLFSlide source = show.createSlide();
			XSLFPictureShape picture = source.createPicture(data);
			picture.setAnchor(BOX);
			OnePagerCanvas canvas = new OnePagerCanvas(show.createSlide());

			// When: the picture is cloned
			XSLFShape cloned = new OnePagerShapeEditorImpl().cloneOnto(canvas, picture);

			// Then: the clone is a picture whose data resolves on the target slide
			assertThat(cloned).isInstanceOf(XSLFPictureShape.class);
			assertThat(((XSLFPictureShape) cloned).getPictureData().getData()).isEqualTo(data.getData());
			assertThat(cloned.getSheet()).isSameAs(canvas.slide());
		}
	}

	@Test
	void shouldMovePlaceRemoveAndPruneTest() throws IOException {
		// Given: a show with three slides and a text box on the second
		try (XMLSlideShow show = new XMLSlideShow()) {
			show.createSlide();
			XSLFSlide keep = show.createSlide();
			show.createSlide();
			XSLFTextBox box = keep.createTextBox();
			box.setAnchor(BOX);
			XSLFTextBox doomed = keep.createTextBox();
			OnePagerShapeEditorImpl editor = new OnePagerShapeEditorImpl();

			// When: the box is moved, re-placed, the other removed, and the other slides pruned
			editor.moveBy(box, editor.toEmu(5), editor.toEmu(-10));
			Rectangle2D moved = box.getAnchor();
			editor.setAnchor(box, new Rectangle2D.Double(1, 2, 3, 4));
			editor.removeShape(doomed);
			editor.removeAllSlidesExcept(show, keep);

			// Then: every operation landed and only the kept slide remains
			assertThat(moved.getX()).isCloseTo(15, org.assertj.core.data.Offset.offset(0.01));
			assertThat(moved.getY()).isCloseTo(10, org.assertj.core.data.Offset.offset(0.01));
			assertThat(box.getAnchor()).isEqualTo(new Rectangle2D.Double(1, 2, 3, 4));
			assertThat(keep.getShapes()).containsExactly(box);
			assertThat(show.getSlides()).containsExactly(keep);
			assertThat(editor.toPoints(editor.toEmu(12.5))).isCloseTo(12.5, org.assertj.core.data.Offset.offset(0.01));
		}
	}
}
