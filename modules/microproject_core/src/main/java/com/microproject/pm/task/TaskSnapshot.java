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
import com.microproject.datatype.Duration;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.BaselineAssignmentProfile;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.HasAssignmentsImpl;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.costing.Accrual;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.snapshot.DataSnapshot;
import com.microproject.datatype.TimeUnit;
/**
 *
 */
public class TaskSnapshot implements DataSnapshot, HasAssignments, Cloneable {
	TaskSchedule currentSchedule;
	HasAssignments hasAssignments = null;
	double fixedCost = 0;
	int fixedCostAccrual = Accrual.Kind.END.code();
	/** Imported whole-task baseline cost; null means calculate from assignments and fixed cost. */
	transient Double importedBaselineCost;
	transient WorkCalendar fixedCostCalendar;
	transient java.util.List<FixedCostProfile> rolledUpFixedCosts = java.util.List.of();
	boolean ignoreResourceCalendar = false;
	private transient int durationDisplayUnit = TimeUnit.NONE;

	public int getDurationDisplayUnit() {
		return durationDisplayUnit;
	}

	public void setDurationDisplayUnit(int durationDisplayUnit) {
		this.durationDisplayUnit = durationDisplayUnit;
	}

	/** Immutable time-phased fixed-cost contribution captured from a descendant task. */
	public static final class FixedCostProfile {
		private final double cost;
		private final int accrualCode;
		private final long start;
		private final long finish;
		private final long rawDuration;
		private final transient WorkCalendar calendar;

		public FixedCostProfile(double cost, int accrualCode, long start, long finish, long rawDuration,
			WorkCalendar calendar) {
			if (!Double.isFinite(cost)) throw new IllegalArgumentException("Fixed cost must be finite");
			Accrual.Kind.fromCode(accrualCode);
			if (calendar == null) throw new IllegalArgumentException("A fixed-cost profile requires its calendar");
			this.cost = cost;
			this.accrualCode = accrualCode;
			this.start = start;
			this.finish = finish;
			this.rawDuration = rawDuration;
			this.calendar = copyCalendar(calendar);
		}

		public double cost(long intervalStart, long intervalEnd) {
			return FixedCostAccrualCalculator.calculate(Accrual.Kind.fromCodeOrNull(accrualCode), cost,
				start, finish, Duration.millis(rawDuration), calendar, intervalStart, intervalEnd);
		}

		public double fixedCost() { return cost; }
		public int accrualCode() { return accrualCode; }
		public long start() { return start; }
		public long finish() { return finish; }
		public long rawDuration() { return rawDuration; }
		public WorkCalendar calendarCopy() { return copyCalendar(calendar); }
		public FixedCostProfile copy() { return new FixedCostProfile(cost, accrualCode, start, finish, rawDuration, calendar); }
	}


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


	public TaskSnapshot(Collection<?> details) {
		hasAssignments=new HasAssignmentsImpl(details);
	}


