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
package com.microproject.pm.graphic.pert;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.function.Consumer;


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
public class PertLayout extends AbstractNetworkLayout {
	protected DependencyGraph dependencyGraph=new DependencyGraph();
	public PertLayout(NetworkParams network){
		super(network);
	}
	public void setCache(NodeModelCache cache){
		super.setCache(cache);
		dependencyGraph.setCache(cache);
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
	
	public void updateBounds(){
	    dependencyGraph.updatePertLevels();
	    
		GraphicConfiguration config=GraphicConfiguration.getInstance();
		
		Point2D origin=new Point2D.Double(config.getPertXOffset(),config.getPertYOffset());
		Rectangle2D ref=new Rectangle2D.Double(config.getPertXOffset(),config.getPertYOffset(),config.getPertCellWidth(),config.getPertCellHeight());
		int row=0;
		int col=-1;
		setEmpty();
		for (Iterator i=cache.getIterator();i.hasNext();){
		    GraphicNode current=(GraphicNode)i.next();
		    int currentCol=cache.getPertLevel(current)-1;
		    if (currentCol<=col) row++;
		    col=currentCol;
		    
		    TexturedShape texturedShape=findShape(current);
		    if (texturedShape==null) continue;
		    double centerX=origin.getX()+ref.getMaxX()*col+ref.getWidth()/2;
		    double centerY=origin.getY()+ref.getMaxY()*row+ref.getHeight()/2;
		    //System.out.println(centerX+"/"+centerY);
		    GeneralPath shape=texturedShape.toGeneralPath(ref.getWidth(),ref.getHeight(),centerX-ref.getWidth()/2,centerY,null);
		    setShape(current,shape,centerX,centerY);
		    Rectangle cellBounds=network.scale(shape.getBounds());
		    if (isEmpty())
				bounds.setBounds(cellBounds);
			else Rectangle.union(bounds,cellBounds,bounds);
		}
		fireLayoutChanged();
	}
	
	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent) {
 
		if (compositeEvent.getRemovedEdges()!=null) dependencyGraph.removeDependencies(compositeEvent.getRemovedEdges());
	    if (compositeEvent.getInsertedEdges()!=null) dependencyGraph.insertDependencies(compositeEvent.getInsertedEdges());
		updateBounds();
	}
}
