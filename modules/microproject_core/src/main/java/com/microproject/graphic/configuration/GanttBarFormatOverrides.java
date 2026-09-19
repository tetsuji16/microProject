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
		private static final BarFormat AUTOMATIC = new BarFormat(null, null, null, null);

		private final Integer startRgb;
		private final Integer middleRgb;
		private final Integer endRgb;
		/** A safe {@link com.microproject.graphic.configuration.shape.PredefinedShape} name for milestones. */
		private final String milestoneShapeName;

		public BarFormat(Integer startRgb, Integer middleRgb, Integer endRgb) {
			this(startRgb, middleRgb, endRgb, null);
		}

		public BarFormat(Integer startRgb, Integer middleRgb, Integer endRgb, String milestoneShapeName) {
			this.startRgb = normalize(startRgb);
			this.middleRgb = normalize(middleRgb);
			this.endRgb = normalize(endRgb);
			this.milestoneShapeName = normalizeMilestoneShapeName(milestoneShapeName);
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

		public String getMilestoneShapeName() {
			return milestoneShapeName;
		}

		public BarFormat withStartRgb(Integer rgb) {
			return new BarFormat(rgb, middleRgb, endRgb, milestoneShapeName);
		}

		public BarFormat withMiddleRgb(Integer rgb) {
			return new BarFormat(startRgb, rgb, endRgb, milestoneShapeName);
		}

		public BarFormat withEndRgb(Integer rgb) {
			return new BarFormat(startRgb, middleRgb, rgb, milestoneShapeName);
		}

		/**
		 * Applies one fill color to every part of an individually formatted bar.
		 * This mirrors Microsoft Project's Fill Color command, including bars
		 * whose configured style uses start or end shapes.
		 */
		public BarFormat withFillRgb(Integer rgb) {
			return new BarFormat(rgb, rgb, rgb, milestoneShapeName);
		}

		public BarFormat withMilestoneShapeName(String shapeName) {
			return new BarFormat(startRgb, middleRgb, endRgb, shapeName);
		}

		public boolean isAutomatic() {
			return startRgb == null && middleRgb == null && endRgb == null && milestoneShapeName == null;
		}

		private static Integer normalize(Integer rgb) {
			return rgb == null ? null : rgb & 0x00FFFFFF;
		}

		private static String normalizeMilestoneShapeName(String shapeName) {
			if ("DIAMOND".equals(shapeName) || "SQUARE".equals(shapeName)
					|| "TRIANGLE_UP".equals(shapeName) || "TRIANGLE_DOWN".equals(shapeName))
				return shapeName;
			return null;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object)
				return true;
			if (!(object instanceof BarFormat other))
				return false;
			return Objects.equals(startRgb, other.startRgb)
					&& Objects.equals(middleRgb, other.middleRgb)
					&& Objects.equals(endRgb, other.endRgb)
					&& Objects.equals(milestoneShapeName, other.milestoneShapeName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(startRgb, middleRgb, endRgb, milestoneShapeName);
		}
	}
}
