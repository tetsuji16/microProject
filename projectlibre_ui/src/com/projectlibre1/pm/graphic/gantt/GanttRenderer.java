/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.gantt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;

import org.apache.commons.collections.Closure;

import com.projectlibre1.pm.graphic.gantt.link_routing.GanttLinkRouting;
import com.projectlibre1.pm.graphic.graph.GraphParams;
import com.projectlibre1.pm.graphic.graph.GraphRenderer;
import com.projectlibre1.pm.graphic.graph.LinkRouting;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldConverter;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.functor.ScheduleIntervalGenerator;
import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.graphic.configuration.TexturedShape;
import com.projectlibre1.graphic.configuration.shape.PredefinedPaint;
import com.projectlibre1.grouping.core.transform.TransformList;
import com.projectlibre1.grouping.core.transform.filtering.BaseFilter;
import com.projectlibre1.options.GanttOption;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.timescale.CalendarUtil;
import com.projectlibre1.timescale.TimeInterval;
import com.projectlibre1.timescale.TimeIterator;
import com.projectlibre1.util.DateTime;
import com.projectlibre1.util.Environment;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre1.util.GanttColorPalette;
import com.projectlibre1.util.MondayComPalette;
import com.projectlibre1.util.MondayGanttTheme;

public class GanttRenderer extends GraphRenderer implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -7437190083991277084L;
	private static final Stroke PROGRESS_LINE_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PROGRESS_LINE_HALO_STROKE = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final int PROGRESS_LINE_POINT_SIZE = 6;
	private static final Color LINK_ARROW_COLOR = new Color(0x5F, 0x64, 0x6D);
	protected NodeRenderer nodeRenderer = new NodeRenderer();
	protected LinkRenderer linkRenderer = new LinkRenderer();
	protected HorizontalLineRenderer horizontalLineRenderer = new HorizontalLineRenderer();
	protected AnnotationRenderer annotationRenderer = new AnnotationRenderer();

    protected GraphicConfiguration config;
    protected JComponent container;
    protected GanttColorPalette palette = new MondayComPalette(); // Default to Monday.com palette


	public GanttRenderer(){
		super();
		config=GraphicConfiguration.getInstance();
	}
	public GanttRenderer(GraphParams graphInfo){
		super(graphInfo);
		config=GraphicConfiguration.getInstance();
		if (graphInfo instanceof JComponent)
			container=(JComponent)graphInfo;
	}

    private Color getProgressLineColor() {
        return palette.getProjectLineColor();
    }
    
    private Color getProgressLineHaloColor() {
        return palette.getChartBackground();
    }

	private void enablePaintHints(Graphics2D g2) {
		FlatUiSupport.enableAntialiasing(g2);
	}

	private boolean isTextureEnabled() {
		return graphInfo != null && graphInfo.useTextures();
	}

    private Schedule getSchedule(Object impl) {
        if (impl instanceof Schedule)
            return (Schedule)impl;
        return null;
    }

	private Object getNodeImpl(GraphicNode node) {
		return node == null || node.getNode() == null ? null : node.getNode().getImpl();
	}
    
    /**
     * Get the current color palette.
     */
    public GanttColorPalette getPalette() {
        return palette;
    }
    
    /**
     * Set the color palette for Gantt rendering.
     */
    public void setPalette(GanttColorPalette palette) {
        if (palette != null) {
            this.palette = palette;
        }
    }

    private Color resolveTaskFillColor(GraphicNode node, BarFormat format, Schedule schedule) {
        if (isBaselineBarFormat(format))
            return palette.getBaselineBarColor();
        return palette.getStatusColor(schedule, getNodeImpl(node));
    }

	private Color resolveTaskFillColor(GraphicNode node, BarFormat format) {
		Object impl = getNodeImpl(node);
		return resolveTaskFillColor(node, format, getSchedule(impl));
	}

	private Color resolveAccentColor(GraphicNode node, BarFormat format, Color statusColor) {
		return palette.getAccentColor(format, statusColor, getNodeImpl(node));
	}

	private boolean isBaselineBarFormat(BarFormat format) {
		if (format == null || format.getId() == null)
			return false;
		String id = format.getId();
		return "Bar.baseline".equals(id) || id.startsWith("Bar.baseline");
	}

	private Color resolveAccentColor(GraphicNode node, BarFormat format) {
		Color statusColor = resolveTaskFillColor(node, format);
		return resolveAccentColor(node, format, statusColor);
	}

	private Paint createBarPaint(Color fillColor, Rectangle2D bounds, boolean backgroundLayer) {
		if (fillColor == null || bounds == null)
			return fillColor;
		return palette.createBarPaint(fillColor, bounds, backgroundLayer, isTextureEnabled());
	}

	private Shape drawConfiguredShape(TexturedShape shape, Graphics2D g2, double w, double h, double x, double y, Color fillColor, Color strokeColor, Rectangle2D bounds, boolean backgroundLayer) {
		Paint oldPaint = shape.getPaint();
		Color oldColor = shape.getColor();
		try {
			Color flatFill = (Color)palette.createBarPaint(fillColor, bounds, backgroundLayer, false);
			Paint paint = isTextureEnabled()
					? createBarPaint(fillColor, bounds, backgroundLayer)
					: new com.projectlibre1.graphic.configuration.shape.PredefinedPaint(
							com.projectlibre1.graphic.configuration.shape.PredefinedPaint.SOLID,
							flatFill,
							flatFill);
			shape.setPaint(paint);
			shape.setColor(strokeColor);
			return shape.draw(g2, w, h, x, y, isTextureEnabled());
		} finally {
			shape.setPaint(oldPaint);
			shape.setColor(oldColor);
		}
	}

	private boolean shouldUseModernCapsuleBar(BarFormat format) {
		if (format == null || format.getId() == null)
			return false;

		String id = format.getId();
		return "Bar.task".equals(id) || "Bar.critical".equals(id) || "Bar.assignment".equals(id) || "Bar.summary".equals(id);
	}

	private boolean isSummaryBarFormat(BarFormat format) {
		return format != null && "Bar.summary".equals(format.getId());
	}

	private boolean shouldUseUniformEndpointColor(BarFormat format) {
		return shouldUseModernCapsuleBar(format);
	}

	private boolean isAssignmentRowsVisible() {
		BaseFilter filter = (BaseFilter)TransformList.getInstance("hidden_filters").getTransform("Filter.Gantt");
		return filter != null && filter.isShowAssignments();
	}

	private boolean shouldSuppressTaskBarForAssignments(GraphicNode node, BarFormat format) {
		if (!isAssignmentRowsVisible() || node == null || format == null)
			return false;
		Object impl = getNodeImpl(node);
		if (!(impl instanceof NormalTask) || node.isSummary())
			return false;
		String formatId = format.getId();
		if (!"Bar.task".equals(formatId) && !"Bar.critical".equals(formatId))
			return false;
		return ((NormalTask)impl).hasRealAssignments();
	}

	private boolean shouldSuppressTaskAnnotationForAssignments(GraphicNode node) {
		if (!isAssignmentRowsVisible() || node == null)
			return false;
		Object impl = getNodeImpl(node);
		return impl instanceof NormalTask && !node.isSummary() && ((NormalTask)impl).hasRealAssignments();
	}

	private Rectangle2D createCapsuleBarBounds(double x, double y, double width, double height) {
		double safeWidth = Math.max(1.5d, width);
		double safeHeight = Math.max(2.0d, height);
		return new Rectangle2D.Double(x, y - safeHeight / 2.0d, safeWidth, safeHeight);
	}

	private Rectangle2D createSummaryBandBounds(double x, double y, double width, double height) {
		double safeWidth = Math.max(1.5d, width);
		double bandHeight = Math.max(4.0d, height * 0.34d);
		double topY = y - height / 2.0d;
		return new Rectangle2D.Double(x, topY, safeWidth, bandHeight);
	}

	private void paintCapsuleBar(Graphics2D g2, Rectangle2D bounds, Color fillColor, Color accentColor, boolean backgroundLayer) {
		if (g2 == null || bounds == null)
			return;

		Paint oldPaint = g2.getPaint();
		Color oldColor = g2.getColor();
		double arc = Math.min(bounds.getHeight(), bounds.getWidth());
		try {
			RoundRectangle2D outer = new RoundRectangle2D.Double(
					bounds.getX(),
					bounds.getY(),
					bounds.getWidth(),
					bounds.getHeight(),
					arc,
					arc);
			Paint outerPaint = createBarPaint(fillColor, bounds, backgroundLayer);
			if (outerPaint instanceof Color)
				g2.setColor((Color)outerPaint);
			else
				g2.setPaint(outerPaint);
			g2.fill(outer);

			if (!backgroundLayer)
				return;

			double inset = Math.max(0.75d, bounds.getHeight() * 0.12d);
			double innerWidth = Math.max(1.0d, bounds.getWidth() - inset * 2.0d);
			double innerHeight = Math.max(1.0d, bounds.getHeight() - inset * 2.0d);
			RoundRectangle2D inner = new RoundRectangle2D.Double(
					bounds.getX() + inset,
					bounds.getY() + inset,
					innerWidth,
					innerHeight,
					Math.min(innerHeight, innerWidth),
					Math.min(innerHeight, innerWidth));
			Paint innerPaint = createBarPaint(fillColor, inner.getBounds2D(), false);
			if (innerPaint instanceof Color)
				g2.setColor((Color)innerPaint);
			else
				g2.setPaint(innerPaint);
			g2.fill(inner);

			g2.setColor(accentColor);
			g2.draw(inner);
		} finally {
			g2.setPaint(oldPaint);
			g2.setColor(oldColor);
		}
	}

	private void paintSummaryBar(Graphics2D g2, Rectangle2D bounds, Color progressColor, Color accentColor, double progressRatio) {
		if (g2 == null || bounds == null)
			return;

		Paint oldPaint = g2.getPaint();
		Color oldColor = g2.getColor();
		double arc = Math.min(bounds.getHeight(), bounds.getWidth());
		try {
			Color baseColor = progressColor == null ? MondayGanttTheme.GROUP_A : progressColor;
			Color summaryFill = MondayGanttTheme.soften(baseColor, 0.58f);
			Color summaryStroke = MondayGanttTheme.shade(summaryFill, 0.18f);
			RoundRectangle2D backgroundBand = new RoundRectangle2D.Double(
					bounds.getX(),
					bounds.getY(),
					bounds.getWidth(),
					bounds.getHeight(),
					arc,
					arc);
			Paint backgroundPaint = createBarPaint(summaryFill, bounds, false);
			if (backgroundPaint instanceof Color)
				g2.setColor((Color)backgroundPaint);
			else
				g2.setPaint(backgroundPaint);
			g2.fill(backgroundBand);

			double clampedRatio = clampProgressValue(progressRatio);
			if (clampedRatio > 0.0d) {
				double progressWidth = Math.max(1.5d, bounds.getWidth() * clampedRatio);
				RoundRectangle2D progressBand = new RoundRectangle2D.Double(
						bounds.getX(),
						bounds.getY(),
						Math.min(bounds.getWidth(), progressWidth),
						bounds.getHeight(),
						arc,
						arc);
				Paint progressPaint = createBarPaint(baseColor, progressBand.getBounds2D(), false);
				if (progressPaint instanceof Color)
					g2.setColor((Color)progressPaint);
				else
					g2.setPaint(progressPaint);
				g2.fill(progressBand);
			}

			g2.setColor(summaryStroke);
			g2.draw(backgroundBand);
		} finally {
			g2.setPaint(oldPaint);
			g2.setColor(oldColor);
		}
	}

	private boolean shouldPaintProgressOverlay(GraphicNode node, BarFormat format) {
		if (node == null || format == null || !format.isMain() || !node.isStarted())
			return false;
		return "Bar.task".equals(format.getId()) || "Bar.critical".equals(format.getId()) || "Bar.summary".equals(format.getId());
	}

	static double progressRatioForSchedule(Schedule schedule) {
		return schedule == null ? 0.0d : clampProgressValue(schedule.getPercentComplete());
	}

	private double progressRatioFor(GraphicNode node) {
		Object impl = getNodeImpl(node);
		Schedule schedule = getSchedule(impl);
		return progressRatioForSchedule(schedule);
	}

	private Color resolveAnnotationColor(BarFormat format) {
		return FlatUiSupport.tableForeground();
	}

	private void paintVerticalMarkerLine(Graphics2D g2, Rectangle bounds, int x, PredefinedPaint paint) {
		if (x < bounds.getX() || x > bounds.getMaxX())
			return;
		Paint oldPaint = g2.getPaint();
		try {
			g2.setPaint(paint);
			g2.drawLine(x, bounds.y, x, bounds.y + bounds.height);
		} finally {
			g2.setPaint(oldPaint);
		}
	}



	private class NodeRenderer implements Closure, IntervalConsumer, Serializable {
		private static final long serialVersionUID = -1348039741030744803L;
		GraphicNode node;
		Graphics2D g2;
		protected GanttBarSingleIntervalGenerator singleIntervalGenerator=new GanttBarSingleIntervalGenerator();
		protected ScheduleInterval interval;
		protected BarFormat format;
		protected int yrow;
		protected int maxLayer=Integer.MAX_VALUE;
		protected int minLayer=0;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			yrow=node.getRow()*rowHeight;
			setLayers(BarFormat.MIN_FOREGROUND_LAYER,BarFormat.MAX_FOREGROUND_LAYER);
		}

		public int getMaxLayer() {
			return maxLayer;
		}
		public void setMaxLayer(int maxLayer) {
			this.maxLayer = maxLayer;
		}

		public int getMinLayer() {
			return minLayer;
		}
		public void setMinLayer(int minLayer) {
			this.minLayer = minLayer;
		}

		public void setLayers(int minLayer,int maxLayer) {
			this.minLayer = minLayer;
			this.maxLayer = maxLayer;
		}

