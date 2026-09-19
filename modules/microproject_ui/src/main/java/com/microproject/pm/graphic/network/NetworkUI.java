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

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.util.List;
import java.util.ListIterator;

import com.microproject.pm.graphic.graph.GraphRenderer;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.GraphicNode;

/**
 *
 */
public abstract class NetworkUI extends GraphUI{
	public NetworkUI(Network graph,GraphRenderer graphRenderer) {
		super(graph,graphRenderer);
	}



//	public AffineTransform getTransform(){
//		return ((Network)getGraph()).getTransform();
//	}
	public int getZoom() {
		return ((Network)getGraph()).getZoom();
	}

	void resetForms(){
		((NetworkRenderer)graphRenderer).resetForms();
	}


	public boolean isEditing(GraphicNode node) {
		return ((NetworkRenderer)graphRenderer).getEditor().isEditing(node);
	}
	public void editNode(GraphicNode node) {
		((NetworkRenderer)graphRenderer).getEditor().initEditorComponent(node,getZoom(),(node==null)?null:((NetworkRenderer)graphRenderer).getBounds(node));
	}

	public List getEditorChange(){
		return ((NetworkRenderer)graphRenderer).getEditor().getCellEditorChange();
	}
	public GraphicNode getEditorNode(){
		return ((NetworkRenderer)graphRenderer).getEditor().getNode();
	}
















    public GraphZone getNodeAt(double x,double y){
		GraphicNode node;
		GeneralPath shape;
		for (ListIterator i=graph.getModel().getNodeIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			shape=((NetworkRenderer)graphRenderer).getShape(node);
			if (shape!=null&&shape.contains(x,y)){
				return node==null?null:new GraphZone(node);
			}
		}
		return null;
    }













	public boolean isOnBarEdge(GraphicNode node,double x,double y){
		double delta=config.getNetworkCellSelectionSquare();
		GeneralPath shape=((NetworkRenderer)graphRenderer).getShape(node);
		double lx=-1;
		double ly=-1;
		double fx=-1;
		double fy=-1;
		int segType;
		for (PathIterator j=shape.getPathIterator(null);!j.isDone();j.next()){
			segType=j.currentSegment(segment);
			switch (segType) {
			case PathIterator.SEG_MOVETO:
				fx=segment[0];
				fy=segment[1];
				lx=fx;
				ly=fy;
			case PathIterator.SEG_LINETO:
			case PathIterator.SEG_CLOSE:
				if (Line2D.ptSegDist(lx,ly,(segType==PathIterator.SEG_CLOSE)?fx:segment[0],(segType==PathIterator.SEG_CLOSE)?fy:segment[1],x,y)<=delta)
					return true;
			break;
			}
			lx=segment[0];
			ly=segment[1];
		}
		return false;
	}





   public void updateShapes(){
   }

}

