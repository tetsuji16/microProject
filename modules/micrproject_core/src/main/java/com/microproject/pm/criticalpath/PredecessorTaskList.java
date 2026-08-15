/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.criticalpath;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;

/**
* This class implements a task list in predecessor/parent order.  That is, the successors of any given
* task are guaranteed to be after that task in the list. Also wbs children are after their parents.
*  This ordering is needed for the critical path algorithm.
*/
public class PredecessorTaskList {
	private static final Logger logger = Logger.getLogger(PredecessorTaskList.class.getName());
	private LinkedList<TaskReference> list = new LinkedList<TaskReference>();
	private final AtomicInteger calculationStateCount = new AtomicInteger(0);
	private boolean markerStatus;
	private int numberOfReverseScheduledTasks = 0;
	public static final int CALCULATION_STATUS_STEP = 3;
	private SchedulingAlgorithm schedulingAlgorithm;
	PredecessorTaskList(SchedulingAlgorithm schedulingAlgorithm) {
		this.schedulingAlgorithm = schedulingAlgorithm;
	}
	void removeTask(Task task) {
		if (task.isReverseScheduled())
			numberOfReverseScheduledTasks--;

		Iterator<TaskReference> i = list.iterator();
		TaskReference current;
		// the item may be in the list once or twice.  It may be that it is in twice, but the
		// task is no longer a parent
		while (i.hasNext()) {
			current = i.next();
			if (current.task == task) {
				i.remove();
			}
		}
	}
	ListIterator<TaskReference> reverseIterator() {
		return list.listIterator(list.size());
	}

/**
 * Helper to arrange one task
 * @param task
 */	
	private void arrangeSingleTask(final Task task) {
		task.arrangeTask(list,markerStatus,0);
    	if (task.isReverseScheduled())
			numberOfReverseScheduledTasks++;
	}
	
/**
 * Add a subproject. It will convert the existing task into a parent and add all children
 * @param subproject
 */	
	public void addSubproject(final Task subproject) {
		// remove sentinels 
		TaskReference startSentinel = list.removeFirst();
		TaskReference endSentinel = list.removeLast();

		// mark tasks to be added as not yet treated
		boolean m = !getMarkerStatus();
		subproject.setMarkerStatus(m);
		subproject.markTaskAsNeedingRecalculation();
		for (Task task : ((SubProj)subproject).getSubproject().getTaskList()) {
			task.setMarkerStatus(m);
			task.markTaskAsNeedingRecalculation();

		}
		
		removeTask(subproject); // remove existing one
		arrangeSingleTask(subproject); // add it back - it will become a parent
		// add child tasks
		for (Task task : ((SubProj)subproject).getSubproject().getTaskList())
			arrangeSingleTask(task);
		
		// put back sentinels
		list.addFirst(startSentinel);
		list.addLast(endSentinel);
	}
    /**
     * Insert a task into the list.  Go thru and insert it after its parent
     * The task being inserted is a new task and as such has no preds/succs. Just insert it after its parent
     *  * @param hasDependencies
     */
    void arrangeTask(Task task) {
        if (task.isReverseScheduled())
			numberOfReverseScheduledTasks++;
    	task.setMarkerStatus(markerStatus);
        TaskReference previousTaskReference;
    	Task previousTask;
        // go thru in reverse order inserting after first predecessor or parent encountered
        ListIterator<TaskReference> i = list.listIterator();
        TaskReference taskReference = new TaskReference(task);
        while (i.hasNext()) {
        	previousTaskReference = i.next();
        	previousTask = previousTaskReference.getTask();
            if (task.getWbsParentTask() == previousTask) {
            	i.add(taskReference);
                return;
            }
        }
        i.previous(); // add before end sentinel
        i.add(taskReference);
    }    
	/**
	 * Return a list iterator - delegates to internal list
	 * @return list iterator
	 */
	ListIterator<TaskReference> listIterator() {
		return list.listIterator();
	}
	
	LinkedList<TaskReference> getList(){
		return list;
	}
	
	public void dump() {
		ListIterator<TaskReference> i = list.listIterator();
		while (i.hasNext()) {
			logger.log(Level.FINE, "{0}", i.next());
		}
	}
	
