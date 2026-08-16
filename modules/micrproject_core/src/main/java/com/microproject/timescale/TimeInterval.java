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
package com.microproject.timescale;

import java.text.DateFormat;
import java.util.Date;

import com.microproject.pm.time.HasStartAndEnd;

/**
 *
 */
public class TimeInterval implements HasStartAndEnd {
	protected long start1;
	protected long end1;
	protected String text1;
	protected long start2;
	protected long end2;
	protected String text2;
	
	public String toString(){
		StringBuilder buf = new StringBuilder();
		DateFormat df = DateFormat.getDateTimeInstance();
		
		buf.append("TimeInterval[")
			.append(df.format(new Date(start1)))
			.append(',')
			.append(df.format(new Date(end1)))
			.append(',')
			.append(text1)
			.append(df.format(new Date(start2)))
			.append(',')
			.append(df.format(new Date(end2)))
			.append(',')
			.append(text2)
			.append(']');
		return buf.toString();
	}
	
	/**
	 * @param start1
	 * @param end1
	 * @param text1
	 * @param start2
	 * @param end2
	 * @param text2
	 */
	public TimeInterval(long start1, long end1, String text1, long start2,
			long end2, String text2) {
		super();
		this.start1 = start1;
		this.end1 = end1;
		this.text1 = text1;
		this.start2 = start2;
		this.end2 = end2;
		this.text2 = text2;
	}
	
	/**
	 * @return Returns the end1.
	 */
	public long getEnd1() {
		return end1;
	}
	/**
	 * @return Returns the end2.
	 */
	public long getEnd2() {
		return end2;
	}
	/**
	 * @return Returns the start1.
	 */
	public long getStart1() {
		return start1;
	}
	/**
	 * @return Returns the start2.
	 */
	public long getStart2() {
		return start2;
	}
	/**
	 * @return Returns the text1.
	 */
	public String getText1() {
		return text1;
	}
	/**
	 * @return Returns the text2.
	 */
	public String getText2() {
		return text2;
	}

	public long getStart() {
		return start1;
	}

	public long getEnd() {
		return end1;
	}
}
