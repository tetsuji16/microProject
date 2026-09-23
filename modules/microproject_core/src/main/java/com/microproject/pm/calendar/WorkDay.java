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
package com.microproject.pm.calendar;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

public class WorkDay extends CalendarEvent implements Comparable, Cloneable,Serializable {

	static final long serialVersionUID = 28283927181117L;
	// These values can serve as sentinels to simplify algorithms
	public static final WorkDay MINIMUM = new WorkDay(0);
	public static final WorkDay MAXIMUM = new WorkDay(Long.MAX_VALUE);
	/**
	 * @param fromDate
	 * @param toDate
	 */
	public WorkDay(long fromDate, long toDate) {
		super(fromDate, toDate);
	}

	public WorkDay(long fromDate, long toDate, String name) {
		super(fromDate, toDate, name);
	}
	@Override
	public WorkDay clone() {
		try {
			WorkDay copy = (WorkDay) super.clone();
			copy.workingHours = workingHours == null ? null : (WorkingHours) workingHours.clone();
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("WorkDay must be cloneable", e);
		}
	}

	/**
	 * @param date
	 */
	public WorkDay(long date) {
		this(date,date);
	}

	public WorkDay() {
		this(0,0);
	}

/**
 * Intersect a day with another one returning the result
 * @param other
 * @return
 */	
	WorkDay intersectWith(WorkDay other) {
		WorkDay result = new WorkDay(Math.max(getStart(), other.getStart()), Math.min(getEnd(), other.getEnd()));
		result.setWorkingHours(workingHours.intersectWith(other.getWorkingHours()));
		return result;
		
	}

	WorkingHours workingHours = new WorkingHours();

	void initialize() {
		workingHours.initialize();
	}
	/**
	 * @return Returns the workingHours.
	 */
	public WorkingHours getWorkingHours() {
		return workingHours;
	}

	/**
	 * @param workingHours The workingHours to set.
	 */
	public void setWorkingHours(WorkingHours workingHours) {
		this.workingHours = workingHours;
	}
	
	
	public boolean hasSameWorkHours(WorkDay d) {
		if (workingHours == null) {
			if (d == null || d.workingHours == null)
				return true;
			else
				return d.hasSameWorkHours(this);
		}
		if (d == null)
			return false;
		return workingHours.equals(d.workingHours);
	}
	@Override
	public boolean equals(Object candidate) {
		return candidate instanceof WorkDay other && getStart() == other.getStart();
	}

	@Override
	public int hashCode() {
		// consistent with the start-only equals above
		return Long.hashCode(getStart());
	}

	public int compare(Object event1, Object event2) {
		if (event2 instanceof Date date)
			return Long.compare(((WorkDay) event1).getStart(), date.getTime());

		if (event2 instanceof Calendar calendar)
			return Long.compare(((WorkDay) event1).getStart(), calendar.getTimeInMillis());

		if (!(event1 instanceof WorkDay first) || !(event2 instanceof WorkDay second))
			return 0;
		return Long.compare(first.getStart(), second.getStart());
	}
	
    public long getDuration() {
    	return workingHours.getDuration();
    }

    public boolean isWorking() {
    	return getDuration() > 0;
    }
	@Override
	public int compareTo(Object to) {
		if (to instanceof WorkDay workDay)
			return Long.compare(getStart(), workDay.getStart());

		if (to instanceof Date date)
			return Long.compare(getStart(), date.getTime());

		if (to instanceof Calendar calendar)
			return Long.compare(getStart(), calendar.getTimeInMillis());
		
		throw new ClassCastException("Cant compare" + to + " to a WorkDay");
	}

	public String toString() {
		return "work day " + new Date(start) + " " + hashCode() + "\n" + workingHours.toString() + "\n";
	}
	
	private static WorkDay defaultWorkDay = null;
	
	public static WorkDay getDefaultWorkDay() {
		if (defaultWorkDay == null) {
			defaultWorkDay = new WorkDay();
			defaultWorkDay.setWorkingHours(WorkingHours.getDefault());
		}
		return defaultWorkDay;
	}
	private static WorkDay nonStopWorkDay = null;
	
	public static WorkDay getNonStopWorkDay() {
		if (nonStopWorkDay == null) {
			nonStopWorkDay = new WorkDay();
			nonStopWorkDay.setWorkingHours(WorkingHours.getNonStop());
		}
		return nonStopWorkDay;
	}

	private static WorkDay nonWorkingWorkDay = null;
	
	public static WorkDay getNonWorkingDay() {
		if (nonWorkingWorkDay == null) {
			nonWorkingWorkDay = new WorkDay();
		}
		return nonWorkingWorkDay;
	}
	
}
