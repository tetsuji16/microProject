package com.projectlibre1.pm.graphic.gantt;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.pm.scheduling.ScheduleInterval;

/**
 * Shared geometry helper for Gantt selection and hit-test rectangles.
 */
final class GanttSelectionGeometrySupport {
	private static final int BAR_MOVE = 3;
	private static final int BAR_MOVE_START = 4;
	private static final int BAR_MOVE_END = 5;
	private static final int PROGRESS_BAR_MOVE = 6;

	private final GraphicNode node;
	private final CoordinatesConverter coord;
	private final GanttUI ui;
	private final GraphicConfiguration config;
	private final double x0;
	private final double x;
	private final int state;
	private final int selectedIntervalNumber;
	private final ScheduleInterval selectedInterval;

	GanttSelectionGeometrySupport(GraphicNode node, CoordinatesConverter coord, GanttUI ui, GraphicConfiguration config,
			double x0, double x, int state, int selectedIntervalNumber, ScheduleInterval selectedInterval) {
		this.node = node;
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
		if (node == null || coord == null || ui == null || config == null) {
			return null;
		}
		double deltaX = x - x0;
		double xStart = getSelectionStartForNode(node, coord, config);
		if (state == PROGRESS_BAR_MOVE) {
			double completedX = coord.toX(node.getCompleted());
			return new Rectangle2D.Double(
					xStart,
					barY(node) + node.getGanttShapeOffset() + (node.getGanttShapeHeight() - config.getGanttProgressBarHeight()) / 2.0d,
					completedX - xStart + deltaX,
					config.getGanttProgressBarHeight());
		}
		double xEnd = (selectedIntervalNumber == 0 && state == BAR_MOVE)
				? getSelectionEndForBar(node, coord, config)
				: getSelectionEndForInterval(node, coord, config, selectedInterval);
		double w = xEnd - xStart;
		switch (state) {
		case BAR_MOVE:
			return new Rectangle2D.Double(xStart + deltaX, barY(node) + node.getGanttShapeOffset(), w, node.getGanttShapeHeight());
		case BAR_MOVE_START:
			return new Rectangle2D.Double(xStart + deltaX, barY(node) + node.getGanttShapeOffset(), w - deltaX, node.getGanttShapeHeight());
		case BAR_MOVE_END:
			return new Rectangle2D.Double(xStart, barY(node) + node.getGanttShapeOffset(), w + deltaX, node.getGanttShapeHeight());
		default:
			return null;
		}
	}

	Rectangle2D createLinkSelectionShadowBounds() {
		if (node == null || coord == null || ui == null || config == null) {
			return null;
		}
		double xStart = getSelectionStartForNode(node, coord, config);
		double xEnd = getSelectionEndForNode(node, coord, config);
		return new Rectangle2D.Double(xStart, barY(node) + node.getGanttShapeOffset(), xEnd - xStart, node.getGanttShapeHeight());
	}

	double getSelectionStartForNode() {
		return getSelectionStartForNode(node, coord, config);
	}

	double getSelectionEndForNode() {
		return getSelectionEndForNode(node, coord, config);
	}

	double getSelectionEndForInterval() {
		return getSelectionEndForInterval(node, coord, config, selectedInterval);
	}

	double getSelectionEndForBar() {
		return getSelectionEndForBar(node, coord, config);
	}

	double getLinkOriginX() {
		if (node == null || coord == null || config == null) {
			return 0.0d;
		}
		long start = selectedIntervalNumber == 0 || selectedInterval == null ? node.getStart() : selectedInterval.getStart();
		double xStart = coord.toX(start);
		double xEnd = selectedIntervalNumber == 0 ? getSelectionEndForNode() : getSelectionEndForInterval();
		return (xStart + xEnd) / 2.0d;
	}

	double getLinkOriginY() {
		if (node == null || ui == null) {
			return 0.0d;
		}
		return barY(node) + node.getGanttShapeOffset() + node.getGanttShapeHeight() / 2.0d;
	}

	static double getSelectionEndForBar(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config) {
		if (isMilestoneNode(node)) {
			return milestoneSelectionEnd(coord.toX(node.getStart()), node.getGanttShapeHeight(), config.getSelectionSquare());
		}
		return CoordinatesConverter.adaptSmallBarEndX(coord.toX(node.getStart()), coord.toX(node.getEnd()), node, config);
	}

	static double getSelectionEndForNode(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config) {
		if (isMilestoneNode(node)) {
			return milestoneSelectionEnd(coord.toX(node.getStart()), node.getGanttShapeHeight(), config.getSelectionSquare());
		}
		return CoordinatesConverter.adaptSmallBarEndX(coord.toX(node.getStart()), coord.toX(node.getEnd()), node, config);
	}

	static double getSelectionEndForInterval(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config, ScheduleInterval interval) {
		if (node == null || coord == null || config == null || interval == null) {
			return 0.0d;
		}
		if (interval != null && interval.getEnd() == interval.getStart() && isMilestoneNode(node)) {
			return getMilestoneSelectionEnd(coord.toX(interval.getStart()), node, config);
		}
		return coord.toX(interval.getEnd());
	}

	static double getSelectionStartForNode(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config) {
		double x = coord.toX(node.getStart());
		if (!isMilestoneNode(node)) {
			return x;
		}
		return milestoneSelectionStart(x, node.getGanttShapeHeight(), config.getSelectionSquare());
	}

	static boolean isMilestoneNode(GraphicNode node) {
		if (node == null || node.getNode() == null || !(node.getNode().getImpl() instanceof Task)) {
			return false;
		}
		return ((Task) node.getNode().getImpl()).isMilestone();
	}

	static double getMilestoneSelectionEnd(double xCenter, GraphicNode node, GraphicConfiguration config) {
		return milestoneSelectionEnd(xCenter, node.getGanttShapeHeight(), config.getSelectionSquare());
	}

	static double milestoneSelectionStart(double xCenter, double shapeHeight, double selectionSquare) {
		double width = Math.max(shapeHeight, selectionSquare);
		return xCenter - width / 2.0d;
	}

	static double milestoneSelectionEnd(double xCenter, double shapeHeight, double selectionSquare) {
		double width = Math.max(shapeHeight, selectionSquare);
		return xCenter + width / 2.0d;
	}

	private double barY(GraphicNode node) {
		return ui.getBarY(node.getRow());
	}
}
