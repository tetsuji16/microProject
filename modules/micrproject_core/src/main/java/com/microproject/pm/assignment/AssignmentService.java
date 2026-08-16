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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.undo.UndoableEditSupport;

import com.microproject.association.AssociationList;
import com.microproject.configuration.Settings;
import com.microproject.datatype.TimeUnit;
import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.undo.AssignmentCreationEdit;
import com.microproject.undo.AssignmentDeletionEdit;
import com.microproject.undo.NodeUndoInfo;
import com.microproject.undo.ScheduleBackupEdit;
import com.microproject.undo.ScheduleEdit;

/**
 * Manages the creation and deleting of assignments as well as events
 */
public class AssignmentService {
	
	private static AssignmentService instance = null;
	public static AssignmentService getInstance() {
		if (instance == null)
			instance = new AssignmentService();
		return instance;
	}
	
	
	public void newAssignments(Collection tasks, Collection resources, double units, long delay, Object eventSource,boolean undo) {
		if (tasks.isEmpty() || resources.isEmpty()) {
			return;
		}
		Project transactionProject = null;
		ResourcePool transactionResourcePool = null;
		for (Object taskObject : tasks) {
			if (!(taskObject instanceof NormalTask))
				throw new IllegalArgumentException("Assignments require normal tasks");
			Project taskProject = ((NormalTask) taskObject).getProject();
			if (transactionProject == null)
				transactionProject = taskProject;
			else if (transactionProject != taskProject)
				throw new IllegalArgumentException("A batch assignment must belong to one project");
		}
		for (Object resourceObject : resources) {
			if (!(resourceObject instanceof Resource))
				throw new IllegalArgumentException("Assignments require resources");
			ResourcePool resourcePool = resourcePoolOf((Resource) resourceObject);
			if (resourcePool != null) {
				if (transactionResourcePool == null)
					transactionResourcePool = resourcePool;
				else if (transactionResourcePool != resourcePool)
					throw new IllegalArgumentException("A batch assignment must use one resource pool");
			}
		}
		BatchUpdate batchUpdate = new BatchUpdate();
		try {
			for (Object taskObject : tasks) {
				NormalTask task = (NormalTask) taskObject;
				batchUpdate.beginIfNeeded(task, tasks, transactionResourcePool, this, undo);
				batchAssignResources(task, resources, units, delay, eventSource);
			}
		} finally {
			batchUpdate.endIfNeeded();
		}
	}

	private ResourcePool resourcePoolOf(Resource resource) {
		return resource.getDocument() instanceof ResourcePool ? (ResourcePool) resource.getDocument() : null;
	}

	private void batchAssignResources(NormalTask task, Collection resources, double units, long delay, Object eventSource) {
		boolean taskHadNoRealAssignments = !task.hasRealAssignments() || !task.hasLaborAssignment();
		TaskState taskState = preserveTaskState(task, taskHadNoRealAssignments);
		Set<Resource> assignedResources = getAssignedResources(task);
		try {
			for (Object resourceObject : resources) {
				Resource resource = (Resource) resourceObject;
				if (assignedResources.contains(resource)) {
					continue;
				}
				Assignment assignment = newAssignment(task, resource, units, delay, eventSource, true);
				if (assignment != null) {
					if (!resource.isLabor()) {
						assignment.setRateUnit(TimeUnit.NON_TEMPORAL);
					}
					assignedResources.add(resource);
				}
			}
		} finally {
			taskState.restore(task);
		}
	}

	private Set<Resource> getAssignedResources(NormalTask task) {
		Set<Resource> assignedResources = new HashSet<Resource>();
		for (Object existing : task.getAssignments()) {
			assignedResources.add(((Assignment) existing).getResource());
		}
		return assignedResources;
	}

	private TaskState preserveTaskState(NormalTask task, boolean batchWillTouchScheduling) {
		if (!batchWillTouchScheduling) {
			return TaskState.noop();
		}
		TaskState state = new TaskState(task.getSchedulingType(), task.isEffortDriven());
		task.setSchedulingType(SchedulingType.FIXED_DURATION);
		task.setEffortDriven(false);
		return state;
	}

