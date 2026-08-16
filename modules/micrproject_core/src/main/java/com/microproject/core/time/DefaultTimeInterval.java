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
public class DefaultTimeInterval implements TimeInterval {
	protected static long EMPTY_START=-1L;
	protected static long EMPTY_END=-1L;
	protected long start=EMPTY_START;
	protected long end=EMPTY_END;
	
	public DefaultTimeInterval(){
		
	}
	public DefaultTimeInterval(long start,long end){
		this.start=start;
		this.end=end;
	}
	
	@Override
	public long getStart() {
		return start;
	}

	@Override
	public void setStart(long start) {
		this.start = start;
	}

	@Override
	public long getEnd() {
		return end;
	}

	@Override
	public void setEnd(long end) {
		this.end = end;
	}

	@Override
	public void union(TimeInterval interval) { 
		if (start > interval.getStart())
			start=interval.getStart();
		if (end < interval.getEnd())
			end=interval.getEnd();
	}

	@Override
	public void inter(TimeInterval interval) {
		if (start < interval.getStart())
			start=interval.getStart();
		if (interval.getEnd() < end)
			end=interval.getEnd();
		if (start > end)
			clear();
	}

	@Override
	public void clear() {
		start=EMPTY_START;
		end=EMPTY_END;
	}

	@Override
	public boolean isEmpty() {
		return start==EMPTY_START || end==EMPTY_END ;
	}

	@Override
	public int compareTo(TimeInterval o) {
		if (isEmpty())
			return -1;
		if (start<o.getStart() ||
				 (start==o.getStart() && end<o.getEnd()))
			return -1;
		if (start==o.getStart() && end==o.getEnd()) 
			return 0;
		else return 1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj==null || ! (obj instanceof DefaultTimeInterval))
			return false;
		DefaultTimeInterval i=(DefaultTimeInterval)obj;
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
