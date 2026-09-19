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

import java.util.Date;
import java.util.Map;

import com.microproject.pm.assignment.timesheet.AssignmentWorkflowState;
import com.microproject.pm.assignment.timesheet.TimesheetStatus;
import com.microproject.pm.snapshot.Snapshottable;

/**
 *
 */
public class AssignmentData extends SerializedDataObject {
	static final long serialVersionUID = 798773653651L;

    protected TaskData task;
    protected EnterpriseResourceData resource;
    protected int snapshotId=Snapshottable.CURRENT.intValue();
    protected long taskId=-1L;

    protected Date cachedStart = null;
    protected Date cachedEnd = null;
    protected int timesheetStatus = TimesheetStatus.NO_DATA;
    protected Date lastTimesheetUpdate = null;
    protected int workflowState = AssignmentWorkflowState.UNDEFINED;
    protected double percentComplete;
    protected long duration;
    protected transient Map<String, Object> attributes;

    public static final SerializedDataObjectFactory FACTORY=new SerializedDataObjectFactory(){
        public SerializedDataObject createSerializedDataObject(){
            return new AssignmentData();
        }
    };

    public EnterpriseResourceData getResource() {
        return resource;
    }
    public void setResource(EnterpriseResourceData resource) {
        this.resource = resource;
        setResourceId((resource==null)?-1L:resource.getUniqueId());
    }
    public TaskData getTask() {
        return task;
    }
    public void setTask(TaskData task) {
        this.task = task;
        setTaskId(task.getUniqueId());
    }




    public long getTaskId() {
		return taskId;
	}
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}
    public long getResourceId() {
		return getUniqueId();
	}
	public void setResourceId(long resourceId) {
		setUniqueId(resourceId);
	}

	public int getSnapshotId() {
        return snapshotId;
    }
    public void setSnapshotId(int snapshotId) {
        this.snapshotId = snapshotId;
    }

    public int getType(){
        return DataObjectConstants.ASSIGNMENT_TYPE;
    }

    public void emtpy(){
    	super.emtpy();
    	resource=null;
    	task=null;
    	cachedStart = null;
    	cachedEnd = null;
    	timesheetStatus = TimesheetStatus.NO_DATA;
    	lastTimesheetUpdate = null;
    	workflowState = AssignmentWorkflowState.UNDEFINED;
    }


	public boolean equals(Object obj){
		if (!super.equals(obj)) return false;
		if (obj instanceof AssignmentData){
			AssignmentData data=(AssignmentData)obj;
			return data.getTaskId()==getTaskId()&&data.getSnapshotId()==getSnapshotId();
		}else return false;
	}
	@Override
	public int hashCode(){
		// consistent with the uniqueId + taskId + snapshotId equals above (issue #177)
		return Long.hashCode(getUniqueId()) * 31 + Long.hashCode(getTaskId()) * 31 + Integer.hashCode(getSnapshotId());
	}
	public final Date getCachedStart() {
		return cachedStart;
	}
	public final void setCachedStart(Date start) {
		this.cachedStart = start;
	}
	public final Date getCachedEnd() {
		return cachedEnd;
	}
	public final void setCachedEnd(Date end) {
		this.cachedEnd = end;
	}
	public final Date getLastTimesheetUpdate() {
		return lastTimesheetUpdate;
	}
	public final void setLastTimesheetUpdate(Date lastTimesheetUpdate) {
		this.lastTimesheetUpdate = lastTimesheetUpdate;
	}
	public final int getTimesheetStatus() {
		return timesheetStatus;
	}
	public final void setTimesheetStatus(int timesheetStatus) {
		this.timesheetStatus = timesheetStatus;
	}
	public final int getWorkflowState() {
		return workflowState;
	}
	public final void setWorkflowState(int workflowState) {
		this.workflowState = workflowState;
	}

	public final boolean isNoDuration() {
		return cachedEnd.equals(cachedStart);
	}

    public double getPercentComplete() {
		return percentComplete;
	}
	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	public void renumber(IDGenerator idGenerator){
    	super.renumber(idGenerator);
    	setTaskId(idGenerator.getId(getTaskId()));
    }

}
