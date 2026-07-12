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
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.scheduling.Schedule;

/**
 * Classic MS Project-inspired color palette for backward compatibility.
 */
public class ClassicMSProjectPalette implements GanttColorPalette {
    private static final Color TASK_BLUE = new Color(0x5B, 0x9B, 0xD5);
    private static final Color DONE_GREEN = new Color(0x70, 0xAD, 0x47);
    private static final Color NOT_STARTED = new Color(0xC9, 0xD8, 0xEA);
    private static final Color GRID_LINE = new Color(0xD9, 0xE2, 0xEC);
    private static final Color TEXT = new Color(0x1F, 0x1F, 0x1F);
    private static final Color LINK = new Color(0x5F, 0x64, 0x6D);
    private static final Color BASELINE = new Color(0xA6, 0xAA, 0xB0);

    @Override
    public String getName() {
        return "Classic MS Project";
    }
    
    @Override
    public Color getChartBackground() {
        return Color.WHITE;
    }
    
    @Override
    public Color getGridLine() {
        return GRID_LINE;
    }
    
    @Override
    public Color getTaskBar(Color statusColor) {
        return statusColor != null ? statusColor : TASK_BLUE;
    }
    
    @Override
    public Color getStatusColor(Schedule schedule, Object context) {
        if (schedule == null) {
            return TASK_BLUE;
        }
        double percentComplete = schedule.getPercentComplete();
        if (percentComplete >= 0.99d) {
            return DONE_GREEN;
        }
        if (percentComplete <= 0.0d) {
            return NOT_STARTED;
        }
        return TASK_BLUE;
    }
    
    @Override
    public Color getAnnotationColor(BarFormat format) {
        return TEXT;
    }

    @Override
    public Color getAccentColor(BarFormat format, Color statusColor, Object context) {
        if (format != null && format.getId() != null) {
            String id = format.getId();
            if ("Bar.baseline".equals(id) || id.startsWith("Bar.baseline")) {
                return BASELINE;
            }
            if ("Link.link1".equals(id)) {
                return LINK;
            }
        }
        return statusColor != null ? statusColor : TASK_BLUE;
    }

    @Override
    public Color getTextColor(Color fillColor) {
        return TEXT;
    }

    @Override
    public Paint createBarPaint(Color fillColor, Rectangle2D bounds, boolean backgroundLayer, boolean textured) {
        Color barColor = getTaskBar(fillColor);
        if (barColor == null) {
            barColor = TASK_BLUE;
        }
        if (bounds == null) {
            return backgroundLayer ? new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 96) : barColor;
        }
        Color top = lighten(barColor, backgroundLayer ? 0.42f : 0.24f);
        Color bottom = darken(barColor, backgroundLayer ? 0.05f : 0.08f);
        if (backgroundLayer) {
            top = withAlpha(top, 180);
            bottom = withAlpha(bottom, 180);
        }
        return new GradientPaint((float)bounds.getX(), (float)bounds.getY(), top,
                (float)bounds.getX(), (float)bounds.getMaxY(), bottom);
    }

    @Override
    public Color getBaselineBarColor() {
        return BASELINE;
    }

    @Override
    public Color getExternalLinkColor() {
        return LINK;
    }
    
    @Override
    public Color getDependencyLinkColor() {
        return LINK;
    }
    
    @Override
    public Color getProjectLineColor() {
        return new Color(0x00, 0x78, 0xD4);
    }
    
    @Override
    public Color getStatusDateLineColor() {
        return new Color(0x00, 0x78, 0xD4);
    }

    private static Color lighten(Color color, float ratio) {
        return mix(color, Color.WHITE, ratio);
    }

    private static Color darken(Color color, float ratio) {
        return mix(color, Color.BLACK, ratio);
    }

    private static Color mix(Color color, Color target, float ratio) {
        float weight = Math.max(0f, Math.min(1f, ratio));
        float targetWeight = 1f - weight;
        int red = Math.round(color.getRed() * targetWeight + target.getRed() * weight);
        int green = Math.round(color.getGreen() * targetWeight + target.getGreen() * weight);
        int blue = Math.round(color.getBlue() * targetWeight + target.getBlue() * weight);
        return new Color(red, green, blue);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