	private static final class BatchUpdate {
		private Project transactionProject;
		private int transactionId;
		private ResourcePool transactionResourcePool;
		private int resourceTransactionId;

		void beginIfNeeded(NormalTask task, Collection tasks, ResourcePool resourcePool, AssignmentService service, boolean undo) {
			if (!undo) {
				return;
			}
			if (transactionId != 0) {
				return;
			}
			transactionProject = task.getProject();
			transactionProject.beginUndoUpdate();
			transactionId = transactionProject.fireMultipleTransaction(0, true);
			transactionResourcePool = resourcePool;
			if (transactionResourcePool != null)
				resourceTransactionId = transactionResourcePool.fireMultipleTransaction(0, true);
			transactionProject.getUndoController().getEditSupport().postEdit(new ScheduleBackupEdit(tasks, service));
		}

		void endIfNeeded() {
			if (transactionId == 0) {
				return;
			}
			if (transactionResourcePool != null)
				transactionResourcePool.fireMultipleTransaction(resourceTransactionId, false);
			transactionProject.fireMultipleTransaction(transactionId, false);
			transactionProject.endUndoUpdate();
		}
	}

	private static final class TaskState {
		private final int schedulingType;
		private final boolean effortDriven;

		private TaskState(int schedulingType, boolean effortDriven) {
			this.schedulingType = schedulingType;
			this.effortDriven = effortDriven;
		}

		static TaskState noop() {
			return new TaskState(-1, false);
		}

		void restore(NormalTask task) {
			if (schedulingType == -1) {
				return;
			}
			task.setSchedulingType(schedulingType);
			task.setEffortDriven(effortDriven);
		}
	}


/**
 * When importing, we don't update or recalculate duration
 * @param task
 * @param resource
 * @param units
 * @param delay
 * @param eventSource
 * @return
 */	
	public Assignment newAssignment(NormalTask task, Resource resource, double units, long delay, Object eventSource,boolean undo) {
		Assignment assignment = Assignment.getInstance(task, resource, units, delay);
		if (!connect(assignment,eventSource,undo))
			return null;
		// NormalTask.addAssignment may copy the default assignment's fields. Apply
		// the caller-provided delay after that normalization so it is not lost.
		if (delay != 0L)
			assignment.setDelay(delay);
		return assignment;
	}
	public Assignment newAssignment(NormalTask task, Resource resource, double units, long delay, Object eventSource) {
		return newAssignment(task,resource,units,delay,eventSource,true);
	}
		
	public boolean connect(Assignment assignment, Object eventSource) {
		return connect(assignment, eventSource,true);
	}
	public boolean connect(Assignment assignment, Object eventSource,boolean undo) {
		if (!connect(assignment,eventSource,new NodeUndoInfo(undo)))
			return false;
//		UndoableEditSupport undoableEditSupport=getUndoableEditSupport(assignment);
//		if (undoableEditSupport!=null&&undo){
//			undoableEditSupport.postEdit(new AssignmentCreationEdit(assignment,eventSource));
//		}
		return true;
	}
	public boolean connect(Node node, Object eventSource, boolean undo) {
		return connect((Assignment)node.getImpl(),eventSource,new NodeUndoInfo(node,undo));
	}
	public boolean connect(Assignment assignment, Object eventSource, NodeUndoInfo undo) {
		if (!assignment.getTask().isAssignable())
			return false;
		((NormalTask)assignment.getTask()).addAssignment(assignment);
		assignment.getResource().addAssignment(assignment);
		if (eventSource != null){
			assignment.getDocument().getObjectEventManager().fireCreateEvent(eventSource,assignment,undo);
			((ResourcePool)assignment.getResource().getDocument()).getObjectEventManager().fireCreateEvent(eventSource,assignment,undo);
		}
		return true;
	}
	public void remove(Node node, Object eventSource,boolean undo) {
		remove((Assignment)node.getImpl(),true,eventSource,new NodeUndoInfo(node,undo));
	}
//	public void remove(Assignment assignment, Object eventSource) {
//		remove(assignment, eventSource,true);
//	}
	public void remove(Assignment assignment, Object eventSource, boolean undo) {
		remove(assignment,true,eventSource,new NodeUndoInfo(undo));
	}
	
