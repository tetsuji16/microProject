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

/**
 * A generator which corresponds to an instant in time.  It has three periods:
 * Everything before and up to the instant, the instant itself, and everything after
 */
public class InstantIntervalGenerator implements IntervalGenerator {
	long start = 0;
	long end;
	long instant;
	public static InstantIntervalGenerator getInstance(long instant) {
		return new InstantIntervalGenerator(instant);
	}

	/**
	 * Constructor
	 * @param instant The instant. Start and end of the active interval are the same
	 */
	private InstantIntervalGenerator(long instant) {
		this.instant = instant;
		start = 0;
		end = instant;
	}

	public Object current() {
		return this;
	}

	public long currentEnd() {
		return end;
	}

	public long currentStart() {
		return start;
	}

	public boolean isCurrentActive() {
		return false;
	}

	public boolean hasNext() {
		return start == 0;
	}

	/** 
	 * Identifies this generator as one that is specific.
	 */
	public boolean canBeShared() {
		return false;
	}

	/** 
	 * Move on to next period.
	 */
	public boolean evaluate(Object arg0) {
		if (start == 0) {
			start = instant;
			// end is already instant
		} else if (start == instant) {
			start = Long.MAX_VALUE;
			end = Long.MAX_VALUE;
		} else {
			return false;
		}
		return true;
	}

}
