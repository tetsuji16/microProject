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



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.function.Consumer;


import com.microproject.algorithm.ReverseQuery;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.association.AssociationList;
import com.microproject.field.FieldContext;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.HasAssignmentsImpl;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.Accrual;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.snapshot.DataSnapshot;
/**
 *
 */
public class TaskSnapshot implements DataSnapshot, HasAssignments, Cloneable {
	TaskSchedule currentSchedule;
	HasAssignments hasAssignments = null;
	double fixedCost = 0;
	int fixedCostAccrual = Accrual.Kind.END.code();
	boolean ignoreResourceCalendar = false;


	public long getEarliestAssignmentStart() {
		return hasAssignments.getEarliestAssignmentStart();
	}

	public boolean hasActiveAssignment(long start, long end) {
		return hasAssignments.hasActiveAssignment(start, end);
	}

	/**
	 * @param modified
	 */
	public void updateAssignment(Assignment modified) {
		hasAssignments.updateAssignment(modified);
	}

	/**
	 * @return Returns the taskSchedule.
	 */
	public TaskSchedule getCurrentSchedule() {
		return currentSchedule;
	}
	/**
	 * @param currentSchedule The taskSchedule to set.
	 */
	public void setCurrentSchedule(TaskSchedule currentSchedule) {
		this.currentSchedule = currentSchedule;
	}
	public HasAssignments getHasAssignments() {
		if (hasAssignments == null) // lazy instantiation
			hasAssignments = new HasAssignmentsImpl();
		return hasAssignments;
	}
	/**
	 * 
	 */
	public TaskSnapshot() {
	}


	public TaskSnapshot(Collection details) {
		hasAssignments=new HasAssignmentsImpl(details);
	}


	public Object clone() {
		TaskSnapshot newOne = null;
		try {
			newOne = (TaskSnapshot) super.clone();
			newOne.currentSchedule = (TaskSchedule) currentSchedule.clone();
			newOne.hasAssignments = (HasAssignments) ((HasAssignmentsImpl)hasAssignments).cloneWithSchedule(newOne.currentSchedule);
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("TaskSnapshot should be cloneable", e);
		}

		return newOne;
	}
	public Object deepCloneWithTask(Task task) {
		
		TaskSnapshot newOne = null;
		try {
			newOne = (TaskSnapshot) super.clone();
			newOne.currentSchedule = (TaskSchedule) currentSchedule.cloneWithTask(task);
			newOne.hasAssignments = (HasAssignments) ((HasAssignmentsImpl)hasAssignments).deepCloneWithTask(task);
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("TaskSnapshot should be cloneable", e);
		}

		return newOne;
	}
	/**
	 * @param assignment
	 */
	public void addAssignment(Assignment assignment) {
		getHasAssignments().addAssignment(assignment);
	}

	/**
	 * @param resource
	 * @return
	 */
	public Assignment findAssignment(Resource resource) {
		return getHasAssignments().findAssignment(resource);
	}

	/**
	 * @param task
	 * @return
	 */
	public Assignment findAssignment(Task task) {
		return getHasAssignments().findAssignment(task);
	}

	/**
	 * @param assignment
	 */
	public void removeAssignment(Assignment assignment) {
		getHasAssignments().removeAssignment(assignment);
	}


	public AssociationList getAssignments() {
		return getHasAssignments().getAssignments();
	}

	/**
	 * @return
	 */
	public int getSchedulingType() {
		return getHasAssignments().getSchedulingType();
	}

	/**
	 * @param schedulingType
	 */
	public void setSchedulingType(int schedulingType) {
		getHasAssignments().setSchedulingType(schedulingType);
	}

	/**
	 * @return
	 */
	public boolean isEffortDriven() {
		return getHasAssignments().isEffortDriven();
	}

	/**
	 * @param effortDriven
	 */
	public void setEffortDriven(boolean effortDriven) {
		getHasAssignments().setEffortDriven(effortDriven);
	}


	public void buildReverseQuery(ReverseQuery reverseQuery) {
		getHasAssignments().buildReverseQuery(reverseQuery);
	}


