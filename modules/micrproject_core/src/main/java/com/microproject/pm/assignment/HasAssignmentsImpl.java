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
package com.microproject.pm.assignment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.TruePredicate;

import com.microproject.algorithm.ReverseQuery;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.algorithm.buffer.IntervalCallback;
import com.microproject.algorithm.buffer.NonGroupedCalculatedValues;
import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.field.FieldContext;
import com.microproject.functor.CollectionVisitor;
import com.microproject.options.ScheduleOption;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.pm.time.MutableInterval;

/**
 * Implementation of class which contains assignments
 */
public class HasAssignmentsImpl implements HasAssignments, HasTimeDistributedData, Serializable, Cloneable{
	//private static Log log = LogFactory.getLog(HasAssignmentsImpl.class);
	transient AssociationList assignments;
	private static final double ALMOST_ZERO = 0.00001D;

	int schedulingRule = ScheduleOption.getInstance().getSchedulingRule();
	boolean effortDriven = ScheduleOption.getInstance().isEffortDriven();

	public HasAssignmentsImpl() {
		assignments = new AssociationList();
	}

	public boolean isReadOnlyEffortDriven(FieldContext fieldContext) {
		return getSchedulingType() == SchedulingType.FIXED_WORK;
	}

	/**
	 * Copy constructor: It does a deep copy of assignments
	 * @param from
	 */
	private HasAssignmentsImpl(HasAssignmentsImpl from) {
		this();
		copyAssignments(from.assignments, new AssignmentFactory() {
			public Assignment create(Assignment assignment) {
				return new Assignment(assignment);
			}
		});
	}
	public HasAssignmentsImpl(Collection<?> details) {
		this();
		Iterator<?> i = details.iterator();
		while (i.hasNext()) {
			assignments.add(new Assignment((AssignmentDetail)i.next()));
		}
	}

	/**
	 * @param schedule
	 * @return
	 */
//	public HasAssignments cloneWithSchedule(TaskSchedule currentSchedule) {
//		return cloneWithSchedule(currentSchedule,null);
//	}
//	public HasAssignments cloneWithSchedule(TaskSchedule currentSchedule,Collection details) {
//		HasAssignmentsImpl newOne;
//		if (details==null) newOne= new HasAssignmentsImpl(this);
//		else newOne= new HasAssignmentsImpl(details);
//		newOne.setScheduleForAssignments(currentSchedule);
//		return newOne;
//	}
	public HasAssignments cloneWithSchedule(TaskSchedule currentSchedule) {
		HasAssignmentsImpl newOne= new HasAssignmentsImpl(this);
		newOne.setScheduleForAssignments(currentSchedule);
		return newOne;
	}

