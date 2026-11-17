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
package com.microproject.util;

import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.task.TaskSpecificFields;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProgressLineSettings;
import com.microproject.util.DateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;

public final class GanttProgress {
	private GanttProgress() {
	}

	public static double ratio(Schedule schedule, Object impl) {
		if (impl instanceof TaskSpecificFields)
			return DisplayMath.clampProgressValue(((TaskSpecificFields)impl).getPercentComplete());
		return schedule == null ? 0.0d : DisplayMath.clampProgressValue(schedule.getPercentComplete());
	}

	public static double ratioForObject(Object impl) {
		return ratio(impl instanceof Schedule ? (Schedule)impl : null, impl);
	}

	public static boolean hasVisibleProgress(Object impl) {
		return ratioForObject(impl) > 0.0d;
	}

	/**
	 * Resolves the Complete Through date used by Microsoft Project-style progress
	 * lines. Task progress is duration based (% Complete), while the schedule
	 * supplies the calendar-aware completion date.
	 */
	/** Resolves the configured current and recurring progress-line dates for a project. */
	public static List<Long> progressLineReferenceDates(Project project, ProgressLineSettings settings) {
		if (project == null || settings == null)
			return List.of();
		long currentDate = settings.referenceDate() == ProgressLineSettings.ReferenceDate.STATUS_DATE
			? project.getStatusDate() : project.getCurrentDate();
		if (currentDate <= 0L)
			currentDate = DateTime.midnightTomorrow() - 1L;
		return progressLineReferenceDates(project.getStart(), project.getEnd(), currentDate, settings);
	}

	static List<Long> progressLineReferenceDates(long projectStart, long projectEnd, long currentDate,
			ProgressLineSettings settings) {
		if (settings == null)
			return List.of();
		LinkedHashSet<Long> dates = new LinkedHashSet<>();
		if (settings.showCurrentLine() && currentDate > 0L)
			dates.add(Long.valueOf(currentDate));
		if (settings.showRecurringLines() && projectStart > 0L && projectEnd >= projectStart) {
			Calendar date = DateTime.calendarInstance();
			date.setTimeInMillis(projectStart);
			int calendarField = switch (settings.recurringIntervalUnit()) {
				case DAY -> Calendar.DAY_OF_MONTH;
				case WEEK -> Calendar.WEEK_OF_YEAR;
				case MONTH -> Calendar.MONTH;
			};
			int count = 0;
			while (date.getTimeInMillis() <= projectEnd && count++ < 10_000) {
				dates.add(Long.valueOf(date.getTimeInMillis()));
				date.add(calendarField, settings.recurringInterval());
			}
		}
		return List.copyOf(dates);
	}

	public static long progressLineDate(Schedule schedule, long referenceDate) {
		if (schedule == null)
			return referenceDate;
		long start = schedule.getStart();
		long end = schedule.getEnd();
		double progress = DisplayMath.clampProgressValue(schedule.getPercentComplete());
		if (referenceDate != 0L && progress >= 1.0d && end <= referenceDate)
			return referenceDate;
		if (referenceDate != 0L && progress <= 0.0d && start >= referenceDate)
			return referenceDate;
		long completedThrough = schedule.getCompletedThrough();
		if (completedThrough <= 0L)
			return start;
		return Math.max(start, Math.min(end, completedThrough));
	}

}
