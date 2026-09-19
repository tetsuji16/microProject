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

import java.util.function.Consumer;


import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.timescale.TimeIterator;

/**
 * A generator corresponding to a start/end with a stepping value
 */
public class TimeIteratorGenerator implements IntervalGenerator, HasStartAndEnd {
	TimeIterator timeIterator;
	HasStartAndEnd currentInterval = null;
	long currentEnd;
	Consumer<Object> visitor = null;
	int index = 0;

	private TimeIteratorGenerator(TimeIterator timeIterator) {
		this.timeIterator = timeIterator;
		currentInterval = timeIterator.next(); // set to first one
	}


	public Object current() {
		return currentInterval;
	}

	public long currentEnd() {
		return currentInterval.getEnd();
	}

	public long currentStart() {
		return currentInterval.getStart();
	}	
	public boolean evaluate(Object obj) {
//		System.out.println("----iterator date " + new java.util.Date(currentInterval.getStart()));
		currentInterval = timeIterator.next();
		index++;
		return timeIterator.hasNext();
	}

	public int compareTo(Object arg0) {
		return 0;
	}

	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return timeIterator.getEnd();
	}

	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return timeIterator.getStart();
	}

	public static TimeIteratorGenerator getInstance(TimeIterator timeIterator) {
		return new TimeIteratorGenerator(timeIterator);
	}

	public boolean isCurrentActive() {
		return true;
	}

	public boolean hasNext() {
		return timeIterator.hasNext();
	}

	public boolean canBeShared() {
		return true;
	}
	/**
	 * @return Returns the index.
	 */
	public int getIndex() {
		return index;
	}
}
