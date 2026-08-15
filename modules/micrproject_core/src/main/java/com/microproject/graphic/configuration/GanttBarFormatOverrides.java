package com.microproject.graphic.configuration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Project-scoped overrides for individually formatted Gantt bars.
 *
 * <p>Overrides are keyed by stable task unique id and Gantt view. They therefore
 * survive row reordering and changes to calculated task state, such as becoming
 * critical.</p>
 */
public final class GanttBarFormatOverrides implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String STANDARD_VIEW = "Gantt";
	public static final String TRACKING_VIEW = "TrackingGantt";

	private final Map<String, BarFormat> formats = new HashMap<>();

	public BarFormat get(String viewName, long taskUniqueId) {
		BarFormat format = formats.get(key(viewName, taskUniqueId));
		return format == null ? BarFormat.automatic() : format;
	}

	public void set(String viewName, long taskUniqueId, BarFormat format) {
		String key = key(viewName, taskUniqueId);
		if (format == null || format.isAutomatic())
			formats.remove(key);
		else
			formats.put(key, format);
	}

	public boolean isEmpty() {
		return formats.isEmpty();
	}

	static String key(String viewName, long taskUniqueId) {
		String normalizedView = TRACKING_VIEW.equals(viewName) ? TRACKING_VIEW : STANDARD_VIEW;
		return normalizedView + ':' + taskUniqueId;
	}

	public static final class BarFormat implements Serializable {
		private static final long serialVersionUID = 1L;
		private static final BarFormat AUTOMATIC = new BarFormat(null, null, null);

		private final Integer startRgb;
		private final Integer middleRgb;
		private final Integer endRgb;

		public BarFormat(Integer startRgb, Integer middleRgb, Integer endRgb) {
			this.startRgb = normalize(startRgb);
			this.middleRgb = normalize(middleRgb);
			this.endRgb = normalize(endRgb);
		}

		public static BarFormat automatic() {
			return AUTOMATIC;
		}

		public Integer getStartRgb() {
			return startRgb;
		}

		public Integer getMiddleRgb() {
			return middleRgb;
		}

		public Integer getEndRgb() {
			return endRgb;
		}

		public BarFormat withStartRgb(Integer rgb) {
			return new BarFormat(rgb, middleRgb, endRgb);
		}

		public BarFormat withMiddleRgb(Integer rgb) {
			return new BarFormat(startRgb, rgb, endRgb);
		}

		public BarFormat withEndRgb(Integer rgb) {
			return new BarFormat(startRgb, middleRgb, rgb);
		}

		/**
		 * Applies one fill color to every part of an individually formatted bar.
		 * This mirrors Microsoft Project's Fill Color command, including bars
		 * whose configured style uses start or end shapes.
		 */
		public BarFormat withFillRgb(Integer rgb) {
			return new BarFormat(rgb, rgb, rgb);
		}

		public boolean isAutomatic() {
			return startRgb == null && middleRgb == null && endRgb == null;
		}

		private static Integer normalize(Integer rgb) {
			return rgb == null ? null : rgb & 0x00FFFFFF;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (!(object instanceof BarFormat other))
				return false;
			return Objects.equals(startRgb, other.startRgb)
					&& Objects.equals(middleRgb, other.middleRgb)
					&& Objects.equals(endRgb, other.endRgb);
		}

		@Override
		public int hashCode() {
			return Objects.hash(startRgb, middleRgb, endRgb);
		}
	}
}
