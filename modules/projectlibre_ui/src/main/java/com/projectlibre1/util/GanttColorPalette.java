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
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices
 * in the Source Code files of the Original Code. You should use the text of this
 * Exhibit A rather than the text found in the Original Code Source Code for Your
 * Modifications.]
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 ************************************************************************/

package com.projectlibre1.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.scheduling.Schedule;

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
}
