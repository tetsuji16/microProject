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
package com.microproject.pm.graphic.xbs;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;
import java.util.ListIterator;


import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.network.NetworkParams;
import com.microproject.pm.graphic.network.layout.AbstractNetworkLayout;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.TexturedShape;

/**
 *
 */
public class XbsLayout extends AbstractNetworkLayout {
	protected List dependencies=new ArrayList();
	public XbsLayout(NetworkParams network){
		super(network);
	}
	public List getDependencies() {
		return dependencies;
	}
	public void setCache(NodeModelCache cache){
		super.setCache(cache);
	}
	
	protected TexturedShapeFinder texturedShapeFinder=new TexturedShapeFinder();
	protected class TexturedShapeFinder implements Consumer<Object>{
		protected BarFormat format;
		protected GraphicNode node;
		protected TexturedShape shape;
		void initialize(GraphicNode node) {
			this.node = node;
			shape=null;
		}
		public void accept(Object arg0) {
			format = (BarFormat)arg0;
			if (format.getMiddle()!=null)
				shape=format.getMiddle();
		}
		public TexturedShape getShape(){
			return shape;
		}
	}
	protected TexturedShape findShape(GraphicNode node){
		texturedShapeFinder.initialize(node);
		barStyles.apply(node.getNode().getImpl(),texturedShapeFinder);
		return texturedShapeFinder.getShape();
	}
	
	
	
	private void setShape(GraphicNode node,Rectangle2D ref,double centerX,double centerY){
	    TexturedShape texturedShape=findShape(node);
	    if (texturedShape==null) return;
	    GeneralPath shape=texturedShape.toGeneralPath(ref.getWidth(),ref.getHeight(),centerX-ref.getWidth()/2,centerY,null);
	    node.setXbsShape(shape,centerX,centerY);
	    Rectangle.union(bounds,network.scale(shape.getBounds()),bounds);
	}
	
	protected int updateBounds(Point2D origin,Rectangle2D ref){//cache in current version isn't a tree
		double x=origin.getX()+ref.getWidth()/2;
		double y=origin.getY()+ref.getHeight()/2;
		GraphicNode node,previous=null;
		int maxLevel=0;
		for (ListIterator i=cache.getIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			if (node.getLevel()>maxLevel) maxLevel=node.getLevel();
			if (previous!=null&&node.getLevel()<=previous.getLevel()){
				setShape(previous,ref,x,y+(previous.getLevel()-1)*(ref.getMaxY()));
				x+=ref.getMaxX();
			}
			previous=node;
		}
		if (previous!=null){
			setShape(previous,ref,x,y+(previous.getLevel()-1)*(ref.getMaxY()));
		}
		return maxLevel;
	}

	protected void updateBounds(int level,Point2D origin,Rectangle2D ref){//cache in current version isn't a tree
		double y=origin.getY()+ref.getHeight()/2+ref.getMaxY()*(level-1);
		Point2D childCenter,center;
		double x0,x1;
		GraphicNode node,child;
		boolean hasChild;
		for (ListIterator i=cache.getIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			if (node.getLevel()==level){
				x0=-1;
				x1=-1;
				hasChild=false;
				while (i.hasNext()){
					child=(GraphicNode)i.next();
					if (child.getLevel()<=level){
						i.previous();
						break;
					}else if (child.getLevel()==level+1){
						hasChild=true;
						childCenter=child.getXbsCenter();
						if (x0==-1||childCenter.getX()<x0) x0=childCenter.getX();
						if (x1==-1||childCenter.getX()>x1) x1=childCenter.getX();
						dependencies.add(new GraphicDependency(node,child,null));
					}
				}
				if (hasChild) setShape(node,ref,(x0+x1)/2,y);
			}
			
		}
	}
	
	
	public void updateBounds(){	    
		GraphicConfiguration config=GraphicConfiguration.getInstance();
		
		Point2D origin=new Point2D.Double(config.getTreeXOffset(),config.getTreeYOffset());
		Rectangle2D ref=new Rectangle2D.Double(config.getTreeXOffset(),config.getTreeYOffset(),config.getTreeCellWidth(),config.getTreeCellHeight());
		setEmpty();
		dependencies.clear();
		
		bounds.setFrame(0.0,0.0,0.0,0.0);
		
		int maxLevel=updateBounds(origin,ref);
		if (maxLevel==0) return;
		for (int level=maxLevel-1;level>0;level--) updateBounds(level,origin,ref);
		
		fireLayoutChanged();
	}
	
	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent) {
    	if (!compositeEvent.isNodeHierarchy()) return;
		updateBounds();
	}
}

