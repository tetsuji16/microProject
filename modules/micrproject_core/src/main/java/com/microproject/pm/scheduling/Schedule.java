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

import com.microproject.functor.IntervalConsumer;
import com.microproject.field.FieldContext;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.time.MutableHasStartAndEnd;
/**
 *
 */

public interface Schedule extends TimeSheetSchedule,MutableHasStartAndEnd, Cloneable {
    public static final double INSTANT_COMPLETION = 1E-30D;	
//    long getDurationSpan();
//    void setDurationSpan(long durationSpan);
//    
//    long getDurationActive();
//    void setDurationActive(long durationActive);
 
	long getActualStart();
	void setActualStart(long actualStart);

	long getActualFinish();
	void setActualFinish(long actualFinish);

	long getActualDuration();
	void setActualDuration(long actualDuration);

    long getDuration();
    void setDuration(long duration);
    default void setDuration(long duration, FieldContext fieldContext) {
    	setDuration(duration);
    }
    
    
    long getElapsedDuration();
    


    long getDependencyStart(); // the date on which constraints push this task.   Can be different from start if splitting a started task/assignment
    void setDependencyStart(long dependencyStart);
    
    
    

    long getResume();
    void setResume(long resume);

	long getStop(); 
	void setStop(long stop);
	long getCompletedThrough(); // uses % complete as opposed to stop which is soonest of all assignmetns
	void setCompletedThrough(long completedThrough);
	long getEarliestStop(); // if many assignments, date where the the first stop occurs in any of them
	void clearDuration();
	void moveRemainingToDate(long date);

	void moveInterval(Object eventSource,long start, long end, ScheduleInterval oldInterval, boolean isChild);
	void consumeIntervals(IntervalConsumer consumer);
	
	WorkCalendar getEffectiveWorkCalendar();
	/**
	 * Split a task or assignment by adding dead time between from and to
	 * @param eventSource
	 * @param from
	 * @param to
	 */
	void split(Object eventSource, long from, long to);
	
	boolean isJustModified();
	
	Object backupDetail();
	void restoreDetail(Object eventSource,Object detail,boolean isChild);
	
}