	int getFreshCalculationStateCount() {
		int current, next;
		do {
			current = calculationStateCount.get();
			// round up to next multiple of CALCULATION_STATUS_STEP (3)
			int remainder = current % CALCULATION_STATUS_STEP;
			next = (remainder == 0) ? current : current + (CALCULATION_STATUS_STEP - remainder);
		} while (!calculationStateCount.compareAndSet(current, next));
		return next;
	}
	int getNextCalculationStateCount() {
		return calculationStateCount.incrementAndGet();
	}
	int getCalculationStateCount() {
		return calculationStateCount.get();
	}

	boolean addAll(Collection<? extends Task> tasks) {
		list.clear();
		toggleMarkerStatus();
		for (Task task : tasks) {
			task.arrangeTask(list,markerStatus,0);
	    	if (task.isReverseScheduled())
				numberOfReverseScheduledTasks++;
		}
		return true;
	}
	
	private void setDebugDependencyOrder() {
		int count = 0;
		Iterator<TaskReference> i = list.iterator();
		while (i.hasNext()) {
			TaskReference ref = i.next();
			if (ref.getType() == TaskReference.PARENT_END)
				continue;
			Task task = ref.getTask();
			task.setDebugDependencyOrder(count++);
		}
	}
	
	void rearrangeAll() {
		LinkedList<TaskReference> oldList = list;
		// store off sentinels to put them back later
		TaskReference startSentinel = list.removeFirst();
		TaskReference endSentinel = list.removeLast();
		list = new LinkedList<TaskReference>();
			
		Iterator<TaskReference> i = oldList.iterator();
		toggleMarkerStatus();
		while (i.hasNext()) {
			Task task = i.next().getTask();
			arrangeSingleTask(task);
		}
		list.addFirst(startSentinel);
		list.addLast(endSentinel);
//		setDebugDependencyOrder();
 	}
	
	boolean hasReverseScheduledTasks() {
		return (numberOfReverseScheduledTasks > 0);
	}
	public static final class TaskReference implements Comparable {
		static final int PARENT_BEGIN = -1;
		static final int CHILD = 0;
		static final int PARENT_END = 1;
		
		public TaskReference(Task task) {
			this.task = task;
		}
		Task task;
		int type = CHILD;
		TaskReference opposite = null;
		long calculationStateCount = 0;
		public Task getTask() {
			return task;
		}
		public int compareTo(Object arg0) {
			if (arg0 instanceof Task)
				return (getTask() == arg0 ? 0 : -1);
			return (arg0 == this ? 0 : -1);
		}
		
		public void setParentBegin() {
			type = PARENT_BEGIN;
		}
		public void setParentEnd() {
			type = PARENT_END;
		}
		public String toString() {
			String result = task.toString();
			if (type == PARENT_BEGIN)
				result += " begin";
			else if (type == PARENT_END)
				result += " end";
			return result;
		}
		
		/**
		 * @return Returns the type.
		 */
		public int getType() {
			return type;
		}
	}
	/**
	 * Refresh the Reverse schedule count - called in response to change in constraint type field
	 */
	void recalculateReverseScheduledCount() {
		numberOfReverseScheduledTasks = 0;
		
		Iterator<TaskReference> i = list.iterator();
		while (i.hasNext()) {
			Task task = i.next().getTask();
	    	if (task.isReverseScheduled())
				numberOfReverseScheduledTasks++;
		}
	}
	
	
	/**
	 * @return Returns the markerStatus.
	 */
	public final boolean getMarkerStatus() {
		return markerStatus;
	}
	
	final boolean toggleMarkerStatus() {
		markerStatus = !markerStatus;
		return markerStatus;
	}
	
	// for debugging - finds position(s) in pred list of a task
	public int[] findTaskPosition(Task t) {
		int[] result;
		if (t.isWbsParent())
			result = new int[2];
		else
			result = new int[1];
			
		Iterator<TaskReference> i = list.iterator();
		int resultIndex = 0;
		int pos = 0;
		while (i.hasNext()) {
			Task task = i.next().getTask();
			if (task == t) {
				result[resultIndex++]  = pos;
				if (resultIndex == result.length)
					break;
			}
			pos++;
		}
		return result;
	}
}
