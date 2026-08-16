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
package com.microproject.pm.assignment.timesheet;

import java.util.Collection;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.microproject.datatype.Rate;
import com.microproject.field.CanBeDirty;
import com.microproject.field.FieldContext;
import com.microproject.functor.IntervalConsumer;
import com.microproject.graphic.configuration.HasCssStyle;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentSpecificFields;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.task.HasNotes;
import com.microproject.pm.task.Project;
import com.microproject.util.DateTime;

public class TimesheetAssignment implements Schedule, AssignmentSpecificFields, TimeDistributedFields, UpdatesFromTimesheet, HasCssStyle, CanBeDirty, Comparable, HasNotes {
	Assignment assignment;
	String taskName;
	String projectName;
	long projectUniqueId;
	long taskUniqueId;
	long resourceUniqueId;
	boolean alreadyTimesheet = false;
	Collection parentsNames = null;
	transient boolean dirty = false;
	
	transient String description;  //used when sending data to partner
	private String notes;
	public final boolean isAlreadyTimesheet() {
		return alreadyTimesheet;
	}

	public final void setAlreadyTimesheet(boolean alreadyTimesheet) {
		this.alreadyTimesheet = alreadyTimesheet;
	}

	public final long getResourceUniqueId() {
		return resourceUniqueId;
	}

	public final long getTaskUniqueId() {
		return taskUniqueId;
	}

	public final Assignment getAssignment() {
		return assignment;
	}

