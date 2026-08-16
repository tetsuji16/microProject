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
package com.microproject.interval;

import java.io.Serializable;
import java.util.Comparator;

import com.microproject.pm.time.MutableHasStartAndEnd;
import com.microproject.util.DateTime;
import com.microproject.util.MathUtils;

/**
 *
 */
public class ValueObjectForInterval implements MutableHasStartAndEnd, Comparable,Comparator, Serializable, Cloneable {
	static final long serialVersionUID = 286111222666L;
	protected static long NA_TIME = DateTime.NA_TIME.getTime();
	long start = NA_TIME;
	protected long end = DateTime.getMaxDate().getTime();
	protected ValueObjectForIntervalTable table;
	protected ValueObjectForInterval(ValueObjectForIntervalTable table, long start) {
		this.table = table;
		this.start = start;
	}
	protected boolean isDefault() {
		return (start == NA_TIME);
	}


	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return start;
	}
	/**
	 * @param start The start to set.
	 */
	public void setStart(long start) {
		if (start < NA_TIME) // check weird case if assigning to Jan 1 1970 at midnight
			start = NA_TIME;
		this.start = start;
	}
	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return end;
	}
	/**
	 * @param end The end to set.
	 */
	public void setEnd(long end) {
		this.end = end;
	}


	public boolean isFirst() {
		return start == NA_TIME;
	}
	
	public int compare(Object arg0, Object arg1) {
		return MathUtils.signum(((ValueObjectForInterval)arg0).start - ((ValueObjectForInterval)arg1).start); 
	}
	public boolean equals(Object arg0) {
		if (! (arg0 instanceof ValueObjectForInterval))
			return false;
		return start == ((ValueObjectForInterval)arg0).start;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(start);
	}

	public int compareTo(Object arg0) {
		return compare(this,arg0);
	}
	public Object clone(){ 
		try {
			ValueObjectForInterval v=(ValueObjectForInterval)super.clone();
			//v.table=(ValueObjectForIntervalTable)v.clone();
			return v;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public ValueObjectForIntervalTable getTable() {
		return table;
	}
	public void setTable(ValueObjectForIntervalTable table) {
		this.table = table;
	}

	
}