	private void setScheduleForAssignments(TaskSchedule currentSchedule) {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment)i.next();
			assignment.setTaskSchedule(currentSchedule);
			assignment.convertToBaselineAssignment(false);
		}
	}

	// Very deep copy of assignments, including task rebinding.
	public HasAssignments deepCloneWithTask(Task task) {
		return (HasAssignmentsImpl) cloneWithTask(task);
	}
	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public Object cloneWithTask(Task task){
			HasAssignmentsImpl clone=(HasAssignmentsImpl)clone();
			clone.assignments=new AssociationList();

			clone.copyAssignments(assignments, new AssignmentFactory() {
				public Assignment create(Assignment assignment) {
					return (Assignment) assignment.cloneWithTask(task);
				}
			});
			return clone;
	}
	public Object cloneWithResource(Resource resource){
		HasAssignmentsImpl clone=(HasAssignmentsImpl)clone();
		clone.assignments=new AssociationList();
		clone.copyAssignments(assignments, new AssignmentFactory() {
			public Assignment create(Assignment assignment) {
				return (Assignment) assignment.cloneWithResource(resource);
			}
		});
		return clone;
}


	public AssociationList getAssignments() {
		return assignments;
	}

	public void addAssignment(Assignment assignment) {
		assignments.add(assignment);
	}

	public void removeAssignment(Assignment assignment) {
		assignments.remove(assignment);
	}

	/**
	 * Finds an assignment given a resource
	 */
	public Assignment findAssignment(Resource resource) {
		return findAssignment(new AssignmentMatcher() {
			public boolean matches(Assignment assignment) {
				return assignment.getResource() == resource;
			}
		});
	}

	/**
	 * Finds an assignment given a task
	 */
	public Assignment findAssignment(Task task) {
		return findAssignment(new AssignmentMatcher() {
			public boolean matches(Assignment assignment) {
				return assignment.getTask() == task;
			}
		});
	}



	public int getSchedulingType() {
		return schedulingRule;
	}

	public void setSchedulingType(int schedulingType) {
		this.schedulingRule = schedulingType;
	}

	public boolean isEffortDriven() {
		return effortDriven;
	}

	public void setEffortDriven(boolean effortDriven) {
		this.effortDriven = effortDriven;
	}

	public void buildReverseQuery(ReverseQuery reverseQuery) {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment) i.next();
			if (assignment.isDefault() && !reverseQuery.isAllowDefaultAssignments())
				continue;
			assignment.buildReverseQuery(reverseQuery);
		}
	}

	public void updateAssignment(Assignment modified) {
		ListIterator<Association> i = assignments.listIterator();
		while (i.hasNext()) {
			Association association = i.next();
			if (!(association instanceof Assignment))
				continue;
			Assignment current = (Assignment) association;
			if (current.getTask() == modified.getTask() && current.getResource() == modified.getResource()) {
				i.set(modified); // replace current with new one
				break;
			}
		}
	}

	public static Consumer<Object> forAllAssignments(Consumer<Object> visitor, Predicate filter) {
		return new CollectionVisitor(visitor,filter) {
			protected final Collection getCollection(Object arg0) {
				return ((HasAssignments)arg0).getAssignments();
			}
		};
	}
	public static Consumer<Object> forAllAssignments(Consumer<Object> visitor) {
		return forAllAssignments(visitor,TruePredicate.INSTANCE);
	}


	public void forEachInterval(Consumer<Object> visitor, Object type, WorkCalendar workCalendar) {
		IntervalVisitorCallback callback = new IntervalVisitorCallback();
		callback.initialize(workCalendar, visitor, true);
		collectIntervals(type, workCalendar, callback);
	}
	public void forEachWorkingInterval(final Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar) {
		IntervalVisitorCallback callback = new IntervalVisitorCallback();
		callback.initialize(workCalendar,visitor,true);
		collectIntervals(ACTUAL_WORK, workCalendar, callback);
/* if the splitting should be at latest bar use this code
		barCallback.finish();
		barCallback.initialize(workCalendar,visitor,false);
*/
		callback.initialize(workCalendar,visitor,true);
		collectIntervals(REMAINING_WORK, workCalendar, callback);
	}

	private void collectIntervals(Object type, WorkCalendar workCalendar, IntervalVisitorCallback callback) {
		NonGroupedCalculatedValues calculatedValues = new NonGroupedCalculatedValues(false,0);
		ListIterator<?> i = assignments.listIterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment)i.next();
			callback.setWorkCalendar(assignment.getEffectiveWorkCalendar());
			assignment.calcDataBetween(type,null,calculatedValues);
		}
		calculatedValues.makeContiguousNonZero(callback,workCalendar);
	}

	private static class IntervalVisitorCallback implements IntervalCallback {
		long barStart = 0;
		WorkCalendar workCalendar;
		MutableInterval interval = new MutableInterval(0,0);
		Consumer<Object> visitor;
		long previousEnd = 0;
		private void executeVisitor(long start, long end) {
			start = Math.max(start,previousEnd); // prevent overlap in case of multiple assignments that do not have same advancement
			if (start > end)
				return;

			interval.setStart(start);
			interval.setEnd(end);

			previousEnd = end;
//System.out.println("bar " + new Date(start) + " " + new Date(end));
			visitor.accept(interval);
			barStart = 0;
		}
		public void setWorkCalendar(WorkCalendar workCalendar) {
			this.workCalendar = workCalendar;

		}
		public void add(int index, long start, long end, double value) {
			if (value <= ALMOST_ZERO) { // because of rounding errors, treat 0 as something very small
				if (workCalendar.compare(end,start,false) == 0)
					return;
				if (barStart > 0) {
					start = workCalendar.adjustInsideCalendar(start,true);
					executeVisitor(barStart,start);
				}
			} else {
				if (barStart == 0) {
//hk					barStart = start;
					barStart = workCalendar.adjustInsideCalendar(start,false);
				}
				if (index == 0) {// last bar, must draw
					end = workCalendar.adjustInsideCalendar(end,true);
					executeVisitor(barStart,end);
//					System.out.println("last bar " + new Date(start) + " " + new Date(end));
				}
			}
		}

		private void initialize(WorkCalendar workCalendar, Consumer<Object> visitor, boolean firstTime) {
			if (firstTime)
				previousEnd = 0;
			barStart = 0;
			this.workCalendar = workCalendar;
			this.visitor = visitor;
		}
	}

	public double acwp(long start, long end) {
		return TimeDistributedDataConsolidator.acwp(start,end,rollupChildren());
	}

	public double bac(long start, long end) {
		return TimeDistributedDataConsolidator.bac(start,end,rollupChildren());
	}

	public double bcwp(long start, long end) {
		return TimeDistributedDataConsolidator.bcwp(start,end,rollupChildren());
	}

	public double bcws(long start, long end) {
		return TimeDistributedDataConsolidator.bcws(start,end,rollupChildren());
	}


	public double cost(long start, long end) {
		return TimeDistributedDataConsolidator.cost(start,end,rollupChildren());
	}

	public double baselineCost(long start, long end) {
		return TimeDistributedDataConsolidator.baselineCost(start,end,rollupChildren());
	}

	public long baselineWork(long start, long end) {
		return TimeDistributedDataConsolidator.baselineWork(start,end,rollupChildren(),true);
	}


	public double actualCost(long start, long end) {
		return TimeDistributedDataConsolidator.actualCost(start,end,rollupChildren());
	}

	public long work(long start, long end) {
		return TimeDistributedDataConsolidator.work(start,end,rollupChildren(),true);
	}

	public long actualWork(long start, long end) {
		return TimeDistributedDataConsolidator.actualWork(start,end,rollupChildren(),true);
	}

	public long remainingWork(long start, long end) {
		return TimeDistributedDataConsolidator.remainingWork(start,end,rollupChildren(),true);
	}

	public void calcDataBetween(Object type, TimeIteratorGenerator generator, CalculatedValues values) {
		Iterator<?> i = getAssignments().iterator();
		while (i.hasNext()) {
			((Assignment)i.next()).calcDataBetween(type,generator,values);
		}
	}

    public static List<Object> extractOppositeList(List<?> list, boolean leftObject) {
		Iterator<?> i = list.iterator();
		ArrayList<Assignment> assignments = new ArrayList<>();
    	while (i.hasNext()) { // go thru tasks or resources
    		Object object = i.next();
			if (! (object instanceof HasAssignments))
				continue;
			HasAssignments hasAssignments = (HasAssignments)object;
			for (Association association : hasAssignments.getAssignments()) {
				if (association instanceof Assignment)
					assignments.add((Assignment) association);
			}
		}
		return AssociationList.extractDistinct(assignments,leftObject);
    }

	public Collection childrenToRollup() {
		return assignments;
	}

	private Collection rollupChildren() {
		return childrenToRollup();
	}



	/**
	 *
	 */
	private class AssignmentDurationSummer implements Consumer<Object> {
		private long sum;
		private WorkCalendar workCalendar;
		AssignmentDurationSummer(WorkCalendar workCalendar) {
			this.workCalendar =workCalendar;
			sum = 0;
		}
		public void accept(Object arg0) {
			HasStartAndEnd interval = (HasStartAndEnd)arg0;
			sum += workCalendar.compare(interval.getEnd(), interval.getStart(),false);
		}
		public long getSum() {
			return sum;
		}
	}

	/**
	 * Compute the sum of active assignment durations.  If there are multiple assignments, then
	 * the calendar time of the union of active periods is used, otherwise, if just one assignment
	 * (which could be the default assignment), use the assignment duration
	 * @param workCalendar
	 * @return
	 */
	public long calcActiveAssignmentDuration(WorkCalendar workCalendar) {
		AssociationList assignments =getAssignments();
		// Most of the time there is just one assignment. If that's the case, use the assignment duration
		if (assignments.size() == 1)
			return ((Assignment)assignments.getFirst()).getDurationMillis();
		AssignmentDurationSummer summer = new AssignmentDurationSummer(workCalendar);
		forEachWorkingInterval(summer,false,workCalendar);
		return summer.getSum();

	}


	private void writeObject(ObjectOutputStream s) throws IOException {
	    s.defaultWriteObject();
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    assignments = new AssociationList();
	}

	public double fixedCost(long start, long end) {
		return 0;
	}
	public double actualFixedCost(long start, long end) {
		return 0;
	}

	public boolean isLabor() {
		return false;
	}

	public boolean hasLaborAssignment() {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			if (((Assignment)i.next()).isLabor())
			return true;
		}
		return false;
	}

	public void invalidateAssignmentCalendars() {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			((Assignment)i.next()).invalidateAssignmentCalendar();
		}
	}

	public boolean hasActiveAssignment(long start, long end) {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment) i.next();
			if (assignment.isActiveBetween(start, end))
				return true;
		}
		return false;
	}

	public long getEarliestAssignmentStart() {
		long result = Long.MAX_VALUE;
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			result = Math.min(result,((Assignment)i.next()).getStart());
		}
		return result;
	}

	private Assignment findAssignment(AssignmentMatcher matcher) {
		Iterator<?> i = assignments.iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment) i.next();
			if (matcher.matches(assignment))
				return assignment;
		}
		return null;
	}

	private void copyAssignments(Collection sourceAssignments, AssignmentFactory factory) {
		Iterator<?> i = sourceAssignments.iterator();
		while (i.hasNext()) {
			assignments.add(factory.create((Assignment) i.next()));
		}
	}

	private interface AssignmentMatcher {
		boolean matches(Assignment assignment);
	}

	private interface AssignmentFactory {
		Assignment create(Assignment assignment);
	}

}
