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
package com.microproject.pm.task;

import com.microproject.util.DataUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;


import com.microproject.algorithm.ReverseQuery;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.association.AssociationFormatParameters;
import com.microproject.association.AssociationList;
import com.microproject.association.AssociationListFormat;
import com.microproject.configuration.Configuration;
import com.microproject.configuration.Settings;
import com.microproject.datatype.Duration;
import com.microproject.datatype.ImageLink;
import com.microproject.datatype.TimeUnit;
import com.microproject.document.Document;
import com.microproject.field.CustomFieldsImpl;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.functor.IntervalConsumer;
import com.microproject.functor.NumberClosure;
import com.microproject.functor.ObjectVisitor;
import com.microproject.graphic.configuration.HasIndicators;
import com.microproject.graphic.configuration.HasTaskIndicators;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.summaries.DeepChildWalker;
import com.microproject.options.CalculationOption;
import com.microproject.options.CalendarOption;
import com.microproject.options.ScheduleOption;
import com.microproject.pm.assignment.Allocation;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentFormat;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.assignment.timesheet.TimesheetHelper;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.costing.Accrual;
import com.microproject.pm.costing.EarnedValueCalculator;
import com.microproject.pm.costing.EarnedValueFields;
import com.microproject.pm.costing.EarnedValueValues;
import com.microproject.pm.criticalpath.TaskSchedule;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.key.HasKeyImpl;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.scheduling.BarClosure;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleUtil;
import com.microproject.pm.scheduling.SchedulingFields;
import com.microproject.pm.scheduling.SchedulingRule;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.pm.snapshot.BaselineScheduleFields;
import com.microproject.pm.snapshot.DataSnapshot;
import com.microproject.pm.snapshot.SnapshottableImpl;
import com.microproject.server.access.ErrorLogger;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;
import com.microproject.util.DisplayMath;

/**
 * @stereotype thing
 */
