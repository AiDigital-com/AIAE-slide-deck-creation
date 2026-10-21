package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.models.OnePagerCanvas;
import com.aidigital.strategyplanning.service.onepager.services.OnePagerShapeEditor;
import org.apache.poi.ooxml.POIXMLDocumentPart.RelationPart;
import org.apache.poi.sl.usermodel.PlaceableShape;
import org.apache.poi.util.Units;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFConnectorShape;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSheet;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.springframework.stereotype.Service;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link OnePagerShapeEditor}.
 *
 * <p>{@code XSLFSheet#getShapes()} builds and caches its shape list once, so it goes stale the
 * moment shape XML is appended directly (as cloning must; there is no public "add this cloned
 * shape" API). Clones are therefore wrapped straight from the freshly added XML node instead of
 * being looked up on the target slide, and nothing reads shapes back off the canvas by position.
 * Serialization writes the underlying XML, so the stale cache never matters.
 */
@Service
public class OnePagerShapeEditorImpl implements OnePagerShapeEditor {

	private static final String SHAPE_NOT_FOUND = "Shape %d not found on slide";
	private static final String UNSUPPORTED_SHAPE = "Unsupported shape type to clone: ";
	private static final String WRAPPER_FAILURE = "Could not construct %s wrapper";

	@Override
	public XSLFShape findShapeById(XSLFSlide slide, int id) {
		for (XSLFShape shape : slide.getShapes()) {
			if (shape.getShapeId() == id) {
				return shape;
			}
		}
		throw new IllegalStateException(SHAPE_NOT_FOUND.formatted(id));
	}

	@Override
	public XSLFShape cloneOnto(OnePagerCanvas canvas, XSLFShape source) {
		XmlObject copy = source.getXmlObject().copy();
		XSLFSlide target = canvas.slide();
		// getSpTree() is protected; getCSld().getSpTree() reaches the same <p:spTree> element.
		CTGroupShape spTree = target.getXmlObject().getCSld().getSpTree();
		int freshId = canvas.nextShapeId().getAndIncrement();
		XSLFShape cloned;
		if (copy instanceof CTShape shape) {
			CTShape added = spTree.addNewSp();
			added.set(shape);
			added.getNvSpPr().getCNvPr().setId(freshId);
			cloned = construct(XSLFAutoShape.class, CTShape.class, added, target);
		} else if (copy instanceof CTGroupShape group) {
			CTGroupShape added = spTree.addNewGrpSp();
			added.set(group);
			added.getNvGrpSpPr().getCNvPr().setId(freshId);
			cloned = construct(XSLFGroupShape.class, CTGroupShape.class, added, target);
		} else if (copy instanceof CTConnector connector) {
			CTConnector added = spTree.addNewCxnSp();
			added.set(connector);
			added.getNvCxnSpPr().getCNvPr().setId(freshId);
			cloned = construct(XSLFConnectorShape.class, CTConnector.class, added, target);
		} else if (copy instanceof CTPicture picture) {
			CTPicture added = spTree.addNewPic();
			added.set(picture);
			added.getNvPicPr().getCNvPr().setId(freshId);
			cloned = construct(XSLFPictureShape.class, CTPicture.class, added, target);
		} else {
			throw new IllegalStateException(UNSUPPORTED_SHAPE + copy.getClass());
		}
		if (source instanceof XSLFPictureShape sourcePicture && cloned instanceof XSLFPictureShape clonedPicture) {
			remapPictureRelationship(target, sourcePicture, clonedPicture);
		}
		return cloned;
	}

	@Override
	public XSLFShape cloneShifted(OnePagerCanvas canvas, XSLFShape source, long dyEmu) {
		XSLFShape cloned = cloneOnto(canvas, source);
		moveBy(cloned, 0, dyEmu);
		return cloned;
	}

	/**
	 * Constructs a POI shape wrapper through its non-public (shape XML, sheet) constructor: POI
	 * offers no public factory for wrapping an already attached shape node, only for creating
	 * blank ones.
	 *
	 * @param <T>          wrapper type
	 * @param <X>          XML bean type the wrapper wraps
	 * @param wrapperClass wrapper class to construct
	 * @param xmlType      XML bean class the constructor takes
	 * @param xml          the attached XML node to wrap
	 * @param sheet        sheet the shape belongs to
	 * @return the constructed wrapper
	 */
	<T extends XSLFShape, X> T construct(Class<T> wrapperClass, Class<X> xmlType, X xml, XSLFSheet sheet) {
		try {
			Constructor<T> constructor = wrapperClass.getDeclaredConstructor(xmlType, XSLFSheet.class);
			constructor.setAccessible(true);
			return constructor.newInstance(xml, sheet);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(WRAPPER_FAILURE.formatted(wrapperClass.getSimpleName()), e);
		}
	}

	/**
	 * Copies a cloned picture's image data into the show and points the clone's blip at a new
	 * relationship on the target slide (registered with POI as well as in the package, so the
	 * clone's picture data resolves), since the copied XML still references the source slide's
	 * relationship id.
	 *
	 * @param target slide the picture was cloned onto
	 * @param source original picture shape
	 * @param cloned cloned picture shape whose embed reference is rewritten
	 */
	void remapPictureRelationship(XSLFSlide target, XSLFPictureShape source, XSLFPictureShape cloned) {
		XSLFPictureData sourceData = source.getPictureData();
		XSLFPictureData newData = target.getSlideShow().addPicture(sourceData.getData(), sourceData.getType());
		RelationPart relation = target.addRelation(null, XSLFRelation.IMAGE_PNG, newData);
		((CTPicture) cloned.getXmlObject()).getBlipFill().getBlip().setEmbed(relation.getRelationship().getId());
	}

	@Override
	public void removeShape(XSLFShape shape) {
		shape.getParent().removeShape(shape);
	}

	@Override
	public void moveBy(XSLFShape shape, long dxEmu, long dyEmu) {
		Rectangle2D anchor = shape.getAnchor();
		setAnchor(shape, new Rectangle2D.Double(
				anchor.getX() + toPoints(dxEmu), anchor.getY() + toPoints(dyEmu),
				anchor.getWidth(), anchor.getHeight()));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setAnchor(XSLFShape shape, Rectangle2D anchor) {
		((PlaceableShape<XSLFShape, ?>) shape).setAnchor(anchor);
	}

	@Override
	public long topEmu(XSLFShape shape) {
		return toEmu(shape.getAnchor().getY());
	}

	@Override
	public long bottomEmu(XSLFShape shape) {
		return toEmu(shape.getAnchor().getY() + shape.getAnchor().getHeight());
	}

	@Override
	public long leftEmu(XSLFShape shape) {
		return toEmu(shape.getAnchor().getX());
	}

	@Override
	public long widthEmu(XSLFShape shape) {
		return toEmu(shape.getAnchor().getWidth());
	}

	@Override
	public long heightEmu(XSLFShape shape) {
		return toEmu(shape.getAnchor().getHeight());
	}

	@Override
	public double toPoints(long emu) {
		return Units.toPoints(emu);
	}

	@Override
	public long toEmu(double points) {
		return Units.toEMU(points);
	}

	@Override
	public void removeAllSlidesExcept(XMLSlideShow show, XSLFSlide keep) {
		List<XSLFSlide> slides = new ArrayList<>(show.getSlides());
		for (int index = slides.size() - 1; index >= 0; index--) {
			if (slides.get(index) != keep) {
				show.removeSlide(index);
			}
		}
	}
}
