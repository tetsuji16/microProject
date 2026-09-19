/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.network;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.network.rendering.FieldChange;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public class NetworkInteractor extends GraphInteractor{
	private static final long serialVersionUID = 5365103090789265267L;
	private static final Logger logger = Logger.getLogger(NetworkInteractor.class.getName());
	protected static final int BAR_SELECTION=4;
	/**
	 *
	 */
	public NetworkInteractor(GraphUI ui) {
		super(ui);
		//popup=new NetworkPopupMenu(this);
	}



    protected void computeNodeSelection(double x,double y){
    	if (((NetworkUI)ui).isOnBarEdge((GraphicNode)selected,x,y))
			state=BAR_MOVE;
		else state=BAR_SELECTION;
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
    	if (state!=BAR_SELECTION) return false;
		GraphicNode node=(GraphicNode)selected;
		GeneralPath shape=getShape(node);
		if (shape==null) return false;
		return (shape.contains(x,y));
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
	    			List changes=nui.getEditorChange();
	    			GraphicNode node=nui.getEditorNode();
	    			nui.editNode(null);
	    			if (changes!=null) for (Iterator i=changes.iterator();i.hasNext();){
	    				FieldChange change=(FieldChange)i.next();
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
			 ((NetworkRenderer)((NetworkUI)ui).getGraphRenderer()).translateShape((GraphicNode)selected,x-x0,y-y0);
			return true;

    	}
    	return false;
    }



}