public class NormalTask extends Task implements Allocation, TaskSpecificFields,
		SchedulingFields, HasAssignments, EarnedValueValues,
		EarnedValueFields, TimeDistributedFields, BaselineScheduleFields,
		HasTaskIndicators {
	private static final Logger logger = Logger.getLogger(NormalTask.class.getName());
	static final long serialVersionUID = 273898992929L;


//	Schedule schedule = null;


	boolean estimated = true;
	int priority = 500;
	private double percentWorkCompleteOverride = Double.NaN;
	public NormalTask(Project project) {
		this(project == null || project.isLocal(),project);
	}
	public NormalTask(boolean local,Project project) {
		super(local);
		this.project = project;
		if (project != null) {
			initializeDates();
			addDefaultAssignment();
		}

	}
	public NormalTask() {
		super();
	}

	/**
	 * Used when creating a task to set initial date and duration conditions
	 *
	 */
	void initializeDates() {
		setRawConstraintType(project == null ? ConstraintType.ASAP : project.getDefaultConstraintType());
		long duration = CalendarOption.getInstance().getDefaultDuration(); //MS uses 1 day estimated
		setRawDuration(duration);
		setWorkCalendar(null);

		// initialize start and end to avoid 0 dates in calculations
		long start = project.getStart();
		currentSchedule.setStart(start);
		currentSchedule.setFinish(start);

		if (ScheduleOption.getInstance().isNewTasksStartToday())
			setWindowEarlyStart(CalendarOption.getInstance()
					.makeValidStart(DateTime.midnightToday(), true));
	}

	/**
	 * This constructor is used to create dummy tasks, such as the UNASSIGNED
	 * instance. We do not want to perform standard initialization on it.
	 *
	 * @param dummy
	 */
	private NormalTask(boolean dummy) {
		super(true);
	}

	private static NormalTask UNASSIGNED = null;

	public static NormalTask getUnassignedInstance() {
		if (UNASSIGNED == null) {
			UNASSIGNED = new NormalTask(true);
			UNASSIGNED.setName(Messages.getString("Text.Unassigned"));
		}
		return UNASSIGNED;
	}

	private Assignment newDefaultAssignment() {
		return Assignment.getInstance(this, ResourceImpl
				.getUnassignedInstance(), 1.0, 0);
	}
	public boolean isNormal() {
		return !isSummary() && !isMilestone() && !isExternal();
	}


	public boolean isCritical() {
		if (isComplete() || isInactiveTask())
			return false;

		int constraintType = getConstraintType();
		if (constraintType == ConstraintType.MSO || constraintType == ConstraintType.MFO)
			return true;
		if (currentSchedule.isForward() && constraintType == ConstraintType.ALAP)
			return true;
		if (!currentSchedule.isForward() && constraintType == ConstraintType.ASAP)
			return true;
		if (getDeadline() != 0L && getEnd() >= getDeadline())
			return true;

		return getTotalSlack() <= CalculationOption.getInstance().getCriticalSlackThreshold();
	}

	public boolean isMilestone() {
		return Duration.millis(getRawDuration()) == 0 || isMarkTaskAsMilestone();
	}
	public double getPercentComplete() {
		if (isZeroDuration()) { // special case for completion on milestones
			int count = 0;
			double pc = 0;
			Assignment ass;
			Iterator i =getAssignments().iterator();
			while (i.hasNext()) {
				ass = ((Assignment)i.next());
				pc += ass.getPercentComplete();
				count++;
			}
			if (count == 0) // shouldn't happen
				return 0;
			return pc / count;
		} else {
			return super.getPercentComplete();
		}
	}

/****************************************************************************************
 * Schedule
 ****************************************************************************************/
	/**
	 * @return
	 */
	public long getDuration() {
		
		long duration;
		if (isWbsParent() || isExternal() || isSubproject()) {
			long raw = getRawDuration();
			if (raw >=0)
				duration = Duration.millis(raw);
			else {
				project.addRepaired(this);
				ErrorLogger.logOnce("raw parent", "repaired bad raw duration" + this,null);
				duration = 0;
			}
		} else {
			AssociationList assignments =getAssignments();
			if (!hasRealAssignments()) {
				duration = Duration.millis(getRawDuration());
			} else if (assignments.size() == 1) {
				duration = ((Assignment)assignments.getFirst()).getDurationMillis();
			} else {
				Iterator i = assignments.iterator();
				long end = 0;
				// get the latest ending assignment
				while (i.hasNext()) {
					end = Math.max(end,((Assignment)i.next()).getEnd());
				}
				// duration is calendar time between assignment end and task start
				duration = getEffectiveWorkCalendar().compare(end,getStart(),false);
			}
		}
		duration = Duration.setAsEstimated(duration,estimated);
		return duration;

//		return calcActiveAssignmentDuration(getEffectiveWorkCalendar());
	}

	/** Quickly check to see if a task has a duration without actually calculating it
	 *
	 * @return true if duration > 0
	 */
	public boolean hasDuration() {
		if (isWbsParent()) {
			return getRawDuration() != 0;
		} else {
			AssociationList assignments =getAssignments();
			if (assignments.size() == 1)
				return ((Assignment)assignments.getFirst()).hasDuration();
			Iterator i = assignments.iterator();
			while (i.hasNext()) {
				if (((Assignment)i.next()).hasDuration())
					return true;
			}
		}
		return false;

	}




	/**
	 * @param duration
	 */
	public void setDuration(long duration) {
		estimated = Duration.isEstimated(duration);
		duration = Duration.millis(duration);
		if (isWbsParent()) {
			setRawDuration(duration);
			markTaskAsNeedingRecalculation();
			return;
		}
		long actualDurationMillis = Duration.millis(getActualDuration());
		setRawDuration(duration); // set the schedule duration, primariy for use when reading a file
		if (duration < actualDurationMillis) // if reducing duration to shorter than the current actual duration
			setPercentComplete(1);
		long remainingDuration = duration - actualDurationMillis;
		getSchedulingRule().adjustRemainingDuration(this, remainingDuration, true);
		applyDurationToCurrentDates(duration);
		if (isInitialized())
			markAllDependentTasksAsNeedingRecalculation(true);
	}
	public void setDuration(long duration, FieldContext fieldContext) {
		if (FieldContext.isTaskSheetUpdate(fieldContext)) {
			TaskSheetScheduleWorkflow.applyDuration(this, duration);
			return;
		}
		setDuration(duration);
	}

	private void applyDurationToCurrentDates(long duration) {
		if (duration == 0) {
			if (isFinishAnchored())
				getCurrentSchedule().setStart(getEnd());
			else
				getCurrentSchedule().setEnd(getStart());
			return;
		}
		if (isFinishAnchored()) {
			long start = calculateStartFromFinish(duration);
			getCurrentSchedule().setStart(start);
		} else {
			long end = getEffectiveWorkCalendar().add(getStart(), duration, false);
			getCurrentSchedule().setEnd(end);
		}
	}

	private long calculateStartFromFinish(long duration) {
		WorkCalendar calendar = getEffectiveWorkCalendar();
		long finish = getEnd();
		long start = calendar.add(finish, -duration, true);
		for (int i = 0; i < 5; i++) {
			long scheduledDuration = calendar.compare(finish, start, false);
			long difference = scheduledDuration - duration;
			if (difference == 0)
				break;
			start += difference;
		}
		return start;
	}

	private boolean isFinishAnchored() {
		int constraintType = getConstraintType();
		return !getCurrentSchedule().isForward()
			|| constraintType == ConstraintType.ALAP
			|| constraintType == ConstraintType.MFO
			|| constraintType == ConstraintType.FNET
			|| constraintType == ConstraintType.FNLT;
	}

/********************************************************************************
 * Calendars
 ***********************************************************************************/

	private WorkCalendar workCalendar = null;

	/**
	 * @return
	 */
	public WorkCalendar getWorkCalendar() {
		return workCalendar;
	}

	public WorkCalendar getEffectiveWorkCalendar() {
		if (workCalendar == null) {
			if (getProject() == null) {
				logger.warning("------No project in getting calendar for task " + getUniqueId() + " " + getName());
				return CalendarService.getInstance().getDefaultInstance();
			}
			return getProject().getEffectiveWorkCalendar();
		}
		return workCalendar;
	}

	/**
	 * @param workCalendar
	 */
	public void setWorkCalendar(WorkCalendar workCalendar) {
		this.workCalendar = workCalendar;
	}


	/**
	 * @return
	 */
	public DataSnapshot getCurrentSnapshot() {
		return snapshots.getCurrentSnapshot();
	}

	/**
	 * @param i
	 * @return
	 */
	public DataSnapshot getSnapshot(Object snapshotId) {
		return snapshots.getSnapshot(snapshotId);
	}

	/**
	 * @param i
	 */
	public void saveCurrentToSnapshot(Object snapshotId) {
		setSnapshot(snapshotId, cloneSnapshot(getSnapshot(CURRENT)));
		markTaskAsNeedingRecalculation(); // for redraw purpooses, not for recalc.
		setDirty(true);
	}
	public void restoreSnapshot(Object snapshotId,Object b) {
		TaskBackup backup=(TaskBackup)b;
		if (backup.snapshot==null) return;
		TaskSnapshot snapshot=(TaskSnapshot)((TaskSnapshot) getSnapshot(CURRENT)).clone();
		//snapshot.setCurrentSchedule(getCurrentSchedule());
		restoreDetail(this, backup, true,snapshot);
		setSnapshot(snapshotId, snapshot);
		markTaskAsNeedingRecalculation(); // for redraw purpooses, not for recalc.
		setDirty(true);
	}

	/**
	 * @param snapshot
	 */
	public void setCurrentSnapshot(DataSnapshot snapshot) {

		snapshots.setCurrentSnapshot(snapshot);
	}

	/**
	 * @param i
	 * @param snapshot
	 */
	public void setSnapshot(Object snapshotId, DataSnapshot snapshot) {
		snapshots.setSnapshot(snapshotId, snapshot);
	}

	/**
	 * @param i
	 */
	public void clearSnapshot(Object snapshotId) {
		snapshots.clearSnapshot(snapshotId);
		markTaskAsNeedingRecalculation(); // for redraw purpooses, not for recalc.
		setDirty(true);
	}

	public boolean hasRealAssignments() {
		return (null == findAssignment(ResourceImpl.getUnassignedInstance()));
	}
	/**
	 * @return
	 */
	public AssociationList getAssignments() {
		return ((TaskSnapshot) getCurrentSnapshot()).getAssignments();
	}

	public AssociationList getRealAssignments() {
		if (hasRealAssignments())
			return getAssignments();
		else
			return new AssociationList(); //empty list
	}

	public boolean isAssignedToMe(){
		for (Iterator i=getAssignments().iterator();i.hasNext();){
			Assignment a=(Assignment)i.next();
			if (a.isMine()) return true;
		}
		return false;
	}

	/**
	 * Add an assignment to the task.  A task always has at least one assignment, whether or not
	 * it has any true assignments.  This is because a default assignment is always present.  This
	 * greatly facilitates other calculations.  This method takes care to either create or delete
	 * the default assignment.
	 *
	 * @param assignment
	 */
	public Assignment addDefaultAssignment(){
		Assignment ass = newDefaultAssignment();
		addAssignment(ass);
		return ass;
	}
	public void addAssignment(Assignment assignment) {
		//project.beginUndoUpdate();
		boolean recalculateDuration = !assignment.isDefault()
				&& assignment.isInitialized() && assignment.isLabor();
		Assignment defaultAssignment = findAssignment(ResourceImpl
				.getUnassignedInstance());

		if (!assignment.isDefault()) {
			// get rid of any default
			if (defaultAssignment != null ) { //Remove any default assignment
 				assignment.usePropertiesOf(defaultAssignment); // the new assignment must take on properties of the default assignment
				AssignmentService.getInstance().remove(defaultAssignment, null,true);
			} else {
				// if the task is started already, then only apply to remaining duration.  This means added delay to new assignment
				if (getActualStart() != 0L)
					assignment.setDelay(Duration.millis(getActualDuration()));
				assignment.adjustRemainingDuration(Duration.millis(getRemainingDuration()),false);
			}
		} else {
			if (defaultAssignment != null) //Remove any default assignment.  This happens importing if the imported task just has no assignments
				AssignmentService.getInstance().remove(defaultAssignment, null,true);

			// use default task duration for the default assignment duraiton
			assignment.setDuration(getRawDuration());
		}

		// must calculate these two values before adding assignment!
		double mostLoadedAssignmentUnits = getMostLoadedAssignmentUnits();
		// Get details of current assignments before change
		double assignedRate = getRemainingUnits();

		// add assignment
		((TaskSnapshot) getCurrentSnapshot()).addAssignment(assignment);

		if (!assignment.isInitialized()) // if reading in, then don't recalc duration
			return;

		// if effort driven then set duration
		if (recalculateDuration && isEffortDriven()) {
			if (assignedRate != 0) {//
				if (getSchedulingType() == SchedulingType.FIXED_DURATION) // fixed duration effort driven has complicated rule - a new assignment is weighted the same as the most loaded assignment, unless that assignment is over 100%
					assignment.adjustRemainingUnits(Math.min(1.0,mostLoadedAssignmentUnits), 1, false, false);
				double newRemainingUnits = assignedRate + assignment.getRemainingLaborUnits();

				getSchedulingRule().adjustRemainingUnits(this,
						newRemainingUnits, assignedRate,
						true, true); // conserve total units
			}
		}
		setDirty(true);
		//project.endUndoUpdate();

	}


	/**
	 * @param assignment
	 */
	public void removeAssignment(Assignment assignment) {
		//project.beginUndoUpdate();
		boolean recalculateDuration = !assignment.isDefault()
				&& assignment.isInitialized(); // && assignment.isLabor();
		// Get details of current assignments before change

		double assignedRate = getRemainingUnits();
		((TaskSnapshot) getCurrentSnapshot()).removeAssignment(assignment);

		if (!assignment.isDefault()) {

			if (recalculateDuration && isEffortDriven()) {
				double newUnits = assignedRate - assignment.getLaborUnits();
				if (newUnits != 0) {
					getSchedulingRule().adjustRemainingUnits(this,
							newUnits, assignedRate,
							true, true); // conserve total units
				}
			}
			if (getAssignments().isEmpty()) {
				Assignment newDefault = newDefaultAssignment();
				newDefault.usePropertiesOf(assignment); // the default assignment must take on properties of the removed assignment
				AssignmentService.getInstance().connect(newDefault, null);
			}
		}
		setDirty(true);
		//project.endUndoUpdate();

	}

	public DataSnapshot cloneSnapshot(DataSnapshot snapshot) {
		return (DataSnapshot) ((TaskSnapshot) snapshot).clone();
	}

	public TaskSnapshot getBaselineSnapshot() {
		return (TaskSnapshot) getSnapshot(CalculationOption.getInstance()
				.getEarnedValueBaselineId());
	}
	public void buildReverseQuery(ReverseQuery reverseQuery) {
		//Do this ones assignments
		((TaskSnapshot) getCurrentSnapshot()).buildReverseQuery(reverseQuery);
		Collection children = getWbsChildrenNodes();
		Object current;
		if (children != null) { //  do for all children as well
			Iterator i = children.iterator();
			Task child;
			while (i.hasNext()) {
				current = ((Node) i.next()).getImpl();
				if (! (current instanceof NormalTask))
					continue;
				child = (Task)current;
				child.buildReverseQuery(reverseQuery);
			}
		}

	}

	public long getBaselineStartOrZero() {
		TaskSnapshot baseline = getBaselineSnapshot();
		if (baseline == null)
			return 0L;
		return getBaselineStart();
	}

	public long getBaselineFinishOrZero() {
		TaskSnapshot baseline = getBaselineSnapshot();
		if (baseline == null)
			return 0L;
		return getBaselineFinish();
	}
	
	public long getBaselineStart() {
		TaskSnapshot baseline = getBaselineSnapshot();
		if (baseline == null)
			return getStart();

		return baseline.getCurrentSchedule().getStart();

	}
	public long getBaselineFinish() {
		TaskSnapshot baseline = getBaselineSnapshot();
		if (baseline == null)
			return getEnd();

		return baseline.getCurrentSchedule().getFinish();

	}

	public int getSchedulingType() {
		return ((TaskSnapshot) getCurrentSnapshot()).getSchedulingType();
	}

	public void setSchedulingType(int schedulingType) {
		((TaskSnapshot) getCurrentSnapshot()).setSchedulingType(schedulingType);
	}

	public boolean isEffortDriven() {
		return ((TaskSnapshot) getCurrentSnapshot()).isEffortDriven();
	}

	public void setEffortDriven(boolean effortDriven) {
		((TaskSnapshot) getCurrentSnapshot()).setEffortDriven(effortDriven);

	}

	public boolean isReadOnlyEffortDriven(FieldContext fieldContext) {
		return ((TaskSnapshot) getCurrentSnapshot()).isReadOnlyEffortDriven(fieldContext);
	}

	public static Consumer<Object> forAllAssignments(Consumer<Object> visitor) {
		return new ObjectVisitor(visitor) {
			protected Object getObject(Object arg0) {
				return ((TaskSnapshot) ((Task) arg0).getCurrentSnapshot())
						.getHasAssignments();
			}
		};
	}

	public double getFixedCost() {
		return ((TaskSnapshot) getCurrentSnapshot()).getFixedCost();
	}
	public void setFixedCost(double fixedCost) {
		((TaskSnapshot) getCurrentSnapshot()).setFixedCost(fixedCost);
	}

	/**
	 * @return Returns the fixedCostAccrual.
	 */
	public final int getFixedCostAccrual() {
		return ((TaskSnapshot) getCurrentSnapshot()).getFixedCostAccrual();
	}
	/**
	 * @param fixedCostAccrual The fixedCostAccrual to set.
	 */
	public final void setFixedCostAccrual(int fixedCostAccrual) {
		((TaskSnapshot) getCurrentSnapshot()).setFixedCostAccrual(fixedCostAccrual);
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Assignment findAssignment(Resource resource) {
		return ((TaskSnapshot) getCurrentSnapshot()).findAssignment(resource);
	}

	public Assignment findAssignment(Task task) {
		return ((TaskSnapshot) getCurrentSnapshot()).findAssignment(task);
	}

	public void updateAssignment(Assignment modified) {
		((TaskSnapshot) getCurrentSnapshot()).updateAssignment(modified);

	}

	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendarToUse) {
		((TaskSnapshot) getCurrentSnapshot()).forEachWorkingInterval(visitor,
				mergeWorking, workCalendarToUse);
	}

	/**
	 * @return
	 */
	public boolean isEstimated() {
		return estimated;
	}

/**
 * Set estimated status of flag. First level parents will have their status set by the CP.  Higher levels
 * will need to be set recursively. Note that a parent will only be asked to updated its estimated
 * status if one of its children has had its estimated status change.
 */
	public void setEstimated(boolean estimated) {
		boolean changed = this.estimated != estimated;
		this.estimated = estimated;
		if (changed && isWbsParent()) { // only deal with parents already since CP handles children and sets first parent level
			NormalTask parent = (NormalTask) this.getWbsParentTask();
			if (parent != null)
				parent.updateEstimatedStatus();
		}

	}

	private void updateEstimatedStatus() {
		Collection children = getWbsChildrenNodes();
		Iterator i = children.iterator();
		Object current;
		NormalTask child;
		boolean childEstimated = false;
		while (i.hasNext()) {
			current = ((Node) i.next()).getImpl();
			if (! (current instanceof NormalTask))
				continue;
			child = (NormalTask) current;
			childEstimated |= child.isEstimated();
		}
		setEstimated(childEstimated);
	}

	/**
	 * set actual start and competion date for parents
	 *
	 */
	protected void assignParentActualDatesFromChildren() {
		NormalTask parent = this;
		while ((parent = (NormalTask) parent.getWbsParentTask()) != null)
			parent.assignActualDatesFromChildren();

	}

	/**
	 * Assigns the actual start and completed date fields of parents based on
	 * children values
	 *
	 */
	public void assignActualDatesFromChildren() {
		long computedActualStart = Long.MAX_VALUE;
		long stop = 0;
		Collection children = getWbsChildrenNodes();
		Iterator i = children.iterator();
		Task child;
		long currentActualStart;
		long oldActualDuration = Duration.millis(getActualDuration());
		Object current;
		while (i.hasNext()) {
			current = ((Node) i.next()).getImpl();
			if (! (current instanceof NormalTask))
				continue;
			child = (NormalTask) current;
			if (!child.inProgress())
				continue;
			if ((currentActualStart = child.getActualStart()) != 0) // if any task has actual start, use the earliest value
				computedActualStart = Math.min(computedActualStart, currentActualStart);

			stop = Math.max(stop, child.getStop());
		}

		long actualDuration = 0;
		if (computedActualStart != Long.MAX_VALUE && stop != 0)
			actualDuration = getEffectiveWorkCalendar().compare(stop,getStart(),false);
		if (computedActualStart != Long.MAX_VALUE)
			setActualStartNoEvent(computedActualStart);
		else
			setActualStartNoEvent(0L);
		
		if (actualDuration != oldActualDuration) {
			double percentComplete =((double)actualDuration) / getDurationMillis();
			currentSchedule.setPercentComplete(percentComplete);
			markTaskAsNeedingRecalculation(); // so it redraws
		}
	}


	public WorkCalendar getTaskCalendar() {
		return getWorkCalendar();
	}

	public void setTaskCalendar(WorkCalendar taskCalendar) {
		if (workCalendar == taskCalendar)
			return;
		CalendarService.getInstance().reassignCalendar(this,workCalendar,taskCalendar);
		setWorkCalendar(taskCalendar);
		invalidateAssignmentCalendars(); // assignments intersection calendars need to be recalculated
	}

	public long getBaselineStart(int numBaseline) {
		TaskSnapshot snapshot = ((TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline)));
		if (snapshot == null)
			return 0;
		return snapshot.getCurrentSchedule().getStart();
	}

	public long getBaselineFinish(int numBaseline) {
		TaskSnapshot snapshot = ((TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline)));
		if (snapshot == null)
			return 0;
		return snapshot.getCurrentSchedule().getEnd();
	}

	public long getBaselineDuration(int numBaseline) {
		TaskSnapshot snapshot = ((TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline)));
		if (snapshot == null)
			return 0;
		return snapshot.getCurrentSchedule().getRawDuration();
	}

	public double getBaselineCost(int numBaseline, long start, long end) {
		TaskSnapshot snapshot = ((TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline)));
		if (snapshot == null)
			return 0;
		return snapshot.cost(start, end);
	}

	public double getBaselineWork(int numBaseline, long start, long end) {
		TaskSnapshot snapshot = ((TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline)));
		if (snapshot == null)
			return 0;
		return snapshot.work(start, end);
	}

	//	public long getWork() {
	//		DoubleSum sumFunctor = new DoubleSum() {
	//
	//			protected double getValueForElement(Object object) {
	//				return ((Assignment)object).calcAll(Assignment.WORK);
	//			}};
	//
	//		DataUtils.forAllDo(getAssignments().iterator(), sumFunctor);
	//		return (long) sumFunctor.getValue();
	//	}



	public String getResourceInitials() {
		return AssociationListFormat.getInstance(
				AssignmentFormat
						.getInstance(AssociationFormatParameters.getInstance(
								this, true, Configuration
										.getFieldFromId("Field.initials"),
								false, false))).format(getAssignments());
	}

	public void setResourceInitials(String resourceInitials)
			throws FieldParseException {
		getAssignments().setAssociations(
				resourceInitials,
				AssignmentFormat
						.getInstance(AssociationFormatParameters.getInstance(
								this, true, Configuration
										.getFieldFromId("Field.initials"),
								false, false)));
	}

	public String getResourcePhonetics() {
		return AssociationListFormat.getInstance(
				AssignmentFormat
						.getInstance(AssociationFormatParameters.getInstance(
								this, true, Configuration
										.getFieldFromId("Field.phonetics"),
								false, true))).format(getAssignments());

	}

	public String getResourceGroup() {
		return AssociationListFormat.getInstance(
				AssignmentFormat.getInstance(AssociationFormatParameters
						.getInstance(this, true, Configuration
								.getFieldFromId("Field.group"), false, false)))
				.format(getAssignments());
	}

	public String getResourceNames() {
		return AssociationListFormat.getInstance(
				AssignmentFormat.getInstance(AssociationFormatParameters
						.getInstance(this, true, Configuration
								.getFieldFromId("Field.name"), true, true)))
				.format(getAssignments());
	}

	public void setResourceNames(String resourceNames)
			throws FieldParseException {
		getAssignments().setAssociations(
				resourceNames,
				AssignmentFormat.getInstance(AssociationFormatParameters
						.getInstance(this, true, Configuration
								.getFieldFromId("Field.name"), true, true)));

	}

	public double getUnits() {
		if (getAssignments().isEmpty())
			return 0;
		long duration = getDurationMillis();
		if (duration == 0.0)
			return 1.0D; // degeneratate case
		if (!isInitialized()) // the case when reading a file, don't boether to
							  // calculate
			return 1.0;
		long work = calcWork();
		if (work == 0) // degenerate case with no work yet
			return 1.0;
		return ((double) work) / duration;
	}

	public double getRemainingUnits() {
		if (getAssignments().isEmpty())
			return 0;
		long duration = Duration.millis(getRemainingDuration());
		if (duration == 0.0)
			return 1.0D; // degeneratate case
		if (!isInitialized()) // the case when reading a file, don't boether to
							  // calculate
			return 1.0;
		long work = getRemainingWork(null);
//		if (work == 0) // degenerate case with no work yet
//			return 1.0;
		return ((double) work) / duration;

	}


	public void setWork(long work, FieldContext context) {

		if (FieldContext.hasInterval(context)) {
			Iterator i = getAssignments().iterator();
			while (i.hasNext()) {
				Assignment assignment = (Assignment) i.next();
				assignment.setWork(work,context);
			}
		} else {
			setWork(work);
		}
	}

	public void setWork(long work) {
		work = Duration.millis(work);
		if (hasLaborAssignment() && work < 60000) {
			work *= Duration.timeUnitFactor(TimeUnit.HOURS);
		}
		long remainingWork = work - getActualWork(null);
		getSchedulingRule().adjustRemainingWork(this, remainingWork, true);
	}

	public long calcWork() {
		if (!hasRealAssignments()) // avoid treating dummy assignment
			return 0;

		return getWork(null);
	}

	public double getMostLoadedAssignmentUnits() {
		double result = 0;
		Iterator i = getAssignments().iterator();
		while (i.hasNext())
			result = Math.max(result,((Assignment) i.next()).getLaborUnits());

		return result;
	}
	public void adjustRemainingDuration(long newDuration, boolean doChildren) {
//~~		setRawDuration(newDuration); // keep units
//hk		long newRemainingDuration = Duration.millis(newDuration) - getActualDuration(); // assignments dont treqt
		long newRemainingDuration = Duration.millis(newDuration); // - getActualDuration(); // assignments dont treqt
													// units
		Iterator i = getAssignments().iterator();
		while (i.hasNext())
			((Assignment) i.next()).adjustRemainingDurationIfWorkingAtTaskEnd(newRemainingDuration);

	}

	/**
	 * Called when an assignment value is modified. We want the task details to
	 * be modified without changing the assignment details
	 *
	 * @param deltaAdded
	 */
	public void adjustUnitsDelta(double deltaAdded) {
		getSchedulingRule().adjustRemainingUnits(this, getRemainingUnits() + deltaAdded,
				getRemainingUnits(), false, false);
	}

	public void adjustRemainingUnits(double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits) {

		if (!doChildren)
			return;
		double multiplier = 1;
		if (conserveTotalUnits) {
			multiplier= oldRemainingUnits / newRemainingUnits;
		}

		double u = newRemainingUnits;
		double remaining = getRemainingUnits();
		double factor= u/remaining;
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment) i.next();
			double r = assignment.getLaborUnits();
