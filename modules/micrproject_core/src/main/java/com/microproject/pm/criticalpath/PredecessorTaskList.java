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
