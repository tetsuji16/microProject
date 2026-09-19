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
package com.microproject.pm.criticalpath;

import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.pm.calendar.WorkCalendar;
public interface ScheduleWindow  {
	
	public static final int INVALID = -1;

    /** @return Returns the lateFinish. */
    public long getLateFinish();

    /** @return Calculates the total float. */
    public long getTotalSlack();
    
    /** @return Calculates the free float. */
    public long getFreeSlack();
    
    /** The amount of excess time an activity has between its Early Start and Late Start dates. */
    public long getStartSlack();
    
    /** The amount of excess time an activity has between its Early Finish and Late Finish dates. */
    public long getFinishSlack();
    


//    public void calcStartAndFinish();

        /** @return Returns the earlyStart. */
    public long getEarlyStart();

    /** @return Returns the lateStart. */
    public long getLateStart();

    /** @return Returns the earlyFinish. */
    public long getEarlyFinish();

    /** @param mustStart The date the task must start on. */
    public void setMustStartOn(long mustStart);

    /** @param mustFinish The date the task must finish on. */
    public void setMustFinishOn(long mustFinish);
    
    /** @return Returns the constraintType. */
    public int getConstraintType();


    /** @param constraintType The constraintType to set. 
     * @throws FieldParseException*/
    public void setConstraintType(int constraintType) throws FieldParseException;

    public void setConstraintDate(long constraintDate);
    public long getConstraintDate();
	public boolean isReadOnlyConstraintDate(FieldContext fieldContext);

//    public long calcSuccessorEarlyStart();
//    public long calcPredecessorLateFinish();
//    public long calcPredecessorLateFinish(Dependency dependency, long duration);
//    public long calcSuccessorEarlyStart(Dependency dependency, long duration);    
    
    /**
     * Set the window values based on a schedule type. 
     * @param constraintType
     * @param date
     */
    public void setScheduleConstraint(int constraintType, long date);
    
    public long getSplitDuration();
    
    public WorkCalendar getEffectiveWorkCalendar();


//	Collection getScheduleChildren();

	/**
	 * @param dependencyDate TODO
	 * @param dependency
	 * @param duration
	 * @return
	 */
//	public long calcStart(Dependency dependency, long duration);

	//cp
	long calcOffsetFrom(long startDate, long dependencyDate, boolean ahead, boolean remainingOnly, boolean useSooner);
	void clearDuration();
	long getRawDuration();
	long getElapsedDuration();
	long getActualStart();
	long getEnd();
	
	long getDeadline();
	void setDeadline(long deadline);
	
    /** @return Returns the windowEarlyStart. */
    public long getWindowEarlyStart();
    /** @param windowEarlyStart The windowEarlyStart to set. */
    public void setWindowEarlyStart(long windowEarlyStart);

    /** @return Returns the windowLateStart. */
    public long getWindowLateStart();
    /** @param windowLateStart The windowLateStart to set. */
    public void setWindowLateStart(long windowLateStart);

    /** @return Returns the windowEarlyFinish. */
    public long getWindowEarlyFinish();
    /** @param windowEarlyFinish The windowEarlyFinish to set. */
    public void setWindowEarlyFinish(long windowEarlyFinish);
  
    /** @return Returns the windowLateFinish. */
    public long getWindowLateFinish();
    /** @param windowLateFinish The windowLateFinish to set. */
    public void setWindowLateFinish(long windowLateFinish);
}
