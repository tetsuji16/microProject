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
package com.microproject.algorithm;

import java.time.Instant;

import com.microproject.model.time.ImmutableInterval;

/**
 * An immutable time interval that has a double value associated
 */
public class IntervalValue extends ImmutableInterval implements DoubleValue {
	private static final long serialVersionUID = -6991401364245973031L;

	private double value;

	private IntervalValue(long start, long end, double value) {
		super(start,end);
		this.value = value;
	}

	/**
	 * @return Returns the value for the interval.
	 */
	@Override
	public double getValue() {
		return value;
	}

	/**
	 * Describes the interval with its endpoints and its value. The endpoints
	 * stay GMT-read milliseconds; only their textual form uses
	 * {@link java.time.Instant} (ADR-0003).
	 */
	@Override
	public String toString() {
		return Instant.ofEpochMilli(getStart()) + "-" + Instant.ofEpochMilli(getEnd()) + " value: " + value;
	}

	/**
	 * Factory method
	 * @param start GMT-read start of the interval in milliseconds
	 * @param end GMT-read end of the interval in milliseconds
	 * @param value value associated with the interval
	 * @return a new interval value
	 */
	public static IntervalValue getInstance(long start, long end, double value) {
		return new IntervalValue(start, end, value);
	}

}
