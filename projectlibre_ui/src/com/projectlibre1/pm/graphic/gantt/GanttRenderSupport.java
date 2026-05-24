package com.projectlibre1.pm.graphic.gantt;

import java.text.DateFormat;
import java.util.Calendar;
import java.awt.Color;

import com.projectlibre1.field.FieldConverter;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.graphic.configuration.TexturedShape;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.timescale.TimeIterator;
import com.projectlibre1.util.DateTime;

/**
 * Shared rendering calculations for the Swing and JavaFX Gantt surfaces.
 */
final class GanttRenderSupport {
	private GanttRenderSupport() {
	}

	static boolean shouldIncludeInProgressLine(GraphicNode node) {
		if (node == null || !node.isSchedule() || node.isAssignment() || node.getNode() == null) {
			return false;
		}
		Object impl = node.getNode().getImpl();
		if (!(impl instanceof Task)) {
			return false;
		}
		Task task = (Task) impl;
		if (node.isSummary() && !node.isCollapsed()) {
			return false;
		}
		if (task.isMilestone() || task.isExternal() || task.isSubproject()) {
			return false;
		}
		long start = task.getStart();
		long end = task.getEnd();
		return start != 0L && end > start;
	}

	static double getProgressLineX(CoordinatesConverter coord, Task task) {
		long start = task.getStart();
		long end = task.getEnd();
		double progress = clampProgress(task.getPercentComplete());
		long today = getStatusDate(task);
		long progressDate;
		if (progress == 1.0d && end <= today) {
			progressDate = today;
		} else if (progress == 0.0d && start >= today) {
			progressDate = today;
		} else {
			progressDate = start + Math.round((end - start) * progress);
		}
		return coord.toX(progressDate);
	}

	static long getStatusDate(Task task) {
		Project project = task.getProject();
		long statusDate = project == null ? 0L : project.getStatusDate();
		return statusDate == 0L ? System.currentTimeMillis() : statusDate;
	}

	static double getProgressLineY(GraphicNode node, GraphicConfiguration config, int rowHeight) {
		int yOffset = config.getGanttBarYOffset() + config.getGanttBarHeight() / 2;
		return rowHeight * node.getRow() + yOffset;
	}

	static double clampProgress(double value) {
		if (value < 0.0d) {
			return 0.0d;
		}
		if (value > 1.0d) {
			return 1.0d;
		}
		return value;
	}

	static String formatAnnotationValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof java.util.Date) {
			String text = DateFormat.getDateInstance(DateFormat.SHORT).format((java.util.Date) value);
			int i = text.lastIndexOf('/');
			if (i > 0) {
				return text.substring(0, i);
			}
			return text;
		}
		return FieldConverter.toString(value, value.getClass(), null);
	}

	static BarGeometry computeBarGeometry(GraphicNode node, ScheduleInterval interval, BarFormat format, CoordinatesConverter coord, GraphicConfiguration config, int rowHeight) {
		double x = coord.toX(interval.getStart());
		double width = CoordinatesConverter.adaptSmallBarEndX(x, coord.toX(interval.getEnd()), node, config) - x;
		double y = node.getRow() * rowHeight + config.getGanttBarYOffset();
		int row = format.getRow();
		double height;
		if (row == 1) {
			height = config.getGanttBarHeight();
		} else {
			height = config.getBaselineHeight();
			y += config.getGanttBarHeight() + config.getBaselineHeight() * (row - 2);
		}
		y += height / 2.0d;
		return new BarGeometry(x, y, width, height);
	}

	static double computeCompletedWidth(GraphicNode node, ScheduleInterval interval, double width, CoordinatesConverter coord, GraphicConfiguration config, boolean completionIsContiguous) {
		double x = coord.toX(interval.getStart());
		double completedW = coord.toX(node.getCompleted()) - x;
		if (completedW > width && !completionIsContiguous) {
			completedW = width;
		}
		return CoordinatesConverter.adaptSmallBarEndX(x, x + completedW, node, config) - x;
	}

	static double computeAnnotationX(GraphicNode node, CoordinatesConverter coord, GraphicConfiguration config) {
		double x0 = coord.toX(node.getStart());
		double x1 = coord.toX(node.getEnd());
		x1 = CoordinatesConverter.adaptSmallBarEndX(x0, x1, node, config);
		return Math.ceil(x1) + config.getGanttBarAnnotationXOffset();
	}

	static int computeRowSeparatorY(GraphicNode node, int rowHeight) {
		return (node.getRow() + 1) * rowHeight - 1;
	}

	static Color resolveBarColor(BarStyles barStyles, Object ganttable) {
		final Color[] resolved = new Color[] { Color.DARK_GRAY };
		if (barStyles == null) {
			return resolved[0];
		}
		barStyles.apply(ganttable, arg0 -> {
			BarFormat format = (BarFormat) arg0;
			TexturedShape middle = format.getMiddle();
			if (middle != null && middle.getColor() != null) {
				resolved[0] = middle.getColor();
			}
		});
		return resolved[0];
	}

	static final class BarGeometry {
		final double x;
		final double y;
		final double width;
		final double height;

		BarGeometry(double x, double y, double width, double height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
	}

	static boolean isWorkingDay(WorkingCalendar calendar, long time) {
		return CalendarService.getInstance().getDay(calendar, time).isWorking();
	}

	static TimeIterator createTimeIterator(CoordinatesConverter coord, double start, double end, boolean useScale2) {
		return coord.getTimeIterator(start, end, useScale2);
	}

	static void advanceCalendarToNextWorkingBoundary(CoordinatesConverter coord, Calendar cal, boolean useScale2) {
		if (useScale2) {
			coord.getTimescaleManager().getScale().increment2(cal);
		} else {
			coord.getTimescaleManager().getScale().increment1(cal);
		}
	}

	static long adjustNonWorkingEnd(CoordinatesConverter coord, Calendar cal, long endNonWorking, boolean useScale2) {
		cal.setTimeInMillis(endNonWorking);
		advanceCalendarToNextWorkingBoundary(coord, cal, useScale2);
		return cal.getTimeInMillis();
	}

	static Calendar newCalendar() {
		return DateTime.calendarInstance();
	}
}
