package com.projectlibre1.pm.graphic.gantt;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Color;

import com.projectlibre1.field.Field;
import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.util.FlatUiSupport;
import com.projectlibre1.util.GanttProgress;

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
		return "Bar.task".equals(format.getId()) || "Bar.critical".equals(format.getId()) || "Bar.summary".equals(format.getId());
	}

	static Color resolveEndpointColor(BarFormat format, Color statusColor, Color accentColor) {
		return GanttBarSupport.shouldUseUniformEndpointColor(format) ? statusColor : accentColor;
	}

	static String annotationKey(Field field, BarFormat format) {
		String fieldName = field == null ? "" : field.getName();
		String formatId = format == null || format.getId() == null ? "" : format.getId();
		return fieldName + "|" + formatId;
	}

	static AnnotationLayout resolveAnnotationLayout(Rectangle clipBounds, double x0, double x1, int annotationOffset, int estimatedWidth) {
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
		int x = preferredRightX;
		int availableWidth = rightAvailableWidth;
		if (availableWidth < 24 && preferredLeftX >= minX) {
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
