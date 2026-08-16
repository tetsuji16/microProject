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

import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.functor.IntervalConsumer;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.Project;
import com.microproject.undo.TaskConstraintEdit;
import com.microproject.undo.FieldEdit;
import com.microproject.undo.ScheduleEdit;
import com.microproject.undo.SplitEdit;
import com.microproject.util.ClassUtils;
import com.microproject.util.DateTime;

/**
 * Singleton service for manipulating a schedule, such as by gantt chart modifications
 */
public class ScheduleService {
	private boolean consuming = false;

	private static Field completedFieldInstance = null;
	public static Field getCompletedField() {
		if (completedFieldInstance == null)
			completedFieldInstance = Configuration.getFieldFromId("Field.stop");
		return completedFieldInstance;
	}

	private static ScheduleService instance = null;

	/**
	 * @return Returns the singleton instance.
	 */
	public static ScheduleService getInstance() {
		if (instance == null)
			instance = new ScheduleService();
		return instance;
	}
	
	
	/**
	 * Private constructor 
	 */
	private ScheduleService() {
		super();
	}
	
	public long getCompleted(Schedule schedule) {
		// this is used for drawing completion on the gantt also. see GanttUI
		return schedule.getCompletedThrough();
	}
	
	public boolean setCompleted(Object eventSource, Schedule schedule, long completed,UndoableEditSupport undoableEditSupport) {
		if (isReadOnly(schedule))
			return false;
		Field completedField=getCompletedField();
		Object oldValue=completedField.getValue(schedule);
		if (oldValue==null) oldValue=Long.valueOf(schedule.getActualStart());
		Object value=Long.valueOf(completed);
		if (value.equals(oldValue)) {
			return false;
		}
		completedField.setValue(schedule,eventSource,value);
		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
			undoableEditSupport.postEdit(new FieldEdit(completedField,schedule,value,oldValue,eventSource,null));
		}
		return true;
	}

	public boolean setConstraint(Object eventSource, Task task, int constraintType, long constraintDate, UndoableEditSupport undoableEditSupport) {
		if (task == null || isReadOnly(task)) {
			return false;
		}

		int oldConstraintType = task.getConstraintType();
		long oldConstraintDate = task.getConstraintDate();
		if (oldConstraintType == constraintType && oldConstraintDate == constraintDate) {
			return false;
		}

		task.setScheduleConstraintAndUpdate(constraintType, constraintDate);
		if (undoableEditSupport != null && !(eventSource instanceof UndoableEdit)) {
			undoableEditSupport.postEdit(new TaskConstraintEdit(task, oldConstraintType, oldConstraintDate, constraintType, constraintDate, eventSource));
		}
		return true;
	}
	
	public static boolean isReadOnly(Schedule schedule){
		return ClassUtils.isObjectReadOnly(schedule);
	}
	
	/**
	 * Set the start or the end of the schedule and fire field event which will cause the critical path to run.  The method
	 * checks to see which of the two - start or end, was modified and only updates the modified one
	 * @param eventSource - the object which is the event source, such as GanttModel
	 * @param schedule - the task or assignment
	 * @param start - start date millis
	 * @param end - end date millis	 * 
	 * @param oldStart is the prior start for the bar.  It will be used to identify what bar changed
	 */
	public boolean setInterval(Object eventSource, Schedule schedule, long start, long end, ScheduleInterval interval,UndoableEditSupport undoableEditSupport) {
		if (isReadOnly(schedule))
			return false;
		Object detailBackup=null;
		start = DateTime.hourFloor(start);
		end = DateTime.hourFloor(end);
		if (interval.getStart() == start && interval.getEnd() == end) // if no move do nothing
			return false;
		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
			detailBackup=schedule.backupDetail();
		}
		
		schedule.moveInterval(eventSource,start,end,interval, false);
		//Undo
		if (detailBackup!=null){
			undoableEditSupport.postEdit(new ScheduleEdit(schedule,detailBackup,start,end,interval,false,eventSource));
		}
		return true;

	}

	/**
	 * Split a task/assignment by adding a nonworking interval.  If there is actual work during the split,
	 * only the nonworking part will be moved.  Unlike other products, we don't let you move actuals.
	 * @param eventSource- the object which is the event source, such as GanttModel
	 * @param schedule - the task or assignment
	 * @param from - beginning of nonwork interval
	 * @param to - end of nonwork interval
	 */
	public boolean split(Object eventSource, Schedule schedule, long from, long to,UndoableEditSupport undoableEditSupport) {
		if (isReadOnly(schedule))
			return false;
		Object detailBackup=null;
		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
			detailBackup=schedule.backupDetail();
		}
		schedule.split(eventSource,DateTime.hourFloor(from),DateTime.hourFloor(to));
		//Undo
		if (detailBackup!=null&&didSplitChangeSchedule(schedule, detailBackup)){
			undoableEditSupport.postEdit(new SplitEdit(schedule,detailBackup,from,to,eventSource));
		}
		return true;
	}

	private boolean didSplitChangeSchedule(Schedule schedule, Object detailBackup) {
		if (!(schedule instanceof Project) || !(detailBackup instanceof Project.ProjectBackup)) {
			return true;
		}
		Project project = (Project) schedule;
		Project.ProjectBackup backup = (Project.ProjectBackup) detailBackup;
		return project.getStart() != backup.getStart() || project.getEnd() != backup.getEnd();
	}
	
	/**
	 * Calls back the consumer for each interval in the schedule.  Currently in only treats splits due to
	 * stop/resume. In the future it will also call back for splits in the work contour itself
	 * @param schedule
	 * @param consumer
	 */
	public void consumeIntervals(Schedule schedule, IntervalConsumer consumer) {
		if (consuming)
			return;
		consuming = true;
		try {
			schedule.consumeIntervals(consumer);
		} finally {
			consuming = false;
		}
	}
}
