/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.view.network;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.microproject.ui.view.graph.GraphInteractor;
import com.microproject.ui.view.graph.GraphUI;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.network.rendering.FieldChange;
import com.microproject.field.FieldParseException;
import com.microproject.node.model.NodeModel;
import com.microproject.undo.EditSupport;
import com.microproject.undo.UndoController;
import com.microproject.view.network.PertManualPositions;

/**
*
*/
public class NetworkInteractor extends GraphInteractor{
	private static final long serialVersionUID = 5365103090789265267L;
	private static final Logger logger = Logger.getLogger(NetworkInteractor.class.getName());
	protected static final int BAR_SELECTION=4;
	/** Existing override, if any. Kept separate from the rendered center so Undo restores auto-layout. */
	private PertManualPositions.Position originalManualPosition;
	/**
	*
	*/
	public NetworkInteractor(GraphUI ui) {
		super(ui);
		//popup=new NetworkPopupMenu(this);
	}



	protected void computeNodeSelection(double x,double y){
		// Clicking inside the box (not on edge) -> move mode (BAR_MOVE)
		// Clicking on the box edge -> selection mode (BAR_SELECTION) for link creation
		if (((NetworkUI)ui).isOnBarEdge((GraphicNode)selected,x,y))
			state=BAR_SELECTION;
		else
			state=BAR_MOVE;
	}


	protected Shape getBarShadowBounds(double x,double y){
		//if (state!=BAR_MOVE) return null;
		GraphicNode node=(GraphicNode)selected;
	GeneralPath shape=getShape(node);
	return shape.createTransformedShape(AffineTransform.getTranslateInstance(x-x0,y-y0));
	}

	protected GeneralPath getShape(GraphicNode node){
	return ((NetworkRenderer)((NetworkUI)ui).getGraphRenderer()).getShape(node);
	}

	protected Rectangle2D getLinkSelectionShadowBounds(GraphicNode node){
	GeneralPath shape=getShape(node);
	return shape.getBounds2D();
	}



	protected void setLinkOrigin(){
	GraphicNode node=(GraphicNode)selected;
	Point2D center= ((NetworkRenderer)((NetworkUI)ui).getGraphRenderer()).getCenter(node);
		x0link=center.getX();
		y0link=center.getY();

	}

	protected boolean switchOnLinkCreation(double x, double y){
		// Link creation starts from box edge (BAR_SELECTION state)
		// Only allow link creation for nodes that can have dependencies
		if (state!=BAR_SELECTION) return false;
		GraphicNode node=(GraphicNode)selected;
		GeneralPath shape=getShape(node);
		if (shape==null) return false;
		// Check if clicking on the edge (connection point)
		return ((NetworkUI)ui).isOnBarEdge(node, x, y);
	}

	@Override
	public void mousePressed(java.awt.event.MouseEvent e) {
		// Capture original position before move starts
		if (!isReadOnly() && selected instanceof GraphicNode && SwingUtilities.isLeftMouseButton(e)) {
			computeNodeSelection(e.getX(), e.getY());
			if (state == BAR_MOVE) {
				GraphicNode node = (GraphicNode) selected;
				PertManualPositions positions = ((Network) ui.getGraph()).getProject()
					.findTransientDocumentState(PertManualPositions.class);
				Long taskUid = node.getNode().getImpl() instanceof com.microproject.pm.task.Task task
					? task.getUniqueId() : null;
				originalManualPosition = positions != null && taskUid != null ? positions.getPosition(taskUid) : null;
			} else {
				originalManualPosition = null;
			}
		}
		super.mousePressed(e);
	}

//    public Cursor selectCursor(){
//    	Cursor cursor=null;
//    	switch (state) {
//		case BAR_SELECTION:
//			cursor=getLinkCursor();
//			break;
//		}
//    	if (cursor==null) super.selectCursor();
//    	else getGraph().setCursor(cursor);
//    	return cursor;
//    }

	protected void select(int x,int y){
	if (selection){
		selectedZone=ui.getObjectAt(x,y);
		if (selectedZone!=null) selected=selectedZone.getObject();
		int savedState=state;
			if (selected==null){
				state=NOTHING_SELECTED;
			}else{
				findState(x,y);
			}
			if (state!=savedState){
				if (savedState==BAR_SELECTION){
					NetworkUI nui=(NetworkUI)ui;
					List<FieldChange> changes = nui.getEditorChange();
					GraphicNode node=nui.getEditorNode();
					nui.editNode(null);
					if (changes != null) for (FieldChange change : changes) {
						try {
							nui.getGraph().getCache().getModel().setFieldValue(change.getField(),node.getNode(), this, change.getValue(), null,NodeModel.NORMAL);
						} catch (FieldParseException e) {
							logger.log(Level.WARNING, "Failed to apply network field change", e);
						}
					}
				}
				selectCursor();
			}
	}
	}

	public boolean executeAction(double x,double y){
	if (selected==null) return false;
	switch (state) {
	case BAR_SELECTION:
		((NetworkUI)ui).editNode((GraphicNode)selected);
		return true;
	case BAR_MOVE:
		GraphicNode node = (GraphicNode)selected;
		// A click on an already-selected box can enter BAR_MOVE before the graph
		// knows whether it became a drag. Do not create a manual override or undo
		// edit unless the pointer actually moved away from the press location.
		if (Math.hypot(x - x0, y - y0) < 2.0) return true;
		// Translate the shape visually
		((NetworkRenderer)((NetworkUI)ui).getGraphRenderer()).translateShape(node, x-x0, y-y0);

		// Calculate new logical center position (100% zoom)
		Point2D newCenter = ((NetworkRenderer)((NetworkUI)ui).getGraphRenderer()).getCenter(node);
		java.awt.geom.AffineTransform transform = ((Network)ui.getGraph()).getTransform();
		Point2D logicalNewCenter = new Point2D.Double(
			(newCenter.getX() - transform.getTranslateX()) / transform.getScaleX(),
			(newCenter.getY() - transform.getTranslateY()) / transform.getScaleY()
		);

		// Get task unique ID
		Long taskUid = node.getNode().getImpl() instanceof com.microproject.pm.task.Task
			? ((com.microproject.pm.task.Task) node.getNode().getImpl()).getUniqueId()
			: null;

		if (taskUid != null) {
					Network network = (Network) ui.getGraph();
					// Post undo edit for the move
					UndoController undoController = network.getProject().getUndoController();
					if (undoController != null) {
						EditSupport editSupport = undoController.getEditSupport();
						if (editSupport != null) {
							editSupport.postEdit(new NetworkBoxMoveEdit(network, taskUid, originalManualPosition,
								new PertManualPositions.Position(logicalNewCenter.getX(), logicalNewCenter.getY()), this));
						}
					}
					// Update manual positions in project's transient state (the undo edit will also do this on redo/undo)
					PertManualPositions pmp = network.getProject().getOrCreateTransientDocumentState(PertManualPositions.class, PertManualPositions::new);
					pmp.putPosition(taskUid, new PertManualPositions.Position(logicalNewCenter.getX(), logicalNewCenter.getY()));
					network.getProject().setDirty(true);
				}
		return true;
	}
	return false;
	}



}

