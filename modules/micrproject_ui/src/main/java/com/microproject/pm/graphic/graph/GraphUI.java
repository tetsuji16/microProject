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
package com.microproject.pm.graphic.graph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.graphic.configuration.GraphicConfiguration;

/**
 *
 */
public class GraphUI extends ComponentUI implements Serializable {
	private static final long serialVersionUID = -8309077056249013471L;
	protected Graph graph;
    protected GraphInteractor interactor;
	protected GraphicConfiguration config;
	protected GraphRenderer graphRenderer;

	public GraphUI(Graph graph,GraphRenderer graphRenderer) {
		super();
		this.graph=graph;
		this.graphRenderer=graphRenderer;
		config=GraphicConfiguration.getInstance();
		//graphRenderer.setGraphInfo(graph);

	}





	public Graph getGraph() {
		return graph;
	}

	public GraphInteractor getInteractor() {
		return interactor;
	}

	@Override
	public void installUI(JComponent component) {
		super.installUI(component);
		if (interactor != null) interactor.install();
	}

	@Override
	public void uninstallUI(JComponent component) {
		dispose();
		super.uninstallUI(component);
	}

	public void dispose() {
		if (interactor != null) interactor.uninstall();
	}




    public GraphZone getObjectAt(double x,double y){
    	GraphZone o=getNodeAt(x,y);
    	return (o==null)?getLinkAt(x,y):o;
    }

    protected double[] segment=new double[6];
    public GraphZone getLinkAt(double x,double y){
    	return getLinkAt(x, y, graph.getModel().getDependencyIterator());
    }
    protected GraphZone getLinkAt(double x,double y,Iterator i){
		double delta=config.getSelectionSquare();
		double flatness=config.getLinkFlatness();
    	Rectangle2D selectionZone=(delta==0)?null:new Rectangle2D.Double(x-delta,y-delta,2*delta+1,2*delta+1);
    	GraphicDependency dependency;
		while(i.hasNext()){
			dependency=(GraphicDependency)i.next();
			java.awt.geom.GeneralPath dependencyPath=graphRenderer.findDependencyPath(dependency);
			if (dependencyPath==null) continue;
			if (selectionZone==null&&dependencyPath.contains(x,y)) return dependency==null?null:new GraphZone(dependency);
			else if (selectionZone!=null){
				int segType;
				double lx=-1;
				double ly=-1;
				for (PathIterator j=(flatness<=0)?dependencyPath.getPathIterator(null):dependencyPath.getPathIterator(null,flatness);!j.isDone();j.next()){
					switch (j.currentSegment(segment)) {
						case PathIterator.SEG_LINETO:
						//case PathIterator.SEG_CLOSE:
							if (Line2D.ptSegDist(lx,ly,segment[0],segment[1],x,y)<=delta)
								return dependency==null?null:new GraphZone(dependency);
							break;
					}
					lx=segment[0];
					ly=segment[1];
				}
			}
		}
		return null;
    }


    public LinkRouting getRouting() {
		return graphRenderer.getRouting();
	}

	public void setRouting(LinkRouting routing) {
		graphRenderer.setRouting(routing);
	}





	public void updateShapes(List nodes){
    	graphRenderer.updateShapes(nodes);
    }
    public void updateShapes(){
    	graphRenderer.updateShapes();
    }
    public void update(Graphics g, JComponent c) {
    	if (c != null && c.isOpaque()) {
    		Color oldColor = g.getColor();
    		g.setColor(c.getBackground());
    		g.fillRect(0, 0, c.getWidth(), c.getHeight());
    		g.setColor(oldColor);
    	}
    	paint(g, c);
    }
    public void paint(Graphics g, JComponent c) {
		graphRenderer.paint(g);
		if (interactor != null && g instanceof java.awt.Graphics2D graphics)
			interactor.paintPreview(graphics);
    }
    public void updateShape(GraphicNode node){
    	graphRenderer.updateShape(node);
    }



    public GraphRenderer getGraphRenderer() {
		return graphRenderer;
	}



	//to override
    public GraphZone getNodeAt(double x,double y){return null;}
    public boolean isEditing(GraphicNode node){return false;}





}
