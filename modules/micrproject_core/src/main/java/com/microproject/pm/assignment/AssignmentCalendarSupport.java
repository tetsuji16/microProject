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