	public final void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}

	public TimesheetAssignment(String projectName, String taskName, long projectUniqueId, long taskUniqueId, long resourceUniqueId,Assignment assignment, String notes) {
		this.projectName = projectName;
		this.taskName = taskName;
		this.projectUniqueId = projectUniqueId;
		this.taskUniqueId = taskUniqueId;
		this.resourceUniqueId = resourceUniqueId;
		this.assignment = assignment;
		this.notes = notes;
	}
	
	public long getDuration() {
		return assignment.getDuration();
	}
	public long getEnd() {
		return assignment.getEnd();
	}
	public long getStart() {
		return assignment.getStart();
	}
	public long getActualDuration() {
		return assignment.getActualDuration();
	}
	public long getActualFinish() {
		return assignment.getActualFinish();
	}
	public long getActualStart() {
		return assignment.getActualStart();
	}
	public double getPercentComplete() {
		return assignment.getPercentComplete();
	}
	public long getRemainingDuration() {
		return assignment.getRemainingDuration();
	}
	public long getResume() {
		return assignment.getResume();
	}
	public long getStop() {
		return assignment.getStop();
	}
	public final String getProjectName() {
		return projectName;
	}
	public final void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public final String getTaskName() {
		return taskName;
	}
	public final void setTaskName(String taskName) {
		this.taskName = taskName;
	}
	public TimesheetAssignment(Assignment assignment) {
		this.assignment = assignment;
	}
	public int getWorkContourType() {
		return assignment.getWorkContourType();
	}
	public void setWorkContourType(int workContourType) {
		if (assignment != null) {
			assignment.setWorkContourType(workContourType);
		}
	}
	public long getResourceAvailability() {
		return assignment.getResourceAvailability();
	}
	public String getTaskId() {
		return taskUniqueId+"";
	}
	public String getResourceName() {
		return assignment == null || assignment.getResource() == null ? null : assignment.getResource().getName();
	}
	public String getResourceId() {
		return resourceUniqueId+"";
	}
	public Rate getRate() {
		return assignment.getRate();
	}
	public void setRate(Rate rate) {
		assignment.setRate(rate);
	}
	
	
	public double getCost(FieldContext fieldContext) {
		return assignment.getCost();
	}
	public long getWork(FieldContext fieldContext) {
		return assignment.getWork(fieldContext);
	}
	public void setWork(long work, FieldContext fieldContext) {
		assignment.setWork(work,fieldContext);
	}
	public boolean isReadOnlyWork(FieldContext fieldContext) {
		return assignment != null && assignment.isReadOnlyWork(fieldContext);
	}
	public double getActualCost(FieldContext fieldContext) {
		return assignment.getActualCost(fieldContext);
	}
	public long getActualWork(FieldContext fieldContext) {
		return assignment.getActualWork(fieldContext);
	}
	public void setActualWork(long actualWork, FieldContext fieldContext) {
		assignment.setActualWork(actualWork,fieldContext);

	}
	public boolean isReadOnlyActualWork(FieldContext fieldContext) {
		return assignment != null && assignment.isReadOnlyActualWork(fieldContext);
	}
	public long getRemainingWork(FieldContext fieldContext) {
		return assignment.getRemainingWork(fieldContext);
	}
	public void setRemainingWork(long remainingWork, FieldContext fieldContext) {
		assignment.setRemainingWork(remainingWork,fieldContext);
	}
	public boolean isReadOnlyRemainingWork(FieldContext fieldContext) {
		return assignment != null && assignment.isReadOnlyRemainingWork(fieldContext);
	}
	public double getBaselineCost(int numBaseline, FieldContext fieldContext) {
		return assignment.getBaselineCost(numBaseline,fieldContext);
	}
	public long getBaselineWork(int numBaseline, FieldContext fieldContext) {
		return assignment.getBaselineWork(numBaseline,fieldContext);
	}
	public boolean fieldHideCost(FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideCost(fieldContext);
	}
	public boolean fieldHideWork(FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideWork(fieldContext);
	}
	public boolean fieldHideBaselineCost(int numBaseline, FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideBaselineCost(numBaseline, fieldContext);
	}
	public boolean fieldHideBaselineWork(int numBaseline, FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideBaselineWork(numBaseline, fieldContext);
	}
	public boolean fieldHideActualCost(FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideActualCost(fieldContext);
	}
	public boolean fieldHideActualWork(FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideActualWork(fieldContext);
	}
	public double getFixedCost(FieldContext fieldContext) {
		return assignment.getFixedCost(fieldContext);
	}
	public void setFixedCost(double fixedCost, FieldContext fieldContext) {
		assignment.setFixedCost(fixedCost,fieldContext);
	}
	public boolean isReadOnlyFixedCost(FieldContext fieldContext) {
		return assignment != null && assignment.isReadOnlyFixedCost(fieldContext);
	}
	public double getActualFixedCost(FieldContext fieldContext) {
		return assignment.getActualFixedCost(fieldContext);
	}
	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return assignment != null && assignment.fieldHideActualFixedCost(fieldContext);
	}
	public double getRemainingCost(FieldContext fieldContext) {
		return assignment.getRemainingCost(fieldContext);
	}
	public void setActualStart(long actualStart) {
		assignment.setActualStart(actualStart);
		
	}
	public void setActualFinish(long actualFinish) {
		assignment.setActualFinish(actualFinish);
	}
	public void setActualDuration(long actualDuration) {
		assignment.setActualDuration(actualDuration);
	}
	public void setRemainingDuration(long remainingDuration) {
		assignment.setRemainingDuration(remainingDuration);
	}
	public void setPercentComplete(double percentComplete) {
		assignment.setPercentComplete(percentComplete);
	}
	public void setDuration(long duration) {
		assignment.setDuration(duration);
	}
	public long getElapsedDuration() {
		return assignment.getElapsedDuration();
	}
	public long getDependencyStart() {
		return assignment.getDependencyStart();
	}
	public void setDependencyStart(long dependencyStart) {
		assignment.setDependencyStart(dependencyStart);
	}
	public void setResume(long resume) {
		assignment.setResume(resume);
	}
	public void setStop(long stop) {
		assignment.setStop(stop);
	}
	public void clearDuration() {
		assignment.clearDuration();
	}
	public void moveRemainingToDate(long date) {
		assignment.moveRemainingToDate(date);
	}
	public void moveInterval(Object eventSource, long start, long end, ScheduleInterval oldInterval, boolean isChild) {
		assignment.moveInterval(eventSource,start,end,oldInterval,isChild);
	}
	public void consumeIntervals(IntervalConsumer consumer) {
		assignment.consumeIntervals(consumer);
	}
	public WorkCalendar getEffectiveWorkCalendar() {
		return assignment.getEffectiveWorkCalendar();
	}
	public void split(Object eventSource, long from, long to) {
		assignment.split(eventSource,from,to);
	}
	public boolean isJustModified() {
		return assignment.isJustModified();
	}
	public void setStart(long start) {
		assignment.setStart(start);
	}
	public void setEnd(long end) {
		assignment.setEnd(end);
	}
	public boolean isComplete() {
		return assignment.isComplete();
	}
	public void setComplete(boolean complete) {
		assignment.setComplete(complete);
	}

	public Project getProject() {
		return assignment.getOwningProject();
	}

	public long getTimesheetFinish() {
		return assignment.getTimesheetFinish();
	}

	public long getTimesheetStart() {
		return assignment.getTimesheetStart();
	}

	public long getLastTimesheetUpdate() {
		return assignment.getLastTimesheetUpdate();
	}

	public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
		if (assignment == null) {
			return false;
		}
		if (assignment.getTimesheetStatus() != TimesheetStatus.VALIDATED) {
			return false;
		}
		assignment.setTimesheetStatus(TimesheetStatus.INTEGRATED);
		assignment.setLastTimesheetUpdate(timesheetUpdateDate);
		dirty = false;
		return true;
	}

	public boolean isPendingTimesheetUpdate() {
		return assignment.isPendingTimesheetUpdate();
	}

	public int getTimesheetStatus() {
		return assignment.getTimesheetStatus();
	}

	public String getTimesheetStatusName() {
		return assignment.getTimesheetStatusName();
	}

	public String getCssStyleClass() {
		return getTimesheetStatusName();
	}

	public void setHierarchy(Collection parentsNames) {
		this.parentsNames = parentsNames;
	}

	public Collection getHierarchy() {
		return parentsNames;
	}
	public boolean isIntegratedOrComplete() {
		if (getTimesheetStatus() == TimesheetStatus.INTEGRATED)
			return true;
		if (getTimesheetStatus() == TimesheetStatus.VALIDATED) // validated timesheets must always be shown in dialog till integrated
			return false;
		return isComplete();
	}

	public final boolean isDirty() {
		return dirty;
	}

	public final void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	
	public long getCachedStart() {
		Date d = assignment.getCachedStart();
		if (d == null)
			return 0;
		d =  DateTime.fromGmt(d);
		return d.getTime();
	}

	public long getCachedEnd() {
		Date d = assignment.getCachedEnd();
		if (d == null)
			return 0;
		return DateTime.gmt(d);
	//	return d.getTime();
	}
	
	public int compareTo(Object arg0) {
		return com.microproject.util.MathUtils.signum(getCachedStart() - ((TimesheetAssignment)arg0).getCachedStart());
	}
	
	public long getReadOnlyDuration() {
		return getDuration();
	}

	public final long getEarliestStop() {
		return assignment.getEarliestStop();
	}
	public final long getCompletedThrough() {
		return assignment.getCompletedThrough();
	}

	public void setCompletedThrough(long completedThrough) {
		assignment.setCompletedThrough(completedThrough);
	}

	public final String getDescription() {
		return description;
	}

	public final void setDescription(String description) {
		this.description = description;
	}
	public String toExternalId() { 
		return taskUniqueId + "/" + resourceUniqueId;
	}

	public Object backupDetail() {
		return assignment == null ? null : assignment.backupDetail();
	}

	public void restoreDetail(Object source,Object detail,boolean isChild) {
		if (assignment != null) {
			assignment.restoreDetail(source, detail, isChild);
		}
	}

	public long getProjectUniqueId() {
		return projectUniqueId;
	}

	public void setProjectUniqueId(long projectUniqueId) {
		this.projectUniqueId = projectUniqueId;
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes=notes;
	}
	
	

}
