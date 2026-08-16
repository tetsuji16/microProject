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

import java.util.function.Consumer;

import java.io.Serializable;


import com.microproject.functor.IntervalConsumer;
import com.microproject.pm.criticalpath.ScheduleWindow;
import com.microproject.pm.time.HasStartAndEnd;


public class BarClosure implements Consumer<Object>, Serializable, Cloneable {
	HasStartAndEnd bounds = null;
	static final long serialVersionUID = 7866653353331L;
		private IntervalConsumer consumer;
		private Schedule schedule;
		private long count;
		public void accept(Object arg0) {
			HasStartAndEnd interval = (HasStartAndEnd)arg0;
			long start = interval.getStart();
			if (schedule instanceof ScheduleWindow && start == schedule.getResume() && ((ScheduleWindow)schedule).getSplitDuration() == 0)
				start = schedule.getStop(); // special case
			count++;
			ScheduleInterval scheduleInterval = new ScheduleInterval(start,interval.getEnd()).intersectWith(bounds);
			if (scheduleInterval.isValid()) // bounds may make it so nothing should be drawn because end < start;
				consumer.consumeInterval(scheduleInterval);
		}
		/**
		 * @param consumer The consumer to set.
		 */
		public void initialize(IntervalConsumer consumer, Schedule schedule) {
			this.consumer = consumer;
			this.schedule = schedule;
			count = 0;
			bounds = null;
		}
		
		public void initCount() {
			count = 0;
		}
		/**
		 * @return Returns the count.
		 */
		public long getCount() {
			return count;
		}
		
		public Object clone(){
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError();
			}
		}
		public final HasStartAndEnd getBounds() {
			return bounds;
		}
		public final void setBounds(HasStartAndEnd bounds) {
			this.bounds = bounds;
		}

	}
