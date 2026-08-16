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
package com.microproject.algorithm.buffer;

import java.io.Serializable;
import java.util.Comparator;

import com.microproject.algorithm.DoubleValue;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.util.MathUtils;


/**
 * Used to hold a point in Calculated values arrays
 */
public class Point implements Comparable, Comparator, Serializable, HasStartAndEnd, DoubleValue {
	static final long serialVersionUID = 629828246846L;
	public Point(long date, double value) {
		this.date = date;
		this.value = value;
	}
	
	void addValue(double v) {
		value += v;
	}
	long date;
	double value;
	
	public String toString() {
		return new java.util.Date(date) + " " + value;
	}

	public int compareTo(Object to) {
		return MathUtils.signum(date - ((Point)to).date);
	}
	/**
	 * @return Returns the date.
	 */
	public long getDate() {
		return date;
	}
	/**
	 * @param date The date to set.
	 */
	public void setDate(long date) {
		this.date = date;
	}
	/**
	 * @return Returns the value.
	 */
	public double getValue() {
		return value;
	}
	/**
	 * @param value The value to set.
	 */
	public void setValue(double value) {
		this.value = value;
	}

	public int compare(Object arg0, Object arg1) {
		return ((Point)arg0).compareTo(arg1);
	}

	public long getStart() {
		return date;
	}

	public long getEnd() {
		return date;
	}
}
