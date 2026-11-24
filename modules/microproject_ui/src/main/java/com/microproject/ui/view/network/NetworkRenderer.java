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


import com.microproject.ui.view.graph.GraphParams;
import com.microproject.ui.view.graph.GraphRenderer;
import com.microproject.ui.view.graph.LinkRouting;
import com.microproject.ui.view.model.cache.GraphicDependency;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.link_routing.NetworkLinkRouting;
import com.microproject.ui.view.network.rendering.NetworkCellEditor;
import com.microproject.ui.view.network.rendering.NetworkCellRenderer;
import com.microproject.view.configuration.BarFormat;
import com.microproject.view.configuration.BarStyles;
import com.microproject.view.configuration.GraphicConfiguration;
import com.microproject.view.configuration.TexturedShape;
import com.microproject.model.dependency.Dependency;
import com.microproject.ui.theme.MicroProjectTheme;
import com.microproject.ui.theme.MicroProjectThemeTokens;

public abstract class NetworkRenderer extends GraphRenderer{
	// Visual Studio's Fluent SystemFillSolidNeutral dark value; this is a product-theme choice, not an MSP-prescribed value.
	// https://learn.microsoft.com/visualstudio/extensibility/ux-guidelines/theme-color-token-reference
	private static final Color DARK_NEUTRAL_CONNECTOR_COLOR = new Color(0x8A8A8A);
	private final DarkLinkArrowShapeCache darkLinkArrowShapeCache = new DarkLinkArrowShapeCache();
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
		container=graphInfoContainer(container);
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
				else if (isDefaultDarkLink(format))
					g2.setColor(defaultDarkLinkColor());
				else
					g2.setColor(format.getMiddle().getColor());
				g2.draw(path);
				if (((NetworkParams)graphInfo).getDisplayOptions().isLinkLabels())
					drawLinkLabel(g2, linkLabelText(dependency), routing);
				
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
		private String linkLabelText(GraphicDependency dependency){
			Dependency dep=dependency.getDependency();
			if (dep==null) return NetworkDisplayOptions.linkLabel(dependency.getType(),0L);
			com.microproject.datatype.DurationDefaults defaults=null;
			if (dep.getSuccessor() instanceof com.microproject.pm.task.Task task&&task.getProject()!=null) defaults=task.getProject().getDurationDefaults();
			return NetworkDisplayOptions.linkLabel(dependency.getType(),dep.getLag(),defaults);
		}
		private void drawLinkLabel(Graphics2D g, String text, LinkRouting routing) {
			java.awt.Font oldFont = g.getFont();
			g.setFont(oldFont.deriveFont(Math.max(8f, oldFont.getSize2D() - 2f)));
			java.awt.FontMetrics metrics = g.getFontMetrics();
			float x = (routing.getFirstX() + routing.getLastX()) / 2f - metrics.stringWidth(text) / 2f;
			float y = (routing.getFirstY() + routing.getLastY()) / 2f - 2f;
			g.drawString(text, x, y);
			g.setFont(oldFont);
		}
		private void drawLinkArrows(Dependency dep, AffineTransform transform, TexturedShape shape, double x, double y) {
			Color oldEndColor = format.getEnd().getColor();
			if (dep != null && dep.isCrossProject())
				shape.setPaint(GraphRenderer.EXTERNAL_LINK_COLOR);
			g2.setColor(shape.getColor());
			LinkRouting routing=getRouting();
			TexturedShape drawShape = shape;
			if (isDefaultDarkLink(format) && (dep == null || !dep.isCrossProject())) {
				drawShape = darkLinkArrowShapeCache.resolve(shape, defaultDarkLinkColor());
				g2.setColor(defaultDarkLinkColor());
			}
			drawShape.draw(g2,x,y,transform,useTextures());
			if (dep != null && dep.isCrossProject())
				shape.setPaint(oldEndColor);
		}

		private boolean isDefaultDarkLink(BarFormat format) {
			if (format == null || !"Link.link1".equals(format.getId()) || format.getStart() != null
				|| format.getEnd() == null || format.getMiddle() == null
				|| !MicroProjectTheme.tokens().appBackground().equals(MicroProjectThemeTokens.dark().appBackground())) return false;
			TexturedShape line = format.getMiddle();
			TexturedShape arrow = format.getEnd();
			return "DARK_SLATE_GRAY".equals(line.getColorName()) && "SOLID".equals(line.getPaintName())
				&& "SOLID".equals(line.getStrokeName()) && line.getShapeName() == null
				&& "LINK_ARROW1".equals(arrow.getShapeName()) && "DARK_SLATE_GRAY".equals(arrow.getColorName())
				&& "SOLID".equals(arrow.getPaintName()) && "SOLID".equals(arrow.getStrokeName())
				&& arrow.getShapeScaleX() == 10d && arrow.getShapeScaleY() == 5d;
		}

