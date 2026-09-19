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

import java.util.Collection;
import java.util.Iterator;

import com.microproject.datatype.Duration;
import com.microproject.field.FieldContext;
import com.microproject.grouping.core.Node;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.util.DateTime;

public final class TaskSheetScheduleWorkflow {
	public static final String START_FIELD_ID = "Field.start";
	public static final String FINISH_FIELD_ID = "Field.finish";
	public static final String DURATION_FIELD_ID = "Field.duration";

	private TaskSheetScheduleWorkflow() {
	}

	public static boolean isScheduleField(String fieldId) {
		return START_FIELD_ID.equals(fieldId) || FINISH_FIELD_ID.equals(fieldId) || DURATION_FIELD_ID.equals(fieldId);
	}

	public static boolean usesSummaryDisplay(Object object, boolean summaryNode, FieldContext context, String fieldId) {
		if (!FieldContext.isTaskSheetUpdate(context) || !isScheduleField(fieldId))
			return false;
		if (object instanceof Project)
			return true;
		return summaryNode && object instanceof Task;
	}

	public static Long getDisplayValue(Object object, String fieldId) {
		if (object instanceof Project)
			return getProjectDisplayValue((Project) object, fieldId);
		if (object instanceof Task)
			return getTaskDisplayValue((Task) object, fieldId);
		return null;
	}

	public static boolean apply(Object object, String fieldId, long value) {
		if (object instanceof Project project) {
			if (START_FIELD_ID.equals(fieldId)) {
				applyProjectStart(project, value);
				return true;
			}
			if (FINISH_FIELD_ID.equals(fieldId)) {
				applyProjectFinish(project, value);
				return true;
			}
			if (DURATION_FIELD_ID.equals(fieldId)) {
				applyProjectDuration(project, value);
				return true;
			}
			return false;
		}
		if (object instanceof NormalTask normalTask && DURATION_FIELD_ID.equals(fieldId)) {
			applyDuration(normalTask, value);
			return true;
		}
		if (object instanceof Task task) {
			if (START_FIELD_ID.equals(fieldId)) {
				applyStart(task, value);
				return true;
			}
			if (FINISH_FIELD_ID.equals(fieldId)) {
				applyFinish(task, value);
				return true;
			}
		}
		return false;
	}

	private static Long getTaskDisplayValue(Task task, String fieldId) {
		RollupSpan rollup = task.calculateRollupSpan();
		SummaryEnvelope envelope = task.getSummaryEnvelope();
		if (START_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualStart() ? envelope.getManualStart() : Long.valueOf(rollup.getStart());
		}
		if (FINISH_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualFinish() ? envelope.getManualFinish() : Long.valueOf(rollup.getFinish());
		}
		if (DURATION_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualDuration() ? envelope.getManualDuration() : Long.valueOf(rollup.getDuration());
		}
		return null;
	}

	private static Long getProjectDisplayValue(Project project, String fieldId) {
		RollupSpan rollup = project.calculateRollupSpan();
		SummaryEnvelope envelope = project.getSummaryEnvelope();
		if (START_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualStart() ? envelope.getManualStart() : Long.valueOf(rollup.getStart());
		}
		if (FINISH_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualFinish() ? envelope.getManualFinish() : Long.valueOf(rollup.getFinish());
		}
		if (DURATION_FIELD_ID.equals(fieldId)) {
			return envelope.hasManualDuration() ? envelope.getManualDuration() : Long.valueOf(rollup.getDuration());
		}
		return null;
	}

