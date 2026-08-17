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
 * Monday.com-inspired color palette using MondayGanttTheme.
 */
public class MondayComPalette implements GanttColorPalette {
    @Override
    public String getName() {
        return "Monday.com";
    }
    
    @Override
    public Color getChartBackground() {
        return MondayGanttTheme.chartBackground();
    }
    
    @Override
    public Color getGridLine() {
        return MondayGanttTheme.gridLine();
    }
    
    @Override
    public Color getTaskBar(Color statusColor) {
        // Monday style - use the status color directly, or GROUP_A as default
        if (statusColor != null) return statusColor;
        return MondayGanttTheme.GROUP_A;
    }
    
    @Override
    public Color getStatusColor(Schedule schedule, Object context) {
        return MondayGanttTheme.statusColor(schedule, context);
    }
    
    @Override
    public Color getAnnotationColor(BarFormat format) {
        if (format != null) {
            return MondayGanttTheme.accentColor(format, Color.BLACK);
        }
        return Color.BLACK;
    }

    @Override
    public Color getAccentColor(BarFormat format, Color statusColor, Object context) {
        if (context instanceof com.microproject.pm.assignment.Assignment) {
            return MondayGanttTheme.GROUP_B;
        }
        return MondayGanttTheme.accentColor(format, statusColor);
    }

    @Override
    public Color getTextColor(Color fillColor) {
        return MondayGanttTheme.textColorFor(fillColor);
    }

    @Override
    public Paint createBarPaint(Color fillColor, Rectangle2D bounds, boolean backgroundLayer, boolean textured) {
        if (!textured) {
            return MondayGanttTheme.withAlpha(fillColor, backgroundLayer ? 76 : 255);
        }
        return MondayGanttTheme.createLayerPaint(fillColor, bounds, backgroundLayer);
    }

    @Override
    public Color getBaselineBarColor() {
        return MondayGanttTheme.BASELINE;
    }

    @Override
    public Color getCriticalTaskColor() {
        return MondayGanttTheme.criticalTaskColor();
    }

    @Override
    public Color getExternalLinkColor() {
        return MondayGanttTheme.externalLinkColor();
    }
    
    @Override
    public Color getDependencyLinkColor() {
        return MondayGanttTheme.DEPENDENCY_LINK;
    }
    
    @Override
    public Color getProjectLineColor() {
        return MondayGanttTheme.projectLine();
    }
    
    @Override
    public Color getStatusDateLineColor() {
        return MondayGanttTheme.statusDateLine();
    }
}
