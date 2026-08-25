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

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.scheduling.ScheduleInterval;

/**
 * Shared geometry helper for Gantt selection and hit-test rectangles.
 */
final class GanttSelectionGeometrySupport {
	private static final int BAR_MOVE = 3;
	private static final int BAR_MOVE_START = 4;
	private static final int BAR_MOVE_END = 5;
	private static final int PROGRESS_BAR_MOVE = 6;

	private final TaskProjectionSnapshot.Row row;
	private final int projectionRow;
	private final GanttBarGeometry barGeometry;
	private final CoordinatesConverter coord;
	private final GanttUI ui;
	private final GraphicConfiguration config;
	private final double x0;
	private final double x;
	private final int state;
	private final int selectedIntervalNumber;
	private final ScheduleInterval selectedInterval;

	GanttSelectionGeometrySupport(TaskProjectionSnapshot.Row row, int projectionRow, GanttBarGeometry barGeometry, CoordinatesConverter coord, GanttUI ui, GraphicConfiguration config,
			double x0, double x, int state, int selectedIntervalNumber, ScheduleInterval selectedInterval) {
		this.row = row;
		this.projectionRow = projectionRow;
		this.barGeometry = barGeometry;
		this.coord = coord;
		this.ui = ui;
		this.config = config;
		this.x0 = x0;
		this.x = x;
		this.state = state;
		this.selectedIntervalNumber = selectedIntervalNumber;
		this.selectedInterval = selectedInterval;
	}

	Shape createBarShadowBounds() {
		if (row == null || coord == null || ui == null || config == null) {
			return null;
		}
		double deltaX = x - x0;
		double xStart = getSelectionStartForNode(row, coord, config, barGeometry.height());
		if (state == PROGRESS_BAR_MOVE) {
			double completedX = coord.toX(row.completed());
			return new Rectangle2D.Double(
					xStart,
					barY() + barGeometry.offset() + (barGeometry.height() - config.getGanttProgressBarHeight()) / 2.0d,
					completedX - xStart + deltaX,
					config.getGanttProgressBarHeight());
		}
		double xEnd = (selectedIntervalNumber == 0 && state == BAR_MOVE)
				? getSelectionEndForBar(row, coord, config, barGeometry.height())
				: getSelectionEndForInterval(row, coord, config, selectedInterval, barGeometry.height());
		double w = xEnd - xStart;
		switch (state) {
		case BAR_MOVE:
			return new Rectangle2D.Double(xStart + deltaX, barY() + barGeometry.offset(), w, barGeometry.height());
		case BAR_MOVE_START:
			return new Rectangle2D.Double(xStart + deltaX, barY() + barGeometry.offset(), w - deltaX, barGeometry.height());
		case BAR_MOVE_END:
			return new Rectangle2D.Double(xStart, barY() + barGeometry.offset(), w + deltaX, barGeometry.height());
		default:
			return null;
		}
	}

	Rectangle2D createLinkSelectionShadowBounds() {
		if (row == null || coord == null || ui == null || config == null) {
			return null;
		}
		double xStart = getSelectionStartForNode(row, coord, config, barGeometry.height());
		double xEnd = getSelectionEndForNode(row, coord, config, barGeometry.height());
		return new Rectangle2D.Double(xStart, barY() + barGeometry.offset(), xEnd - xStart, barGeometry.height());
	}

	double getSelectionStartForNode() {
		return getSelectionStartForNode(row, coord, config, barGeometry.height());
	}

	double getSelectionEndForNode() {
		return getSelectionEndForNode(row, coord, config, barGeometry.height());
	}

	double getSelectionEndForInterval() {
		return getSelectionEndForInterval(row, coord, config, selectedInterval, barGeometry.height());
	}

	double getSelectionEndForBar() {
		return getSelectionEndForBar(row, coord, config, barGeometry.height());
	}

	double getLinkOriginX() {
		if (row == null || coord == null || config == null) {
			return 0.0d;
		}
		long start = selectedIntervalNumber == 0 || selectedInterval == null ? row.start() : selectedInterval.getStart();
		double xStart = coord.toX(start);
		double xEnd = selectedIntervalNumber == 0 ? getSelectionEndForNode() : getSelectionEndForInterval();
		return (xStart + xEnd) / 2.0d;
	}

	double getLinkOriginY() {
		if (row == null || ui == null) {
			return 0.0d;
		}
		return barY() + barGeometry.offset() + barGeometry.height() / 2.0d;
	}

	static double getSelectionEndForBar(TaskProjectionSnapshot.Row row, CoordinatesConverter coord, GraphicConfiguration config, double shapeHeight) {
		if (row.milestone()) {
			return milestoneSelectionEnd(coord.toX(row.start()), shapeHeight, config.getSelectionSquare());
		}
		return adaptSmallBarEndX(row, coord, config);
	}

	static double getSelectionEndForNode(TaskProjectionSnapshot.Row row, CoordinatesConverter coord, GraphicConfiguration config, double shapeHeight) {
		if (row.milestone()) {
			return milestoneSelectionEnd(coord.toX(row.start()), shapeHeight, config.getSelectionSquare());
		}
		return adaptSmallBarEndX(row, coord, config);
	}

	static double getSelectionEndForInterval(TaskProjectionSnapshot.Row row, CoordinatesConverter coord, GraphicConfiguration config, ScheduleInterval interval, double shapeHeight) {
		if (row == null || coord == null || config == null || interval == null) {
			return 0.0d;
		}
		if (interval.getEnd() == interval.getStart() && row.milestone()) {
			return getMilestoneSelectionEnd(coord.toX(interval.getStart()), shapeHeight, config);
		}
		return coord.toX(interval.getEnd());
	}

	static double getSelectionStartForNode(TaskProjectionSnapshot.Row row, CoordinatesConverter coord, GraphicConfiguration config, double shapeHeight) {
		double x = coord.toX(row.start());
		if (!row.milestone()) {
			return x;
		}
		return milestoneSelectionStart(x, shapeHeight, config.getSelectionSquare());
	}

	private static double adaptSmallBarEndX(TaskProjectionSnapshot.Row row, CoordinatesConverter coord,
			GraphicConfiguration config) {
		double start = coord.toX(row.start());
		double end = coord.toX(row.end());
		return row.intervals().size() <= 1 && start < end && end - start < config.getGanttBarMinWidth()
				? start + config.getGanttBarMinWidth() : end;
	}

	static double getMilestoneSelectionEnd(double xCenter, double shapeHeight, GraphicConfiguration config) {
		return milestoneSelectionEnd(xCenter, shapeHeight, config.getSelectionSquare());
	}

	static double milestoneSelectionStart(double xCenter, double shapeHeight, double selectionSquare) {
		double width = Math.max(shapeHeight, selectionSquare);
		return xCenter - width / 2.0d;
	}

	static double milestoneSelectionEnd(double xCenter, double shapeHeight, double selectionSquare) {
		double width = Math.max(shapeHeight, selectionSquare);
		return xCenter + width / 2.0d;
	}

	private double barY() {
		return ui.getBarY(projectionRow);
	}
}
