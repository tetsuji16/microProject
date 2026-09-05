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
package com.microproject.server.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 */
public class TaskData extends SerializedDataObject {
	static final long serialVersionUID = 37382828283746L;
	public static final int CREATION_STATUS_NORMAL = 0;
	public static final int CREATION_STATUS_TIMESHEET = 1;

	protected CalendarData calendar;
    protected Collection<AssignmentData> assignments;
    protected Collection<LinkData> predecessors;
    protected TaskData parentTask;
    protected long childPosition;
    protected long parentTaskId=-1L;
    protected long calendarId=-1;
    protected boolean external = false;
    protected long projectId = 0L;
    protected String externalProjectFile;
    protected long subprojectId = 0L; // for subproject tasks, what project they represent
    protected Map<String, Object> subprojectFieldValues = null;
	/** Linked child filename and insertion read-only mode for local master projects. */
	protected String subprojectFile;
	protected boolean subprojectReadOnly;
	/** Master-local stable identity of a linked subproject reference. */
	protected String subprojectReferenceId;
	protected String storedSubprojectPath;
	protected String canonicalSubprojectPath;
	protected String lastKnownSubprojectProjectId;
	protected long lastKnownSubprojectModifiedTime;
    protected boolean timesheetCreated = false;
    protected String notes;
    protected transient Map<String, Object> attributes;

// this code is to set fields which are exposed in database
//    protected long start;
//	  protected long finish;
//    protected long baselineStart;
//    protected long baselineFinish;
//    protected long completedThrough;
//    protected double percentComplete;



    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new TaskData();
        }
    };

    public Collection<AssignmentData> getAssignments() {
        return assignments;
    }
    public void setAssignments(Collection<AssignmentData> assignments) {
        this.assignments = assignments;
    }
    public CalendarData getCalendar() {
        return calendar;
    }
    public void setCalendar(CalendarData calendar) {
        this.calendar = calendar;
        setCalendarId((calendar==null)?-1L:calendar.getUniqueId());
    }

    /*public Collection getPredecessors() {
        return predecessors;
    }
    public void setPredecessors(Collection predecessors) {
        this.predecessors = predecessors;
    }*/
    public Collection<LinkData> getPredecessors() {
        return predecessors;
    }
    public void setPredecessors(Collection<LinkData> predecessors) {
        this.predecessors = predecessors;
    }
	/**
	 * @return Returns the childPosition.
	 */
	public long getChildPosition() {
		return childPosition;
	}
	/**
	 * @param childPosition The childPosition to set.
	 */
	public void setChildPosition(long childPosition) {
		this.childPosition = childPosition;
	}
	/**
	 * @return Returns the parentTask.
	 */
	public TaskData getParentTask() {
		return parentTask;
	}
	/**
	 * @param parentTask The parentTask to set.
	 */
	public void setParentTask(TaskData parentTask) {
		this.parentTask = parentTask;
        setParentTaskId((parentTask==null)?-1L:parentTask.getUniqueId());
	}


    public long getParentTaskId() {
		return parentTaskId;
	}
	public void setParentTaskId(long parentTaskId) {
		this.parentTaskId = parentTaskId;
	}
	public int getType(){
        return DataObjectConstants.TASK_TYPE;
    }
    public long getCalendarId() {
		return calendarId;
	}
	public void setCalendarId(long calendarId) {
		this.calendarId = calendarId;
	}

	public void emtpy(){
    	super.emtpy();
    	calendar=null;
    	assignments=null;
    	predecessors=null;
    	parentTask=null;
    	external = false;
    }

	//syncronizer
	public void addAssignment(AssignmentData assignmentData){
		if (assignments==null) assignments=new ArrayList<>();
		assignmentData.setTask(this);
		assignments.add(assignmentData);
	}
	public void addPredecessor(LinkData linkData){
		if (predecessors==null) predecessors=new ArrayList<>();
		linkData.setSuccessor(this);
		predecessors.add(linkData);
	}
	public final boolean isExternal() {
		return external;
	}
	public final void setExternal(boolean external) {
		this.external = external;
	}
	public final long getProjectId() {
		return projectId;
	}
	public final void setProjectId(long projectId) {
		this.projectId = projectId;
	}
	public final String getExternalProjectFile() {
		return externalProjectFile;
	}
	public final void setExternalProjectFile(String externalProjectFile) {
		this.externalProjectFile = externalProjectFile;
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	public final boolean isSubproject() {
		return subprojectId != 0;
	}
	public final void setSubprojectId(long subprojectId) {
		this.subprojectId = subprojectId;
	}
	public final long getSubprojectId() {
		return subprojectId;
	}
	public final Map<String, Object> getSubprojectFieldValues() {
		return subprojectFieldValues;
	}
	public final void setSubprojectFieldValues(Map<String, Object> subprojectFieldValues) {
		this.subprojectFieldValues = subprojectFieldValues;
	}
	public final String getSubprojectFile() {
		return subprojectFile;
	}
	public final void setSubprojectFile(String subprojectFile) {
		this.subprojectFile = subprojectFile;
	}
	public final boolean isSubprojectReadOnly() {
		return subprojectReadOnly;
	}
	public final void setSubprojectReadOnly(boolean subprojectReadOnly) {
		this.subprojectReadOnly = subprojectReadOnly;
	}
	public final String getSubprojectReferenceId() {
		return subprojectReferenceId;
	}
	public final void setSubprojectReferenceId(String subprojectReferenceId) {
		this.subprojectReferenceId = subprojectReferenceId;
	}
	public final String getStoredSubprojectPath() { return storedSubprojectPath; }
	public final void setStoredSubprojectPath(String value) { storedSubprojectPath = value; }
	public final String getCanonicalSubprojectPath() { return canonicalSubprojectPath; }
	public final void setCanonicalSubprojectPath(String value) { canonicalSubprojectPath = value; }
	public final String getLastKnownSubprojectProjectId() { return lastKnownSubprojectProjectId; }
	public final void setLastKnownSubprojectProjectId(String value) { lastKnownSubprojectProjectId = value; }
	public final long getLastKnownSubprojectModifiedTime() { return lastKnownSubprojectModifiedTime; }
	public final void setLastKnownSubprojectModifiedTime(long value) { lastKnownSubprojectModifiedTime = value; }
	public boolean isTimesheetCreated() {
		return timesheetCreated;
	}
	public void setTimesheetCreated(boolean timesheetCreated) {
		this.timesheetCreated = timesheetCreated;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

// this code is to set fields which are exposed in database
//    public long getStart() {
//		return start;
//	}
//	public void setStart(long start) {
//		this.start = start;
//	}
//	public long getFinish() {
//		return finish;
//	}
//	public void setFinish(long finish) {
//		this.finish = finish;
//	}
//	public long getBaselineStart() {
//		return baselineStart;
//	}
//	public void setBaselineStart(long baselineStart) {
//		this.baselineStart = baselineStart;
//	}
//	public long getBaselineFinish() {
//		return baselineFinish;
//	}
//	public void setBaselineFinish(long baselineFinish) {
//		this.baselineFinish = baselineFinish;
//	}
//	public long getCompletedThrough() {
//		return completedThrough;
//	}
//	public void setCompletedThrough(long completedThrough) {
//		this.completedThrough = completedThrough;
//	}
//	public double getPercentComplete() {
//		return percentComplete;
//	}
//	public void setPercentComplete(double percentComplete) {
//		this.percentComplete = percentComplete;
//	}

}
