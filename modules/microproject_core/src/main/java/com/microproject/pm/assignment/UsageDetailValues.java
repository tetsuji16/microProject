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
package com.microproject.pm.assignment;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.interval.ValueObjectForInterval;

/**
 * Per-period values of the usage detail rows that are derived from other work values:
 * cumulative work, overallocation and remaining availability.
 */
public final class UsageDetailValues {
	private UsageDetailValues() {
	}

	/** Running total of work from the beginning of time through the end of the period. */
	public static long cumulativeWork(HasTimeDistributedData data, long periodEnd) {
		if (data == null || periodEnd <= 0L)
			return 0L;
		return data.work(0L, periodEnd);
	}

	/**
	 * Work the resource can do in the period: the working time of its calendar inside each
	 * availability interval, times the maximum units of that interval.
	 */
	public static long availableWork(Resource resource, long start, long end) {
		if (resource == null || end <= start || resource.getAvailabilityTable() == null)
			return 0L;
		WorkCalendar calendar = calendarOf(resource);
		if (calendar == null)
			return 0L;
		double capacity = 0D;
		for (ValueObjectForInterval interval : resource.getAvailabilityTable().getList()) {
			long from = Math.max(start, interval.getStart());
			long to = Math.min(end, interval.getEnd());
			if (from < to) {
				double units = ((com.microproject.pm.availability.Availability) interval).getMaximumUnits();
				capacity += Math.max(0L, calendar.compare(to, from, false)) * Math.max(0D, units);
			}
		}
		return Math.round(capacity);
	}

	/** The calendar of the resource; a resource without its own calendar works on the calendar of its assignments (project calendar). */
	private static WorkCalendar calendarOf(Resource resource) {
		WorkCalendar calendar = resource.getEffectiveWorkCalendar();
		if (calendar != null || resource.getAssignments() == null)
			return calendar;
		for (Object assignment : resource.getAssignments()) {
			if (assignment instanceof Assignment own && own.getEffectiveWorkCalendar() != null)
				return own.getEffectiveWorkCalendar();
		}
		return null;
	}

	/** Work above the availability of the period. */
	public static long overallocation(long work, long availableWork) {
		return Math.max(0L, work - availableWork);
	}

	/** Availability left after the work of the period; never below zero. */
	public static long remainingAvailability(long work, long availableWork) {
		return Math.max(0L, availableWork - work);
	}
}
