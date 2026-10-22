/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.graphic.views;

import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledScrollPane;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;

/** Shared view-only range resolution and zoom behavior for selected tasks. */
public final class SelectedTasksTimescaleZoom {
	private static final long MINIMUM_RANGE_MILLIS = 86_400_000L;

	private SelectedTasksTimescaleZoom() {
	}

	static DateRange resolve(Collection<?> selected) {
		if (selected == null || selected.isEmpty())
			return null;
		long[] span = { Long.MAX_VALUE, Long.MIN_VALUE };
		for (Object value : selected)
			accumulate(value, span);
		return span[0] == Long.MAX_VALUE ? null : new DateRange(span[0], span[1]);
	}

	/** Document-derived: task rows use task dates, assignment rows their own dates, resource rows their assignments' span. */
	private static void accumulate(Object value, long[] span) {
		if (value instanceof Task task) {
			include(task.getStart(), task.getEnd(), span);
		} else if (value instanceof Assignment assignment) {
			if (!include(assignment.getStart(), assignment.getEnd(), span) && assignment.getTask() != null)
				include(assignment.getTask().getStart(), assignment.getTask().getEnd(), span);
		} else if (value instanceof Resource resource && resource.getAssignments() != null) {
			for (Object assigned : resource.getAssignments())
				if (assigned instanceof Assignment assignment)
					accumulate(assignment, span);
		}
	}

	private static boolean include(long start, long end, long[] span) {
		if (start <= 0 || end < start)
			return false;
		span[0] = Math.min(span[0], start);
		span[1] = Math.max(span[1], end);
		return true;
	}

	static boolean canZoom(Collection<?> selected) {
		return resolve(selected) != null;
	}

	static boolean zoom(Collection<?> selected, CoordinatesConverter coord, ScaledScrollPane scrollPane) {
		DateRange range = resolve(selected);
		if (range == null || coord == null || scrollPane == null)
			return false;
		int viewportWidth = scrollPane.getViewport().getExtentSize().width;
		if (viewportWidth <= 0)
			return false;
		var manager = coord.getTimescaleManager();
		long minimumEnd = range.start() + MINIMUM_RANGE_MILLIS;
		long fitEnd = Math.max(range.end(), minimumEnd);
		int targetScale = manager.getScaleCount() - 1;
		for (int scale = 0; scale < manager.getScaleCount(); scale++) {
			var candidate = manager.getScale(scale);
			double startX = candidate.toX(range.start() - coord.getOrigin());
			double endX = candidate.toX(fitEnd - coord.getOrigin());
			int left = (int) Math.floor(startX);
			int firstLeftThatCoversEnd = (int) Math.ceil(endX - viewportWidth);
			if (firstLeftThatCoversEnd <= left) {
				targetScale = scale;
				break;
			}
		}
		while (manager.getCurrentScaleIndex() > targetScale && coord.canZoomIn())
			coord.zoomIn();
		while (manager.getCurrentScaleIndex() < targetScale && coord.canZoomOut())
			coord.zoomOut();
		SwingUtilities.invokeLater(() -> {
			if (scrollPane.getViewport() != null)
				scrollPane.getHorizontalScrollBar().setValue(Math.max(0, (int) Math.floor(coord.toX(range.start()))));
		});
		return true;
	}

	static DateRange visibleRange(CoordinatesConverter coord, ScaledScrollPane scrollPane) {
		if (coord == null || scrollPane == null)
			return null;
		var viewport = scrollPane.getViewport();
		int left = viewport.getViewPosition().x;
		return new DateRange((long) coord.toTime(left),
				(long) coord.toTime(left + viewport.getExtentSize().width));
	}

	public record DateRange(long start, long end) {
	}
}
