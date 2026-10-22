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

import java.util.IdentityHashMap;
import java.util.Map;

import com.microproject.datatype.Duration;
import com.microproject.field.FieldContext;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.undo.ScheduleEdit;
import com.microproject.util.DateTime;

public final class TaskSheetScheduleWorkflow {
	public static final String START_FIELD_ID = "Field.start";
	public static final String FINISH_FIELD_ID = "Field.finish";
	public static final String DURATION_FIELD_ID = "Field.duration";
	public static final String PERCENT_COMPLETE_FIELD_ID = "Field.percentComplete";

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

	/** Applies an explicit task-sheet percent entry while retaining task-duration semantics. */
	public static boolean applyTaskPercentComplete(Object object, String fieldId, FieldContext context, Object value) {
		if (!FieldContext.isTaskSheetUpdate(context) || !PERCENT_COMPLETE_FIELD_ID.equals(fieldId)
				|| !(object instanceof NormalTask task) || !(value instanceof Number percent))
			return false;
		double enteredPercent = percent.doubleValue();
		if (!Double.isFinite(enteredPercent))
			throw new IllegalArgumentException("Percent complete must be finite");
		task.setPercentComplete(enteredPercent);
		if (!task.isWbsParent())
			task.setImportedPercentComplete(enteredPercent);
		return true;
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
		runAtomicScheduleEdit(task, () -> applyStartMutation(task, start));
	}

	private static void runAtomicScheduleEdit(Task task, Runnable edit) {
		Project project = task.getProject();
		if (project == null) {
			edit.run();
			return;
		}
		ProjectTaskSnapshot snapshot = ProjectTaskSnapshot.capture(project);
		java.util.Map<Task, Integer> calculationStates = new java.util.IdentityHashMap<>();
		for (Task affected : project.getTaskList())
			calculationStates.put(affected, affected.getCalculationStateCount());
		project.runAtomicModelCommand(markApplied -> {
			markApplied.run();
			try {
				edit.run();
			} catch (RuntimeException | Error failure) {
				try {
					snapshot.restore(project, TaskSheetScheduleWorkflow.class);
					for (var entry : calculationStates.entrySet())
						entry.getKey().setCalculationStateCount(entry.getValue());
				} catch (RuntimeException | Error rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			}
			return null;
		}, ignored -> project.getTasks().contains(task), ignored -> null);
	}

	private static void applyStartMutation(Task task, long start) {
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
		runAtomicScheduleEdit(task, () -> applyFinishMutation(task, finish));
	}

	private static void applyFinishMutation(Task task, long finish) {
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
		Project project = task.getProject();
		ProjectTaskSnapshot before = project == null ? null : ProjectTaskSnapshot.capture(project);
		Map<Task, Integer> beforeCalculationStates = captureCalculationStates(project);
		runAtomicScheduleEdit(task, () -> {
			applyDurationMutation(task, duration);
			if (project != null && project.getTasks().contains(task))
				ScheduleEdit.postProjectSnapshotEdit(project, before, beforeCalculationStates,
					captureCalculationStates(project), TaskSheetScheduleWorkflow.class);
		});
	}

	private static Map<Task, Integer> captureCalculationStates(Project project) {
		Map<Task, Integer> states = new IdentityHashMap<>();
		if (project != null)
			for (Task affected : project.getTaskList()) states.put(affected, affected.getCalculationStateCount());
		return states;
	}

	private static void applyDurationMutation(NormalTask task, long duration) {
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
			task.getProject().recalculate();
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
			task.setScheduleConstraint(ConstraintType.Kind.SNLT, start);
		} else {
			task.setScheduleConstraint(ConstraintType.Kind.SNET, start);
		}
	}

	private static void updateFinishConstraint(Task task, long finish) {
		long projectFinish = task.getProject().getEnd();
		if (projectFinish != 0L && projectFinish < finish) {
			task.setScheduleConstraint(ConstraintType.Kind.FNET, finish);
		} else {
			task.setScheduleConstraint(ConstraintType.Kind.FNLT, finish);
		}
	}

}