	public void remove(Assignment assignment, boolean cleanTaskLink, Object eventSource, boolean undo) {
		remove(assignment,cleanTaskLink,eventSource,new NodeUndoInfo(undo));
//		remove(assignment,(undo)?UNDO:eventSource);
//		UndoableEditSupport undoableEditSupport=getUndoableEditSupport(assignment);
//		if (undoableEditSupport!=null&&undo){
//			undoableEditSupport.postEdit(new AssignmentDeletionEdit(assignment,eventSource));
//		}
	}
	public void remove(Collection assignments, Object eventSource,boolean undo) {
		UndoableEditSupport undoableEditSupport=null;
		
		try {
			for (Iterator<?> i=assignments.iterator();i.hasNext();){
				Assignment assignment=(Assignment)i.next();
//				if (undoableEditSupport==null&&undo){
//					undoableEditSupport=getUndoableEditSupport(assignment);
//					if (undoableEditSupport!=null){
//						undoableEditSupport.beginUpdate();
//					}
//				}
				remove(assignment,true,eventSource,undo);
			}
		} finally{
//			if (undoableEditSupport!=null&&undo){
//				undoableEditSupport.endUpdate();
//			}
		}
	}

	public void remove(Assignment assignment, boolean cleanTaskLink, Object eventSource, NodeUndoInfo undo) {
			NormalTask task=(NormalTask)assignment.getTask();
			Resource resource=assignment.getResource();
			            
			if (task.findAssignment(resource) == null)
				return; // avoids endless loop 9/1/06 hk

			
			if (cleanTaskLink) task.removeAssignment(assignment);
			resource.removeAssignment(assignment);
			
//		//remove assignment snapshots too 18/7/2006 lc
//		//if (resource!=ResourceImpl.getUnassignedInstance())
//        for (int s=0;s<Settings.numBaselines();s++){
//            TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(new Integer(s));
//            if (snapshot==null) continue;
//            AssociationList snapshotAssignments=snapshot.getHasAssignments().getAssignments();
//            if (snapshotAssignments.size()>0){
//                for (Iterator j=snapshotAssignments.iterator();j.hasNext();){
//                    Assignment snapshotAssignment=(Assignment)j.next();
//                    if (snapshotAssignment.getTask()==assignment.getTask()&&snapshotAssignment.getResource()==assignment.getResource())
//                    	j.remove();
//                }
//            }
//            //if (snapshotAssignments.size()==0&&s!=Snapshottable.CURRENT.intValue()) task.setSnapshot(new Integer(s), null);
//        }

			
//			if (eventSource == null){ //case when default assignment is removed 
//				if ((undo==null||(undo!=null&&undo.isUndo()))){
//					UndoableEditSupport undoableEditSupport=getUndoableEditSupport(assignment);
//					if (undoableEditSupport!=null){
//						undoableEditSupport.postEdit(new AssignmentDeletionEdit(assignment));
//					}
//				}
//
//			}else {
			if (eventSource != null){
				if (cleanTaskLink) assignment.getDocument().getObjectEventManager().fireDeleteEvent(eventSource,assignment,undo);
				if (assignment.getResource().getDocument() != null) // it's null if local project
					((ResourcePool)assignment.getResource().getDocument()).getObjectEventManager().fireDeleteEvent(eventSource,assignment);
			}
	}

	public void remove(Collection assignmentList, Object eventSource) {
		Assignment assignment;
		Iterator<?> i = assignmentList.iterator();
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			remove(assignment,true,eventSource,null);
		}
	}
	
	//fix
	public void remove(Collection assignmentList, Collection toRemove) {
		Iterator<?> i = assignmentList.iterator();
		while (i.hasNext())
			toRemove.add(i.next());
	}
	
	
	//undo
	public UndoableEditSupport getUndoableEditSupport(Assignment assignment) {
		if (assignment.getTask()==null) return null;
		else return assignment.getTask().getProject().getUndoController().getEditSupport();
	}
	
}	
	
	
