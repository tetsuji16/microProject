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
import java.util.function.Consumer;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map;

import javax.swing.JComponent;


import com.microproject.pm.graphic.link_routing.GanttLinkRouting;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.GraphRenderer;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.field.FieldConverter;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.TexturedShape;
import com.microproject.graphic.configuration.shape.PredefinedPaint;
import com.microproject.options.GanttOption;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.Project;
import com.microproject.timescale.CalendarUtil;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttColorPalette;
import com.microproject.util.MicrosoftProjectGanttPalette;

public class GanttRenderer extends GraphRenderer implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -7437190083991277084L;
	private static final Stroke PROGRESS_LINE_STROKE = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PROGRESS_LINE_HALO_STROKE = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke PROGRESS_BAR_STROKE = new BasicStroke(1.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SPLIT_CONNECTOR_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
			BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.5f, 2.5f }, 0.0f);
	private static final int PROGRESS_LINE_POINT_SIZE = 6;
	private transient Map<ProjectionRowKey, GanttBarGeometry> barGeometry = new java.util.HashMap<>();
	private transient long barGeometryRevision = Long.MIN_VALUE;
	private transient TaskProjectionSnapshot renderValues = TaskProjectionSnapshot.empty();

    protected GraphicConfiguration config;
    protected JComponent container;
	protected GanttColorPalette palette = new MicrosoftProjectGanttPalette();

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
			if (graphInfo instanceof Gantt gantt) gantt.refreshProjectionCapture();
        }
    }

	GanttBarGeometry getBarGeometry(ProjectionRowKey key) {
		if (barGeometry == null || barGeometryRevision != geometryContextRevision() || key == null)
			return new GanttBarGeometry(0.0d, config.getGanttBarHeight());
		return barGeometry.getOrDefault(key, new GanttBarGeometry(0.0d, config.getGanttBarHeight()));
	}

	boolean isGeometryCurrent() {
		return barGeometry != null && barGeometryRevision == geometryContextRevision();
	}

	private void setBarGeometry(ProjectionRowKey key, double offset, double height) {
		if (barGeometry == null) barGeometry = new java.util.HashMap<>();
		if (key != null) barGeometry.put(key, new GanttBarGeometry(offset, height));
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
				Color progressBaseColor = palette.getSummaryProgressColor(progressColor == null ? baseColor : progressColor);
				Color summaryFill = palette.getSummaryBackgroundColor(baseColor);
				Color summaryStroke = accentColor == null ? baseColor : accentColor;
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




    public void updateShapes(ListIterator ignored){
		beginGeometryPass();
		barGeometry.clear();
		barGeometryRevision=geometryContextRevision();

    	Rectangle bounds = ((GanttParams)graphInfo).getGanttBounds();
    	CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
    	if (coord==null) return;
		double rowHeight=((GanttParams)graphInfo).getRowHeight();

		int i0=(int)Math.floor(bounds.getY()/rowHeight);
		int i1=(int)Math.ceil(bounds.getMaxY()/rowHeight);

		renderValues = graphInfo.getCache() instanceof ViewNodeModelCache cache
				? cache.getInstalledProjectionSnapshot().values() : TaskProjectionSnapshot.empty();
		for (int row = Math.max(0, i0); row < Math.min(i1, renderValues.rows().size()); row++) {
			TaskProjectionSnapshot.Row value = renderValues.rowAt(row);
			if (value == null || value.voidRow()) continue;
			for (TaskProjectionSnapshot.Bar bar : renderValues.ganttRow(value.key()).bars()) {
				BarFormat format = formatById(bar.formatId());
				if (format != null) paintSnapshotBar(null, row, value.key(), bar, format);
			}
		}
    }

	@Override
	protected long geometryContextRevision() {
		long revision = super.geometryContextRevision();
		if (!(graphInfo instanceof GanttParams params)) return revision;
		CoordinatesConverter coord = params.getCoord();
		if (coord != null) {
			revision = mix(revision, coord.getOrigin());
			revision = mix(revision, coord.getEnd());
		}
		Rectangle bounds = params.getGanttBounds();
		if (bounds != null) {
			revision = mix(revision, bounds.width);
			revision = mix(revision, bounds.height);
		}
		revision = mix(revision, params.getRowHeight());
		revision = mix(revision, config == null ? 0L : config.getGanttBarHeight());
		revision = mix(revision, renderValues.renderRevision());
		return revision;
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
		Set<ProjectionRowKey> keys = gantt.getHighlightedRowKeys();
		if (keys == null || keys.isEmpty())
			return;
		int rowHeight = ((GanttParams) graphInfo).getRowHeight();
		if (rowHeight <= 0)
			return;
		Color oldColor = g2.getColor();
		try {
			g2.setColor(FlatUiSupport.spreadsheetRangeSelectionBackground());
			for (ProjectionRowKey key : keys) {
				int row = gantt.getProjectionRow(key);
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
		for (TaskProjectionSnapshot.Row row : renderValues.rows()) {
			if (!shouldIncludeInProgressLine(row))
				continue;
			int x = (int)Math.round(getProgressLineX(coord, row));
			int y = (int)Math.round(getProgressLineY(row));
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
		for (TaskProjectionSnapshot.Row row : renderValues.rows()) {
			if (!shouldIncludeInProgressLine(row))
				continue;

			double progressX = getProgressLineX(coord, row);
			double y = getProgressLineY(row);
			if (path == null) {
				path = new GeneralPath();
				referenceX = coord.toX(renderValues.projectStatusDate());
				path.moveTo((float)referenceX, (float)(y - ((GanttParams)graphInfo).getRowHeight() / 2.0d));
			}
			path.lineTo((float)progressX, (float)y);
			lastY = y;
		}
		if (path != null)
			path.lineTo((float)referenceX, (float)(lastY + ((GanttParams)graphInfo).getRowHeight() / 2.0d));
		return path;
	}

	private BarFormat formatById(String id) {
		if (id == null || graphInfo == null || graphInfo.getBarStyles() == null) return null;
		for (var style : graphInfo.getBarStyles().getRows()) {
			BarFormat format = style.getBarFormat();
			if (format != null && id.equals(format.getId())) return format;
		}
		return null;
	}

	private double adaptEnd(double start, double end, TaskProjectionSnapshot.Row row) {
		return end > start && end - start < config.getGanttBarMinWidth() && row.intervals().size() <= 1
				? start + config.getGanttBarMinWidth() : end;
	}

	private void paintSnapshotNode(Graphics2D g2, int rowIndex, TaskProjectionSnapshot.Row row, boolean background) {
		enablePaintHints(g2);
		for (TaskProjectionSnapshot.Bar bar : renderValues.ganttRow(row.key()).bars()) {
			boolean backgroundLayer = bar.layer() >= BarFormat.MIN_BACKGROUND_LAYER
					&& bar.layer() <= BarFormat.MAX_BACKGROUND_LAYER;
			if (background != backgroundLayer) continue;
			BarFormat format = formatById(bar.formatId());
			if (format != null) paintSnapshotBar(g2, rowIndex, row.key(), bar, format);
		}
	}

	private void paintSnapshotBar(Graphics2D g2, int rowIndex, ProjectionRowKey key,
			TaskProjectionSnapshot.Bar bar, BarFormat format) {
		CoordinatesConverter coord = ((GanttParams)graphInfo).getCoord();
		double height = bar.row() == 1 ? config.getGanttBarHeight() : config.getBaselineHeight();
		double y = rowIndex * ((GanttParams)graphInfo).getRowHeight() + config.getGanttBarYOffset();
		if (bar.row() != 1) y += config.getGanttBarHeight() + config.getBaselineHeight() * (bar.row() - 2);
		y += height / 2.0d;
		if (g2 != null && GanttBarSupport.shouldPreserveSplitIntervals(format) && bar.intervals().size() > 1) {
			Color oldColor = g2.getColor();
			Stroke oldStroke = g2.getStroke();
			try {
				g2.setColor(new Color(bar.middleRgb(), true));
				g2.setStroke(SPLIT_CONNECTOR_STROKE);
				for (int i = 1; i < bar.intervals().size(); i++)
					g2.draw(new Line2D.Double(coord.toX(bar.intervals().get(i - 1).end()), y,
							coord.toX(bar.intervals().get(i).start()), y));
			} finally {
				g2.setColor(oldColor);
				g2.setStroke(oldStroke);
			}
		}
		for (int index = 0; index < bar.intervals().size(); index++) {
			TaskProjectionSnapshot.Interval interval = bar.intervals().get(index);
			if (interval.end() > 100000000000000L) continue;
			double x = coord.toX(interval.start());
			double width = adaptEnd(x, coord.toX(interval.end()), renderValues.rowAt(rowIndex)) - x;
			double ratio = index < bar.progressRatios().size() ? bar.progressRatios().get(index) : 0.0d;
			paintSnapshotInterval(g2, key, bar, format, x, y, width, height, ratio);
		}
	}

	private void paintSnapshotInterval(Graphics2D g2, ProjectionRowKey key, TaskProjectionSnapshot.Bar bar,
			BarFormat format, double x, double y, double width, double height, double ratio) {
		Color middle = new Color(bar.middleRgb());
		Color start = new Color(bar.startRgb());
		Color end = new Color(bar.endRgb());
		Color accent = GanttBarSupport.shouldUseUniformEndpointColor(format) ? middle : start;
		Rectangle2D bounds = GanttBarSupport.createCapsuleBarBounds(x, y, width, height);
		Rectangle2D summary = GanttBarSupport.createSummaryBandBounds(x, y, width, height);
		if (g2 == null && format.isMain()) {
			if ("Bar.summary".equals(format.getId())) setBarGeometry(key, summary.getY() - y + height / 2, Math.max(height, summary.getHeight()));
			else if (GanttBarSupport.shouldUseModernCapsuleBar(format)) setBarGeometry(key, 0.0d, bounds.getHeight());
			else if (format.getMiddle() != null) {
				Rectangle2D shape = format.getMiddle().toGeneralPath(width, height, x, y, null).getBounds2D();
				setBarGeometry(key, shape.getY() - y + height / 2, shape.getHeight());
			}
			return;
		}
		if (g2 == null) return;
		Color progress = new Color(bar.progressRgb());
		if (format.getMiddle() != null) {
			if ("Bar.summary".equals(format.getId())) paintSummaryBar(g2, summary, middle, progress, accent, ratio);
			else if (GanttBarSupport.shouldUseModernCapsuleBar(format))
				paintCapsuleBar(g2, bounds, ratio < 1.0d ? palette.getProgressTrackColor(middle) : middle, accent, true);
			else drawConfiguredShape(format.getMiddle(), g2, width, height, x, y, middle, accent, bounds, true);
			if (bar.progressVisible() && !"Bar.summary".equals(format.getId())) {
				Rectangle2D progressBounds = GanttBarSupport.progressOverlayBounds(x, y, width,
						config.getGanttProgressBarHeight(), ratio);
				if (progressBounds != null) {
					paintCapsuleBar(g2, progressBounds, progress, accent, false);
					paintProgressIndicator(g2, progressBounds);
				}
			}
		}
		if (format.getStart() != null) drawConfiguredShape(format.getStart(), g2, height, height, x, y,
				start, start, new Rectangle2D.Double(x, y - height / 2, height, height), false);
		if (format.getEnd() != null) drawConfiguredShape(format.getEnd(), g2, height, height, x + width, y,
				end, end, new Rectangle2D.Double(x + width, y - height / 2, height, height), false);
	}

	private void paintSnapshotAnnotation(Graphics2D g2, int rowIndex, TaskProjectionSnapshot.Row row) {
		TaskProjectionSnapshot.GanttRow gantt = renderValues.ganttRow(row.key());
		String text = gantt.annotation();
		if (text == null || text.isBlank()) return;
		Font base = FlatUiSupport.uiFont();
		int size = gantt.fontSize() == 0 ? base.getSize() : gantt.fontSize();
		Font font = gantt.fontFamily() == null || gantt.fontFamily().isBlank()
				? base.deriveFont(gantt.fontStyle(), (float)size)
				: new Font(gantt.fontFamily(), gantt.fontStyle(), size);
		if (gantt.fontStrikethrough()) {
			Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>(font.getAttributes());
			attributes.put(java.awt.font.TextAttribute.STRIKETHROUGH, java.awt.font.TextAttribute.STRIKETHROUGH_ON);
			font = font.deriveFont(attributes);
		}
		FontMetrics metrics = g2.getFontMetrics(font);
		CoordinatesConverter coord = ((GanttParams)graphInfo).getCoord();
		double x0 = coord.toX(row.start());
		double x1 = adaptEnd(x0, coord.toX(row.end()), row);
		Rectangle clip = g2.getClipBounds() == null ? ((GanttParams)graphInfo).getGanttBounds() : g2.getClipBounds();
		GanttRendererSupport.AnnotationLayout layout = GanttRendererSupport.resolveAnnotationLayout(clip, x0, x1,
				config.getGanttBarAnnotationXOffset(), metrics.stringWidth(text));
		if (layout == null) return;
		String clipped = GanttRendererSupport.clipAnnotationText(metrics, text, layout.availableWidth);
		if (clipped == null || clipped.isEmpty()) return;
		int rowHeight = ((GanttParams)graphInfo).getRowHeight();
		int textTop = rowIndex * rowHeight + Math.max(0, (rowHeight - metrics.getHeight()) / 2);
		int clipHeight = Math.min(rowHeight, Math.max(metrics.getHeight() + 2, config.getGanttBarHeight() + 2));
		Rectangle oldClip = g2.getClipBounds();
		Font oldFont = g2.getFont();
		Color oldColor = g2.getColor();
		g2.setFont(font);
		g2.clipRect(layout.x, textTop, layout.availableWidth, clipHeight);
		g2.setColor(palette.getChartBackground());
		g2.fillRect(Math.max(layout.x - 2, clip.x), textTop,
				Math.min(layout.availableWidth, metrics.stringWidth(clipped) + 4) + 2, clipHeight);
		g2.setColor(new Color(gantt.fontRgb()));
		g2.drawString(clipped, layout.x, textTop + metrics.getAscent());
		if (oldClip != null) g2.setClip(oldClip);
		g2.setFont(oldFont);
		g2.setColor(oldColor);
	}

	private void paintSnapshotLine(Graphics2D g2, int rowIndex) {
		TaskProjectionSnapshot.Row row = renderValues.rowAt(rowIndex);
		if (row == null || !renderValues.ganttRow(row.key()).horizontalLine()
				|| !((GanttParams)graphInfo).isGridLinesVisible()) return;
		Rectangle bounds = g2.getClipBounds();
		Color old = g2.getColor();
		g2.setColor(palette.getGridLine());
		g2.drawLine(bounds.x, (rowIndex + 1) * ((GanttParams)graphInfo).getRowHeight() - 1,
				bounds.x + bounds.width, (rowIndex + 1) * ((GanttParams)graphInfo).getRowHeight() - 1);
		g2.setColor(old);
	}

	private void paintSnapshotLink(Graphics2D g2, TaskProjectionSnapshot.Edge edge) {
		TaskProjectionSnapshot.Row from = renderValues.rowAt(renderValues.rowOf(edge.predecessor()));
		TaskProjectionSnapshot.Row to = renderValues.rowAt(renderValues.rowOf(edge.successor()));
		if (from == null || to == null) return;
		for (String formatId : renderValues.edgeFormatIds(edge)) {
			BarFormat format = formatById(formatId);
			if (format == null) continue;
			GanttLinkRouting routing = (GanttLinkRouting)((GanttParams)graphInfo).getRouting();
			CoordinatesConverter coord = ((GanttParams)graphInfo).getCoord();
			int fromSign = edge.type() == DependencyType.SF || edge.type() == DependencyType.SS ? -1 : 1;
			int toSign = edge.type() == DependencyType.FS || edge.type() == DependencyType.SS ? -1 : 1;
			double fx0 = coord.toX(from.start());
			double fx1 = adaptEnd(fx0, coord.toX(from.end()), from);
			double tx0 = coord.toX(to.start());
			double tx1 = adaptEnd(tx0, coord.toX(to.end()), to);
			double x0 = fromSign < 0 ? fx0 : fx1;
			double x1 = toSign < 0 ? tx0 : tx1;
			int rowHeight = ((GanttParams)graphInfo).getRowHeight();
			int yOffset = config.getGanttBarYOffset() + config.getGanttBarHeight() / 2;
			int y0 = rowHeight * renderValues.rowOf(edge.predecessor()) + yOffset;
			int y1 = rowHeight * renderValues.rowOf(edge.successor()) + yOffset;
			GeneralPath path = getDependencyPath(edge);
			path.reset();
			double targetHeight = getBarGeometry(edge.successor()).height();
			routing.routePath(path, x0, y0, x1, y1, Math.max(y0, y1),
					y1 + targetHeight / 2, y1 - targetHeight / 2, edge.type());
			Color oldColor = g2.getColor();
			Stroke oldStroke = g2.getStroke();
			Color linkColor = edge.crossProject() ? palette.getExternalLinkColor() : palette.getDependencyLinkColor();
			if (edge.disabled()) g2.setStroke(DISABLED_LINK_STROKE);
			g2.setColor(linkColor);
			g2.draw(path);
			try {
				if (format.getStart() != null) {
					double theta = routing.getFirstAngle();
					paintSnapshotArrow(g2, format.getStart(), linkColor, routing.getFirstX(), routing.getFirstY(),
							theta == 0 ? null : AffineTransform.getRotateInstance(theta, routing.getFirstX(), routing.getFirstY()));
				}
				if (format.getEnd() != null) {
					double theta = routing.getLastAngle();
					paintSnapshotArrow(g2, format.getEnd(), linkColor, routing.getLastX(), routing.getLastY(),
							theta == Math.PI || theta == -Math.PI ? null
								: AffineTransform.getRotateInstance(Math.PI - theta, routing.getLastX(), routing.getLastY()));
				}
			} finally {
				g2.setColor(oldColor);
				g2.setStroke(oldStroke);
			}
		}
	}

	private void paintSnapshotArrow(Graphics2D g2, TexturedShape shape, Color color, double x, double y,
			AffineTransform transform) {
		Paint oldPaint = shape.getPaint();
		Color oldColor = shape.getColor();
		shape.setPaint(color);
		shape.setColor(color);
		shape.draw(g2, x, y, transform, useTextures());
		shape.setPaint(oldPaint);
		shape.setColor(oldColor);
	}

	private boolean shouldIncludeInProgressLine(TaskProjectionSnapshot.Row row) {
		return row != null && row.schedule() && !row.assignment() && !row.voidRow()
				&& (!row.summary() || row.collapsed()) && !row.milestone() && !row.external()
				&& !row.subproject() && row.start() != 0L && row.end() > row.start();
	}

	private double getProgressLineX(CoordinatesConverter coord, TaskProjectionSnapshot.Row row) {
		long referenceDate = renderValues.projectStatusDate();
		double progress = Math.max(0.0d, Math.min(1.0d, row.percentComplete()));
		long progressDate;
		if (referenceDate != 0L && progress >= 1.0d && row.end() <= referenceDate)
			progressDate = referenceDate;
		else if (referenceDate != 0L && progress <= 0.0d && row.start() >= referenceDate)
			progressDate = referenceDate;
		else if (row.completed() <= 0L)
			progressDate = row.start();
		else
			progressDate = Math.max(row.start(), Math.min(row.end(), row.completed()));
		return coord.toX(progressDate);
	}

	private double getProgressLineY(TaskProjectionSnapshot.Row row) {
		int rowHeight=((GanttParams)graphInfo).getRowHeight();
		int yOffset=config.getGanttBarYOffset()+config.getGanttBarHeight()/2;
		return rowHeight*renderValues.rowOf(row.key())+yOffset;
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

		if (!(graphInfo.getCache() instanceof ViewNodeModelCache cache)) return;
		renderValues = cache.getInstalledProjectionSnapshot().values();
		beginGeometryPass();
		int from = Math.max(0, i0);
		int to = Math.min(i1, renderValues.rows().size());
		for (int rowIndex = from; rowIndex < to; rowIndex++) {
			TaskProjectionSnapshot.Row value = renderValues.rowAt(rowIndex);
			if (value == null || !value.schedule()) continue;
			paintSnapshotNode(g2, rowIndex, value, true);
			paintSnapshotLine(g2, rowIndex);
		}
		for (TaskProjectionSnapshot.Edge edge : renderValues.edges()) paintSnapshotLink(g2, edge);
		for (int rowIndex = from; rowIndex < to; rowIndex++) {
			TaskProjectionSnapshot.Row value = renderValues.rowAt(rowIndex);
			if (value != null && value.schedule()) paintSnapshotNode(g2, rowIndex, value, false);
		}
		for (int rowIndex = from; rowIndex < to; rowIndex++) {
			TaskProjectionSnapshot.Row value = renderValues.rowAt(rowIndex);
			if (value != null && value.schedule()) paintSnapshotAnnotation(g2, rowIndex, value);
		}
		paintProgressLine(g2);

		if (visibleBounds!=null) g2.setClip(svgClip);

	}



}

