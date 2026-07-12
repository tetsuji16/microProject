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
 * Copyright (c) 2012-2019. All Rights Reserved.
 ************************************************************************/

package com.projectlibre1.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.scheduling.Schedule;

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
        if (context instanceof com.projectlibre1.pm.assignment.Assignment) {
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
