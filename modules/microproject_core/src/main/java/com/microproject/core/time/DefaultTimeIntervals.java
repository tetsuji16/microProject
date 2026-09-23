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
package com.microproject.core.time;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * @author Laurent Chretienneau
 *
 */
/** @deprecated Use {@link com.microproject.pm.time.MutableInterval} for a single range. */
@Deprecated(forRemoval = false)
public class DefaultTimeIntervals implements TimeIntervals {
	protected static long EMPTY_START=-1L;
	protected static long EMPTY_END=-1L;
	protected TreeSet<TimeInterval> intervals;

	public DefaultTimeIntervals(){
		intervals = new TreeSet<>(Comparator.comparingLong(TimeInterval::getStart));
	}
	public DefaultTimeIntervals(long start,long end){
		this();
		intervals.add(new DefaultTimeInterval(start, end));
	}
	
	
	@Override
	public long getStart() {
		return intervals.isEmpty()?EMPTY_START:intervals.first().getStart();
	}

	@Override
	public void setStart(long start) {
		TimeInterval t;
		if (isEmpty()) {
			t = new DefaultTimeInterval();
			intervals.add(t);
		} else {
			t = intervals.first();
		}
		t.setStart(start);
	}

	@Override
	public long getEnd() {
		return isEmpty()?EMPTY_END:intervals.last().getEnd();
	}

	@Override
	public void setEnd(long end) {
		TimeInterval t;
		if (isEmpty()) {
			t = new DefaultTimeInterval();
			intervals.add(t);
		} else {
			t = intervals.last();
		}
		t.setEnd(end);
	}

	@Override
	public Collection<TimeInterval> getIntervals() {
		return intervals;
	}

	@Override
	public void addInterval(TimeInterval interval) {
		intervals.add(interval);
	}
	@Override
	public Iterator<TimeInterval> iterator() {
		return intervals.iterator();
	}

	@Override
	public void clear() {
		intervals.clear();
	}

	@Override
	public boolean isEmpty() {
		return intervals.isEmpty();
	}

	@Override
	public int size() {
		return intervals.size();
	}
	
	@Override
	public void union(TimeInterval interval) {
		//intersection start
		TimeInterval before=intervals.lower(interval);
		if (before!=null && interval.getStart()<=before.getEnd()) 
			interval=new DefaultTimeInterval(before.getStart(),interval.getEnd());
		
		//intersection end
		NavigableSet<TimeInterval> inter=intervals.subSet(interval, true, new DefaultTimeInterval(interval.getEnd(),interval.getEnd()), true);
		if (!inter.isEmpty()){
			TimeInterval after=inter.last();
			if (after!=null && interval.getEnd()<=after.getEnd())
				interval=new DefaultTimeInterval(interval.getStart(),after.getEnd());
		}
		
		intervals.removeAll(inter);	
		intervals.add(interval);
	}

	@Override
	public void inter(TimeInterval interval) {
	}
	
	@Override
	public void union(long start, long end) {
		union(new DefaultTimeInterval(start, end));		
	}
	@Override
	public void inter(long start, long end) {
		inter(new DefaultTimeInterval(start, end));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultTimeIntervals other))
			return false;
		if (size() != other.size())
			return false;
		Iterator<TimeInterval> thisIntervals = iterator();
		Iterator<TimeInterval> otherIntervals = other.iterator();
		while (thisIntervals.hasNext()) {
			if (!thisIntervals.next().equals(otherIntervals.next()))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return intervals.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		int i=0;
		for (TimeInterval interval : intervals){
			if (!(i++ == 0)) s.append(", ");
			s.append(interval);
		}
		return s.toString();
	}

	
}