/**
 * This is the callback which is called from barStyles.apply() below
 */
		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			if (format.getLayer()>maxLayer||format.getLayer()<minLayer) return;
			if (shouldSuppressTaskBarForAssignments(node, format)) return;



		    ScheduleIntervalGenerator intervalGenerator;
			if (format.getScheduleIntervalGenerator()==null){
				singleIntervalGenerator.initialize(graphInfo.getCache().getModel(),format.getFromField(),format.getToField());
				intervalGenerator=singleIntervalGenerator;
			}else{
				intervalGenerator=format.getScheduleIntervalGenerator();
			}

			intervalGenerator.consumeIntervals(node,this);

		}



		public void consumeInterval(ScheduleInterval interval){
//			System.out.println("GanttUI consuming interval " + new java.util.Date(interval.getStart()) + " " + new java.util.Date(interval.getEnd()));
//			if (interval.getEnd() < interval.getStart())
//				return;
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			if (interval.getEnd()>100000000000000L){
				// this hasn't happened in years. whatever caused it is fixed, but keeping just in case
				System.out.println("ERROR!!! leads to OutOfMemoryError, consumeInterval interval="+interval.getStart()+", "+CalendarUtil.toString(interval.getStart())+", "+interval.getEnd()+", "+CalendarUtil.toString(interval.getEnd())+"...");
				return;
			}
			double x=coord.toX(interval.getStart());
			double width=CoordinatesConverter.adaptSmallBarEndX(x,coord.toX(interval.getEnd()),node,config)-x;
//			double width=coord.toW(interval.getEnd()-interval.getStart());
			double height;
			double y=yrow+config.getGanttBarYOffset();
			int row=format.getRow();
		    if (row==1){
		    	height=config.getGanttBarHeight();
		    }
		    else{
		    	height=config.getBaselineHeight();
			    y+=config.getGanttBarHeight()+config.getBaselineHeight()*(row-2);
		    }
	    	y+=height/2;

			double dw=height;

			if (format.getMiddle()!=null){
				Color statusColor = resolveTaskFillColor(node, format);
				Color accentColor = resolveAccentColor(node, format, statusColor);
				Rectangle2D barBounds = createCapsuleBarBounds(x, y, width, height);
				Rectangle2D summaryBounds = createSummaryBandBounds(x, y, width, height);

				if (g2==null&&format.isMain()){
					if (isSummaryBarFormat(format)) {
						node.setGanttShapeOffset(summaryBounds.getY()-y+height/2);
						node.setGanttShapeHeight(Math.max(height, summaryBounds.getHeight()));
					} else if (shouldUseModernCapsuleBar(format)) {
						node.setGanttShapeOffset(0.0d);
						node.setGanttShapeHeight(barBounds.getHeight());
					} else {
						Shape shape=format.getMiddle().toGeneralPath(
								width,
								height,
								x,
								y,
								null);
						Rectangle2D bounds=shape.getBounds2D();
						node.setGanttShapeOffset(bounds.getY()-y+height/2);
						node.setGanttShapeHeight(bounds.getHeight());
					}
				}else if (g2 != null){
					if (isSummaryBarFormat(format))
						paintSummaryBar(g2, summaryBounds, statusColor, accentColor, progressRatioFor(node));
					else if (shouldUseModernCapsuleBar(format))
						paintCapsuleBar(g2, barBounds, statusColor, accentColor, true);
					else
						drawConfiguredShape(format.getMiddle(), g2, width, height, x, y, statusColor, accentColor, barBounds, true);

					// draw middle before ends
					if (shouldPaintProgressOverlay(node, format) && !isSummaryBarFormat(format)){
						long completedT=node.getCompleted();
						if (completedT>=interval.getStart()){
							double completedW=coord.toX(completedT)-x;
							if (completedW>width && !GanttOption.getInstance().isCompletionIsContiguous())
								completedW=width;
							completedW=CoordinatesConverter.adaptSmallBarEndX(x, x+completedW, node,config)-x;
							double progressHeight = config.getGanttProgressBarHeight();
							Rectangle2D progressBounds = createCapsuleBarBounds(x, y, completedW, progressHeight);
							paintCapsuleBar(g2, progressBounds, statusColor, accentColor, false);
						}
					}
				}
			}
			if (g2==null) return;

			Color statusColor = resolveTaskFillColor(node, format);
			Color accentColor = resolveAccentColor(node, format, statusColor);
			Color endpointColor = shouldUseUniformEndpointColor(format) ? statusColor : accentColor;
			if (format.getStart()!=null) drawConfiguredShape(format.getStart(), g2, dw, height, x , y, endpointColor, endpointColor, new Rectangle2D.Double(x, y - height / 2.0, dw, height), false);
			if (format.getEnd()!=null) drawConfiguredShape(format.getEnd(), g2, dw, height, x+width, y, endpointColor, endpointColor, new Rectangle2D.Double(x + width, y - height / 2.0, dw, height), false); //TODO case when no start symbol


		}

	}

	private class AnnotationRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -137778741030744803L;
		protected BarFormat format;
		GraphicNode node;
		Graphics2D g2;
		protected int yrow;
		FontMetrics fontMetrics;
		Font annotationFont;
		Set<String> renderedAnnotationKeys;
		boolean annotationRenderedForNode;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			config=((GanttParams)graphInfo).getConfiguration();
			yrow=node.getRow()*rowHeight;
			annotationFont = FlatUiSupport.uiFont().deriveFont(Font.PLAIN);
			fontMetrics = g2.getFontMetrics(annotationFont);
			renderedAnnotationKeys = new HashSet<String>();
			annotationRenderedForNode = false;
		}

		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			if (shouldSuppressTaskAnnotationForAssignments(node))
				return;
			Field field=format.getField();
			if (field==null) return;
			if (annotationRenderedForNode)
				return;
			String annotationKey = field.getName() + "|" + (format.getId() == null ? "" : format.getId());
			if (!renderedAnnotationKeys.add(annotationKey))
				return;
			Object value=getAnnotationValue(field);
			if (value==null) return;
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();

