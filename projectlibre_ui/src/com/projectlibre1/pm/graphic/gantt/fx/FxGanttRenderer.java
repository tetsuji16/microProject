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
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License. The
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
package com.projectlibre1.pm.graphic.gantt.fx;

import java.awt.geom.PathIterator;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.graphic.configuration.BarStyle;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.pm.graphic.gantt.link_routing.DefaultGanttLinkRouting;
import com.projectlibre1.pm.graphic.gantt.link_routing.GanttLinkRouting;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldConverter;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.task.Task;

/**
 * JavaFX-based Gantt chart renderer.
 * Draws task bars, dependency links, annotations, non-working days and progress lines
 * using JavaFX Canvas GraphicsContext.
 */
public class FxGanttRenderer {

    private static final Color PROGRESS_LINE_COLOR = Color.rgb(0xCC, 0x00, 0x00);
    private static final Color PROGRESS_LINE_HALO_COLOR = Color.WHITE;
    private static final double PROGRESS_LINE_POINT_SIZE = 6.0;

    private static final Color BAR_COLOR_NORMAL = Color.rgb(80, 150, 220);
    private static final Color BAR_COLOR_COMPLETE = Color.rgb(60, 140, 60);
    private static final Color BAR_COLOR_INPROGRESS = Color.rgb(60, 120, 200);
    private static final Color BAR_COLOR_SUMMARY = Color.rgb(60, 60, 60);
    private static final Color BAR_COLOR_PROJECT = Color.rgb(100, 100, 180);
    private static final Color LINK_COLOR = Color.rgb(80, 80, 120);
    private static final Color ROW_ALT_COLOR = Color.rgb(245, 245, 250);
    private static final Color LINE_COLOR = Color.rgb(220, 220, 220);

    protected GraphicConfiguration config;
    protected GanttLinkRouting linkRouting = new DefaultGanttLinkRouting();

    public FxGanttRenderer() {
        this.config = GraphicConfiguration.getInstance();
    }

    /**
     * Main render entry point. Called from JavaFX thread.
     */
    public void render(GraphicsContext gc, FxGanttChart chart,
                       CoordinatesConverter coord, NodeModelCache cache,
                       double width, double height) {
        if (cache == null || coord == null) return;

        // Clear
        gc.clearRect(0, 0, width, height);
        gc.setFontSmoothingType(FontSmoothingType.LCD);

        // Background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        // Non-working days
        if (coord.getTimescaleManager() != null) {
            paintNonWorkingDays(gc, coord, 0, 0, width, height);
        }

        int rowHeight = chart.getRowHeight();

        // First pass: nodes (background bars, annotations)
        for (int i = 0; i < cache.getSize(); i++) {
            GraphicNode node = (GraphicNode) cache.getElementAt(i);
            if (node == null || node.isVoid() || !node.isSchedule()) continue;

            int y = node.getRow() * rowHeight;

            // Alternating row background
            if (i % 2 == 0) {
                gc.setFill(ROW_ALT_COLOR);
                gc.fillRect(0, y, width, rowHeight);
            }

            // Horizontal line separator at row top
            gc.setStroke(LINE_COLOR);
            gc.setLineWidth(0.5);
            gc.strokeLine(0, y, width, y);

            // Task bar background
            paintNode(gc, node, chart, coord, true);

            // Annotation (text labels after bar)
            paintAnnotation(gc, node, chart, coord, cache);
        }

        // Second pass: dependency links
        Iterator depIter = cache.getVisibleDependencies().getIterator();
        while (depIter.hasNext()) {
            GraphicDependency dep = (GraphicDependency) depIter.next();
            paintLink(gc, dep, chart, coord, cache);
        }

        // Third pass: nodes foreground (bar outline)
        for (int i = 0; i < cache.getSize(); i++) {
            GraphicNode node = (GraphicNode) cache.getElementAt(i);
            if (node == null || node.isVoid() || !node.isSchedule()) continue;
            paintNode(gc, node, chart, coord, false);
        }

        // Progress line
        if (chart.isProgressLineEnabled()) {
            paintProgressLine(gc, chart, coord, cache);
        }
    }

    /**
     * Compute pixel x0/x1 for a task bar from task dates.
     */
    protected double[] computeBarPixels(GraphicNode node, CoordinatesConverter coord,
                                         int rowHeight, int barYOffset, int barHeight) {
        Object impl = node.getNode().getImpl();
        if (!(impl instanceof Task)) return null;

        Task task = (Task) impl;
        long start = task.getStart();
        long finish = task.getEnd();
        if (finish < start) return null;

        double x0 = coord.toX(start);
        double x1 = coord.toX(finish);
        if (x1 <= x0) {
            int minWidth = Math.max(1, config.getGanttBarMinWidth());
            x1 = x0 + minWidth;
        }

        return new double[]{x0, x1};
    }

