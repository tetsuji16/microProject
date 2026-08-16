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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.ListIterator;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;


import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.GraphRenderer;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.network.link_routing.NetworkLinkRouting;
import com.microproject.pm.graphic.network.rendering.NetworkCellEditor;
import com.microproject.pm.graphic.network.rendering.NetworkCellRenderer;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.pm.dependency.Dependency;

public abstract class NetworkRenderer extends GraphRenderer{
	protected LinkRenderer linkRenderer;
	protected NetworkCellRenderer renderer;
	protected NetworkCellEditor editor;
	protected boolean vertical=true;
	protected CellRendererPane rendererPane;
	protected JComponent container;
	
	protected GraphicConfiguration config;

	
	
	public NetworkRenderer(){
		super();
		init();
	}
	public NetworkRenderer(GraphParams graphInfo){
		super(graphInfo);
		init();
	}
	
	public void init(){
		GraphParams graphInfo=getGraphInfo();
		if (graphInfo instanceof JComponent)
			container=(JComponent)graphInfo;
		config=GraphicConfiguration.getInstance();
		linkRenderer = new LinkRenderer();
		renderer=new NetworkCellRenderer(graphInfo);
		if (container!=null){
			editor=new NetworkCellEditor(graphInfo,container);
			rendererPane=new CellRendererPane();
			container.add(rendererPane);
		}
	}


	public boolean isVertical() {
		return vertical;
	}
	public void setVertical(boolean vertical) {
		this.vertical = vertical;
		((NetworkLinkRouting)getRouting()).setVertical(vertical);
	}
	

	private class LinkRenderer implements Consumer<Object>{
		protected BarFormat format;
		protected GraphicDependency dependency;
		protected Graphics2D g2;
		void initialize(Graphics2D g2, GraphicDependency dependency) {
			this.g2 = g2;
			this.dependency = dependency;
		}


		
		public void accept(Object arg0) {
			format = (BarFormat)arg0;
			
			//if (format.getMiddle()!=null){
			    GraphicNode from=dependency.getPredecessor();
			    GraphicNode to=dependency.getSuccessor();
//			    Rectangle2D fromBounds=scale(getBounds(from));
//			    Rectangle2D toBounds=scale(getBounds(to));
			    double[] fromPoints=new double[4];
			    double[] toPoints=new double[4];
			    updateLinkConnections(from,fromPoints);
			    updateLinkConnections(to,toPoints);
			    Point2D fromCenter=getCenter(from);
			    Point2D toCenter=getCenter(to);
			    if (fromCenter == null)
			    	return;
			    
				GeneralPath path=dependency.getPath();
				NetworkLinkRouting routing=(NetworkLinkRouting)getRouting();
				if (vertical) routing.routePath(path,fromCenter.getX(),fromPoints[3],toCenter.getX(),toPoints[2],(fromPoints[3]+toPoints[2])/2,dependency.getType());
				else routing.routePath(path,fromPoints[1],fromCenter.getY(),toPoints[0],toCenter.getY(),(fromPoints[1]+toPoints[0])/2,dependency.getType());
				
				
				
				Color oldColor=g2.getColor();
				Stroke oldStroke = g2.getStroke();
				Dependency dep = dependency.getDependency();
				if (dep != null && dep.isDisabled())
					g2.setStroke(GraphRenderer.DISABLED_LINK_STROKE);
				if (dep != null && dep.isCrossProject())
					g2.setColor(GraphRenderer.EXTERNAL_LINK_COLOR);
				else
					g2.setColor(format.getMiddle().getColor());
				g2.draw(path);
				
			//}
			if (format.getStart()==null&&format.getEnd()==null) return;
			if (format.getStart()!=null){
				double theta=routing.getFirstAngle();
				AffineTransform transform=(theta==0)?null:AffineTransform.getRotateInstance(theta,routing.getFirstX(),routing.getFirstY());
				drawLinkArrows(dep,transform,format.getStart(),routing.getFirstX(),routing.getFirstY());
			}
			if (format.getEnd()!=null){
				double theta=routing.getLastAngle();
				AffineTransform transform=(theta==Math.PI||theta==-Math.PI)?null:AffineTransform.getRotateInstance(Math.PI-theta,routing.getLastX(),routing.getLastY());
				drawLinkArrows(dep,transform,format.getEnd(),routing.getLastX(),routing.getLastY());
			}
			if (oldColor!=null) g2.setColor(oldColor);
			if (oldStroke!= null) g2.setStroke(oldStroke);

		}
		private void drawLinkArrows(Dependency dep, AffineTransform transform, TexturedShape shape, double x, double y) {
			Color oldEndColor = format.getEnd().getColor();
			if (dep != null && dep.isCrossProject())
				shape.setPaint(GraphRenderer.EXTERNAL_LINK_COLOR);
			g2.setColor(shape.getColor());
			LinkRouting routing=getRouting();
			shape.draw(g2,x,y,transform,useTextures());
			if (dep != null && dep.isCrossProject())
				shape.setPaint(oldEndColor);
		}

	}

	public void paintLink(Graphics2D g2, GraphicDependency dependency){
		BarStyles barStyles = graphInfo.getBarStyles();
		linkRenderer.initialize(g2,dependency);
		barStyles.apply(dependency,linkRenderer,true,false,false, false);
	}

	
	
	protected GeneralPath getShape(GraphicNode node){
		return scale(getNonScaledShape(node));
	}
	protected abstract GeneralPath getNonScaledShape(GraphicNode node);
	protected abstract void translateNonScaledShape(GraphicNode node, double dx,double dy);
	protected void translateShape(GraphicNode node, double dx,double dy){
		Point2D v=scaleVector_1(new Point2D.Double(dx,dy));
		translateNonScaledShape(node,v.getX(),v.getY());
	}
	protected abstract Point2D getNonScaledCenter(GraphicNode node);
	protected Point2D getCenter(GraphicNode node){
		return scale(getNonScaledCenter(node));
	}
	
