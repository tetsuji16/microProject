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
import java.util.Date;
import java.util.GregorianCalendar;

import com.microproject.pm.time.ImmutableInterval;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/**
 * Immutable work range corresponding to start and end times during the day.
 */
public class WorkRange extends ImmutableInterval implements Cloneable,Serializable{ /* immutable */
	static final long serialVersionUID = 9997878787333L;
	private boolean overtime = false;
	
	
	public WorkRange(long start, long end) throws WorkRangeException  {
		this(start,end,false);
	}
	

	public WorkRange(long start, long end, boolean overtime) throws WorkRangeException  {
		super(start,end);
		if (end == 0)
			this.end = DateTime.hour24();
		
		if (end <= start)
			throw new WorkRangeException(Messages.getString("WorkRangeException.EndMustBeAfterStart"));
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("WorkRange must be cloneable", e);
		}
		
	}
	public long calcWorkingHours() {
		return getElapsedDuration();
	}

	/**
	 * @return Returns the overtime.
	 */
	public boolean isOvertime() {
		return overtime;
	}
	/**
	 * @param overtime The overtime to set.
	 */
	public void setOvertime(boolean overtime) {
		this.overtime = overtime;
	}
	
	boolean overlaps(WorkRange other) {
		if (other == null)
			return false;
		if ( (start > other.start && end > other.end) ||
			 (other.start > start && other.end > end))
			 return false;
		return true;
	}
	
	boolean isBefore(WorkRange other) {
		if (other == null)
			return true;
		return  (start < other.start && end < other.end); // change done here was > before
	}
	
	public String toString() {
		String result;
		GregorianCalendar cal = DateTime.calendarInstance();;
		cal.setTimeInMillis(start);
		GregorianCalendar cal2 = DateTime.calendarInstance();;
		cal2.setTimeInMillis(end);

		return cal.get(GregorianCalendar.HOUR_OF_DAY) +":" +cal.get(GregorianCalendar.MINUTE) + "-" +
		cal2.get(GregorianCalendar.HOUR_OF_DAY) +":" +cal2.get(GregorianCalendar.MINUTE);
	}
	
	public static Date getNormalized(long time) {
		GregorianCalendar cal = DateTime.calendarInstance();;
		cal.setTimeInMillis(time);
//		cal.roll(GregorianCalendar.HOUR_OF_DAY,false);
		return cal.getTime();
	}

	public Date getNormalizedStartTime() {
		return getNormalized(start);
	}
	public Date getNormalizedEndTime() {
		return getNormalized(end);
	}
}
