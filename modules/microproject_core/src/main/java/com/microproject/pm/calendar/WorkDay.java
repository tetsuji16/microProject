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

import com.microproject.util.MathUtils;
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
	public Object clone() {
		WorkDay newOne = null;
		try {
			newOne = (WorkDay) super.clone();
			newOne.workingHours = workingHours == null ? null : (WorkingHours) workingHours.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("WorkDay must be cloneable", e);
		}
		return newOne;
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
		WorkDay result = new WorkDay(Math.max(getStart(),other.getStart()),Math.min(getStart(),other.getStart()));
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
	public boolean equals(Object e) {
		if (! (e instanceof WorkDay))
			return false;
		return (getStart() == ((WorkDay)e).getStart());
	}

	@Override
	public int hashCode() {
		// consistent with the start-only equals above
		return Long.hashCode(getStart());
	}

	public int compare(Object event1, Object event2) {
		
		if (event2 instanceof Date) // if comparing to a date
			return MathUtils.signum(((WorkDay)event1).getStart() - ((Date)event2).getTime()); 

		if (event2 instanceof Calendar) // if comparing to a date
			return MathUtils.signum(((WorkDay)event1).getStart() - ((Calendar)event2).getTimeInMillis()); 
		
		if (! (event1 instanceof WorkDay) || ! (event2 instanceof WorkDay))
			return 0;
		
		return MathUtils.signum(((WorkDay)event1).getStart() - ((WorkDay)event2).getStart());
	}
	
    public long getDuration() {
    	return workingHours.getDuration();
    }

    public boolean isWorking() {
    	return getDuration() > 0;
    }
	public int compareTo(Object to) {
		if (to instanceof WorkDay) // if comparing to a date
			return MathUtils.signum((getStart() - ((WorkDay)to).getStart()));

		if (to instanceof Date) // if comparing to a date
			return MathUtils.signum(getStart() - ((Date)to).getTime()); 

		if (to instanceof Calendar) // if comparing to a date
			return MathUtils.signum(getStart() - ((Calendar)to).getTimeInMillis()); 
		
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
