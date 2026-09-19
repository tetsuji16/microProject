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
package com.microproject.pm.assignment;



import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.association.AssociationList;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.EarnedValueValues;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.scheduling.SchedulingFields;
import com.microproject.pm.task.Task;
/**
 * Interface for assignment container
 */
public interface HasAssignments extends HasTimeDistributedData, SchedulingFields, EarnedValueValues {
	AssociationList getAssignments();
	void addAssignment(Assignment assignment);
	void removeAssignment(Assignment assignment);
	Assignment findAssignment(Resource resource);
	Assignment findAssignment(Task task);
	public void updateAssignment(Assignment modified);
	public int getSchedulingType();
	public void setSchedulingType(int schedulingType);
	public void calcDataBetween(Object type, TimeIteratorGenerator generator, CalculatedValues values);
	public long calcActiveAssignmentDuration(WorkCalendar workCalendar);	
	boolean hasLaborAssignment();
	void invalidateAssignmentCalendars();
	boolean hasActiveAssignment(long start, long end);
	long getEarliestAssignmentStart();
}
