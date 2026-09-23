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
package com.microproject.pm.calendar;

import java.io.Serializable;
import java.util.Objects;

import com.microproject.util.DateTime;

/** A named weekday pattern that applies inclusively to a calendar date range. */
public final class WorkWeekPeriod implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	private String name;
	private long start;
	private long end;
	private WorkWeek week;

	public WorkWeekPeriod(String name, long start, long end, WorkWeek week) {
		setName(name);
		setRange(start, end);
		this.week = copyOf(Objects.requireNonNull(week, "week"));
	}

	public String getName() { return name; }

	public void setName(String name) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("Work week name is required");
		this.name = name.trim();
	}

	public long getStart() { return start; }
	public long getEnd() { return end; }

	public void setRange(long start, long end) {
		long startDay = DateTime.dayFloor(start);
		long endDay = DateTime.dayFloor(end);
		if (startDay > endDay) throw new IllegalArgumentException("Work week start must not be after its finish");
		this.start = startDay;
		this.end = endDay;
	}

	public boolean contains(long date) {
		long day = DateTime.dayFloor(date);
		return day >= start && day <= end;
	}

	public WorkDay getWeekDay(int day) {
		WorkDay result = week.getWeekDay(day);
		return result == null ? null : result.clone();
	}

	public void setWeekDay(int day, WorkDay workDay) {
		week.setWeekDay(day, workDay == null ? null : workDay.clone());
	}

	public WorkWeek getWeek() { return copyOf(week); }

	WorkDay workDayForDate(long date) {
		return week.getWeekDay(CalendarDefinition.getDayOfWeek(date));
	}

	@Override public WorkWeekPeriod clone() {
		try {
			WorkWeekPeriod result = (WorkWeekPeriod) super.clone();
			result.week = copyOf(week);
			return result;
		} catch (CloneNotSupportedException impossible) {
			throw new IllegalStateException("WorkWeekPeriod must be cloneable", impossible);
		}
	}

	boolean hasSameRange(WorkWeekPeriod other) {
		return start == other.start && end == other.end;
	}

	private static WorkWeek copyOf(WorkWeek source) {
		return source.clone();
	}
}
