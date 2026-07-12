package com.projectlibre1.pm.assignment;

import com.projectlibre1.pm.calendar.InvalidCalendarIntersectionException;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;

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
