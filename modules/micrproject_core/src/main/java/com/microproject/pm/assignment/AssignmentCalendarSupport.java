package com.microproject.pm.assignment;

import com.microproject.pm.calendar.InvalidCalendarIntersectionException;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/** Resolves the effective calendar used by an assignment. */
final class AssignmentCalendarSupport {
	private AssignmentCalendarSupport() {
	}

	static WorkCalendar resolve(AssignmentDetail detail) {
		if (detail.getActualExceptionsCalendar() != null)
			return detail.getActualExceptionsCalendar();
		if (detail.getBaselineCalendar() != null)
			return detail.getBaselineCalendar();

		Resource resource = detail.getResource();
		Task task = detail.getTask();
		if (((NormalTask) task).isIgnoreResourceCalendar()
				|| detail.isInvalidIntersectionCalendar()
				|| resource.getEffectiveWorkCalendar() == null)
			return task.getEffectiveWorkCalendar();
		if (task.getWorkCalendar() == null)
			return resource.getEffectiveWorkCalendar();

		if (detail.getIntersectionCalendar() == null) {
			try {
				detail.setIntersectionCalendar(((WorkingCalendar) task.getEffectiveWorkCalendar())
						.intersectWith((WorkingCalendar) resource.getEffectiveWorkCalendar()));
			} catch (InvalidCalendarIntersectionException e) {
				detail.setIntersectionCalendar(WorkingCalendar.INVALID_INTERSECTION_CALENDAR);
				Alert.error(Messages.getString("Message.invalidIntersection"));
				return task.getEffectiveWorkCalendar();
			}
		}
		return detail.getIntersectionCalendar();
	}
}
