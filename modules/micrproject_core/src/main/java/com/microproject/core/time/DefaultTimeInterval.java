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

import java.util.Date;

/**
 * @author Laurent Chretienneau
 *
 */
/**
 * @deprecated Use {@link com.microproject.pm.time.MutableInterval}.
 */
@Deprecated(forRemoval = false)
public class DefaultTimeInterval implements TimeInterval, Comparable<TimeInterval> {
	protected static long EMPTY_START=-1L;
	protected static long EMPTY_END=-1L;
	protected long start=EMPTY_START;
	protected long end=EMPTY_END;
	private transient com.microproject.pm.time.MutableInterval canonical;
	
	public DefaultTimeInterval(){
	}
	public DefaultTimeInterval(long start,long end){
		this.start=start;
		this.end=end;
	}

	private com.microproject.pm.time.MutableInterval canonical() {
		if (canonical == null)
			canonical = new com.microproject.pm.time.MutableInterval(start, end);
		else {
			canonical.setStart(start);
			canonical.setEnd(end);
		}
		return canonical;
	}

	private void synchronizeFromCanonical() {
		start = canonical.getStart();
		end = canonical.getEnd();
	}

	@Override
	public long getStart() { return start; }

	@Override
	public void setStart(long start) { this.start = start; }

	@Override
	public long getEnd() { return end; }

	@Override
	public void setEnd(long end) { this.end = end; }

	@Override
	public void union(TimeInterval interval) { 
		canonical().union(interval);
		synchronizeFromCanonical();
	}

	@Override
	public void inter(TimeInterval interval) {
		canonical().inter(interval);
		synchronizeFromCanonical();
	}

	@Override
	public void clear() { start=EMPTY_START; end=EMPTY_END; }

	@Override
	public boolean isEmpty() { return start==EMPTY_START || end==EMPTY_END; }

	@Override
	public int compareTo(TimeInterval interval) {
		if (isEmpty()) return -1;
		if (start < interval.getStart()
				|| (start == interval.getStart() && end < interval.getEnd())) return -1;
		if (start == interval.getStart() && end == interval.getEnd()) return 0;
		return 1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj==null || (!(obj instanceof DefaultTimeInterval)
				&& !(obj instanceof com.microproject.pm.time.Interval)))
			return false;
		com.microproject.pm.time.HasStartAndEnd i=(com.microproject.pm.time.HasStartAndEnd)obj;
		return start==i.getStart() && end==i.getEnd();
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(start) * 31 + Long.hashCode(end);
	}
	
	@Override
	public String toString() {
		return "["+new Date(start)+","+new Date(end)+"]";
	}
	
	

}
