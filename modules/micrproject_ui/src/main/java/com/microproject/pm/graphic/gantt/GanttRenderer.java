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
package com.microproject.pm.graphic.gantt;

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
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;


import com.microproject.pm.graphic.gantt.link_routing.GanttLinkRouting;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.GraphRenderer;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.configuration.Configuration;
import com.microproject.functor.IntervalConsumer;
import com.microproject.functor.ScheduleIntervalGenerator;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.graphic.configuration.shape.PredefinedPaint;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.CommonTransform;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.options.GanttOption;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.timescale.CalendarUtil;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttColorPalette;
import com.microproject.util.GanttProgress;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.MondayComPalette;
import com.microproject.util.MondayGanttTheme;

public class GanttRenderer extends GraphRenderer implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -7437190083991277084L;
	private static final Logger logger = Logger.getLogger(GanttRenderer.class.getName());
	private static final Stroke PROGRESS_LINE_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PROGRESS_LINE_HALO_STROKE = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PROGRESS_BAR_STROKE = new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SPLIT_CONNECTOR_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.5f, 2.5f }, 0.0f);
	private static final int PROGRESS_LINE_POINT_SIZE = 6;
	protected NodeRenderer nodeRenderer = new NodeRenderer();
	protected LinkRenderer linkRenderer = new LinkRenderer();
	protected HorizontalLineRenderer horizontalLineRenderer = new HorizontalLineRenderer();
	protected AnnotationRenderer annotationRenderer = new AnnotationRenderer();

    protected GraphicConfiguration config;
    protected JComponent container;
	protected GanttColorPalette palette = new MondayComPalette();

	/** Colors resolved exactly as an automatically formatted task bar is painted. */
	public record DisplayedBarColors(Integer startRgb, Integer middleRgb, Integer endRgb) {
	}


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

	public DisplayedBarColors resolveDisplayedBarColors(Task task) {
		if (task == null)
			return new DisplayedBarColors(BarColorField.DEFAULT_BAR_RGB, BarColorField.DEFAULT_BAR_RGB,
					BarColorField.DEFAULT_BAR_RGB);
		BarFormat format = resolveMainBarFormat(task);
		Color middle = task.isCritical() ? palette.getCriticalTaskColor() : palette.getStatusColor(task, task);
		Color endpoint = format != null && GanttBarSupport.shouldUseUniformEndpointColor(format)
				? middle
				: palette.getAccentColor(format, middle, task);
		GanttBarFormatOverrides.BarFormat individual = graphInfo instanceof Gantt gantt
				&& GanttBarSupport.isIndividuallyFormattable(format)
				? gantt.getBarFormat(task)
				: GanttBarFormatOverrides.BarFormat.automatic();
		return new DisplayedBarColors(
				individual.getStartRgb() == null ? endpoint.getRGB() & 0x00FFFFFF : individual.getStartRgb(),
				individual.getMiddleRgb() == null ? middle.getRGB() & 0x00FFFFFF : individual.getMiddleRgb(),
				individual.getEndRgb() == null ? endpoint.getRGB() & 0x00FFFFFF : individual.getEndRgb());
	}

	private BarFormat resolveMainBarFormat(Task task) {
		BarStyles barStyles = graphInfo == null ? null : graphInfo.getBarStyles();
		if (barStyles == null)
			return null;
		final BarFormat[] mainFormat = new BarFormat[1];
		barStyles.apply(task, argument -> {
			BarFormat candidate = (BarFormat)argument;
			if (mainFormat[0] == null && candidate.isMain())
				mainFormat[0] = candidate;
		});
		return mainFormat[0];
	}

    private Color resolveTaskFillColor(GraphicNode node, BarFormat format, Schedule schedule) {
        Color defaultColor = GanttBarSupport.isBaselineBarFormat(format)
                ? palette.getBaselineBarColor()
                : palette.getStatusColor(schedule, getNodeImpl(node));
        GanttBarFormatOverrides.BarFormat individualFormat = getIndividualBarFormat(node, format);
        if (individualFormat.getMiddleRgb() != null)
            return new Color(individualFormat.getMiddleRgb());
        if (!GanttBarSupport.isBaselineBarFormat(format) && isCriticalTask(getNodeImpl(node)))
            return palette.getCriticalTaskColor();
        return defaultColor;
    }

	private boolean isCriticalTask(Object impl) {
		return impl instanceof Task task && task.isCritical();
	}

	private Color resolveTaskFillColor(GraphicNode node, BarFormat format) {
		Object impl = getNodeImpl(node);
		return resolveTaskFillColor(node, format, getSchedule(impl));
	}

	private Color resolveProgressFillColor(GraphicNode node) {
		Object impl = getNodeImpl(node);
		return palette.getStatusColor(getSchedule(impl), impl);
	}

	private Color resolveAccentColor(GraphicNode node, BarFormat format, Color statusColor) {
		return palette.getAccentColor(format, statusColor, getNodeImpl(node));
	}

	private Color resolveAccentColor(GraphicNode node, BarFormat format) {
		Color statusColor = resolveTaskFillColor(node, format);
		return resolveAccentColor(node, format, statusColor);
	}

	private GanttBarFormatOverrides.BarFormat getIndividualBarFormat(GraphicNode node, BarFormat format) {
		Object impl = getNodeImpl(node);
		if (!(graphInfo instanceof Gantt gantt)
				|| !(impl instanceof Task task)
				|| !GanttBarSupport.isIndividuallyFormattable(format))
			return GanttBarFormatOverrides.BarFormat.automatic();
		return gantt.getBarFormat(task);
	}

	private Color resolveEndpointColor(GraphicNode node, BarFormat format, Color defaultColor, boolean start) {
		GanttBarFormatOverrides.BarFormat individualFormat = getIndividualBarFormat(node, format);
		Integer rgb = start ? individualFormat.getStartRgb() : individualFormat.getEndRgb();
		return rgb == null ? defaultColor : new Color(rgb);
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
			Paint paint = isTextureEnabled()
					? createBarPaint(fillColor, bounds, backgroundLayer)
					: palette.createBarPaint(fillColor, bounds, backgroundLayer, false);
			shape.setPaint(paint);
			shape.setColor(strokeColor);
			return shape.draw(g2, w, h, x, y, isTextureEnabled());
		} finally {
			shape.setPaint(oldPaint);
			shape.setColor(oldColor);
		}
	}

	private boolean shouldSuppressTaskBarForAssignments(GraphicNode node, BarFormat format) {
		return GanttRendererSupport.shouldSuppressTaskBarForAssignments(
				getNodeImpl(node),
				node != null && node.isSummary(),
				format,
				isAssignmentRowsVisible());
	}

	private boolean shouldSuppressTaskAnnotationForAssignments(GraphicNode node) {
		return GanttRendererSupport.shouldSuppressTaskAnnotationForAssignments(
				getNodeImpl(node),
				node != null && node.isSummary(),
				isAssignmentRowsVisible());
	}

	private boolean isAssignmentRowsVisible() {
		Object transform = TransformList.getInstance("hidden_filters").getTransform("Filter.Gantt");
		return transform instanceof CommonTransform && ((CommonTransform) transform).isShowAssignments();
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

	private void paintSummaryBar(Graphics2D g2, Rectangle2D bounds, Color barColor, Color progressColor,
			Color accentColor, double progressRatio) {
		if (g2 == null || bounds == null)
			return;

			Paint oldPaint = g2.getPaint();
			Color oldColor = g2.getColor();
			try {
				Color baseColor = barColor == null ? palette.getProjectLineColor() : barColor;
				Color progressBaseColor = progressColor == null ? baseColor : progressColor;
				Color summaryFill = MondayGanttTheme.soften(baseColor, 0.82f);
				Color summaryStroke = accentColor == null ? MondayGanttTheme.shade(summaryFill, 0.18f) : accentColor;
			Rectangle2D backgroundBand = new Rectangle2D.Double(
					bounds.getX(),
					bounds.getY(),
					bounds.getWidth(),
					bounds.getHeight());
			Paint backgroundPaint = createBarPaint(summaryFill, bounds, false);
			if (backgroundPaint instanceof Color)
				g2.setColor((Color)backgroundPaint);
			else
				g2.setPaint(backgroundPaint);
			g2.fill(backgroundBand);

			Rectangle2D progressBounds = GanttBarSupport.summaryProgressBounds(bounds, progressRatio);
			if (progressBounds != null) {
				Rectangle2D progressBand = new Rectangle2D.Double(
						progressBounds.getX(),
						progressBounds.getY(),
						progressBounds.getWidth(),
						progressBounds.getHeight());
				Paint progressPaint = createBarPaint(progressBaseColor, progressBand.getBounds2D(), false);
				if (progressPaint instanceof Color)
					g2.setColor((Color)progressPaint);
				else
					g2.setPaint(progressPaint);
				g2.fill(progressBand);
				paintProgressIndicator(g2, progressBand);
			}

			g2.setColor(summaryStroke);
			g2.draw(backgroundBand);
		} finally {
			g2.setPaint(oldPaint);
			g2.setColor(oldColor);
		}
	}

	static void paintProgressIndicator(Graphics2D g2, Rectangle2D bounds) {
		if (g2 == null || bounds == null || bounds.getWidth() <= 0.0d)
			return;
		Color oldColor = g2.getColor();
		Stroke oldStroke = g2.getStroke();
		try {
			double inset = Math.min(2.0d, bounds.getWidth() / 2.0d);
			double startX = bounds.getX() + inset;
			double endX = Math.max(startX, bounds.getMaxX() - inset);
			g2.setColor(Color.BLACK);
			g2.setStroke(PROGRESS_BAR_STROKE);
			g2.draw(new Line2D.Double(startX, bounds.getCenterY(), endX, bounds.getCenterY()));
		} finally {
			g2.setColor(oldColor);
			g2.setStroke(oldStroke);
		}
	}

	private boolean shouldPaintProgressOverlay(GraphicNode node, BarFormat format) {
		return GanttRendererSupport.shouldPaintProgressOverlay(getNodeImpl(node), format);
	}

	static double progressRatioForSchedule(Schedule schedule) {
		return GanttBarSupport.progressRatioForSchedule(schedule);
	}

	static double progressRatioForObject(Object impl) {
		return GanttProgress.ratioForObject(impl);
	}

	static Rectangle2D createCapsuleBarBounds(double x, double y, double width, double height) {
		return GanttBarSupport.createCapsuleBarBounds(x, y, width, height);
	}

	static Rectangle2D createSummaryBandBounds(double x, double y, double width, double height) {
		return GanttBarSupport.createSummaryBandBounds(x, y, width, height);
	}

	static Rectangle2D progressOverlayBounds(double x, double y, double totalWidth, double progressHeight, double progressRatio) {
		return GanttBarSupport.progressOverlayBounds(x, y, totalWidth, progressHeight, progressRatio);
	}

	static Rectangle2D summaryProgressBounds(Rectangle2D summaryBounds, double progressRatio) {
		return GanttBarSupport.summaryProgressBounds(summaryBounds, progressRatio);
	}

	static ScheduleInterval mergeIntervalsForDisplay(Iterable<ScheduleInterval> intervals) {
		return GanttBarSupport.mergeIntervalsForDisplay(intervals);
	}

	static List<ScheduleInterval> displayIntervals(BarFormat format, Iterable<ScheduleInterval> generatedIntervals,
			ScheduleInterval plannedInterval) {
		return GanttBarSupport.displayIntervals(format, generatedIntervals, plannedInterval);
	}

	static List<ScheduleInterval> splitGaps(List<ScheduleInterval> intervals) {
		return GanttBarSupport.splitGaps(intervals);
	}

	static List<Double> progressRatiosForIntervals(List<ScheduleInterval> intervals, double progressRatio) {
		return GanttBarSupport.progressRatiosForIntervals(intervals, progressRatio);
	}

	private double progressRatioFor(GraphicNode node) {
		Object impl = getNodeImpl(node);
		return progressRatioForObject(impl);
	}

	private static Schedule getScheduleForObject(Object impl) {
		return impl instanceof Schedule ? (Schedule)impl : null;
	}

	private ScheduleInterval plannedIntervalFor(GraphicNode node, BarFormat format) {
		if (node == null || format == null || format.getFromField() == null || format.getToField() == null || graphInfo == null || graphInfo.getCache() == null)
			return null;
		NodeModel nodeModel = graphInfo.getCache().getModel();
		Node modelNode = node.getNode();
		if (nodeModel == null || modelNode == null)
			return null;
		Object startDate = format.getFromField().getValue(modelNode, nodeModel, null);
		Object finishDate = format.getToField().getValue(modelNode, nodeModel, null);
		if (!(startDate instanceof Date) || !(finishDate instanceof Date))
			return null;
		return new ScheduleInterval(((Date)startDate).getTime(), ((Date)finishDate).getTime());
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



	private class NodeRenderer implements Consumer<Object>, IntervalConsumer, Serializable {
		private static final long serialVersionUID = -1348039741030744803L;
		GraphicNode node;
		Graphics2D g2;
		protected GanttBarSingleIntervalGenerator singleIntervalGenerator=new GanttBarSingleIntervalGenerator();
		protected ScheduleInterval interval;
		protected BarFormat format;
		protected int yrow;
		protected int maxLayer=Integer.MAX_VALUE;
		protected int minLayer=0;
		protected double intervalProgressRatio;

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
		public void accept(Object arg0) {
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

			ArrayList<ScheduleInterval> intervals = new ArrayList<ScheduleInterval>();
			intervalGenerator.consumeIntervals(node, new IntervalConsumer() {
				@Override
				public void consumeInterval(ScheduleInterval interval) {
					intervals.add(interval);
				}
			});
			List<ScheduleInterval> displayIntervals = GanttBarSupport.displayIntervals(
					format, intervals, plannedIntervalFor(node, format));
			if (displayIntervals.isEmpty())
				return;
			paintSplitConnectors(displayIntervals);
			List<Double> progressRatios = GanttBarSupport.progressRatiosForIntervals(
					displayIntervals, progressRatioFor(node));
			for (int i = 0; i < displayIntervals.size(); i++) {
				intervalProgressRatio = progressRatios.get(i);
				consumeInterval(displayIntervals.get(i));
			}

		}

		private void paintSplitConnectors(List<ScheduleInterval> displayIntervals) {
			if (g2 == null || !GanttBarSupport.shouldPreserveSplitIntervals(format))
				return;
			List<ScheduleInterval> gaps = GanttBarSupport.splitGaps(displayIntervals);
			if (gaps.isEmpty())
				return;
			CoordinatesConverter coord = ((GanttParams)graphInfo).getCoord();
			double height = format.getRow() == 1 ? config.getGanttBarHeight() : config.getBaselineHeight();
			double y = yrow + config.getGanttBarYOffset() + height / 2.0d;
			Color oldColor = g2.getColor();
			Stroke oldStroke = g2.getStroke();
			try {
				g2.setColor(resolveTaskFillColor(node, format));
				g2.setStroke(SPLIT_CONNECTOR_STROKE);
				for (ScheduleInterval gap : gaps) {
					g2.draw(new Line2D.Double(coord.toX(gap.getStart()), y, coord.toX(gap.getEnd()), y));
				}
			} finally {
				g2.setColor(oldColor);
				g2.setStroke(oldStroke);
			}
		}



		public void consumeInterval(ScheduleInterval interval){
//			System.out.println("GanttUI consuming interval " + new java.util.Date(interval.getStart()) + " " + new java.util.Date(interval.getEnd()));
//			if (interval.getEnd() < interval.getStart())
//				return;
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			if (interval.getEnd()>100000000000000L){
				// this hasn't happened in years. whatever caused it is fixed, but keeping just in case
				logger.log(Level.SEVERE, "Suspicious gantt interval that could lead to OutOfMemoryError: start={0}, end={1}",
					new Object[] { interval.getStart(), interval.getEnd() });
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
				Rectangle2D barBounds = GanttBarSupport.createCapsuleBarBounds(x, y, width, height);
				Rectangle2D summaryBounds = GanttBarSupport.createSummaryBandBounds(x, y, width, height);

				if (g2==null&&format.isMain()){
					if (format != null && "Bar.summary".equals(format.getId())) {
						node.setGanttShapeOffset(summaryBounds.getY()-y+height/2);
						node.setGanttShapeHeight(Math.max(height, summaryBounds.getHeight()));
					} else if (GanttBarSupport.shouldUseModernCapsuleBar(format)) {
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
					double progressRatio = intervalProgressRatio;
					Color progressFillColor = resolveProgressFillColor(node);
					Color progressTrackColor = (GanttBarSupport.shouldUseModernCapsuleBar(format) && progressRatio < 1.0d)
							? MondayGanttTheme.soften(statusColor, 0.46f)
							: statusColor;
					if (format != null && "Bar.summary".equals(format.getId()))
						paintSummaryBar(g2, summaryBounds, statusColor, progressFillColor, accentColor, progressRatio);
					else if (GanttBarSupport.shouldUseModernCapsuleBar(format))
						paintCapsuleBar(g2, barBounds, progressTrackColor, accentColor, true);
					else
						drawConfiguredShape(format.getMiddle(), g2, width, height, x, y, statusColor, accentColor, barBounds, true);

					// draw middle before ends
					if (shouldPaintProgressOverlay(node, format) && !(format != null && "Bar.summary".equals(format.getId()))){
						double progressHeight = config.getGanttProgressBarHeight();
						Rectangle2D progressBounds = GanttBarSupport.progressOverlayBounds(x, y, width, progressHeight, progressRatio);
						if (progressBounds != null) {
							paintCapsuleBar(g2, progressBounds, progressFillColor, accentColor, false);
							paintProgressIndicator(g2, progressBounds);
						}
					}
				}
			}
			if (g2==null) return;

			Color statusColor = resolveTaskFillColor(node, format);
			Color accentColor = resolveAccentColor(node, format, statusColor);
			Color endpointColor = GanttBarSupport.shouldUseUniformEndpointColor(format) ? statusColor : accentColor;
			Color startColor = resolveEndpointColor(node, format, endpointColor, true);
			Color endColor = resolveEndpointColor(node, format, endpointColor, false);
			if (format.getStart()!=null) drawConfiguredShape(format.getStart(), g2, dw, height, x , y, startColor, startColor, new Rectangle2D.Double(x, y - height / 2.0, dw, height), false);
			if (format.getEnd()!=null) drawConfiguredShape(format.getEnd(), g2, dw, height, x+width, y, endColor, endColor, new Rectangle2D.Double(x + width, y - height / 2.0, dw, height), false);


		}

	}

	private class AnnotationRenderer implements Consumer<Object>, Serializable {
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

		public void accept(Object arg0) {
			format = (BarFormat)arg0;
			if (shouldSuppressTaskAnnotationForAssignments(node))
				return;
			Field field=resolveAnnotationField(format);
			if (field==null) return;
			if (annotationRenderedForNode)
				return;
			String annotationKey = GanttRendererSupport.annotationKey(field, format);
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
			String s = DateFieldSupport.annotationTextFor(value, field);
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
			int estimatedWidth = fontMetrics.stringWidth(s);
			GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(clipBounds, x0, x1, annotationOffset, estimatedWidth);
			if (layout == null)
				return;
			int x = layout.x;
			int availableWidth = layout.availableWidth;
			String clipped = GanttRendererSupport.clipAnnotationText(fontMetrics, s, availableWidth);
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
			g2.setColor(GanttRendererSupport.resolveAnnotationColor());
			g2.clipRect(x, textTop, availableWidth, clipHeight);
			g2.drawString(clipped, x, textBaseline);
			if (originalClip != null)
				g2.setClip(originalClip);
			g2.setFont(oldFont);
			g2.setColor(oldColor);
		}

		private Object getAnnotationValue(Field field) {
			Object value=field.getValue(node.getNode(),graphInfo.getCache().getModel(),null);
			if (value!=null) return value;
			Object impl=getNodeImpl(node);
			return impl==null?null:field.getValue(impl,null);
		}

		private Field resolveAnnotationField(BarFormat format) {
			if (graphInfo instanceof Gantt) {
				String fieldId = ((Gantt) graphInfo).getAnnotationFieldId();
				if (fieldId != null) {
					return Configuration.getFieldFromId(fieldId);
				}
			}
			return format == null ? null : format.getField();
		}



	}

	private class HorizontalLineRenderer implements Consumer<Object>, Serializable {
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

		public void accept(Object arg0) {
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


	private class LinkRenderer implements Consumer<Object>, Serializable {
		private static final long serialVersionUID = -2031158189787837110L;
		protected BarFormat format;
		protected GraphicDependency dependency;
		protected Graphics2D g2;
		void initialize(Graphics2D g2, GraphicDependency dependency) {
			this.g2 = g2;
			this.dependency = dependency;
		}


		private double[] extraPoints=new double[3];
		public void accept(Object arg0) {
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

	/**
	 * Highlights the complete calendar row of every selected task, spanning the
	 * full chart width so the selection made in the task table is also visible
	 * in the chart (issue #179). The band is painted before the bars so task
	 * bars and grid lines stay visible on top.
	 */
	void paintSelectedRows(Graphics2D g2, Rectangle bounds) {
		if (g2 == null || bounds == null || !(graphInfo instanceof Gantt gantt))
			return;
		Set<Integer> rows = gantt.getHighlightedRows();
		if (rows == null || rows.isEmpty())
			return;
		int rowHeight = ((GanttParams) graphInfo).getRowHeight();
		if (rowHeight <= 0)
			return;
		Color oldColor = g2.getColor();
		try {
			g2.setColor(FlatUiSupport.spreadsheetRangeSelectionBackground());
			for (int row : rows) {
				if (row < 0)
					continue;
				int y = row * rowHeight;
				if (y + rowHeight < bounds.y || y > bounds.y + bounds.height)
					continue;
				g2.fillRect(bounds.x, y, bounds.width, rowHeight);
			}
		} finally {
			g2.setColor(oldColor);
		}
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
		double referenceX = Double.NaN;
		double lastY = Double.NaN;
		for (Iterator<GraphicNode> i=nodeList.iterator(); i.hasNext();) {
			GraphicNode node = i.next();
			if (!shouldIncludeInProgressLine(node))
				continue;

			Task task = (Task)node.getNode().getImpl();
			double progressX = getProgressLineX(coord, task);
			double y = getProgressLineY(node);
			if (path == null) {
				path = new GeneralPath();
				referenceX = coord.toX(getProgressReferenceDate(task));
				path.moveTo((float)referenceX, (float)(y - ((GanttParams)graphInfo).getRowHeight() / 2.0d));
			}
			path.lineTo((float)progressX, (float)y);
			lastY = y;
		}
		if (path != null)
			path.lineTo((float)referenceX, (float)(lastY + ((GanttParams)graphInfo).getRowHeight() / 2.0d));
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
		long today = getProgressReferenceDate(task);
		long progressDate = GanttProgress.progressLineDate(task, today);
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

	protected BarFormat calendarFormat;
	protected Consumer<Object> calendarClosure=new Consumer<Object>() { public void accept(Object arg0) {
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
		paintSelectedRows(g2, clipBounds);

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