//			if (!assignment.isLabor())
//				continue;
			if (conserveTotalUnits)
				getSchedulingRule().adjustRemainingUnits(assignment, assignment.getRemainingLaborUnits() * multiplier, assignment.getRemainingLaborUnits(), false, false);
			else {
				getSchedulingRule().adjustRemainingUnits(assignment,factor*r,r, false, false);
			}
		}

	}

	public void adjustRemainingWork(double multiplier, boolean doChildren) {
//		long newDuration = (long) (getDurationMillis() * multiplier);
//~~		setRawDuration(newDuration);
		//need to always do children regardless of doChildren flag
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			Assignment assignment = (Assignment) i.next();
			if (!assignment.isLabor())
				continue;
			getSchedulingRule().adjustRemainingWork(assignment,(long) (assignment.getRemainingWork()*multiplier),false);
		}
	}

	/**
	 * Gets a (singleton) instance of the scheduling rule to use for the task
	 *
	 * @return scheduling rule to use in adjust...() calculations
	 */
	public SchedulingRule getSchedulingRule() {
		return SchedulingType.getSchedulingRuleInstance(getSchedulingType());

	}

	public boolean isReadOnlyUnits(FieldContext fieldContext) {
		return true;
	}
	public long getCompletedThrough() {
		long start = getStart();
		if (start == 0)
			return 0;
		long actualDuration = DateTime.closestDate(getDurationMillis() * getPercentComplete());
		return getEffectiveWorkCalendar().add(start,actualDuration,true);
	}



	/**
	 * Stop is the earliest completion date of the assignments
	 * @return
	 */
	public long getStop() {
//		if (isWbsParent( )) {
//			long start = getStart();
//			if (start == 0)
//				return 0;
//			long actualDuration = DateTime.closestDate(getDurationMillis() * getPercentComplete());
//			return getEffectiveWorkCalendar().add(start,actualDuration,true);
//		}
		return getEarliestStop();
		//&&&&&
//		long stop = 0;
//		Assignment assignment;
//		Iterator i = getAssignments().iterator();
//		while (i.hasNext()) {
//			assignment = (Assignment)i.next();
//			stop = Math.max(stop,assignment.getStop());
//		}
//		return stop;
	}

	//Used when an assignment advancement changes
	public void adjustActualStartFromAssignments() {
		Assignment assignment;
		Iterator i = getAssignments().iterator();
		long start = 0L;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			if (assignment.getPercentComplete() > 0.0D ) {
				start= getStart();
				break;
			}
		}
