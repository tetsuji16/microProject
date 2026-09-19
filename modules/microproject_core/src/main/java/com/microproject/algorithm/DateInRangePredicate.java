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

import org.apache.commons.collections.Predicate;

import com.microproject.pm.time.HasStartAndEnd;

/**
 * A predicate which tests whether an interval is enclosed in a range
 */
public class DateInRangePredicate implements Predicate, HasStartAndEnd {
	long start;
	long end;
	/**
	 * 
	 */
	private DateInRangePredicate(long start, long end) {
		super();
		this.start = start;
		this.end = end;
	}

	public boolean evaluate(Object arg0) {
		HasStartAndEnd interval = (HasStartAndEnd)arg0;
//		boolean x = (interval.getStart() >= start && interval.getEnd() <= end);
//		System.out.println("where " + x + "range: " 
//				+ new java.util.Date(start) + "-"
//				+ new java.util.Date(end) + "  interval "
//				+ new java.util.Date(interval.getStart()) + "-"
//				+ new java.util.Date(interval.getEnd()));
		return (interval.getStart() >= start && interval.getEnd() <= end);
	}
		
	/**
	 * Factory method
	 * @param start
	 * @param end
	 * @return
	 */	
	public static DateInRangePredicate getInstance(long start, long end) {
		return new DateInRangePredicate(start,end);
	}
	
	// intersect with another range
	public void limitTo(long start, long end) {
		if (start > this.start)
			this.start = start;
		if (end  < this.end)
			this.end = end;
	}
	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}
	
	public String toString() {
		return new java.util.Date(start) + " " + new java.util.Date(end);
	}

}
