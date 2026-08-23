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
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import com.microproject.graphic.configuration.BarFormat;
import com.microproject.pm.scheduling.Schedule;

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
    private static final Color CRITICAL_RED = new Color(0xFF, 0x00, 0x00);

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
    public Color getCriticalTaskColor() {
        return CRITICAL_RED;
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

    @Override
    public Color getSummaryBackgroundColor(Color statusColor) {
        return lighten(statusColor == null ? TASK_BLUE : statusColor, 0.82f);
    }

    @Override
    public Color getSummaryProgressColor(Color statusColor) {
        return statusColor == null ? TASK_BLUE : statusColor;
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
