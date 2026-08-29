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

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Shape;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.BarFormat;
import com.microproject.graphic.configuration.shape.PredefinedShape;
import com.microproject.pm.task.NormalTask;
import com.microproject.preference.GlobalPreferences;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttProgress;

/**
 * Shared pure helpers for GanttRenderer decisions and annotation layout.
 */
final class GanttRendererSupport {
	static final class AnnotationLayout {
		final int x;
		final int availableWidth;

		AnnotationLayout(int x, int availableWidth) {
			this.x = x;
			this.availableWidth = availableWidth;
		}
	}

	private GanttRendererSupport() {
	}

	static Shape individualMilestoneShape(String shapeName, double size, double x, double y) {
		if (!"DIAMOND".equals(shapeName) && !"SQUARE".equals(shapeName)
				&& !"TRIANGLE_UP".equals(shapeName) && !"TRIANGLE_DOWN".equals(shapeName))
			return null;
		PredefinedShape shape = PredefinedShape.find(shapeName);
		return shape == null ? null : shape.toGeneralPath(size, size, x, y);
	}

	static boolean shouldSuppressTaskBarForAssignments(Object impl, boolean summary, BarFormat format, boolean assignmentRowsVisible) {
		if (!assignmentRowsVisible || impl == null || format == null)
			return false;
		if (!(impl instanceof NormalTask) || summary)
			return false;
		String formatId = format.getId();
		if (!"Bar.task".equals(formatId) && !"Bar.critical".equals(formatId))
			return false;
		return ((NormalTask)impl).hasRealAssignments();
	}

	static boolean shouldSuppressTaskAnnotationForAssignments(Object impl, boolean summary, boolean assignmentRowsVisible) {
		if (!assignmentRowsVisible || impl == null)
			return false;
		return impl instanceof NormalTask && !summary && ((NormalTask)impl).hasRealAssignments();
	}

	static boolean shouldPaintProgressOverlay(Object impl, BarFormat format) {
		if (impl == null || format == null || !format.isMain())
			return false;
		if (!GanttProgress.hasVisibleProgress(impl))
			return false;
		return "Bar.task".equals(format.getId())
				|| "Bar.critical".equals(format.getId())
				|| "Bar.summary".equals(format.getId())
				|| "Bar.assignment".equals(format.getId());
	}

	static Color resolveEndpointColor(BarFormat format, Color statusColor, Color accentColor) {
		return GanttBarSupport.shouldUseUniformEndpointColor(format) ? statusColor : accentColor;
	}

	static Color resolveEndpointColor(com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat format,
			Color defaultColor, boolean start) {
		Integer rgb = start ? format.getStartRgb() : format.getEndRgb();
		return rgb == null ? defaultColor : new Color(rgb);
	}

	static String annotationKey(Field field, BarFormat format) {
		String fieldName = field == null ? "" : field.getName();
		String formatId = format == null || format.getId() == null ? "" : format.getId();
		return fieldName + "|" + formatId;
	}

	static AnnotationLayout resolveAnnotationLayout(Rectangle clipBounds, double x0, double x1, int annotationOffset, int estimatedWidth) {
		return resolveAnnotationLayout(clipBounds, x0, x1, annotationOffset, estimatedWidth,
				GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO);
	}

	static AnnotationLayout resolveAnnotationLayout(Rectangle clipBounds, double x0, double x1, int annotationOffset,
			int estimatedWidth, String position) {
		if (clipBounds == null)
			return null;
		int clipLeft = clipBounds.x;
		int clipRight = clipBounds.x + clipBounds.width;
		boolean barVisible = x1 >= clipLeft && x0 <= clipRight;
		int preferredRightX = (int)Math.ceil(x1) + annotationOffset;
		int preferredLeftX = (int)Math.floor(x0) - annotationOffset - estimatedWidth;
		boolean rightLabelVisible = preferredRightX + estimatedWidth >= clipLeft && preferredRightX <= clipRight;
		boolean leftLabelVisible = preferredLeftX + estimatedWidth >= clipLeft && preferredLeftX <= clipRight;
		if (!barVisible && !rightLabelVisible && !leftLabelVisible)
			return null;
		int minX = clipBounds.x + 4;
		int maxTextWidth = Math.max(64, Math.min(180, clipBounds.width / 5));
		int rightAvailableWidth = Math.min(maxTextWidth, clipRight - preferredRightX - 4);
		boolean leftRequested = GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT.equals(position);
		boolean rightRequested = GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT.equals(position);
		int x = leftRequested ? preferredLeftX : preferredRightX;
		int availableWidth = leftRequested
				? Math.min(maxTextWidth, clipRight - preferredLeftX - 4)
				: rightAvailableWidth;
		if (!rightRequested && availableWidth < 24 && preferredLeftX >= minX) {
			x = preferredLeftX;
			availableWidth = Math.min(maxTextWidth, clipRight - preferredLeftX - 4);
		}
		if (availableWidth <= 0)
			return null;
		return new AnnotationLayout(x, availableWidth);
	}

	static String clipAnnotationText(FontMetrics fontMetrics, String text, int availableWidth) {
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

	static Color resolveAnnotationColor() {
		return FlatUiSupport.tableForeground();
	}
}
