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
package com.microproject.pm.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.microproject.association.AssociationList;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.criticalpath.TaskSchedule;

public class TaskSnapshotBackup {
	protected TaskSchedule currentSchedule;
	protected ArrayList<Object> assignmentDetails;
	protected TaskSnapshotBackup(TaskSchedule currentSchedule, ArrayList<Object> assignmentDetails) {
		this.currentSchedule = currentSchedule;
		this.assignmentDetails = assignmentDetails;
	}
	public Collection<Object> getAssignmentDetails() {
		return assignmentDetails;
	}
	public TaskSchedule getCurrentSchedule() {
		return currentSchedule;
	}
	
	public static TaskSnapshotBackup backup(TaskSnapshot snapshot,boolean backupSchedule){
		if (snapshot==null) return null;
		AssociationList assignments=snapshot.getAssignments();
		Iterator i = assignments.iterator();
		Assignment assignment;
		ArrayList<Object> detail = new ArrayList<>(assignments.size());
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			detail.add(assignment.backupDetail());
		}
		TaskSchedule scheduleBackup = snapshot.getCurrentSchedule();
		if (scheduleBackup != null) {
			scheduleBackup = (TaskSchedule) scheduleBackup.clone();
		}
		return new TaskSnapshotBackup(scheduleBackup,detail);
	}
	public static void restore(TaskSnapshot snapshot,TaskSnapshotBackup backup){
		if (backup==null||snapshot==null) return;
		if (backup.getAssignmentDetails()!=null) snapshot.setCurrentSchedule(backup.getCurrentSchedule());
		AssociationList assignments=snapshot.getAssignments();
		Iterator i = assignments.iterator();
		Assignment assignment;
		Iterator<?> j=backup.getAssignmentDetails().iterator();
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.restoreDetail(j.next());
		}
	}
}
