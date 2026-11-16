/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.network;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;

import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.DurationFormat;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.Task;

/**
 * Per-view display choices of the Network Diagram Format tab (progress marks, link labels, straight
 * links, collapsed boxes, box spacing and per-box-type border styles) together with the pure
 * decisions the renderer and the layout take from them.
 */
public final class NetworkDisplayOptions {
	public enum BoxType { NORMAL, CRITICAL, MILESTONE, SUMMARY, INSERTED_PROJECT }
	public enum BoxShape { RECTANGLE, ROUNDED, DIAMOND }
	public enum ProgressMark { NONE, SLASH, CROSS }

	/** Border colour (RGB) and shape of one kind of box; a null colour keeps the configured one. */
	public record BoxStyle(Integer rgb, BoxShape shape) {
		public static final BoxStyle DEFAULT = new BoxStyle(null, BoxShape.RECTANGLE);
		public Color color() { return rgb == null ? null : new Color(rgb); }
	}

	/** Collapsed boxes keep this share of the configured box size. */
	public static final double COLLAPSED_SCALE = 0.4;
	public static final int MAX_SPACING = 200;

	private boolean progressMarks;
	private boolean linkLabels;
	private boolean straightLinks;
	private boolean collapsed;
	private int boxSpacing;
	private final Map<BoxType, BoxStyle> styles = new EnumMap<>(BoxType.class);

	public boolean isProgressMarks() { return progressMarks; }
	public void setProgressMarks(boolean value) { progressMarks = value; }
	public boolean isLinkLabels() { return linkLabels; }
	public void setLinkLabels(boolean value) { linkLabels = value; }
	public boolean isStraightLinks() { return straightLinks; }
	public void setStraightLinks(boolean value) { straightLinks = value; }
	public boolean isCollapsed() { return collapsed; }
	public void setCollapsed(boolean value) { collapsed = value; }
	public int getBoxSpacing() { return boxSpacing; }
	public void setBoxSpacing(int value) { boxSpacing = Math.max(0, Math.min(MAX_SPACING, value)); }

	public BoxStyle getStyle(BoxType type) {
		return styles.getOrDefault(type, BoxStyle.DEFAULT);
	}
	public void setStyle(BoxType type, BoxStyle style) {
		if (style == null || BoxStyle.DEFAULT.equals(style)) styles.remove(type);
		else styles.put(type, style);
	}
	public boolean hasStyles() { return !styles.isEmpty(); }

	/** The kind of box a task is drawn as. */
	public static BoxType boxType(Object impl) {
		if (!(impl instanceof Task task)) return BoxType.NORMAL;
		if (task.isSubproject()) return BoxType.INSERTED_PROJECT;
		if (task.isSummary()) return BoxType.SUMMARY;
		if (task.isMilestone()) return BoxType.MILESTONE;
		if (task.isCritical()) return BoxType.CRITICAL;
		return BoxType.NORMAL;
	}

	/** One diagonal on a task in progress, a cross on a complete one (fraction 0..1). */
	public static ProgressMark progressMark(double percentComplete) {
		if (percentComplete >= 1.0) return ProgressMark.CROSS;
		return percentComplete > 0.0 ? ProgressMark.SLASH : ProgressMark.NONE;
	}

	/** "FS" for a plain link, "SS+2d" or "FS-1d" when the link has lag or lead. */
	public static String linkLabel(DependencyType.Kind kind, long lag) {
		return linkLabel(kind, lag, null);
	}

	/** As {@link #linkLabel(DependencyType.Kind, long)}, with the lag unit sizes (hours per day ...) of the owning project. */
	public static String linkLabel(DependencyType.Kind kind, long lag, DurationDefaults defaults) {
		if (lag == 0) return kind.name();
		Duration duration = new Duration(Math.abs(lag));
		String text = defaults == null ? DurationFormat.getInstance().format(duration)
			: DurationFormat.getProjectInstance().format(duration, defaults);
		return kind.name() + (lag < 0 ? "-" : "+") + text;
	}

	/** Box size the layout reserves, given the configured full box size. */
	public Dimension boxSize(int width, int height) {
		double scale = collapsed ? COLLAPSED_SCALE : 1.0;
		return new Dimension((int) Math.round(width * scale), (int) Math.round(height * scale));
	}

	/** The outline drawn for a box of the given shape inside bounds. */
	public static Shape outline(BoxShape shape, Rectangle2D bounds) {
		double arc = Math.min(bounds.getWidth(), bounds.getHeight()) / 3;
		switch (shape) {
			case ROUNDED:
				return new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), arc, arc);
			case DIAMOND:
				Path2D.Double path = new Path2D.Double();
				path.moveTo(bounds.getCenterX(), bounds.getMinY());
				path.lineTo(bounds.getMaxX(), bounds.getCenterY());
				path.lineTo(bounds.getCenterX(), bounds.getMaxY());
				path.lineTo(bounds.getMinX(), bounds.getCenterY());
				path.closePath();
				return path;
			default:
				return bounds;
		}
	}

	/** Compact text form for the view workspace (plain strings deserialise under the safe object filter). */
	public String encodeStyles() {
		StringBuilder text = new StringBuilder();
		for (Map.Entry<BoxType, BoxStyle> entry : styles.entrySet()) {
			if (text.length() > 0) text.append(';');
			text.append(entry.getKey()).append(':')
				.append(entry.getValue().rgb() == null ? "-" : Integer.toHexString(entry.getValue().rgb()))
				.append(':').append(entry.getValue().shape());
		}
		return text.toString();
	}

	public void decodeStyles(String text) {
		styles.clear();
		if (text == null || text.isBlank()) return;
		for (String part : text.split(";")) {
			String[] fields = part.split(":");
			if (fields.length != 3) continue;
			try {
				setStyle(BoxType.valueOf(fields[0]), new BoxStyle("-".equals(fields[1]) ? null : Integer.parseUnsignedInt(fields[1], 16),
					BoxShape.valueOf(fields[2])));
			} catch (IllegalArgumentException ignored) {
				// a style written by another version is skipped
			}
		}
	}
}
