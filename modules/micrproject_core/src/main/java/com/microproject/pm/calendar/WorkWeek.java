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
package com.microproject.pm.calendar;

import java.io.Serializable;

import com.microproject.contrib.util.Log;
import com.microproject.contrib.util.LogFactory;
import com.microproject.strings.Messages;

public class WorkWeek implements Cloneable,Serializable {
	static final long serialVersionUID = 2555674567677L;
    public static final int DAYS_IN_WEEK = 7;
	public static final long MS_IN_WEEK = DAYS_IN_WEEK * 24L*60*60*1000;
    WorkDay workDay[] = new WorkDay[DAYS_IN_WEEK];
    static Log log = LogFactory.getLog(WorkWeek.class);

    private static String WEEKDAY_MASK = new String(Messages.getString("Calendar.WeekdayBitMaskFromSundayToSaturday"));
	public Object clone() {
		WorkWeek newOne = new WorkWeek();
		for (int i = 0; i < DAYS_IN_WEEK; i++) {
			if (workDay[i] == null)
				newOne.workDay[i] = null;
			else
				newOne.workDay[i] = (WorkDay) workDay[i].clone();
		}
		return newOne;
	}

	public WorkWeek(WorkDay[] days) {
		this.workDay = (WorkDay[]) days.clone();
        updateWorkingDuration();
	}
	
    public WorkWeek() {
        for (int i = 0; i < DAYS_IN_WEEK; i++)
            workDay[i] = null;
    }
    public WorkDay getWeekDay(int dayNum) {
//    	if (dayNum < 0)
//    		System.out.println("day num is " + dayNum);    	
    	return workDay[dayNum];
    }
    
    WorkWeek intersectWith(WorkWeek other) throws InvalidCalendarIntersectionException {
    	WorkWeek result = new WorkWeek();
        for (int i = 0; i < DAYS_IN_WEEK; i++)
            result.workDay[i] = workDay[i].intersectWith(other.getWeekDay(i));
        result.updateWorkingDuration();
        if (result.getDuration() == 0) // a calendar cannot have no working time for its work week
        	throw new InvalidCalendarIntersectionException();
        return result;
    	
    }
    public void setWeekDay(int dayNum, WorkDay day) {
    	if (day != null)
    		day.initialize();
    	workDay[dayNum] = day;
        updateWorkingDuration();    	
    }
    
    public void setWeekDays(WorkDay day) {
    	for (int i = 0; i < DAYS_IN_WEEK; i++) {
    		if (WEEKDAY_MASK.charAt(i) == '1') {
    			setWeekDay(i,day);
    		}
    	}
        updateWorkingDuration();    	
    }
    public void setWeekends(WorkDay day) {
    	for (int i = 0; i < DAYS_IN_WEEK; i++) {
    		if (WEEKDAY_MASK.charAt(i) == '0') {
    			setWeekDay(i,day);
    		}
    	}
        updateWorkingDuration();    	
    }
    
    public void addDaysFrom(WorkWeek from) {
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
        	if (from.workDay[i] != null)
        		workDay[i] = from.workDay[i];
        	if (workDay[i] != null)
        		workDay[i].initialize(); // calc hours - fixes bug in importing project 2007 files
        }
        updateWorkingDuration();
    }
    
    private long workingDuration = 0;

    void updateWorkingDuration() {
    	workingDuration = 0;
    	for (int i = 0; i < DAYS_IN_WEEK; i++) {
    		if (workDay[i] != null)
    			workingDuration += workDay[i].getDuration();
    	}
    }

    public final long getDuration() {
    	return workingDuration;
    }
}
