package com.aidigital.strategyplanning.service.onepager.services;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

import java.awt.geom.Rectangle2D;

/**
 * Raw OOXML shape operations the one-pager assembly is built from: looking shapes up by id,
 * cloning shape XML from one slide onto another, and moving shapes in EMU.
 */
public interface OnePagerShapeEditor {

	/**
	 * Finds a top-level shape on a slide by its shape id.
	 *
	 * @param slide slide to search
	 * @param id    shape id to find
	 * @return the matching shape
	 * @throws IllegalStateException when no shape with that id exists on the slide
	 */
	XSLFShape findShapeById(XSLFSlide slide, int id);

	/**
	 * Clones a shape's XML onto the canvas, giving the clone a fresh id and, for pictures, its own
	 * image relationship on the target slide.
	 *
	 * @param canvas canvas the clone is appended to
	 * @param source shape to clone
	 * @return a live wrapper around the newly added shape
	 */
	XSLFShape cloneOnto(OnePagerCanvas canvas, XSLFShape source);

	/**
	 * Clones a shape onto the canvas and moves the clone down by {@code dyEmu}.
	 *
	 * @param canvas canvas the clone is appended to
	 * @param source shape to clone
	 * @param dyEmu  vertical offset in EMU
	 * @return the moved clone
	 */
	XSLFShape cloneShifted(OnePagerCanvas canvas, XSLFShape source, long dyEmu);

	/**
	 * Removes a shape from its slide.
	 *
	 * @param shape shape to remove
	 */
	void removeShape(XSLFShape shape);

	/**
	 * Moves a shape by the given offsets, keeping its size.
	 *
	 * @param shape shape to move
	 * @param dxEmu horizontal offset in EMU
	 * @param dyEmu vertical offset in EMU
	 */
	void moveBy(XSLFShape shape, long dxEmu, long dyEmu);

	/**
	 * Replaces a shape's anchor.
	 *
	 * @param shape  shape to place
	 * @param anchor new anchor in points
	 */
	void setAnchor(XSLFShape shape, Rectangle2D anchor);

	/**
	 * Returns a shape's top edge.
	 *
	 * @param shape shape to measure
	 * @return top edge in EMU
	 */
	long topEmu(XSLFShape shape);

	/**
	 * Returns a shape's bottom edge.
	 *
	 * @param shape shape to measure
	 * @return bottom edge in EMU
	 */
	long bottomEmu(XSLFShape shape);

	/**
	 * Returns a shape's left edge.
	 *
	 * @param shape shape to measure
	 * @return left edge in EMU
	 */
	long leftEmu(XSLFShape shape);

	/**
	 * Returns a shape's width.
	 *
	 * @param shape shape to measure
	 * @return width in EMU
	 */
	long widthEmu(XSLFShape shape);

	/**
	 * Returns a shape's height.
	 *
	 * @param shape shape to measure
	 * @return height in EMU
	 */
	long heightEmu(XSLFShape shape);

	/**
	 * Converts EMU to points.
	 *
	 * @param emu length in EMU
	 * @return length in points
	 */
	double toPoints(long emu);

	/**
	 * Converts points to EMU.
	 *
	 * @param points length in points
	 * @return length in EMU
	 */
	long toEmu(double points);

	/**
	 * Removes every slide from the show except the given one.
	 *
	 * @param show slide show to prune
	 * @param keep the slide to keep
	 */
	void removeAllSlidesExcept(XMLSlideShow show, XSLFSlide keep);
}