	static void applyStart(Task task, long start) {
		if (task.isWbsParent()) {
			task.getSummaryEnvelope().setManualStart(task.getEffectiveWorkCalendar().adjustInsideCalendar(start, false));
			return;
		}
		if (!(task instanceof NormalTask normalTask) || task.getActualStart() != 0)
			return;

		WorkCalendar calendar = task.getEffectiveWorkCalendar();
		long normalizedStart = calendar.adjustInsideCalendar(start, false);
		long duration = Duration.millis(task.getDuration());
		long normalizedEnd = (duration == 0L) ? normalizedStart : calendar.add(normalizedStart, duration, false);
		if (!hasAssignedWork(normalTask)) {
			normalTask.setManualDates(normalizedStart, normalizedEnd);
			normalTask.setRawDuration(Duration.setAsEstimated(duration, normalTask.isEstimated()));
			normalTask.markAllDependentTasksAsNeedingRecalculation(true);
			normalTask.assignParentActualDatesFromChildren();
		} else {
			normalTask.moveInterval(task, normalizedStart, normalizedEnd, new ScheduleInterval(task.getStart(), task.getEnd()), false);
		}
		updateStartConstraint(task, normalizedStart);
	}

	static void applyFinish(Task task, long finish) {
		if (task.isWbsParent()) {
			task.getSummaryEnvelope().setManualFinish(DateTime.closestDate(finish));
			return;
		}
		if (!(task instanceof NormalTask normalTask))
			return;

		WorkCalendar calendar = task.getEffectiveWorkCalendar();
		long normalizedFinish = DateTime.closestDate(finish);
		long duration = Duration.millis(task.getDuration());
		long normalizedStart = (duration == 0L) ? normalizedFinish : calculateStartFromFinish(calendar, normalizedFinish, duration);
		if (!hasAssignedWork(normalTask)) {
			normalTask.setManualDates(normalizedStart, normalizedFinish);
			normalTask.setRawDuration(Duration.setAsEstimated(duration, normalTask.isEstimated()));
			normalTask.markAllDependentTasksAsNeedingRecalculation(true);
			normalTask.assignParentActualDatesFromChildren();
		} else {
			normalTask.moveInterval(task, normalizedStart, normalizedFinish, new ScheduleInterval(task.getStart(), task.getEnd()), false);
		}
		updateFinishConstraint(task, normalizedFinish);
	}

	static void applyDuration(NormalTask task, long duration) {
		if (task.isWbsParent()) {
			task.getSummaryEnvelope().setManualDuration(Duration.millis(duration));
			return;
		}
		task.setDuration(duration);
		// A newly entered manual task has no real assignments.  Its raw duration
		// is updated by setDuration(), but the manual schedule can subsequently
		// retain its old zero-length interval.  Keep the task-table fields as one
		// atomic schedule edit: Duration changes Finish from the current Start.
		if (!hasAssignedWork(task)) {
			WorkCalendar calendar = task.getEffectiveWorkCalendar();
			long start = task.getCurrentSchedule().getStart();
			if (start == 0L)
				start = task.getProject().getStart();
			start = calendar.adjustInsideCalendar(start, false);
			long normalizedDuration = Duration.millis(duration);
			task.setManualDates(start, normalizedDuration == 0L ? start : calendar.add(start, normalizedDuration, false));
			task.markAllDependentTasksAsNeedingRecalculation(true);
			task.assignParentActualDatesFromChildren();
		}
	}

	/** A task without assignments is as directly editable as one with only the unassigned placeholder. */
	private static boolean hasAssignedWork(NormalTask task) {
		return !task.getAssignments().isEmpty() && task.hasRealAssignments();
	}

	static void applyProjectStart(Project project, long start) {
		project.getSummaryEnvelope().setManualStart(project.getEffectiveWorkCalendar().adjustInsideCalendar(start, false));
	}

	static void applyProjectFinish(Project project, long finish) {
		project.getSummaryEnvelope().setManualFinish(DateTime.closestDate(finish));
	}

	static void applyProjectDuration(Project project, long duration) {
		project.getSummaryEnvelope().setManualDuration(Duration.millis(duration));
	}

