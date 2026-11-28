/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.assignment;



import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.association.AssociationList;
import com.microproject.model.calendar.WorkCalendar;
import com.microproject.model.costing.EarnedValueValues;
import com.microproject.model.resource.Resource;
import com.microproject.schedule.SchedulingFields;
import com.microproject.model.task.Task;
/**
 * Contract for a task or other model object that owns an association list of assignments.
 * Assignment queries and time-distributed calculations operate on that same association.
 */
public interface HasAssignments extends HasTimeDistributedData, SchedulingFields, EarnedValueValues {
	/** Returns the association list containing this object's assignments. */
	AssociationList getAssignments();
	/** Adds the assignment to this object's assignment association. */
	void addAssignment(Assignment assignment);
	/** Removes the assignment from this object's assignment association. */
	void removeAssignment(Assignment assignment);
	/** Finds the assignment for a resource, or returns {@code null} when absent. */
	Assignment findAssignment(Resource resource);
	/** Finds the assignment for a task, or returns {@code null} when absent. */
	Assignment findAssignment(Task task);
	/** Updates the assignment entry after one of its values changes. */
	public void updateAssignment(Assignment modified);
	/** Returns the scheduling type used to distribute assignment work. */
	public int getSchedulingType();
	/** Sets the scheduling type used to distribute assignment work. */
	public void setSchedulingType(int schedulingType);
	/** Calculates time-distributed values over the requested interval. */
	public void calcDataBetween(Object type, TimeIteratorGenerator generator, CalculatedValues values);
	/** Calculates assigned time that is active in the supplied work calendar. */
	public long calcActiveAssignmentDuration(WorkCalendar workCalendar);	
	/** Returns whether at least one assignment is a labor assignment. */
	boolean hasLaborAssignment();
	/** Invalidates work calendars cached by the assignments. */
	void invalidateAssignmentCalendars();
	/** Returns whether an assignment has active work between the supplied endpoints. */
	boolean hasActiveAssignment(long start, long end);
	/** Returns the earliest assignment start, or {@link Long#MAX_VALUE} when empty. */
	long getEarliestAssignmentStart();
}