//		System.out.println("adjusting actual start to " + new java.util.Date(start));
		setActualStart(start);
		assignParentActualDatesFromChildren();

	}

	/**
	 * @param stop
	 */
	public void setStop(long stop) {
		if (stop == getStop())
			return;
		stop = DateTime.closestDate(stop);
		stop = Math.min(stop,getEnd());

		Iterator i = getAssignments().iterator();
		Assignment assignment;
		long computedActualStart = Long.MAX_VALUE;
		long assignmentActualStart;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.setStop(stop);
			assignmentActualStart = assignment.getActualStart();
			if (assignmentActualStart != 0 && assignmentActualStart < computedActualStart)
				computedActualStart = assignmentActualStart;
		}
		if (computedActualStart == Long.MAX_VALUE)
			computedActualStart = 0;
		setActualStart(computedActualStart);
		assignParentActualDatesFromChildren();

		// if % complete went down to 0, then the plan changed and need to recalculate all.
		if (computedActualStart == 0) {
			getDocument().getObjectEventManager().fireUpdateEvent(this, this,
					Configuration.getFieldFromId("Field.start"));
		} else {
			getProject().fireScheduleChanged(this, ScheduleEvent.ACTUAL, this);
		}

	}
	/**
	 * @return
	 */
	public long getResume() {
		long resume = Long.MAX_VALUE;
		Assignment assignment;
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			resume = Math.min(resume,assignment.getResume());
		}
		return resume;
	}
	/**
	 * @param resume
	 */
	public void setResume(long resume) {
		Assignment assignment;
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.setResume(resume);
		}
	}




	private void setStopNoExtend(long stop) {
			long start = getStart();
		if (stop < start) {// don't allow completion before start
			setActualDuration(0);
			stop = start;
		} else {
			long duration = getEffectiveWorkCalendar().compare(stop,
					start, false);
			duration = Math.min(duration, getDurationMillis()); // don't ever
																// change finish
			setActualDuration(duration);
		}
//		scheduleWindow.setStop(stop);
	}

	/***************************************************************************
	 * TimeDistributedData T********
	 **************************************************************************/
	/**
	 * For parent tasks, we don't want to count their one day of work
	 */
	private boolean isParentWithoutAssignments() {
		return (isWbsParent() && !hasRealAssignments());
	}

	public double cost(long start, long end) {
		if (isParentWithoutAssignments())
			return 0.0D;
		return ((TaskSnapshot) getCurrentSnapshot()).cost(start, end);
	}

	public long work(long start, long end) {
		if (isParentWithoutAssignments())
			return 0L;
		return ((TaskSnapshot) getCurrentSnapshot()).work(start, end);
	}

	public double actualCost(long start, long end) {
		if (isParentWithoutAssignments())
			return 0.0D;

		return ((TaskSnapshot) getCurrentSnapshot()).actualCost(start, end);
	}

	public long actualWork(long start, long end) {
		if (isParentWithoutAssignments())
			return 0L;
		return ((TaskSnapshot) getCurrentSnapshot()).actualWork(start, end);
	}
	public long remainingWork(long start, long end) {
		if (isParentWithoutAssignments())
			return 0L;
		return ((TaskSnapshot) getCurrentSnapshot()).remainingWork(start, end);
	}

	public double baselineCost(long start, long end) {
		if (getBaselineSnapshot() == null)
			return 0;

		return getBaselineSnapshot().cost(start, end);
	}

	public long baselineWork(long start, long end) {
		if (getBaselineSnapshot() == null)
			return 0;
		return getBaselineSnapshot().work(start, end);
	}

	/***************************************************************************
	 * EarnedValueValues
	 **************************************************************************/

	public double acwp(long start, long end) {
		return ((TaskSnapshot) getCurrentSnapshot()).acwp(start, end);
	}

	public double bac(long start, long end) {
		return ((TaskSnapshot) getCurrentSnapshot()).bac(start, end);
	}

	public double bcwp(long start, long end) {
		return ((TaskSnapshot) getCurrentSnapshot()).bcwp(start, end);
	}

	public double bcws(long start, long end) {
		return ((TaskSnapshot) getCurrentSnapshot()).bcws(start, end);
	}

	boolean isInRange(long start, long finish) {
		long s = getStart();
		return (finish > s && start < getEnd());
	}

	private boolean isFieldHidden(FieldContext fieldContext) {
		return fieldContext != null && !isInRange(fieldContext.getStart(),fieldContext.getEnd());
	}

	private boolean isBaselineFieldHidden(int numBaseline,FieldContext fieldContext) {
		TaskSnapshot baseline = (TaskSnapshot) getSnapshot(Integer.valueOf(numBaseline));
		if (baseline == null)
			return true;

		 if (fieldContext == null) // the baseline exists, but no time range
			 return false;
		 return (fieldContext.getStart() >= baseline.getCurrentSchedule().getFinish() || fieldContext.getEnd() <= baseline.getCurrentSchedule().getStart());
	}

	private boolean isEarnedValueFieldHidden(FieldContext fieldContext) {
		if (isFieldHidden(fieldContext))
			return true;
		if (fieldContext == null)
			return false;
		return project.getStatusDate() < fieldContext.getStart();
	}

	/***************************************************************************
	 * Time Distributed Fields
	 **************************************************************************/
	public boolean fieldHideCost(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideWork(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideActualCost(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideActualWork(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBaselineCost(int numBaseline,FieldContext fieldContext) {
		return isBaselineFieldHidden(numBaseline,fieldContext);
	}
	public boolean fieldHideBaselineWork(int numBaseline,FieldContext fieldContext) {
		return isBaselineFieldHidden(numBaseline,fieldContext);
	}
	public boolean fieldHideAcwp(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideBcwp(FieldContext fieldContext) {
		return isEarnedValueFieldHidden(fieldContext);
	}
	public boolean fieldHideBcws(FieldContext fieldContext) {
		return isEarnedValueFieldHidden(fieldContext);
	}
	public boolean fieldHideCv(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSv(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideEac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideVac(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideCpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideCvPercent(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideSvPercent(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}
	public boolean fieldHideTcpi(FieldContext fieldContext) {
		return isFieldHidden(fieldContext);
	}

	public double getCost(FieldContext fieldContext) {
		return getFixedCost(fieldContext)
				+ cost(FieldContext.start(fieldContext), FieldContext
						.end(fieldContext));
	}

	public long getWork(FieldContext fieldContext) {
		return work(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}
	public double getActualFixedCost(FieldContext fieldContext) {
		return fixedCost(FieldContext.start(fieldContext), Math.min(getStop(),FieldContext // only up to completion
				.end(fieldContext)));
	}
	public double getFixedCost(FieldContext fieldContext) {
		if (!FieldContext.hasInterval(fieldContext))
			return ((TaskSnapshot) getCurrentSnapshot()).getFixedCost();

		return fixedCost(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public double actualFixedCost(long start, long end) {
		return fixedCost(start,Math.min(getStop(),end));
	}

	/** Calculate the fixed cost for the task given its accrual type and percent complete
	 */
	public double fixedCost(long start, long end) {
		long taskStart = getStart();
		long taskEnd = getEnd();
		double fixed = 0.0;
		double fixedCost = getFixedCost();
		if (getFixedCostAccrual() == Accrual.START) {
			if (taskStart >= start && taskStart <= end) // if task starts in this range
				fixed = fixedCost;
		} else if (getFixedCostAccrual() == Accrual.PRORATED) {
			// find overlapping actual time
			start = Math.max(start,taskStart);
			end = Math.min(end,taskEnd);
			if (start < end) { // if valid range
				long overlappingDuration = getEffectiveWorkCalendar().compare(end,start,false);
				double fraction = ((double)overlappingDuration) / getDurationMillis();
				fixed = fixedCost * fraction;
			}
		} else  { // END accrual by default
			if (taskEnd >= start && taskEnd <= end) // if task ends in this range
				fixed = fixedCost;
		}
		return fixed;
	}
	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return false;
	}



	public double getActualCost(FieldContext fieldContext) {
		return getActualFixedCost(fieldContext) + actualCost(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public long getActualWork(FieldContext fieldContext) {
		return actualWork(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}
	public long getRemainingWork(FieldContext fieldContext) {
		return remainingWork(FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}
	public long getRemainingWork() {
		return getRemainingWork(null);
	}
	public double getRemainingCost(FieldContext fieldContext) {
		return getCost(fieldContext) - getActualCost(fieldContext);
	}


	//Baseline versions
	public double getBaselineCost(int numBaseline, FieldContext fieldContext) {
		TaskSnapshot snapshot = (TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline));
		if (snapshot == null)
			return 0.0D;
		return ((TaskSnapshot) getSnapshot(Integer.valueOf(numBaseline))).cost(
				FieldContext.start(fieldContext), FieldContext
						.end(fieldContext));
	}

	public long getBaselineWork(int numBaseline, FieldContext fieldContext) {
		TaskSnapshot snapshot = (TaskSnapshot) getSnapshot(Integer.valueOf(
				numBaseline));
		if (snapshot == null)
			return 0L;
		return ((TaskSnapshot) getSnapshot(Integer.valueOf(numBaseline))).work(
				FieldContext.start(fieldContext), FieldContext
						.end(fieldContext));
	}

	/***************************************************************************
	 * Earned Value Fields
	 **************************************************************************/
	public double getAcwp(FieldContext fieldContext) {
		return acwp(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public double getBac(FieldContext fieldContext) {
		return bac(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public double getBcwp(FieldContext fieldContext) {
		return bcwp(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public double getBcws(FieldContext fieldContext) {
		return bcws(FieldContext.start(fieldContext), FieldContext
				.end(fieldContext));
	}

	public double getCv(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cv(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getSv(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().sv(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getEac(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().eac(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getVac(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().vac(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getCpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cpi(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getSpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().spi(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}
	public double getCsi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().csi(this,FieldContext.start(fieldContext),FieldContext.end(fieldContext));
	}

	public double getCvPercent(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().cvPercent(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getSvPercent(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().svPercent(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public double getTcpi(FieldContext fieldContext) {
		return EarnedValueCalculator.getInstance().tcpi(this,
				FieldContext.start(fieldContext),
				FieldContext.end(fieldContext));
	}

	public void calcDataBetween(Object type, TimeIteratorGenerator generator,
			CalculatedValues values) {
		((TaskSnapshot) getCurrentSnapshot()).calcDataBetween(type, generator,
				values);

	}


	public void setPercentComplete(double percentComplete) {
		if (percentComplete > 1.0) {
			logger.warning("percent complete more than 100%");
			percentComplete = 1.0;
		} else if (percentComplete < 0){
			logger.warning("percent complete less than 0%");
			percentComplete = 0.0;
		}
		updateInactivePercentComplete(percentComplete);
		if (isZeroDuration()) { // special case for completion on milestones
			updateAssignmentPercentComplete(percentComplete);
		} else {
			updateAssignmentPercentComplete(percentComplete);
			long actualDuration = DateTime.closestDate(getDurationMillis() * percentComplete);
			setActualDuration(actualDuration);
			long stop = getEffectiveWorkCalendar().add(getStart(), actualDuration, false);
			DeepChildWalker.recursivelyTreatBranch(getProject().getTaskOutline(),
					this, new NumberClosure(stop) {
						public void accept(Object arg0) {
							if (arg0 == null) {
								return;
							}
							Object nodeObject = ((Node) arg0).getImpl();
							if (nodeObject instanceof NormalTask) { // do not treat assignments
								NormalTask task = ((NormalTask)nodeObject);
								task.setStop(Math.min(longValue(),task.getEnd())); // do within range of task
							}
						}
					});
		}
	}

	private void updateAssignmentPercentComplete(double percentComplete) {
		final double pc = percentComplete;
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			((Assignment) i.next()).setPercentComplete(pc);
		}
	}

	public double getPercentWorkComplete() {
		if (isWbsParent())
			return PercentWorkCompleteService.aggregate(this);
		long work = Duration.millis(getWork(null));
		if (work == 0)
			return Double.isNaN(percentWorkCompleteOverride) ? 0.0d : percentWorkCompleteOverride;
		else
			return ((double) Duration.millis(getActualWork(null))) / work;
	}

	public void setPercentWorkComplete(double percentWorkComplete) {
		if (percentWorkComplete < 0)
			percentWorkComplete = 0;
		if (percentWorkComplete > 1)
			percentWorkComplete = 1;
		if (isWbsParent()) {
			PercentWorkCompleteService.distribute(this, percentWorkComplete);
			return;
		}
		long work = Duration.millis(getWork(null));
		if (work == 0L) {
			percentWorkCompleteOverride = percentWorkComplete;
			return;
		}
		percentWorkCompleteOverride = Double.NaN;
		setPercentComplete(percentWorkComplete);

	}

	void applyPercentWorkCompleteOverride(double percentWorkComplete) {
		setPercentWorkComplete(DisplayMath.clampProgressValue(percentWorkComplete));
	}

	public boolean hasPercentWorkCompleteOverride() {
		return !Double.isNaN(percentWorkCompleteOverride);
	}

	/**
	 * Cleans up all links to and form this task and removes all assignments,
	 * including baseline ones.
	 *
	 * @param eventSource -
	 *            if not null, then events will be sent indicating the removal
	 *            of links and assignments
	 */
	void cleanUp(Object eventSource,boolean deep,boolean undo,boolean cleanDependencies) {
		super.cleanUp(eventSource,deep,undo,cleanDependencies); // gets rid of dependencies

		// for all snapshots
		if (deep){
			TaskSnapshot snapshot;
			for (int i = 0; i < Settings.numBaselines(); i++) {
				Integer snapshotId = Integer.valueOf(i);
				snapshot = (TaskSnapshot) getSnapshot(snapshotId);
				if (snapshot != null) {
					// send events only for current snapshot
					Object useEventSource = (getCurrentSnapshot() == snapshot) ? eventSource
							: null;

					LinkedList toRemove = new LinkedList(); //fix
					AssignmentService.getInstance().remove(
							snapshot.getAssignments(), toRemove);
					AssignmentService.getInstance().remove(toRemove, useEventSource,false);

					if (snapshot != getCurrentSnapshot())
						getProject().fireBaselineChanged(eventSource, this,
								snapshotId, false);
				}

			}
		}
	}

	public Collection childrenToRollup() {
		return ((TaskSnapshot) getCurrentSnapshot()).getHasAssignments()
				.childrenToRollup();
	}

	// some functions useful for API
	public double getCost() {
		return getCost(null);
	}

	public double getBaselineCost() {
		return getBaselineCost(0, null);
	}

	public double getBaselineCost(int number) {
		return getBaselineCost(number, null);
	}

	public double getWork() {
		return getWork(null);
	}

	public double getBaselineWork() {
		return getBaselineWork(0, null);
	}

	public double getBaselineWork(int number) {
		return getBaselineWork(number, null);
	}


/**
 *  Useful for drawing bars
 */
	public long getTotalSlackStart() {
		return (getConstraintType() == ConstraintType.ALAP) ? getEarlyStart() : getEarlyFinish();

	}
/**
 *  Useful for drawing bars
 */	public long getTotalSlackEnd() {
		return (getConstraintType() == ConstraintType.ALAP) ? getLateStart() : getLateFinish();
	}

	/**
	 * Offset the given date by the duration of the remaining duration.
	 */
	public long calcOffsetFrom(long startDate, long dependencyDate, boolean ahead, boolean remainingOnly, boolean useSooner) {

		//		This is a task based implementation- for parents dont use their assignments
		if (isWbsParent()) {
			long d = remainingOnly ? Duration.millis(getRemainingDuration()) : getDurationMillis();
			if (!ahead)
				d = -d;
			return getEffectiveWorkCalendar().add(startDate,d,useSooner);
		}
//
//
//		This is an assignment based implementation

		Iterator i = getAssignments().iterator();
		long result;
		Assignment assignment;
		if (startDate < 0)
			result = ahead ? Long.MIN_VALUE : 0;
		else
			result = ahead ? 0 : Long.MAX_VALUE;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			long offsetDate = assignment.calcOffsetFrom(startDate,dependencyDate,ahead,remainingOnly,useSooner);
			result = ahead ? Math.max(result,offsetDate) : Math.min(result,offsetDate);
		}
		return result;
	}

	public long calcActiveAssignmentDuration(WorkCalendar workCalendarToUse) {
		return ((TaskSnapshot) getCurrentSnapshot()).calcActiveAssignmentDuration(workCalendarToUse);
	}

	public void moveRemainingToDate(long date) {
		date = getEffectiveWorkCalendar().adjustInsideCalendar(date,false);
		if (getActualStart() == 0L)
			setStart(date); // if not started, change start
		else if (inProgress()) {
			Iterator i = getAssignments().iterator();
			Assignment assignment;
			while (i.hasNext()) {
				assignment = (Assignment)i.next();
				assignment.moveRemainingToDate(date);
			}
		} // do nothing for completed tasks
	}
	public void moveInterval(Object eventSource, long start, long end, ScheduleInterval oldInterval, boolean isChild) {
		WorkCalendar cal = getEffectiveWorkCalendar();
		long requestedStart = start;
		long requestedEnd = end;
		boolean requestedZeroDuration = requestedStart == requestedEnd;
		boolean requestedCrossedInterval = requestedStart > requestedEnd;
		start = cal.adjustInsideCalendar(start,false);
		end = requestedZeroDuration ? start : CalendarOption.getInstance().makeValidEnd(end, false);
		if (requestedCrossedInterval) {
			if (cal.compare(requestedStart, oldInterval.getStart(), false) != 0) {
				end = start;
			} else {
				start = end;
			}
		}
		if (isMilestone() || oldInterval.getStart() == oldInterval.getEnd()) {
			if (cal.compare(start, getStart(), false) == 0) {
				return;
			}
			setCurrentScheduleStart(start);
			setCurrentScheduleFinish(start);
			setRawDuration(0L);
			recalculate(eventSource);
			assignParentActualDatesFromChildren();
			return;
		}
		if (!isWbsParent() && !isExternal() && !isSubproject() && !hasRealAssignments()) {
			long originalStart = getStart();
			long originalEnd = getEnd();
			long newStart = start;
			long newEnd = end;

			if (newEnd < newStart) {
				if (cal.compare(start, oldInterval.getStart(), false) != 0) {
					newEnd = newStart;
				} else {
					newStart = newEnd;
				}
			}
			if (newStart == originalStart && newEnd == originalEnd) {
				return;
			}

			setCurrentScheduleStart(newStart);
			setDuration(Duration.setAsEstimated(cal.compare(newEnd, newStart, false), estimated));
			setCurrentScheduleFinish(newEnd);
			markTaskAsNeedingRecalculation();
			assignParentActualDatesFromChildren();
			return;
		}
		boolean shifting = cal.compare(start,oldInterval.getStart(),false) != 0;
		long assignmentStart = getEarliestAssignmentStart();
		long amountFromStart = cal.compare(oldInterval.getStart(),assignmentStart,false); // possible that they are not the same but there is no working time between them
		if (shifting && amountFromStart == 0L) { // see if first bar shifted -The first bar is drawn from the first assignment and not from the task start.
			long shift = cal.compare(start,assignmentStart,false);
			long newTaskStart = cal.add(getStart(),shift,false);
			long newTaskEnd = cal.add(getEnd(),shift,false);
			setCurrentScheduleStart(newTaskStart);
			setCurrentScheduleFinish(newTaskEnd);

			Iterator i = getAssignments().iterator();
			Assignment assignment;
			while (i.hasNext()) {
				assignment = (Assignment)i.next();
				long assignmentStartBeforeMove = assignment.getStart();
				long assignmentEndBeforeMove = assignment.getEnd();
				assignment.moveInterval(
						eventSource,
						cal.add(assignmentStartBeforeMove, shift, false),
						cal.add(assignmentEndBeforeMove, shift, false),
						new ScheduleInterval(assignmentStartBeforeMove, assignmentEndBeforeMove),
						true);
			}
			setRawDuration(Duration.setAsEstimated(cal.compare(newTaskEnd, newTaskStart, false), estimated));
		} else {
			long amount =cal.compare(end,oldInterval.getEnd(),false);
			if (amount == 0L) // skip if nothing moved
				return;

			Iterator i = getAssignments().iterator();
			Assignment assignment;
			while (i.hasNext()) {
				assignment = (Assignment)i.next();
				assignment.moveInterval(eventSource,start,end,oldInterval, true);
			}
		}
		setRawDuration(getDurationMillis()); // this fixes all sorts of pbs

		recalculate(eventSource); // need to recalculate
		assignParentActualDatesFromChildren();


//		//Undo
//		UndoableEditSupport undoableEditSupport=getProject().getUndoController().getEditSupport();
//		if (undoableEditSupport!=null&&!(eventSource instanceof UndoableEdit)){
//			undoableEditSupport.postEdit(new ScheduleEdit(this,new ScheduleInterval(start,end),oldInterval,isChild,eventSource));
//		}


	}


	public void split(Object eventSource, long from, long to) {
		from = getEffectiveWorkCalendar().adjustInsideCalendar(from,false);
		to = getEffectiveWorkCalendar().adjustInsideCalendar(to,false);

		if (from == to) { // if from is same as two, split one day
			to = getEffectiveWorkCalendar().add(from,CalendarOption.getInstance().getMillisPerDay(),false);
		}

		Iterator i = getAssignments().iterator();
		Assignment assignment;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.split(eventSource,from,to);
		}
		recalculate(eventSource); // need to recalculate
		assignParentActualDatesFromChildren();

	}

	protected transient static BarClosure barClosureInstance = new BarClosure();
	public void consumeIntervals(IntervalConsumer consumer) {
		if (isWbsParent() || isSubproject()) {
			consumer.consumeInterval(new ScheduleInterval(getStart(),getEnd()));
			return;
		}
		barClosureInstance.initialize(consumer,this);
		forEachWorkingInterval(barClosureInstance,true, getEffectiveWorkCalendar());

		if (barClosureInstance.getCount() == 0) { // if no bars drawn
			consumer.consumeInterval(new ScheduleInterval(getStart(),getEnd()));
		}
	}


	public void setEnd(long end) {
 		long start = getStart();
 		if (start == 0) { // if the end date is entered on a new line creating the task need to set the start correctly
 			start = CalendarOption.getInstance().makeValidStart(DateTime.midnightToday(),true);
 			getCurrentSchedule().setStart(start);
 		}
 		end = CalendarOption.getInstance().makeValidEnd(end, true);
 		if (end < start)
 			end = start;
		long oldEnd = getEnd();
		if (end != oldEnd) {
			super.setEnd(end);
			Iterator i = getAssignments().iterator();
			Assignment assignment;
			while (i.hasNext()) {
				assignment = (Assignment)i.next();
				assignment.setEnd(end);
			}
//			System.out.println("Old End"  + new Date(oldEnd) + " input end " + new Date(end )+ " resulting End " + new Date(getEnd()) + " duration " + DurationFormat.format(getDuration()));
			if (!hasRealAssignments())
				setRawDuration(getEffectiveWorkCalendar().compare(getEnd(), getStart(), false));
			else
				setRawDuration(getDurationMillis());
		}
		assignParentActualDatesFromChildren();
	}


	public void setActualStart(long actualStart) {
		actualStart = getEffectiveWorkCalendar().adjustInsideCalendar(actualStart, false);
		if (actualStart == getActualStart())
			return;

		setActualStartNoEvent(actualStart);
		markTaskAsNeedingRecalculation();
		getProject().fireScheduleChanged(this, ScheduleEvent.ACTUAL, this);
	}

	public void setActualStartNoEvent(long actualStart) {
		long old = getActualStart();
		if (actualStart == old)
			return;
		this.actualStart = actualStart;
		assignParentActualDatesFromChildren();

	}

	public boolean isIgnoreResourceCalendar() {
		return ((TaskSnapshot) getCurrentSnapshot()).isIgnoreResourceCalendar();
	}


	public void setIgnoreResourceCalendar(boolean ignoreResourceCalendar) {
		((TaskSnapshot) getCurrentSnapshot()).setIgnoreResourceCalendar(ignoreResourceCalendar);
	}

	public boolean isDefault(){
	    return this==UNASSIGNED;
	}

	private static short DEFAULT_VERSION=2;
	private short version=DEFAULT_VERSION;

	public short getVersion() {
		return version;
	}
/* The serialization version must be private. This lets subclasses call this code */
	protected void doWriteObject(ObjectOutputStream s) throws IOException {
	    s.defaultWriteObject();
	    hasKey.serialize(s);
	    customFields.serialize(s);
	    if (version<1) currentSchedule.serialize(s);
	    else{
	    	int sCount=0;
            for (int i=0;i<Settings.numBaselines();i++){
                TaskSnapshot snapshot=(TaskSnapshot)getSnapshot(Integer.valueOf(i));
                if (snapshot!=null) sCount++;
    	    }
            s.writeInt(sCount);
            for (int i=0;i<Settings.numBaselines();i++){
                TaskSnapshot snapshot=(TaskSnapshot)getSnapshot(Integer.valueOf(i));
                if (snapshot==null) continue;
                s.writeInt(i);
                snapshot.serialize(s);
            }
	    }
	}
	private void writeObject(ObjectOutputStream s) throws IOException {
		doWriteObject(s);
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();

	    hasKey=HasKeyImpl.deserialize(s,this);
	    customFields=CustomFieldsImpl.deserialize(s);
	    if (version<1)currentSchedule=TaskSchedule.deserialize(s);
	    else{
	    	snapshots = new SnapshottableImpl(Settings.numBaselines());
	    	int sCount=s.readInt();
            for (int i=0;i<sCount;i++){
            	int snapshotId=s.readInt();
                TaskSnapshot snapshot=TaskSnapshot.deserialize(s,this);
                setSnapshot(Integer.valueOf(snapshotId), snapshot);
            }
	    }

	    if(version<1) super.initializeTransientTaskObjects();
	    else super.initializeTransientTaskObjectsAfterDeserialization();
//	    barClosureInstance = new BarClosure();
//	    This shouldn't be called -hk 4/feb/05
//	    initializeDates();

	    version=DEFAULT_VERSION;
	}
	public Object clone(){
		Task task=(Task)super.clone();
//		task.barClosureInstance = new BarClosure();



		return task;
	}
	public void cloneTo(Task task){
		if (task instanceof NormalTask){
			NormalTask n=(NormalTask)task;
			n.estimated=estimated;
			n.priority = priority;
			n.version=version;
			n.workCalendar = workCalendar;
			n.percentWorkCompleteOverride = percentWorkCompleteOverride;
		}

		super.cloneTo(task);
	}

	public void serialize(ObjectOutputStream s) throws IOException {}

	public boolean isReadOnlyWork(FieldContext fieldContext) {
		if (!hasLaborAssignment())
			return true;
		if (fieldContext == null)
			return false;
		return !hasActiveAssignment(fieldContext.getStart(), fieldContext.getEnd());
	}

	public void setActualWork(long actualWork, FieldContext context) {

		if (FieldContext.hasInterval(context)) {
			Iterator i = getAssignments().iterator();
			while (i.hasNext()) {
				Assignment assignment = (Assignment) i.next();
				assignment.setActualWork(actualWork,context);
			}
		} else {
			long workValue = Duration.millis(actualWork);
			long totalWork = Duration.millis(getWork(null));
			if (workValue == 0L) {
				setPercentComplete(0);
			} else if (workValue <= totalWork) {
				setPercentComplete(((double) workValue) / totalWork);
			} else {
				long  date = ReverseQuery.getDateAtValue(WORK, this, workValue, false);
				setStop(date);
			}
		}
	}

	public boolean isReadOnlyActualWork(FieldContext fieldContext) {
		return false;
	}

	public void setRemainingWork(long remainingWork, FieldContext fieldContext) {
		setActualWork(getWork(fieldContext) - Duration.millis(remainingWork), fieldContext);
	}

	public boolean isReadOnlyRemainingWork(FieldContext fieldContext) {
		return isReadOnlyWork(fieldContext);
	}

	public void setFixedCost(double fixedCost, FieldContext fieldContext) {
		if (!FieldContext.hasInterval(fieldContext))
			setFixedCost(fixedCost);
	}

	public boolean isReadOnlyFixedCost(FieldContext fieldContext) {
		return FieldContext.hasInterval(fieldContext);
	}

	public boolean isLabor() {
		return true;
	}

	public boolean hasLaborAssignment() {
		return ((TaskSnapshot) getCurrentSnapshot()).hasLaborAssignment();
	}

	public void setRawDuration(long duration) {
		currentSchedule.setRawDuration(duration);
	}


	public void setParentDuration() {
		if (!isWbsParent())
			return;
		currentSchedule.assignDatesFromChildren(null);
		long duration = getDurationMillis();
		getSchedulingRule().adjustRemainingDuration(this, duration - Duration.millis(getActualDuration()), true);
	}

	public void invalidateAssignmentCalendars() {
		((TaskSnapshot) getCurrentSnapshot()).invalidateAssignmentCalendars();
	}

	public Document invalidateCalendar() {
		invalidateAssignmentCalendars();
		markTaskAsNeedingRecalculation();
		return getProject();
	}

	public boolean hasActiveAssignment(long start, long end) {
		return ((TaskSnapshot) getCurrentSnapshot()).hasActiveAssignment(start, end);
	}


	public boolean isInvalidIntersectionCalendar() {
		Iterator i = getAssignments().iterator();
		while (i.hasNext()) {
			if (((Assignment)i.next()).isInvalidIntersectionCalendar())
				return true;
		}
		return false;
	}

	public HasIndicators getIndicators() {
		return this;
	}

	public long getEarliestAssignmentStart() {
		return ((TaskSnapshot) getCurrentSnapshot()).getEarliestAssignmentStart();
	}

	public boolean isParentWithAssignments() {
		return isWbsParent() && hasRealAssignments();
	}

	public void setComplete(boolean complete) {
		ScheduleUtil.setComplete(this,complete);
	}

	public boolean applyTimesheet(Collection fieldArray, long timesheetUpdateDate) {
		return TimesheetHelper.applyTimesheet(getAssignments(),fieldArray,timesheetUpdateDate);
	}

	public long getLastTimesheetUpdate() {
		return TimesheetHelper.getLastTimesheetUpdate(getAssignments());
	}

	public boolean isPendingTimesheetUpdate() {
		return TimesheetHelper.isPendingTimesheetUpdate(getAssignments());
	}

	public int getTimesheetStatus() {
		return TimesheetHelper.getTimesheetStatus(getAssignments());
	}

	public String getTimesheetStatusName() {
		return TimesheetHelper.getTimesheetStatusName(getTimesheetStatus());
	}

	public final long getEarliestStop() {
		long stop = Long.MAX_VALUE;
		Schedule s;
		Object nodeImpl;
		if (isWbsParent()) {
			Collection children = getWbsChildrenNodes();
			Iterator i = children.iterator();
			while (i.hasNext()) {
				Object x = i.next();
				if (!(x instanceof Node))
					continue;
				nodeImpl = ((Node)x).getImpl();
				if (! (nodeImpl  instanceof Schedule))
					continue;
				s = (Schedule)nodeImpl;
				stop = Math.min(stop,s.getEarliestStop());
			}
		} else {
			Iterator i = getAssignments().iterator();
			while (i.hasNext()) {
				Assignment ass = (Assignment)i.next();
				stop = Math.min(stop,ass.getEarliestStop());
			}
		}
		return stop;
	}

	public void setCompletedThrough(long completedThrough) {
		completedThrough = DateTime.closestDate(completedThrough);
		completedThrough = Math.min(completedThrough,getEnd());
		if (completedThrough == getCompletedThrough())
			return;

		Iterator i = getAssignments().iterator();
		Assignment assignment;
		long computedActualStart = Long.MAX_VALUE;
		long assignmentActualStart;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.setCompletedThrough(completedThrough);
			assignmentActualStart = assignment.getActualStart();
			if (assignmentActualStart != 0 && assignmentActualStart < computedActualStart)
				computedActualStart = assignmentActualStart;
		}
		if (computedActualStart == Long.MAX_VALUE)
			computedActualStart = 0;
		setActualStart(computedActualStart);
		assignParentActualDatesFromChildren();

		// if % complete went down to 0, then the plan changed and need to recalculate all.
		if (computedActualStart == 0) {
			getDocument().getObjectEventManager().fireUpdateEvent(this, this,
					Configuration.getFieldFromId("Field.start"));
		} else {
			getProject().fireScheduleChanged(this, ScheduleEvent.ACTUAL, this);
		}
	}
	public long getFinishOffset() {
		return EarnedValueCalculator.getInstance().getFinishOffset(this);
	}

	public long getStartOffset() {
		return EarnedValueCalculator.getInstance().getStartOffset(this);
	}
	public ImageLink getBudgetStatusIndicator() {
		return EarnedValueCalculator.getInstance().getBudgetStatusIndicator(getCpi(null));
	}

	public ImageLink getScheduleStatusIndicator() {
		return EarnedValueCalculator.getInstance().getScheduleStatusIndicator(getSpi(null));
	}
	public Object backupDetail(){
		return backupDetail(null);
	}
	public Object backupDetail(Object snapshotId) {
		TaskSnapshot snapshot=(TaskSnapshot)((snapshotId==null)?getCurrentSnapshot():getSnapshot(snapshotId));
		TaskSnapshotBackup snapshotBackup=TaskSnapshotBackup.backup(snapshot,/*snapshotId!=null*/true);
		TaskBackup backup=new TaskBackup();
		backup.snapshot=snapshotBackup;
		backup.windowEarlyFinish=windowEarlyFinish;
		backup.windowEarlyStart=windowEarlyStart;
		backup.windowLateFinish=windowLateFinish;
		backup.windowLateStart=windowLateStart;
		backup.actualStart=actualStart;
		return backup;
	}

	public void restoreDetail(Object source,Object backup,boolean isChild) {
		restoreDetail(source, backup, isChild,(TaskSnapshot)getCurrentSnapshot());
	}
	public void restoreDetail(Object source,Object backup,boolean isChild,TaskSnapshot snapshot) {
		TaskBackup b=(TaskBackup)backup;
		windowEarlyFinish=b.windowEarlyFinish;
		windowEarlyStart=b.windowEarlyStart;
		windowLateFinish=b.windowLateFinish;
		windowLateStart=b.windowLateStart;
		actualStart=b.actualStart;
		TaskSnapshotBackup.restore(snapshot, b.snapshot);
		if (snapshot == getCurrentSnapshot()) {
			currentSchedule = snapshot.getCurrentSchedule();
			if (currentSchedule != null) {
				currentSchedule.initSerialized(this,TaskSchedule.CURRENT);
			}
		}
		if (!isChild) recalculate(source); //to send update event
	}

	private static abstract class ResultClosure implements Consumer<Object>{
		boolean result=false;
	}
	public boolean renumber(final boolean localOnly){
		ResultClosure c=new ResultClosure(){
			public void accept(Object arg0) {
                result|=((Assignment)arg0).renumber(localOnly);
			}
		};
		boolean r=c.result;
		forSnapshotsAssignments(c, true);
		return r|hasKey.renumber(localOnly);
	}


	public boolean isLocal() {
		return hasKey.isLocal();
	}

	public void setLocal(boolean local) {
		hasKey.setLocal(local);
	}
	public boolean isSlipped() {
		long bf = getBaselineFinish();
		return bf != 0 && getEnd() > bf;
	}
	public void setTaskAssignementAndPredsDirty() {
		setDirty(true);
		Iterator a = getAssignments().iterator();
		while (a.hasNext())
			((Assignment)a.next()).setDirty(true);
		Iterator d=getDependencyList(true).iterator();
		while (d.hasNext())
			((Dependency)d.next()).setDirty(false);

	}
	//			task.setDirty(false);
//	task.setLastSavedStart(task.getStart()); //
//	task.setLastSavedFinish(task.getEnd());
//	Iterator j = task.getAssignments().iterator();
//	while (j.hasNext())
//		((Assignment)j.next()).setDirty(false);
//	j=task.getDependencyList(true).iterator();
//	while (j.hasNext())
//		((Dependency)j.next()).setDirty(false);
//}
	
	
	//claur import shortcuts
	public void setCurrentScheduleStart(long start){
		getCurrentSchedule().setStart(start);
	}
	
	public void setCurrentScheduleFinish(long finish){
		getCurrentSchedule().setFinish(finish);
	}

}