		private Color defaultDarkLinkColor() {
			return DARK_NEUTRAL_CONNECTOR_COLOR;
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
	// SEG_LINETO deliberately continues into the SEG_CLOSE branch, which draws the segment.
	@SuppressWarnings("fallthrough")
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
			Iterator<GraphicDependency> dependencies = getDependenciesIterator();
			while (dependencies.hasNext()) paintLink(g2, dependencies.next());
			
			
			GraphicNode node;
			Rectangle bounds;
			for (ListIterator<GraphicNode> i = graphInfo.getCache().getIterator(); i.hasNext();) {
				node = i.next();
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
			paintNodeContent(g,node,bounds);
			paintNodeDecorations(g,node,bounds);
		}

		/** Collapsed boxes show the task ID only. */
		private void paintCollapsedBox(Graphics2D g,GraphicNode node,Rectangle bounds){
			Object impl=node.getNode().getImpl();
			String id=impl instanceof com.microproject.pm.task.Task task?Long.toString(task.getId()):"";
			Color oldColor=g.getColor();
			g.setColor(javax.swing.UIManager.getColor("Table.background")==null?Color.WHITE:javax.swing.UIManager.getColor("Table.background"));
			NetworkDisplayOptions.BoxStyle style=((NetworkParams)graphInfo).getDisplayOptions().getStyle(NetworkDisplayOptions.boxType(impl));
			java.awt.Shape outline=NetworkDisplayOptions.outline(style.shape(),new java.awt.geom.Rectangle2D.Double(bounds.x,bounds.y,bounds.width-1,bounds.height-1));
			g.fill(outline);
			Color border=style.color()!=null?style.color():javax.swing.UIManager.getColor("Table.foreground")==null?Color.DARK_GRAY:javax.swing.UIManager.getColor("Table.foreground");
			g.setColor(border);
			g.draw(outline);
			g.setColor(javax.swing.UIManager.getColor("Table.foreground")==null?Color.DARK_GRAY:javax.swing.UIManager.getColor("Table.foreground"));
			java.awt.FontMetrics metrics=g.getFontMetrics();
			g.drawString(id,bounds.x+(bounds.width-metrics.stringWidth(id))/2,bounds.y+(bounds.height+metrics.getAscent()-metrics.getDescent())/2);
			g.setColor(oldColor);
		}

		/** Progress marks, drawn over the box (the border style is part of the box itself, see FormComponent). */
		private void paintNodeDecorations(Graphics2D g,GraphicNode node,Rectangle bounds){
			NetworkDisplayOptions options=((NetworkParams)graphInfo).getDisplayOptions();
			Object impl=node.getNode().getImpl();
			Color oldColor=g.getColor();
			Stroke oldStroke=g.getStroke();
			if (options.isProgressMarks()&&impl instanceof com.microproject.pm.task.Task task){
				NetworkDisplayOptions.ProgressMark mark=NetworkDisplayOptions.progressMark(task.getPercentComplete());
				if (mark!=NetworkDisplayOptions.ProgressMark.NONE){
					g.setColor(javax.swing.UIManager.getColor("Table.foreground")==null?Color.DARK_GRAY:javax.swing.UIManager.getColor("Table.foreground"));
					g.setStroke(new java.awt.BasicStroke(1.5f));
					g.drawLine(bounds.x,bounds.y+bounds.height,bounds.x+bounds.width,bounds.y);
					if (mark==NetworkDisplayOptions.ProgressMark.CROSS)
						g.drawLine(bounds.x,bounds.y,bounds.x+bounds.width,bounds.y+bounds.height);
				}
			}
			g.setColor(oldColor);
			g.setStroke(oldStroke);
		}

		private void paintNodeContent(Graphics2D g,GraphicNode node,Rectangle bounds){
			if (((NetworkParams)graphInfo).getDisplayOptions().isCollapsed()){
				paintCollapsedBox(g,node,bounds);
				return;
			}
			if (isEditing(node)){
				editor.paintEditor(node);
			}else{
				JComponent c=renderer.getRendererComponent(node,((NetworkParams)graphInfo).getZoom());
				if (c == null) return;
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

		   public Iterator<GraphicDependency> getDependenciesIterator(){
		   		return graphInfo.getCache().getEdgesIterator();
		   }

}

