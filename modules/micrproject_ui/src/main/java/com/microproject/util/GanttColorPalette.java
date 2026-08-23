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
package com.microproject.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.scheduling.Schedule;

/**
 * Color palette interface for Gantt chart theming.
 *
 * <p>Provides a consistent way to apply different color schemes (Monday.com, MS Project,
 * etc.) to Gantt charts while maintaining proper contrast, accessibility, and recognizing
 * different task types, statuses, and relationships.</p>
 */
public interface GanttColorPalette {
    /**
     * Human-readable palette name for UI display.
     */
    String getName();
    
    /**
     * Background color for the main chart surface.
     */
    Color getChartBackground();
    
    /**
     * Color for grid lines and structural dividers.
     */
    Color getGridLine();
    
    /**
     * Primary color for task bars, considering task status.
     * Implementations should apply proper contrast/brightness rules.
     *
     * @param statusColor Base color hint (e.g., MondayGanttTheme status colors)
     */
    Color getTaskBar(Color statusColor);
    
    /**
     * Compute task status color from schedule information.
     * Implementations map status (done, working, stuck) to specific colors.
     *
     * @param schedule Schedule/plan for the task
     * @param context Additional context indicators
     */
    Color getStatusColor(Schedule schedule, Object context);
    
    /**
     * Color for annotation elements (numeric displays, labels, etc.)
     *
     * @param format Annotation type/configuration
     */
    Color getAnnotationColor(BarFormat format);

    /**
     * Accent/stroke color for a bar format.
     */
    Color getAccentColor(BarFormat format, Color statusColor, Object context);

    /**
     * Text color that keeps labels readable on top of a bar.
     */
    Color getTextColor(Color fillColor);

    /**
     * Paint for a foreground or background bar layer.
     */
    Paint createBarPaint(Color fillColor, Rectangle2D bounds, boolean backgroundLayer, boolean textured);

    /**
     * Color for baseline snapshot bars.
     */
    Color getBaselineBarColor();

    /**
     * Color for tasks on the critical path (MS Project renders them red).
     * Only used when the task has no user-defined individual bar color.
     */
    Color getCriticalTaskColor();

    /**
     * External dependency color for cross-project links.
     */
    Color getExternalLinkColor();

    /**
     * Color for dependency links between tasks.
     */
    Color getDependencyLinkColor();
    
    /**
     * Color for project deadline/status lines.
     */
    Color getProjectLineColor();
    
    /**
     * Color for status date markers.
     */
    Color getStatusDateLineColor();

    /**
     * Background color for a summary-task bar, derived from its status color.
     */
    Color getSummaryBackgroundColor(Color statusColor);

    /**
     * Foreground color for the completed portion of a summary-task bar.
     */
    Color getSummaryProgressColor(Color statusColor);
}