    /**
     * Draw a single task bar.
     */
    protected void paintNode(GraphicsContext gc, GraphicNode node, FxGanttChart chart,
                              CoordinatesConverter coord, boolean background) {
        int rowHeight = chart.getRowHeight();
        int barHeight = config.getGanttBarHeight();
        int barYOffset = config.getGanttBarYOffset();
        int y = node.getRow() * rowHeight;

        double[] pixels = computeBarPixels(node, coord, rowHeight, barYOffset, barHeight);
        if (pixels == null) return;

        double x0 = pixels[0];
        double x1 = pixels[1];

        Color barColor = getBarColor(node);

        if (node.isSummary()) {
            if (background) {
                gc.setFill(barColor);
                gc.fillRect(x0, y + barYOffset + 2, x1 - x0, barHeight - 4);
            } else {
                gc.setStroke(barColor);
                gc.setLineWidth(1.0);
                gc.strokeRect(x0, y + barYOffset + 2, x1 - x0, barHeight - 4);
            }
        } else {
            if (background) {
                gc.setFill(barColor);
                gc.fillRoundRect(x0, y + barYOffset, x1 - x0, barHeight, 3, 3);
            } else {
                gc.setStroke(Color.rgb(80, 80, 80));
                gc.setLineWidth(0.8);
                gc.strokeRoundRect(x0, y + barYOffset, x1 - x0, barHeight, 3, 3);
            }
        }
    }

    /**
     * Draw text annotations from BarStyles.
     */
    protected void paintAnnotation(GraphicsContext gc, GraphicNode node, FxGanttChart chart,
                                    CoordinatesConverter coord, NodeModelCache cache) {
        BarStyles styles = chart.barStyles;
        if (styles == null) return;

        // Only process annotation formats
        BarFormat annFormat = getAnnotationFormat(styles, node);
        if (annFormat == null) return;

        int rowHeight = chart.getRowHeight();
        int barYOffset = config.getGanttBarYOffset();
        int barHeight = config.getGanttBarHeight();
        int y = node.getRow() * rowHeight;

        gc.setFont(Font.font("SansSerif", 10));

        // Get annotation value
        Field field = annFormat.getField();
        if (field == null) return;

        Object value = field.getValue(node.getNode(), cache.getModel(), null);
        if (value == null) return;

        String text;
        if (value instanceof Date) {
            Date d = (Date) value;
            text = DateFormat.getDateInstance(DateFormat.SHORT).format(d);
            int slash = text.lastIndexOf('/');
            if (slash > 0) text = text.substring(0, slash);
        } else if (value instanceof Number) {
            text = FieldConverter.toString(value, value.getClass(), null);
        } else {
            text = value.toString();
        }

        // Compute position: place annotation at end of bar
        double[] pixels = computeBarPixels(node, coord, rowHeight, barYOffset, barHeight);
        if (pixels == null) return;

        int annotationX = (int) Math.ceil(pixels[1]) + config.getGanttBarAnnotationXOffset();
        int textY = y + barYOffset + (barHeight / 2) + 4;

        gc.setFill(Color.rgb(80, 80, 80));
        gc.fillText(text, annotationX, textY);
    }

    /**
     * Find the first annotation BarFormat from the styles for this node.
     */
    private BarFormat getAnnotationFormat(BarStyles styles, Object ganttable) {
        Iterator i = styles.getRows().iterator();
        while (i.hasNext()) {
            BarStyle row = (BarStyle) i.next();
            if (row.isAnnotation() && !row.isLink() && !row.isHorizontalGrid() && row.evaluate(ganttable)) {
                return row.getBarFormat();
            }
        }
        // Fallback: use first non-link bar format
        i = styles.getRows().iterator();
        while (i.hasNext()) {
            BarStyle row = (BarStyle) i.next();
            if (!row.isLink() && !row.isHorizontalGrid() && row.evaluate(ganttable)) {
                return row.getBarFormat();
            }
        }
        return null;
    }

