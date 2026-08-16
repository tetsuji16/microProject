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

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.Date;


/**
 * Merge is a functor which groups time intervals together and calls a vistor object on the resulting merged intervals
 * You can specify a comparator as well to determine whether two intervals are mergeable
 */
public class Merge implements Consumer<Object> {
	boolean started = false;
	long currentStart;
	long currentEnd;
	Object currentObject = null;
	Consumer<Object> callBack;
	Comparator comparator = null;
//	boolean ignoreZeroValueIntervals = false;
	
	public static Merge getInstance(Consumer<Object> callBack) {
		return new Merge(callBack);
	}

	public static Merge getInstance(Consumer<Object> callBack, Comparator comparator) {
		return new Merge(callBack, comparator);
	}

	private Merge(Consumer<Object> callBack) {
		this.callBack = callBack;
		initializeDates();
	}
	
	private Merge(Consumer<Object> callBack, Comparator comparator) {
		this(callBack);
		this.comparator = comparator;
	}
	
	public void setCallBack(Consumer<Object> callBack) {
		this.callBack = callBack;
	}
	
	private void initializeDates() {
		currentStart = Long.MAX_VALUE;
		currentEnd = Long.MIN_VALUE;
	}
	
	private void treatCurrentInterval() {
		double value = 0.0D;
		if (currentObject != null)
			value = ((DoubleValue)currentObject).getValue();
		else {
			// System.out.println("Merge.treatCurrentInterval currentObject is null - using 0.0 for value");
		}
//		if (!ignoreZeroValueIntervals || value != 0.0D)
		callBack.accept(IntervalValue.getInstance(currentStart, currentEnd, value)); // finish previous
		started = false;
		initializeDates();
	}
	/* 
	 * Execution of the functor.
	 * This takes care of merging and calling the visitor.
	 */
	public void accept(Object obj) {
		Query query = (Query)obj;
		IntervalGenerator generator = query.getGroupByGenerator();
		if (generator.isCurrentActive()) {
			if (started && comparator != null && comparator.compare(currentObject,generator.current()) != 0)  // if comparator doesnt match
				treatCurrentInterval();

			// starting
			currentObject = generator.current();
			started = true;
			
			currentStart = Math.min(currentStart,query.getStart()) ;			
			currentEnd = Math.max(currentEnd,query.getEnd());
//System.out.println("in Merge" + new Date(currentStart) + " " + new Date(currentEnd));			
			if (!generator.hasNext()) // if no more intervals, then terminate this one
				treatCurrentInterval();
		} else {
			if (generator.current() != null) {
				// ending
				started = false;
				treatCurrentInterval();				
			}
		}
	}
	
	public class MergedInterval {
		public MergedInterval(long start, long end, Object template) {
			this.start = start;
			this.end = end;
			this.template = template;
		}
		long start;
		long end;
		Object template;
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
		 * @return Returns the template.
		 */
		public Object getTemplate() {
			return template;
		}
		
		public String toString() {
			return new Date(start) + "-" + new Date(end) + template;
		}

	}
}