	/**
	 * @param visitor
	 * @return
	 */
	public static Consumer<Object> forAllAssignments(Consumer<Object> visitor) {
		return HasAssignmentsImpl.forAllAssignments(visitor);
	}
	/**
	 * @param visitor
	 * @param mergeWorking
	 */
	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar) {
		hasAssignments.forEachWorkingInterval(visitor, mergeWorking, workCalendar);
	}
	/**
	 * @return
	 */
	public boolean isReadOnlyEffortDriven(FieldContext fieldContext) {
		return hasAssignments.isReadOnlyEffortDriven(fieldContext);
	}

	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double actualCost(long start, long end) {
		return hasAssignments.actualCost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long actualWork(long start, long end) {
		return hasAssignments.actualWork(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long remainingWork(long start, long end) {
		return hasAssignments.remainingWork(start, end);
	}	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double acwp(long start, long end) {
		return hasAssignments.acwp(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bac(long start, long end) {
		return hasAssignments.bac(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bcwp(long start, long end) {
		return hasAssignments.bcwp(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double bcws(long start, long end) {
		return hasAssignments.bcws(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double cost(long start, long end) {
		return hasAssignments.cost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long work(long start, long end) {
		return hasAssignments.work(start, end);
	}
	/**
	 * @param type
	 * @param generator
	 * @param values
	 */
	public void calcDataBetween(Object type, TimeIteratorGenerator generator,
			CalculatedValues values) {
		hasAssignments.calcDataBetween(type, generator, values);
	}
	/**
	 * @return
	 */
	public Collection childrenToRollup() {
		return hasAssignments.childrenToRollup();
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public double baselineCost(long start, long end) {
		return hasAssignments.baselineCost(start, end);
	}
	/**
	 * @param start
	 * @param end
	 * @return
	 */
	public long baselineWork(long start, long end) {
		return hasAssignments.baselineWork(start, end);
	}
	/**
	 * @param workCalendar
	 * @return
	 */
	public long calcActiveAssignmentDuration(WorkCalendar workCalendar) {
		return hasAssignments.calcActiveAssignmentDuration(workCalendar);
	}

	public double fixedCost(long start, long end) {
		return 0;
	}
	public double actualFixedCost(long start, long end) {
		return 0;
	}
	/**
	 * @return Returns the fixedCost.
	 */
	public final double getFixedCost() {
		return fixedCost;
	}
	/**
	 * @param fixedCost The fixedCost to set.
	 */
	public final void setFixedCost(double fixedCost) {
		this.fixedCost = fixedCost;
	}
	/**
	 * @return Returns the fixedCostAccrual.
	 */
	public final int getFixedCostAccrual() {
		return fixedCostAccrual;
	}
	/**
	 * @param fixedCostAccrual The fixedCostAccrual to set.
	 */
	public final void setFixedCostAccrual(int fixedCostAccrual) {
		this.fixedCostAccrual = fixedCostAccrual;
	}
	/**
	 * @return Returns the ignoreResourceCalendar.
	 */
	public final boolean isIgnoreResourceCalendar() {
		return ignoreResourceCalendar;
	}
	/**
	 * @param ignoreResourceCalendar The ignoreResourceCalendar to set.
	 */
	public final void setIgnoreResourceCalendar(boolean ignoreResourceCalendar) {
		this.ignoreResourceCalendar = ignoreResourceCalendar;
	}

	public boolean isLabor() {
		return true;
	}
	public boolean hasLaborAssignment() {
		return hasAssignments.hasLaborAssignment();
	}
	public void invalidateAssignmentCalendars() {
		hasAssignments.invalidateAssignmentCalendars();
	}
	
	
	
	public void serialize(ObjectOutputStream s) throws IOException {
		currentSchedule.serialize(s);
		//s.writeObject(hasAssignments);
		s.writeDouble(fixedCost);
		s.writeInt(fixedCostAccrual);
		s.writeBoolean(ignoreResourceCalendar);
	    s.writeInt(hasAssignments.getSchedulingType());
	    s.writeBoolean(hasAssignments.isEffortDriven());
	}
	
	//call init to complete initialization
	public static TaskSnapshot deserialize(ObjectInputStream s,NormalTask hasAssignments) throws IOException, ClassNotFoundException  {
	    TaskSnapshot t=new TaskSnapshot();
	    TaskSchedule schedule=TaskSchedule.deserialize(s);
	    schedule.setTask(hasAssignments);
	    t.setCurrentSchedule(schedule);
	    t.hasAssignments=new HasAssignmentsImpl();//(HasAssignments)s.readObject();
	    
	    t.setFixedCost(s.readDouble());
	    t.setFixedCostAccrual(s.readInt());
	    t.setIgnoreResourceCalendar(s.readBoolean());
	   
	    if (hasAssignments.getVersion()>=2){
	    	t.hasAssignments.setSchedulingType(s.readInt());
	    	t.hasAssignments.setEffortDriven(s.readBoolean());
	    }
	    return t;
	}

}