    /**
     * Draw dependency link between two tasks using GeneralPath from routing.
     */
    protected void paintLink(GraphicsContext gc, GraphicDependency dep, FxGanttChart chart,
                              CoordinatesConverter coord, NodeModelCache cache) {
        GraphicNode pred = dep.getPredecessor();
        GraphicNode succ = dep.getSuccessor();
        if (pred == null || succ == null) return;

        int rowHeight = chart.getRowHeight();

        // Get end x of predecessor (right side of its bar)
        Object predImpl = pred.getNode().getImpl();
        Object succImpl = succ.getNode().getImpl();
        if (!(predImpl instanceof Task) || !(succImpl instanceof Task)) return;

        Task predTask = (Task) predImpl;
        Task succTask = (Task) succImpl;

        long predFinish = predTask.getEnd();
        long succStart = succTask.getStart();
        if (predFinish <= 0 || succStart <= 0) return;

        double xStart = coord.toX(predFinish);
        double xEnd = coord.toX(succStart);
        double yStart = pred.getRow() * rowHeight + rowHeight / 2.0;
        double yEnd = succ.getRow() * rowHeight + rowHeight / 2.0;

        gc.setStroke(LINK_COLOR);
        gc.setLineWidth(1.2);

        // Draw a simple 3-segment dependency line
        double midY = (yStart + yEnd) / 2.0;

        // Horizontal from predecessor right edge
        gc.strokeLine(xStart, yStart, xStart + 8, yStart);
        // Vertical
        gc.strokeLine(xStart + 8, yStart, xStart + 8, midY);
        // Horizontal to successor
        gc.strokeLine(xStart + 8, midY, xEnd - 8, midY);
        // Vertical down to successor
        gc.strokeLine(xEnd - 8, midY, xEnd - 8, yEnd);
        // Final horizontal into successor
        gc.strokeLine(xEnd - 8, yEnd, xEnd, yEnd);

        // Arrowhead at the end
        drawArrowhead(gc, xEnd - 8, yEnd, xEnd, yEnd);
    }

    private void drawArrowhead(GraphicsContext gc, double fromX, double fromY,
                                double toX, double toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowLen = 8.0;
        double arrowAngle = Math.toRadians(25);

        double x1 = toX - arrowLen * Math.cos(angle - arrowAngle);
        double y1 = toY - arrowLen * Math.sin(angle - arrowAngle);
        double x2 = toX - arrowLen * Math.cos(angle + arrowAngle);
        double y2 = toY - arrowLen * Math.sin(angle + arrowAngle);

        gc.setFill(LINK_COLOR);
        gc.fillPolygon(new double[]{toX, x1, x2}, new double[]{toY, y1, y2}, 3);
    }

    /**
     * Shade non-working days in the chart background.
     */
    protected void paintNonWorkingDays(GraphicsContext gc, CoordinatesConverter coord,
                                        double offX, double offY,
                                        double width, double height) {
        try {
            double origin = coord.getOrigin();
            double end = coord.getEnd();

            double startMs = coord.toTime(origin);
            double endMs = coord.toTime(end);

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis((long) startMs);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long dayStart = cal.getTimeInMillis();
            int dayMs = 24 * 60 * 60 * 1000;

            gc.setFill(Color.rgb(240, 240, 245));

            while (dayStart < endMs) {
                cal.setTimeInMillis(dayStart);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                if (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY) {
                    double x0 = coord.toX(dayStart) - offX;
                    double x1 = coord.toX(dayStart + dayMs) - offX;
                    if (x1 > 0 && x0 < width) {
                        gc.fillRect(Math.max(0, x0), 0,
                                     Math.min(x1, width) - Math.max(0, x0),
                                     height);
                    }
                }
                dayStart += dayMs;
            }
        } catch (Exception e) {
            // Silently handle rendering errors for non-working days
        }
    }

    /**
     * Draw the progress line (vertical line at today's date).
     */
    protected void paintProgressLine(GraphicsContext gc, FxGanttChart chart,
                                      CoordinatesConverter coord, NodeModelCache cache) {
        Date now = new Date();
        double x = coord.toX(now.getTime());
        if (x < 0) return;

        double height = chart.getHeight();
        if (height <= 0 && cache != null) {
            height = (cache.getSize() + 1) * chart.getRowHeight();
        }

        // Halo
        gc.setStroke(PROGRESS_LINE_HALO_COLOR);
        gc.setLineWidth(4.0);
        gc.strokeLine(x, 0, x, height);

        // Main line
        gc.setStroke(PROGRESS_LINE_COLOR);
        gc.setLineWidth(2.0);
        gc.strokeLine(x, 0, x, height);

        // Diamond at top
        gc.setFill(PROGRESS_LINE_COLOR);
        double half = PROGRESS_LINE_POINT_SIZE / 2.0;
        double[] xPts = {x, x + half, x, x - half};
        double[] yPts = {0 - half, 0, 0 + half, 0};
        gc.fillPolygon(xPts, yPts, 4);
    }

    /**
     * Determine bar color for a task node.
     */
    protected Color getBarColor(GraphicNode node) {
        if (node.isSummary()) {
            return BAR_COLOR_SUMMARY;
        }
        if (node.isGroup()) {
            return BAR_COLOR_PROJECT;
        }
        Object impl = node.getNode().getImpl();
        if (impl instanceof Task) {
            Task task = (Task) impl;
            double pct = task.getPercentComplete();
            if (pct >= 1.0) {
                return BAR_COLOR_COMPLETE;
            } else if (pct > 0.0) {
                return BAR_COLOR_INPROGRESS;
            }
        }
        return BAR_COLOR_NORMAL;
    }
}