//			int y=yrow+config.getGanttBarHeight()+config.getGanttBarYOffset();
//			int x=(int)Math.ceil(coord.toX(node.getEnd()))+config.getGanttBarAnnotationXOffset();
//			Color oldColor=g2.getColor();
//			g2.setColor(format.getMiddle().getColor());
//			g2.drawString(ObjectConverterManager.toString(value,value.getClass()), x, y);
//			if (oldColor!=null) g2.setColor(oldColor);
			String s;
			if (value instanceof Date){
				Date d=(Date)value;
				s=DateFormat.getDateInstance(DateFormat.SHORT).format(d);
				int i=s.lastIndexOf('/');
				if (i>0) s=s.substring(0, i);
			}
			else s=FieldConverter.toString(value,value.getClass(),null);
			if (s==null||s.trim().length()==0) return;
			int y=yrow+config.getGanttBarYOffset();//+config.getGanttBarAnnotationYOffset();
			double x0=coord.toX(node.getStart());
			double x1=coord.toX(node.getEnd());
			x1=CoordinatesConverter.adaptSmallBarEndX(x0,x1,node,config);

			int h=config.getGanttBarHeight();
			int annotationOffset = config.getGanttBarAnnotationXOffset();
			Rectangle clipBounds = g2.getClipBounds();
			if (clipBounds == null)
				clipBounds = ((GanttParams)graphInfo).getGanttBounds();
			int clipLeft = clipBounds.x;
			int clipRight = clipBounds.x + clipBounds.width;
			int estimatedWidth = fontMetrics.stringWidth(s);
			int preferredRightX=(int)Math.ceil(x1)+annotationOffset;
			int preferredLeftX=(int)Math.floor(x0)-annotationOffset-estimatedWidth;
			boolean barVisible = x1 >= clipLeft && x0 <= clipRight;
			boolean rightLabelVisible = preferredRightX + estimatedWidth >= clipLeft && preferredRightX <= clipRight;
			boolean leftLabelVisible = preferredLeftX + estimatedWidth >= clipLeft && preferredLeftX <= clipRight;
			if (!barVisible && !rightLabelVisible && !leftLabelVisible)
				return;
			int minX = clipBounds.x + 4;
			int maxTextWidth = Math.max(64, Math.min(180, clipBounds.width / 5));
			int rightAvailableWidth = Math.min(maxTextWidth, clipRight - preferredRightX - 4);
			int x = preferredRightX;
			int availableWidth = rightAvailableWidth;
			if (availableWidth < 24 && preferredLeftX >= minX) {
				x = preferredLeftX;
				availableWidth = Math.min(maxTextWidth, clipRight - preferredLeftX - 4);
			}
			if (availableWidth <= 0)
				return;
			String clipped = clipAnnotationText(s, availableWidth);
			if (clipped == null || clipped.isEmpty())
				return;
			annotationRenderedForNode = true;
			int rowHeight = ((GanttParams)graphInfo).getRowHeight();
			int textTop = yrow + Math.max(0, (rowHeight - fontMetrics.getHeight()) / 2);
			int textBaseline = textTop + fontMetrics.getAscent();
			int clipHeight = Math.min(rowHeight, Math.max(fontMetrics.getHeight() + 2, h + 2));
			Rectangle originalClip = g2.getClipBounds();
			Font oldFont = g2.getFont();
			Color oldColor = g2.getColor();
			g2.setFont(annotationFont);
			g2.setColor(resolveAnnotationColor(format));
			g2.clipRect(x, textTop, availableWidth, clipHeight);
			g2.drawString(clipped, x, textBaseline);
			if (originalClip != null)
				g2.setClip(originalClip);
			g2.setFont(oldFont);
			g2.setColor(oldColor);
		}

		private String clipAnnotationText(String text, int availableWidth) {
			if (text == null)
				return null;
			String normalized = text.trim();
			if (normalized.isEmpty())
				return normalized;
			if (availableWidth <= 0)
				return null;
			if (fontMetrics.stringWidth(normalized) <= availableWidth)
				return normalized;
			String ellipsis = "...";
			int ellipsisWidth = fontMetrics.stringWidth(ellipsis);
			if (ellipsisWidth >= availableWidth)
				return normalized.substring(0, 1);
			int end = normalized.length();
			while (end > 1) {
				String candidate = normalized.substring(0, end) + ellipsis;
				if (fontMetrics.stringWidth(candidate) <= availableWidth)
					return candidate;
				end--;
			}
			return normalized.substring(0, 1);
		}

		private Object getAnnotationValue(Field field) {
			Object value=field.getValue(node.getNode(),graphInfo.getCache().getModel(),null);
			if (value!=null) return value;
			Object impl=getNodeImpl(node);
			return impl==null?null:field.getValue(impl,null);
		}



	}

	private class HorizontalLineRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -6350307720624037262L;
		protected BarFormat format;
		GraphicNode node;
		Graphics2D g2;
		protected int yrow;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			config=((GanttParams)graphInfo).getConfiguration();
			yrow=(node.getRow()+1)*rowHeight -1; // draws under each row

		}

		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			if (!((GanttParams)graphInfo).isGridLinesVisible()) {
				return;
			}
			Rectangle bounds = g2.getClipBounds();
			Stroke oldStroke = g2.getStroke();
			Color oldColor = g2.getColor();
			enablePaintHints(g2);
			g2.setColor(palette.getGridLine());
			g2.drawLine(bounds.x,yrow,bounds.x+bounds.width,yrow);
			g2.setColor(oldColor);
			g2.setStroke(oldStroke);
		}
	}


	private class LinkRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -2031158189787837110L;
		protected BarFormat format;
		protected GraphicDependency dependency;
		protected Graphics2D g2;
		void initialize(Graphics2D g2, GraphicDependency dependency) {
			this.g2 = g2;
			this.dependency = dependency;
		}


		private double[] extraPoints=new double[3];
		public void execute(Object arg0) {
			format = (BarFormat)arg0;

			GanttLinkRouting routing=(GanttLinkRouting)((GanttParams)graphInfo).getRouting();
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			//if (format.getMiddle()!=null){
			    GraphicNode from=dependency.getPredecessor();
			    GraphicNode to=dependency.getSuccessor();
			    int type=dependency.getType();
				int fromSign=(type==DependencyType.SF||type==DependencyType.SS)?-1:1;
				int toSign=(type==DependencyType.FS||type==DependencyType.SS)?-1:1;
				double fx0=coord.toX(from.getStart());
				double fx1=coord.toX(from.getEnd());
				fx1=CoordinatesConverter.adaptSmallBarEndX(fx0,fx1,from,config);
				double tx0=coord.toX(to.getStart());
				double tx1=coord.toX(to.getEnd());
				tx1=CoordinatesConverter.adaptSmallBarEndX(tx0,tx1,to,config);
				double x0=fromSign<0?fx0:fx1;
				double x1=toSign<0?tx0:tx1;
				int rowHeight=((GanttParams)graphInfo).getRowHeight();
				int yOffset=config.getGanttBarYOffset()+config.getGanttBarHeight()/2;
				int y0=rowHeight*from.getRow();
				int y1=rowHeight*to.getRow();
				double y2=Math.max(y0,y1);
				y0+=yOffset;
				y1+=yOffset;

				GeneralPath path=dependency.getPath();
				((GanttLinkRouting)routing).routePath(path,x0,y0,x1,y1,y2,y1+to.getGanttShapeHeight()/2,y1-to.getGanttShapeHeight()/2,type);



				enablePaintHints(g2);
				Color oldColor=g2.getColor();
				Stroke oldStroke = g2.getStroke();
				Dependency dep = dependency.getDependency();
				if (dep.isDisabled()) g2.setStroke(DISABLED_LINK_STROKE);
				Color linkColor = dep.isCrossProject() ? palette.getExternalLinkColor() : palette.getDependencyLinkColor();
				g2.setColor(linkColor);
				g2.draw(path);

			//}
			try {
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
			} finally {
				if (oldColor!=null) g2.setColor(oldColor);
				if (oldStroke!= null) g2.setStroke(oldStroke);
			}
		}

		private void drawLinkArrows(Dependency dep, AffineTransform transform, TexturedShape shape, double x, double y) {
			Paint oldPaint = shape.getPaint();
			Color oldEndColor = shape.getColor();
			Color linkColor = dep.isCrossProject() ? palette.getExternalLinkColor() : palette.getDependencyLinkColor();
			shape.setPaint(linkColor);
			shape.setColor(linkColor);
			g2.setColor(linkColor);
			shape.draw(g2,x,y,transform,useTextures());
			shape.setPaint(oldPaint);
			shape.setColor(oldEndColor);
		}
	}


    public void updateShapes(ListIterator nodeIterator){

    	Rectangle bounds = ((GanttParams)graphInfo).getGanttBounds();
    	CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
    	if (coord==null) return;
		double rowHeight=((GanttParams)graphInfo).getRowHeight();

		int i0=(int)Math.floor(bounds.getY()/rowHeight);
		int i1=(int)Math.ceil(bounds.getMaxY()/rowHeight);

		GraphicNode node;
		@SuppressWarnings("unchecked")
		ListIterator<GraphicNode> i=((GanttParams)graphInfo).getCache().getIterator(i0);
		while (i.hasNext()&&i.nextIndex()<i1){
			int row=i.nextIndex();
			node=i.next();
			node.setRow(row);
			if (!node.isVoid()) updateShape(node);
		}
    }

    public void updateShape(GraphicNode node){
    	if (((GanttParams)graphInfo).getCoord()==null) return; //not initialized
    	BarStyles barStyles = graphInfo.getBarStyles();
		if (barStyles == null) return;
		nodeRenderer.initialize(null,node);
		barStyles.apply(node.getNode().getImpl(),nodeRenderer);

    }

	public void paintNode(Graphics2D g2,GraphicNode node, boolean background){
		BarStyles barStyles = graphInfo.getBarStyles();
		if (barStyles == null) return;
		enablePaintHints(g2);
		nodeRenderer.initialize(g2,node);

		if (background)
			nodeRenderer.setLayers(BarFormat.MIN_BACKGROUND_LAYER,BarFormat.MAX_BACKGROUND_LAYER);
		else nodeRenderer.setLayers(BarFormat.MIN_FOREGROUND_LAYER,BarFormat.MAX_FOREGROUND_LAYER);
		barStyles.apply(node.getNode().getImpl(),nodeRenderer);

	}

	public void paintAnnotation(Graphics2D g2,GraphicNode node){
		BarStyles barStyles = graphInfo.getBarStyles();
		if (barStyles == null) return;
		enablePaintHints(g2);
		annotationRenderer.initialize(g2,node);
		barStyles.apply(node.getNode().getImpl(),annotationRenderer,false,true,false, false);
	}

	public void paintHorizontalLine(Graphics2D g2,GraphicNode node){
		BarStyles barStyles = graphInfo.getBarStyles();
		if (barStyles == null) return;
		enablePaintHints(g2);
		horizontalLineRenderer.initialize(g2,node);
		barStyles.apply(node.getNode().getImpl(),horizontalLineRenderer,false,false,false, true);
	}

	public void paintLink(Graphics2D g2, GraphicDependency dependency){
		BarStyles barStyles = graphInfo.getBarStyles();
		if (barStyles == null) return;
		enablePaintHints(g2);
		linkRenderer.initialize(g2,dependency);
		barStyles.apply(dependency,linkRenderer,true,false,false, false);
	}

	private void paintChartBackground(Graphics2D g2, Rectangle bounds) {
		if (g2 == null || bounds == null)
			return;
		Paint oldPaint = g2.getPaint();
        g2.setColor(palette.getChartBackground());
		g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		if (oldPaint != null)
			g2.setPaint(oldPaint);
	}

	private void paintProgressLine(Graphics2D g2) {
		if (!(graphInfo instanceof Gantt))
			return;

		Gantt gantt = (Gantt) graphInfo;
		if (!gantt.isProgressLineEnabled())
			return;

		GeneralPath path = createProgressLinePath();
		if (path == null)
			return;

		Color oldColor = g2.getColor();
		Stroke oldStroke = g2.getStroke();
		paintProgressLinePath(g2, path, getProgressLineHaloColor(), PROGRESS_LINE_HALO_STROKE);
		paintProgressLinePath(g2, path, getProgressLineColor(), PROGRESS_LINE_STROKE);
		paintProgressLinePoints(g2);
		if (oldColor != null)
			g2.setColor(oldColor);
		if (oldStroke != null)
			g2.setStroke(oldStroke);
	}

	private void paintProgressLinePath(Graphics2D g2, GeneralPath path, Color color, Stroke stroke) {
		enablePaintHints(g2);
		g2.setColor(color);
		g2.setStroke(stroke);
		g2.draw(path);
	}

	private void paintProgressLinePoints(Graphics2D g2) {
		CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
		if (coord == null)
			return;
		int size = PROGRESS_LINE_POINT_SIZE;
		int half = size / 2;
		for (Iterator<GraphicNode> i=nodeList.iterator(); i.hasNext();) {
			GraphicNode node = i.next();
			if (!shouldIncludeInProgressLine(node))
				continue;
			Task task = (Task)node.getNode().getImpl();
			int x = (int)Math.round(getProgressLineX(coord, task));
			int y = (int)Math.round(getProgressLineY(node));
			g2.setColor(getProgressLineHaloColor());
			g2.fillOval(x - half - 1, y - half - 1, size + 2, size + 2);
			g2.setColor(getProgressLineColor());
			g2.fillOval(x - half, y - half, size, size);
		}
	}

	private GeneralPath createProgressLinePath() {
		CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
		if (coord == null)
			return null;

		GeneralPath path = null;
		for (Iterator<GraphicNode> i=nodeList.iterator(); i.hasNext();) {
			GraphicNode node = i.next();
			if (!shouldIncludeInProgressLine(node))
				continue;

			Task task = (Task)node.getNode().getImpl();
			double progressX = getProgressLineX(coord, task);
			double y = getProgressLineY(node);
			if (path == null) {
				path = new GeneralPath();
				path.moveTo((float)progressX, (float)y);
			} else {
				path.lineTo((float)progressX, (float)y);
			}
		}
		return path;
	}

	private boolean shouldIncludeInProgressLine(GraphicNode node) {
		if (node == null || !node.isSchedule() || node.isAssignment() || node.getNode() == null)
			return false;

		Object impl = node.getNode().getImpl();
		if (!(impl instanceof Task))
			return false;

		Task task = (Task)impl;
		if (node.isSummary() && !node.isCollapsed())
			return false;
		if (task.isMilestone() || task.isExternal() || task.isSubproject())
			return false;

		long start = task.getStart();
		long end = task.getEnd();
		return start != 0L && end > start;
	}

	private double getProgressLineX(CoordinatesConverter coord, Task task) {
		long start = task.getStart();
		long end = task.getEnd();
		double progress = clampProgressValue(task.getPercentComplete());
		long today = getProgressReferenceDate(task);
		long progressDate;
		if (today != 0L && progress == 1.0d && end <= today)
			progressDate = today;
		else if (today != 0L && progress == 0.0d && start >= today)
			progressDate = today;
		else
			progressDate = start + Math.round((end - start) * progress);
		return coord.toX(progressDate);
	}

	static long getProgressReferenceDate(Task task) {
		Project project = task.getProject();
		return project == null ? 0L : project.getStatusDate();
	}

	private double getProgressLineY(GraphicNode node) {
		int rowHeight=((GanttParams)graphInfo).getRowHeight();
		int yOffset=config.getGanttBarYOffset()+config.getGanttBarHeight()/2;
		return rowHeight*node.getRow()+yOffset;
	}

	static double clampProgressValue(double value) {
		if (value < 0.0d)
			return 0.0d;
		if (value > 1.0d)
			return 1.0d;
		return value;
	}


	protected BarFormat calendarFormat;
	protected Closure calendarClosure=new Closure(){
		public void execute(Object arg0) {
			calendarFormat = (BarFormat)arg0;
		}
	};
	protected BarFormat getCalendarFormat(){
		calendarFormat=null;
		if (calendarFormat==null){
			BarStyles barStyles = graphInfo.getBarStyles();
			if (barStyles == null) {
				return null;
			}
			barStyles.apply(null,calendarClosure,false,false,true, false);
		}
		return calendarFormat;
	}

	public void paintNonWorkingDays(Graphics2D g2,Rectangle bounds){
		BarFormat calFormat=getCalendarFormat();
		if (calFormat==null) return;
		//non working days
			Color oldColor=g2.getColor();
			Paint oldPaint=g2.getPaint();
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			Project project=coord.getProject();
			WorkingCalendar wc=(WorkingCalendar)project.getWorkCalendar();

			if (coord.getTimescaleManager().isShowWholeDays()){
				boolean useScale2=coord.getTimescaleManager().getCurrentScaleIndex()==0; //valid only for current time scales
				TimeIterator i=coord.getTimeIterator(bounds.getX(), bounds.getMaxX(),useScale2);
				long startNonworking=-1L,endNonWorking=-1L;
				Calendar cal=DateTime.calendarInstance();

				PredefinedPaint paint=(PredefinedPaint)calFormat.getMiddle().getPaint();//new PredefinedPaint(PredefinedPaint.DOT_LINE,Colors.VERY_LIGHT_GRAY,Color.WHITE);
				paint.applyPaint(g2, useTextures());
				while (i.hasNext()){
					TimeInterval interval=i.next();
					long s=interval.getStart();
					if (CalendarService.getInstance().getDay(wc, s).isWorking()){
						if (startNonworking!=-1L){
							drawNonWorking(g2, startNonworking, endNonWorking, cal, coord, bounds,useScale2);
							startNonworking=endNonWorking=-1L;
						}
					}else{
						if (startNonworking==-1L) startNonworking=s;
						endNonWorking=s;

					}
				}
				if (startNonworking!=-1L){
					drawNonWorking(g2, startNonworking, endNonWorking, cal, coord, bounds,useScale2);
					startNonworking=endNonWorking=-1L;
				}
			}

		if (container!=null){

//Slow with with Java >6
//			//scale2 separation lines
//			TimeIterator i=coord.getTimeIterator(bounds.getX(), bounds.getMaxX(),true);
//			g2.setPaint(new PredefinedPaint(PredefinedPaint.DOT_LINE2,Color.GRAY,g2.getBackground()));
//			while (i.hasNext()){
//				TimeInterval interval=i.next();
//				int startX=(int)Math.round(coord.toX(interval.getStart()));
//				g2.drawLine(startX,bounds.y,startX,bounds.y+bounds.height);
//			}

			//project start
			int projectStartX=(int)Math.round(coord.toX(project.getStart()));
			paintVerticalMarkerLine(g2, bounds, projectStartX,
					new PredefinedPaint(PredefinedPaint.DASH_LINE, palette.getProjectLineColor(), g2.getBackground()));

			//project start
			long statusDate = project.getStatusDate();
			if (statusDate != 0) {
				int statusDateX=(int)Math.round(coord.toX(statusDate));
				paintVerticalMarkerLine(g2, bounds, statusDateX,
						new PredefinedPaint(PredefinedPaint.DOT_LINE2, palette.getStatusDateLineColor(), g2.getBackground()));
			}


			if (oldColor!=null) g2.setColor(oldColor);
			if (oldPaint!=null) g2.setPaint(oldPaint);

		}
	}

	private void drawNonWorking(Graphics2D g2,long startNonworking,long endNonWorking, Calendar cal,CoordinatesConverter coord, Rectangle bounds,boolean userScale2){
		cal.setTimeInMillis(endNonWorking);
		if (userScale2) coord.getTimescaleManager().getScale().increment2(cal);
		else coord.getTimescaleManager().getScale().increment1(cal);
		endNonWorking=cal.getTimeInMillis();
		g2.fillRect((int)Math.round(coord.toX(startNonworking)), bounds.y, (int)Math.round(coord.toW(endNonWorking-startNonworking)), bounds.height);
	}


	ArrayList<GraphicNode> nodeList = new ArrayList<GraphicNode>();
    public void paint(Graphics g) {
    	paint(g,null);
    }
    public void paint(Graphics g,Rectangle visibleBounds) {
		Graphics2D g2=(Graphics2D)g;
    	//CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();

		Rectangle clipBounds = g2.getClipBounds();
		Rectangle svgClip=clipBounds;
		if (clipBounds==null){
			clipBounds=((GanttParams)getGraphInfo()).getGanttBounds();
			//start at O,O because it's already translated
			if (visibleBounds==null) clipBounds=new Rectangle(0,1,clipBounds.width,clipBounds.height-2);//1 pixel offset needed for edge
//			else clipBounds=new Rectangle(visibleBounds.x-clipBounds.x,visibleBounds.y-clipBounds.y,visibleBounds.width,visibleBounds.height);
			else {
				clipBounds=visibleBounds;
				g2.setClip(clipBounds);
			}
		}

		paintChartBackground(g2, clipBounds);
		paintNonWorkingDays(g2,clipBounds);

		//Modif for offline graphics

		double rowHeight=((GanttParams)graphInfo).getRowHeight();

		int i0=(int)Math.floor(clipBounds.getY()/rowHeight);
		int i1;
		if (visibleBounds==null) i1=(int)Math.ceil(clipBounds.getMaxY()/rowHeight);
		else i1=(int)Math.floor(clipBounds.getMaxY()/rowHeight);
		//double t0=coord.toTime(clipBounds.getX());
		//double t1=coord.toTime(clipBounds.getMaxX());

		nodeList.clear();

		GraphicNode node;
//		for (ListIterator i=graph.getModel().getNodeIterator(i0);i.hasNext()&&i.nextIndex()<=i1;){
//			node=(GraphicNode)i.next();
//			if (!node.isSchedule()) continue;
//			nodeList.add(node);
//			node.setRow(i.previousIndex());
//			paintNode(g2,node,true);
//		} //Because row not initialized for some nodes

		NodeModelCache cache=graphInfo.getCache();
		@SuppressWarnings("unchecked")
		ListIterator<GraphicNode> i=cache.getIterator(i0);
		for (;i.hasNext()&&i.nextIndex()<i1;){
			int row=i.nextIndex();
			node=i.next();
			node.setRow(row);
			if (!node.isSchedule()) continue;
			nodeList.add(node);
			paintAnnotation(g2,node);
			paintNode(g2,node,true);
			paintHorizontalLine(g2,node);
		}

		GraphicDependency dependency;
		@SuppressWarnings("unchecked")
		Iterator<GraphicDependency> dependencyIterator = cache.getVisibleDependencies().getIterator();
		for (;dependencyIterator.hasNext();){
			dependency=dependencyIterator.next();
			//if (nodeList.contains(dependency.getPredecessor())||nodeList.contains(dependency.getSuccessor()))
				paintLink(g2,dependency);
		}

		for (ListIterator<GraphicNode> nodeIterator=nodeList.listIterator();nodeIterator.hasNext();){
			node=nodeIterator.next();
			paintNode(g2,node,false);
		}
		paintProgressLine(g2);

		if (visibleBounds!=null) g2.setClip(svgClip);

	}



}
