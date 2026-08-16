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
package com.microproject.pm.scheduling;


import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.util.DateTime;

/**
 * class to hold bar information
 */
public class ScheduleInterval implements HasStartAndEnd, Cloneable {
	long start;
	long end;
	long minimumStart = 0;
	long maximumStart = DateTime.getMaxDate().getTime();
	long minimumEnd = 0;
	long maximumEnd = maximumStart;
	
	public ScheduleInterval(long start, long end) {
		this.start = start;
		this.end = end;
	}
	
	public boolean isValid() {
		return start <= end;
	}
	public boolean canChangeStart() {
		return minimumStart != maximumStart;
	}
	
	public boolean canChangeDuration() {
		return minimumEnd != maximumEnd;
	}
	
	boolean canChangeOwner() { // for now don't allow dragging to a different row
		return false;
	}
	
	/**
	 * @return Returns the maximumEnd.
	 */
	public long getMaximumEnd() {
		return maximumEnd;
	}
	/**
	 * @param maximumEnd The maximumEnd to set.
	 */
	public void setMaximumEnd(long maximumEnd) {
		this.maximumEnd = maximumEnd;
	}
	/**
	 * @return Returns the maximumStart.
	 */
	public long getMaximumStart() {
		return maximumStart;
	}
	/**
	 * @param maximumStart The maximumStart to set.
	 */
	public void setMaximumStart(long maximumStart) {
		this.maximumStart = maximumStart;
	}
	/**
	 * @return Returns the minimumEnd.
	 */
	public long getMinimumEnd() {
		return minimumEnd;
	}
	/**
	 * @param minimumEnd The minimumEnd to set.
	 */
	public void setMinimumEnd(long minimumEnd) {
		this.minimumEnd = minimumEnd;
	}
	/**
	 * @return Returns the minimumStart.
	 */
	public long getMinimumStart() {
		return minimumStart;
	}
	/**
	 * @param minimumStart The minimumStart to set.
	 */
	public void setMinimumStart(long minimumStart) {
		this.minimumStart = minimumStart;
	}
	

	
	
	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public ScheduleInterval intersectWith(HasStartAndEnd bounds) {
		if (bounds == null)
			return this;
		ScheduleInterval result = (ScheduleInterval) clone();
		result.start = Math.max(start,bounds.getStart());
		result.end = Math.min(end, bounds.getEnd());
		return result;
	}
	public String toString() {
		return "Start:" + new java.util.Date(start) + " End:" + new java.util.Date(end);
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("ScheduleInterval should be cloneable", e);
		}
	}
}
