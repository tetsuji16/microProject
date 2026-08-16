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

import java.util.Calendar;

import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.util.DateTime;

/**
 *
 */
public class TimeIterator implements HasStartAndEnd {
	//protected int calendarField=Calendar.DAY_OF_WEEK;
	//protected int calendarIncrement=1;
	protected long startTime;
	protected long endTime;
	protected Calendar calendar1;
	protected Calendar calendar2;
	protected TimeScale scale;
	private boolean hasNext=true;
	private long next2=-1;
	private boolean useLargeScale;
	/**
	 * @param startTime
	 * @param endTime
	 */
	public TimeIterator(long startTime, long endTime,TimeScale scale,long startReference) {
		this(startTime,endTime,scale,startReference,false);
	}
	public TimeIterator(long startTime, long endTime,TimeScale scale,long startReference,boolean useLargeScale) {
		//System.out.println("TimeIterator: "+CalendarUtil.toString(startTime)+","+CalendarUtil.toString(endTime)+","+CalendarUtil.toString(startReference));
		this.useLargeScale=useLargeScale;
		long s;
		long e;
		if (startTime<=endTime){
			s=startTime;
			e=endTime;
		}else{
			//this case appears with clip rectangles
			e=startTime;
			s=endTime;
		}
		long startRef=startReference;
		this.startTime = s;
		this.endTime = (s==e)?(e+1):e;
		this.scale=scale;
		
		if (useLargeScale){
			calendar2=DateTime.calendarInstance();
			
			calendar2=DateTime.calendarInstance();
			calendar2.setTimeInMillis(s);
			if (startRef==-1) scale.floor2(calendar2);
			else{
				scale.floor2(calendar2,startRef);
			}
			
			this.startTime=calendar2.getTimeInMillis();
		}else{
			calendar1=DateTime.calendarInstance();
			
			calendar1=DateTime.calendarInstance();
			calendar1.setTimeInMillis(s);
			if (startRef==-1) scale.floor1(calendar1);
			else{
				scale.floor1(calendar1,startRef);
			}

			calendar2=DateTime.calendarInstance();
			calendar2.setTimeInMillis(s);
			
			scale.floor2(calendar2,startRef);
			
			this.startTime=calendar1.getTimeInMillis();
		}
	}
	public TimeIterator(double startTime, double endTime,TimeScale scale,long startReference) {
		this(startTime,endTime,scale,startReference,false);
	}
	public TimeIterator(double startTime, double endTime,TimeScale scale,long startReference, boolean useLargeScale) {
		this(CalendarUtil.toLongTime(startTime),CalendarUtil.toLongTime(endTime),scale,startReference,useLargeScale);
	}
	public boolean hasNext(){
		return hasNext;
	}
	public TimeInterval next(){
		if (!hasNext) return null;
		if (useLargeScale){
			long begin2=calendar2.getTimeInMillis();
			scale.increment2(calendar2);
			long end2=calendar2.getTimeInMillis();
			if (end2>=endTime) hasNext=false;
			//System.out.println("large begin2="+CalendarUtil.toString(begin2));
			String text2=scale.getText2(begin2);
			
			return new TimeInterval(begin2,end2,text2,-1L,-1L,null);
		}else{
			long begin1=calendar1.getTimeInMillis();
			scale.increment1(calendar1);
			long end1=calendar1.getTimeInMillis();
			if (end1>=endTime) hasNext=false;
			//System.out.println("begin1="+CalendarUtil.toString(begin1));
			String text1=scale.getText1(begin1);
			
			long begin2=-1;
			long end2=-1;
			String text2=null;
			if (next2==-1||begin1>=next2){
				begin2=calendar2.getTimeInMillis();
				scale.increment2(calendar2);
				end2=calendar2.getTimeInMillis();
				next2=end2;
				//System.out.println("begin2="+CalendarUtil.toString(begin2));
				text2=scale.getText2(begin2);
			}
			return new TimeInterval(begin1,end1,text1,begin2,end2,text2);
			
		}
	}
	
	
	
	/**
	 * @return Returns the endTime.
	 */
	public long getEnd() {
		return endTime;
	}
	/**
	 * @return Returns the startTime.
	 */
	public long getStart() {
		return startTime;
	}
}
