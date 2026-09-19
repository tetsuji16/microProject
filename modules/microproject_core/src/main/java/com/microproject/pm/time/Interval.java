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
package com.microproject.pm.time;

import java.io.Serializable;
import java.util.Comparator;

import com.microproject.util.MathUtils;

/**
 *
 */
public abstract class Interval implements HasStartAndEnd, Comparable, Comparator, Serializable {
	static final long serialVersionUID = 26373991911119L;
	protected long start;
	protected long end;
	
	public Interval(long start, long end) {
		this.start = start;
		this.end = end;
	}
	
	public long getStart() {
		return start;
	}
	public long getEnd() {
		return end;
	}
	
	public long getElapsedDuration() {
		return end-start; // no calendars needed
	}

	public int compareTo(Object arg0) {
		return compare(this,arg0);
	}
	public int compare(Object t1, Object t2) {
		if (! (t1 instanceof HasStartAndEnd) || ! (t2 instanceof HasStartAndEnd))
			return 0;
		
		return MathUtils.signum(((HasStartAndEnd)t1).getStart() - ((HasStartAndEnd)t2).getStart());
	}

	public boolean equals(Object arg0) {
		if (!(arg0 instanceof Interval))
			return false;
		Interval to = (Interval)arg0;
		return (start == to.start && end == to.end);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(start) * 31 + Long.hashCode(end);
	}
}
