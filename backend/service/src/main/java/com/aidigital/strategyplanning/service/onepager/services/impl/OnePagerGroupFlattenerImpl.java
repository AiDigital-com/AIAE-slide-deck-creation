package com.aidigital.strategyplanning.service.onepager.services.impl;

import com.aidigital.strategyplanning.service.onepager.services.OnePagerGroupFlattener;
import org.apache.poi.ooxml.util.POIXMLUnits;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGroupTransform2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D;
import org.openxmlformats.schemas.presentationml.x2006.main.CTConnector;
import org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Default implementation of {@link OnePagerGroupFlattener}, working on the slide's OOXML directly
 * because POI offers no ungroup operation.
 */
@Service
public class OnePagerGroupFlattenerImpl implements OnePagerGroupFlattener {

	private static final String GROUP_NOT_FOUND = "Group %d not found on slide";

	@Override
	public void flatten(XSLFSlide slide, List<Integer> groupIds) {
		CTGroupShape spTree = slide.getXmlObject().getCSld().getSpTree();
		for (int groupId : groupIds) {
			int index = groupIndex(spTree, groupId);
			CTGroupShape group = spTree.getGrpSpArray(index);
			CTGroupTransform2D transform = group.getGrpSpPr().getXfrm();
			for (CTShape child : group.getSpList()) {
				bake(child.getSpPr().getXfrm(), transform);
				spTree.addNewSp().set(child);
			}
			for (CTPicture child : group.getPicList()) {
				bake(child.getSpPr().getXfrm(), transform);
				spTree.addNewPic().set(child);
			}
			for (CTConnector child : group.getCxnSpList()) {
				bake(child.getSpPr().getXfrm(), transform);
				spTree.addNewCxnSp().set(child);
			}
			for (CTGroupShape child : group.getGrpSpList()) {
				bakeGroup(child.getGrpSpPr().getXfrm(), transform);
				spTree.addNewGrpSp().set(child);
			}
			spTree.removeGrpSp(index);
		}
	}

	/**
	 * Finds the position of a top-level group among the slide's groups.
	 *
	 * @param spTree  the slide's shape tree
	 * @param groupId shape id of the group
	 * @return index into the shape tree's group list
	 * @throws IllegalStateException when no top-level group carries the id
	 */
	int groupIndex(CTGroupShape spTree, int groupId) {
		List<CTGroupShape> groups = spTree.getGrpSpList();
		for (int index = 0; index < groups.size(); index++) {
			if (groups.get(index).getNvGrpSpPr().getCNvPr().getId() == groupId) {
				return index;
			}
		}
		throw new IllegalStateException(GROUP_NOT_FOUND.formatted(groupId));
	}

	/**
	 * Rewrites a child shape's transform from its group's child space into slide space.
	 *
	 * @param child     the child's own transform, edited in place
	 * @param transform the enclosing group's transform
	 */
	void bake(CTTransform2D child, CTGroupTransform2D transform) {
		if (child == null) {
			return;
		}
		apply(child.getOff(), child.getExt(), transform);
	}

	/**
	 * Rewrites a nested group's own transform from the enclosing group's child space into slide
	 * space. Its child space is left alone, so its own children keep their relative layout.
	 *
	 * @param child     the nested group's transform, edited in place
	 * @param transform the enclosing group's transform
	 */
	void bakeGroup(CTGroupTransform2D child, CTGroupTransform2D transform) {
		if (child == null) {
			return;
		}
		apply(child.getOff(), child.getExt(), transform);
	}

	/**
	 * Maps an offset and extent through a group transform: the group's child box is scaled onto
	 * its slide box, and the shape moves and scales with it.
	 *
	 * @param off       the shape's offset in child space, edited in place
	 * @param ext       the shape's extent in child space, edited in place
	 * @param transform the group's transform
	 */
	void apply(CTPoint2D off, CTPositiveSize2D ext, CTGroupTransform2D transform) {
		double scaleX = scale(transform.getExt().getCx(), transform.getChExt().getCx());
		double scaleY = scale(transform.getExt().getCy(), transform.getChExt().getCy());
		long originX = POIXMLUnits.parseLength(transform.getOff().xgetX());
		long originY = POIXMLUnits.parseLength(transform.getOff().xgetY());
		long childOriginX = POIXMLUnits.parseLength(transform.getChOff().xgetX());
		long childOriginY = POIXMLUnits.parseLength(transform.getChOff().xgetY());
		long x = POIXMLUnits.parseLength(off.xgetX());
		long y = POIXMLUnits.parseLength(off.xgetY());
		off.setX(Math.round(originX + (x - childOriginX) * scaleX));
		off.setY(Math.round(originY + (y - childOriginY) * scaleY));
		ext.setCx(Math.round(ext.getCx() * scaleX));
		ext.setCy(Math.round(ext.getCy() * scaleY));
	}

	/**
	 * Ratio between a group's slide extent and its child extent along one axis.
	 *
	 * @param slideExtent the group's extent on the slide
	 * @param childExtent the group's child-space extent
	 * @return the scale factor, or 1 when the child extent is zero
	 */
	double scale(long slideExtent, long childExtent) {
		return childExtent == 0 ? 1.0 : (double) slideExtent / childExtent;
	}
}