	private static long calculateStartFromFinish(WorkCalendar calendar, long finish, long duration) {
		long start = calendar.add(finish, -duration, true);
		for (int i = 0; i < 5; i++) {
			long scheduledDuration = calendar.compare(finish, start, false);
			long difference = scheduledDuration - duration;
			if (difference == 0L)
				break;
			start += difference;
		}
		return start;
	}

	private static void updateStartConstraint(Task task, long start) {
		long projectStart = task.getProject().getStart();
		if (projectStart > start) {
			task.setScheduleConstraint(ConstraintType.SNLT, start);
		} else {
			task.setScheduleConstraint(ConstraintType.SNET, start);
		}
	}

	private static void updateFinishConstraint(Task task, long finish) {
		long projectFinish = task.getProject().getEnd();
		if (projectFinish != 0L && projectFinish < finish) {
			task.setScheduleConstraint(ConstraintType.FNET, finish);
		} else {
			task.setScheduleConstraint(ConstraintType.FNLT, finish);
		}
	}

	static RollupSpan calculateTaskRollup(Task task) {
		if (!task.isWbsParent()) {
			long start = task.getCurrentSchedule().getStart();
			long finish = task.getCurrentSchedule().getFinish();
			long duration = task.getEffectiveWorkCalendar().compare(finish, start, false);
			return new RollupSpan(start, finish, duration);
		}

		Collection children = task.getWbsChildrenNodes();
		if (children == null || children.isEmpty()) {
			long start = task.getCurrentSchedule().getStart();
			long finish = task.getCurrentSchedule().getFinish();
			long duration = task.getEffectiveWorkCalendar().compare(finish, start, false);
			return new RollupSpan(start, finish, duration);
		}

		long start = Long.MAX_VALUE;
		long finish = Long.MIN_VALUE;
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			Object current = ((Node) iterator.next()).getImpl();
			if (!(current instanceof Task childTask))
				continue;
			RollupSpan childRollup = childTask.calculateRollupSpan();
			if (childRollup.getStart() != 0L)
				start = Math.min(start, childRollup.getStart());
			if (childRollup.getFinish() != 0L)
				finish = Math.max(finish, childRollup.getFinish());
		}

		if (start == Long.MAX_VALUE || finish == Long.MIN_VALUE) {
			long fallbackStart = task.getCurrentSchedule().getStart();
			long fallbackFinish = task.getCurrentSchedule().getFinish();
			long fallbackDuration = task.getEffectiveWorkCalendar().compare(fallbackFinish, fallbackStart, false);
			return new RollupSpan(fallbackStart, fallbackFinish, fallbackDuration);
		}

		long duration = task.getEffectiveWorkCalendar().compare(finish, start, false);
		return new RollupSpan(start, finish, duration);
	}

	static RollupSpan calculateProjectRollup(Project project) {
		Collection children = project.getTaskModel().getChildren(null);
		if (children == null || children.isEmpty()) {
			long duration = project.getEffectiveWorkCalendar().compare(project.getEnd(), project.getStart(), false);
			return new RollupSpan(project.getStart(), project.getEnd(), duration);
		}

		long start = Long.MAX_VALUE;
		long finish = Long.MIN_VALUE;
		Iterator iterator = children.iterator();
		while (iterator.hasNext()) {
			Object current = ((Node) iterator.next()).getImpl();
			if (!(current instanceof Task task))
				continue;
			RollupSpan childRollup = task.calculateRollupSpan();
			if (childRollup.getStart() != 0L)
				start = Math.min(start, childRollup.getStart());
			if (childRollup.getFinish() != 0L)
				finish = Math.max(finish, childRollup.getFinish());
		}

		if (start == Long.MAX_VALUE || finish == Long.MIN_VALUE) {
			long duration = project.getEffectiveWorkCalendar().compare(project.getEnd(), project.getStart(), false);
			return new RollupSpan(project.getStart(), project.getEnd(), duration);
		}

		long duration = project.getEffectiveWorkCalendar().compare(finish, start, false);
		return new RollupSpan(start, finish, duration);
	}
}