	protected Rectangle getBounds(GraphicNode node){
		GeneralPath shape=getShape(node);
		if (shape==null) return null;
		else return shape.getBounds();
	}
	protected Rectangle getNonScaledBounds(GraphicNode node){
		GeneralPath shape=getNonScaledShape(node);
		if (shape==null) return null;
		else return shape.getBounds();
	}
    protected double[] segment=new double[6];
	protected void updateLinkConnections(GraphicNode node,double[] linkPoints){
		GeneralPath shape=getShape(node);
		if (shape==null) return;
		Point2D center=getCenter(node);
		linkPoints[0]=center.getX();
		linkPoints[1]=center.getX();
		linkPoints[2]=center.getY();
		linkPoints[3]=center.getY();
		double x0=0.0,y0=0.0,x1=0.0,y1=0.0,x2=0.0,y2=0.0,x,y;
		for (PathIterator j=shape.getPathIterator(null);!j.isDone();j.next()){
			int segmentType=j.currentSegment(segment);
			switch (segmentType) {
				case PathIterator.SEG_MOVETO:
					x0=segment[0];
					y0=segment[1];
					x2=x0;
					y2=y0;
					break;
				case PathIterator.SEG_LINETO:
					x2=segment[0];
					y2=segment[1];
				case PathIterator.SEG_CLOSE:
					if (segmentType==PathIterator.SEG_CLOSE){
						x2=x0;
						y2=y0;
					}
					//works only convex shapes
					double lambda;
					if (y2!=y1){
						x=(center.getY()-y1)*(x2-x1)/(y2-y1)+x1;
						lambda=(x2==x1)?0:(x-x1)/(x2-x1);
						if (x1==x2||(lambda>=0&&lambda<=1)){
							if (x<linkPoints[0]) linkPoints[0]=x;
							if (x>linkPoints[1]) linkPoints[1]=x;
						}
					}
					if (x2!=x1){
						y=(center.getX()-x1)*(y2-y1)/(x2-x1)+y1;
						lambda=(y2==x1)?0:(y-y1)/(y2-y1);
						if (y1==y2||(lambda>=0&&lambda<=1)){
							if (y<linkPoints[2]) linkPoints[2]=y;
							if (y>linkPoints[3]) linkPoints[3]=y;
						}
					}

					break;
			}
			x1=x2;
			y1=y2;
		}
	}

    public void paint(Graphics g) {
    	paint(g,null);
    }
    public void paint(Graphics g,Rectangle visibleBounds) {
	    	Graphics2D g2=(Graphics2D)g;

			Rectangle clipBounds = g2.getClipBounds();
			Rectangle svgClip=clipBounds;
			if (clipBounds==null){
				clipBounds=getGraphInfo().getDrawingBounds();
				//start at O,O because it's already translated
				if (visibleBounds==null) clipBounds=new Rectangle(0,0,clipBounds.width,clipBounds.height);
				else {
					clipBounds=visibleBounds;
					g2.setClip(clipBounds);
				}
			}
			//Modif for offline graphics
			
			GraphicDependency dependency;
			for (Iterator i=getDependenciesIterator();i.hasNext();){
				dependency=(GraphicDependency)i.next();
				paintLink(g2,dependency);
			}
			
			
			GraphicNode node;
			Rectangle bounds;
			for (ListIterator i=graphInfo.getCache().getIterator();i.hasNext();){
				node=(GraphicNode)i.next();
				bounds=getBounds(node);
				if (bounds==null) continue;
				if (clipBounds.intersects(bounds))
					paintNode(g2,node);
			}
			
			if (visibleBounds!=null) g2.setClip(svgClip);
	   }
	   
	   

		public GeneralPath scale(GeneralPath path){
			return ((NetworkParams)graphInfo).scale(path);
		}
		public Point2D scale(Point2D p){
			if (p == null)
				return null;
			return ((NetworkParams)graphInfo).scale(p);
		}
		public Point2D scaleVector(Point2D p){
			return ((NetworkParams)graphInfo).scaleVector(p);
		}
		public Point2D scaleVector_1(Point2D p){
			return ((NetworkParams)graphInfo).scaleVector_1(p);
		}
		
		public Rectangle scale(Rectangle r){
			return ((NetworkParams)graphInfo).scale(r);
		}
		
		public void paintNode(Graphics2D g,GraphicNode node){
			Rectangle bounds=getBounds(node);
			if (isEditing(node)){
				editor.paintEditor(node);
			}else{
				JComponent c=renderer.getRendererComponent(node,((NetworkParams)graphInfo).getZoom());
				if (container==null){
					//c=new JLabel("test");
			    	c.setDoubleBuffered(false);
			    	c.setOpaque(false);
					c.setSize(bounds.width, bounds.height);
			    	g.translate(bounds.x,bounds.y);
			    	c.doLayout();
			    	c.print(g);
			    	g.translate(-bounds.x,-bounds.y);
				}
				else rendererPane.paintComponent(g,c,container,bounds.x,bounds.y,bounds.width,bounds.height,true);
			}
		}

		public void resetForms(){
			renderer.resetForms();
			if (editor!=null) editor.resetForms();
		}
		public boolean isEditing(GraphicNode node) {
			if (editor==null) return false;
			return editor.isEditing(node);
		}


		public NetworkCellEditor getEditor() {
			return editor;
		}

		   public Iterator getDependenciesIterator(){
		   		return graphInfo.getCache().getEdgesIterator();
		   }

}

