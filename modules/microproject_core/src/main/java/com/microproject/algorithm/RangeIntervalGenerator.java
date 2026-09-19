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
package com.microproject.algorithm;

import java.util.GregorianCalendar;

import com.microproject.util.DateTime;

/**
 * A generator corresponding to a start/end with an optional stepping value.  The stepping value is specified as 
 * a unit type of the Calendar class (DAY_OF_YEAR for example) as a gregorian calendar is used.
 */
public class RangeIntervalGenerator implements IntervalGenerator {
	long start;
	long end;
	long step;
	int calendarStepUnit;
	int calendarStepAmount = 1;
	long currentEnd;
	long nextEnd;
	GregorianCalendar stepCal = null;

	private RangeIntervalGenerator() {
		this(0,Long.MAX_VALUE);
	}


	private RangeIntervalGenerator(long start, long end) {
		this.start = start;
		this.end = end;
		currentEnd = end;
		step = end; // make sure that will go past end
		nextEnd = end + step;
	}

	private RangeIntervalGenerator(long start, long end, int calendarStepUnit) {
		this.start = start;
		this.end = end;
		this.calendarStepUnit = calendarStepUnit;
		stepCal = new GregorianCalendar();
		stepCal.setTimeInMillis(start);
		stepCal.add(calendarStepUnit,calendarStepAmount);
		currentEnd = stepCal.getTimeInMillis();
		if (currentEnd > end) // in case just one time period
			currentEnd = end;
		stepCal.add(calendarStepUnit,calendarStepAmount);
		nextEnd = stepCal.getTimeInMillis();
	}
	


	/**
	 * Used if filtering values between dates, not in a groupBy
	 * @param start
	 * @param end
	 * @return
	 */
	public static RangeIntervalGenerator betweenInstance(long start, long end) {
		RangeIntervalGenerator result = getInstance(start, end);
		result.currentEnd = start;
		return result;
	}
	
	public Object current() {
		return this;
	}

	public long currentEnd() {
		return currentEnd;
	}

	public long currentStart() {
		return start;
	}	
	public boolean evaluate(Object obj) {
		start = currentEnd; // move on to next interval.  If only one, then will stop here
		currentEnd = nextEnd;
		if (currentEnd > end)
			currentEnd = end;
		
		if (stepCal != null) {
			stepCal.add(calendarStepUnit,calendarStepAmount);
			nextEnd = stepCal.getTimeInMillis();
		} else {
			nextEnd += step;
		}
		
		if (start >= end)
			return false;
		return true;
	}

	public int compareTo(Object arg0) {
		return 0;
	}

	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Continuous time is considered from 1/1/1970 to 1/1/3000.  Note I do not use Long.MAX_VALUE because adding to it would cause wrapping to negative values 
	 */
	public static RangeIntervalGenerator continuous() {
		return getInstance(0, DateTime.getMaxCalendar().getTimeInMillis());
	}
	public static RangeIntervalGenerator empty() {
		return getInstance(0, 0);	
	}

	public static RangeIntervalGenerator getInstance() {
		return new RangeIntervalGenerator();
	}

	public static RangeIntervalGenerator getInstance(long start, long end) {
		return new RangeIntervalGenerator(start, end);
	}

	public static RangeIntervalGenerator getInstance(long start, long end, int calendarStepUnit) {
		return new RangeIntervalGenerator(start, end, calendarStepUnit);
	}

	public boolean isCurrentActive() {
		return true;
	}

	public boolean hasNext() {
		return (nextEnd < end);
	}

	public boolean canBeShared() {
		return true;
	}
}