	public Object clone() {
		TaskSnapshot newOne = null;
		try {
			newOne = (TaskSnapshot) super.clone();
			newOne.currentSchedule = (TaskSchedule) currentSchedule.clone();
			newOne.hasAssignments = (HasAssignments) ((HasAssignmentsImpl)hasAssignments)
				.cloneSnapshotWithSchedule(newOne.currentSchedule);
			newOne.importedBaselineCost = importedBaselineCost;
			if (fixedCostCalendar != null)
				newOne.fixedCostCalendar = (WorkCalendar) fixedCostCalendar.clone();
			newOne.rolledUpFixedCosts = copyFixedCostProfiles(rolledUpFixedCosts);
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
			newOne.hasAssignments = (HasAssignments) ((HasAssignmentsImpl)hasAssignments)
				.deepCloneSnapshotWithTask(task);
			newOne.importedBaselineCost = importedBaselineCost;
			if (fixedCostCalendar != null)
				newOne.fixedCostCalendar = (WorkCalendar) fixedCostCalendar.clone();
			newOne.rolledUpFixedCosts = copyFixedCostProfiles(rolledUpFixedCosts);
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
	 * Adds one independently identified assignment profile to a saved baseline snapshot.
	 * Summary roll-ups can contain multiple child contributions for the same resource;
	 * those detached profiles must remain separate so their absolute time-phased values
	 * are preserved. Live/current assignment lists continue to enforce one task/resource
	 * association.
	 */
	public void addBaselineRollupAssignment(Task owner, int baselineSlot, Assignment assignment) {
		if (baselineSlot < 0 || baselineSlot >= Snapshottable.CURRENT.intValue())
			throw new IllegalArgumentException("Baseline roll-up assignments require a baseline slot");
		if (owner == null || owner.getSnapshot(Integer.valueOf(baselineSlot)) != this)
			throw new IllegalArgumentException("Baseline roll-up assignments require their attached baseline snapshot");
		if (assignment == null || assignment.isDefault() || !assignment.isBaselineAssignment())
			throw new IllegalArgumentException("A baseline roll-up requires a real baseline assignment");
		if (assignment.getTask() != owner)
			throw new IllegalArgumentException("A baseline roll-up assignment must belong to the snapshot owner");
		for (Object existing : getAssignments()) {
			Assignment current = (Assignment) existing;
			if (current == assignment || current.getUniqueId() == assignment.getUniqueId())
				throw new IllegalArgumentException("A baseline roll-up assignment identity can only be stored once");
		}
		AssociationList assignments = getAssignments();
		assignments.add(assignments.size(), assignment);
	}

	/** Captures the detached profiles in one attached baseline slot. */
	public java.util.List<BaselineAssignmentProfile> captureBaselineProfiles(Task owner, int baselineSlot) {
		validateBaselineSlotOwner(owner, baselineSlot);
		java.util.ArrayList<BaselineAssignmentProfile> profiles = new java.util.ArrayList<>();
		for (Object value : getAssignments()) {
			Assignment assignment = (Assignment) value;
			if (assignment.isDefault()) continue;
			if (assignment.getTask() != owner || !assignment.isBaselineAssignment())
				throw new IllegalStateException("Baseline snapshot contains a non-detached assignment");
			profiles.add(BaselineAssignmentProfile.capture(assignment));
		}
		return java.util.List.copyOf(profiles);
	}

	/** Replaces one baseline slot only after every decoded profile passes validation. */
	public void replaceBaselineRollupAssignments(Task owner, int baselineSlot,
		java.util.Collection<Assignment> replacements) {
		validateBaselineSlotOwner(owner, baselineSlot);
		java.util.ArrayList<Assignment> validated = new java.util.ArrayList<>();
		java.util.HashSet<Long> profileIds = new java.util.HashSet<>();
		for (Assignment assignment : replacements) {
			if (assignment == null || assignment.isDefault() || !assignment.isBaselineAssignment()
					|| assignment.getTask() != owner || !profileIds.add(Long.valueOf(assignment.getUniqueId())))
				throw new IllegalArgumentException("Invalid or duplicate detached baseline assignment");
			validated.add(assignment);
		}
		AssociationList current = getAssignments();
		current.clear();
		for (Assignment assignment : validated) addBaselineRollupAssignment(owner, baselineSlot, assignment);
	}

	private void validateBaselineSlotOwner(Task owner, int baselineSlot) {
		if (baselineSlot < 0 || baselineSlot >= Snapshottable.CURRENT.intValue())
			throw new IllegalArgumentException("Baseline profiles require a baseline slot");
		if (owner == null || owner.getSnapshot(Integer.valueOf(baselineSlot)) != this)
			throw new IllegalArgumentException("Baseline profiles require their attached task snapshot");
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
		if (importedBaselineCost != null && FieldContext.isScalar(start, end))
			return importedBaselineCost.doubleValue();
		double total = hasAssignments.baselineCost(start, end) + baselineFixedCost(start, end);
		for (FixedCostProfile profile : rolledUpFixedCosts) total += profile.cost(start, end);
		return total;
	}

	public Double getImportedBaselineCost() {
		return importedBaselineCost;
	}

	public void setImportedBaselineCost(Double cost) {
		if (cost != null && !Double.isFinite(cost.doubleValue()))
			throw new IllegalArgumentException("Baseline cost must be finite");
		importedBaselineCost = cost;
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
		return baselineFixedCost(start, end);
	}

	private double baselineFixedCost(long start, long end) {
		if (currentSchedule == null) return 0.0D;
		WorkCalendar calendar = fixedCostCalendar;
		if (calendar == null) return 0.0D;
		return FixedCostAccrualCalculator.calculate(Accrual.Kind.fromCodeOrNull(fixedCostAccrual),
			fixedCost, currentSchedule.getStart(), currentSchedule.getFinish(),
			Duration.millis(currentSchedule.getRawDuration()),
			calendar, start, end);
	}

	public WorkCalendar getFixedCostCalendar() { return fixedCostCalendar; }
	public FixedCostProfile captureOwnFixedCostProfile(WorkCalendar fallbackCalendar) {
		if (currentSchedule == null || fixedCost == 0.0D) return null;
		WorkCalendar calendar = fixedCostCalendar == null ? fallbackCalendar : fixedCostCalendar;
		if (calendar == null) return null;
		return new FixedCostProfile(fixedCost, fixedCostAccrual, currentSchedule.getStart(),
			currentSchedule.getFinish(), currentSchedule.getRawDuration(), calendar);
	}
	public java.util.List<FixedCostProfile> getRolledUpFixedCostProfiles() {
		return rolledUpFixedCosts.stream().map(FixedCostProfile::copy).toList();
	}
	public void setRolledUpFixedCostProfiles(java.util.List<FixedCostProfile> profiles) {
		rolledUpFixedCosts = copyFixedCostProfiles(profiles);
	}
	public void setPreparedRolledUpFixedCostProfiles(java.util.List<FixedCostProfile> profiles) {
		rolledUpFixedCosts = profiles == null || profiles.isEmpty() ? java.util.List.of() : java.util.List.copyOf(profiles);
	}
	public void setPreparedFixedCostCalendar(WorkCalendar calendar) {
		fixedCostCalendar = calendar;
	}
	public void setFixedCostCalendar(WorkCalendar calendar) {
		fixedCostCalendar = calendar == null ? null : copyCalendar(calendar);
	}
	public static WorkCalendar copyCalendarForSnapshot(WorkCalendar calendar) {
		return calendar == null ? null : copyCalendar(calendar);
	}

	private static java.util.List<FixedCostProfile> copyFixedCostProfiles(java.util.List<FixedCostProfile> profiles) {
		if (profiles == null || profiles.isEmpty()) return java.util.List.of();
		return profiles.stream().map(FixedCostProfile::copy).toList();
	}

	private static WorkCalendar copyCalendar(WorkCalendar calendar) {
		if (calendar instanceof WorkingCalendar workingCalendar) return workingCalendar.copyForBaselineSnapshot();
		try {
			return (WorkCalendar) calendar.clone();
		} catch (CloneNotSupportedException exception) {
			throw new IllegalArgumentException("Baseline fixed-cost calendar cannot be copied", exception);
		}
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
		if (!Double.isFinite(fixedCost))
			throw new IllegalArgumentException("Fixed cost must be a finite currency amount");
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
