/*******************************************************************************
 * MIT License
 *
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
package com.microproject.exchange.mpx.converter;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import com.microproject.exchange.mpx.converter.type.MpxAccrueTypeConverter;
import com.microproject.exchange.mpx.converter.type.MpxPriorityConverter;
import com.microproject.exchange.mpx.converter.type.MpxSchedulingTypeConverter;
import com.microproject.exchange.converter.type.DateUTCConverter;
import com.microproject.datatype.DurationDefaults;
import com.microproject.model.calendar.WorkCalendar;
import com.microproject.model.calendar.WorkingCalendar;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Task;
import com.microproject.model.task.TaskSplitInterval;

import org.mpxj.Duration;
import org.mpxj.TaskMode;

/**
 * Converts an MPXJ Task into a microproject Task.
 * Only fields that exist on the microproject Task model are mapped; fields that
 * the microproject model does not carry (cost, external, ...) are intentionally
 * skipped. See issue #154 for the model-extension discussion.
 * @author Laurent Chretienneau
 */
public class MpxTaskConverter {
	private MpxCalendarConverter calendarConverter = new MpxCalendarConverter();

	public void from(org.mpxj.Task mpxTask, Task task, MpxImportState state) {
		DurationDefaults durationDefaults = task.getProject() == null
			? MpxProjectConverter.durationDefaults(state.getMpxProjectFile() == null ? null
				: state.getMpxProjectFile().getProjectProperties())
			: task.getProject().getDurationDefaults();
		WorkCalendar calendar;
		if (mpxTask.getCalendar() == null) {
			calendar = state.getProjectBaseCalendar();
		} else {
			calendar = state.getImportedCalendar(mpxTask.getCalendar());
			if (calendar == null) {
				calendar = WorkingCalendar.getStandardBasedInstance();
				calendar.setName(mpxTask.getName());
				calendarConverter.from(mpxTask.getCalendar(), (WorkingCalendar) calendar, state);
				state.registerImportedCalendar(calendar, mpxTask.getCalendar());
			}
		}
		task.setWorkCalendar(calendar);

		if (mpxTask.getName() != null)
			task.setName(mpxTask.getName());
		if (mpxTask.getWBS() != null)
			task.setWbs(mpxTask.getWBS());
		if (mpxTask.getNotes() != null)
			task.setNotes(mpxTask.getNotes());
		if (mpxTask.getID() != null)
			task.setId(mpxTask.getID().longValue());
		if (mpxTask.getUniqueID() != null)
			task.setUniqueId(mpxTask.getUniqueID().longValue());
		// Apply the imported dates before the source constraint. Task.setStart may
		// infer a product constraint when a date precedes the project's current
		// start; the source file's explicit constraint must remain authoritative.
		if (mpxTask.getEarnedValueMethod() != null)
			task.setEarnedValueMethod(mpxTask.getEarnedValueMethod().getValue());
		if (mpxTask.getMilestone())
			task.setMarkTaskAsMilestone(true);

		task.setCreated(MpxDates.toDate(mpxTask.getCreateDate()));
		task.setDeadline(toLong(mpxTask.getDeadline()));
		task.setLevelingDelay(toLong(mpxTask.getLevelingDelay()));
		task.setRecurringTask(mpxTask.getRecurring());
		task.setLevelingCanSplit(!mpxTask.getRecurring() && mpxTask.getLevelingCanSplit());
		task.setLevelAssignments(mpxTask.getLevelAssignments());

		// Keep the imported start as a scheduling constraint, but restore the current
		// schedule directly below. Editing setters can update assignments and trigger
		// critical-path calculations before the imported project is fully assembled.
		if (mpxTask.getStart() != null)
			task.setStart(toLong(mpxTask.getStart()));
		if (mpxTask.getConstraintType() != null) {
			try {
				task.setConstraintType(mpxTask.getConstraintType().getValue());
			} catch (com.microproject.field.FieldParseException e) {
				// leave default constraint type
			}
		}
		task.setConstraintDate(toLong(mpxTask.getConstraintDate()));
		if (mpxTask.getStart() != null)
			task.getCurrentSchedule().setStart(toLong(mpxTask.getStart()));
		if (mpxTask.getFinish() != null)
			task.getCurrentSchedule().setFinish(toLong(mpxTask.getFinish()));
		if (task instanceof NormalTask normalTask) {
			if (mpxTask.getActualStart() != null)
				normalTask.setActualStartNoEvent(toLong(mpxTask.getActualStart()));
			if (mpxTask.getDuration() != null) {
				long duration = MpxUtils.toProjectDuration(mpxTask.getDuration(), durationDefaults);
				normalTask.setRawDuration(duration);
				// An MPXJ Duration never carries the estimated bit; the stored MSPDI
				// Estimated field is authoritative (issue #887).
				normalTask.setEstimated(mpxTask.getEstimated());
			}
			if (mpxTask.getPercentageComplete() != null)
				normalTask.setImportedPercentComplete(toRatio(mpxTask.getPercentageComplete()));
		} else {
			if (mpxTask.getActualStart() != null)
				task.setActualStart(toLong(mpxTask.getActualStart()));
			if (mpxTask.getDuration() != null)
				task.setDuration(MpxUtils.toProjectDuration(mpxTask.getDuration(), durationDefaults));
			if (mpxTask.getPercentageComplete() != null)
				task.setPercentComplete(toRatio(mpxTask.getPercentageComplete()));
		}
		if (mpxTask.getPhysicalPercentComplete() != null)
			task.setPhysicalPercentComplete(toRatio(mpxTask.getPhysicalPercentComplete()));
		MpxCustomFieldsConverter.from(mpxTask, task.getCustomFields(),
			com.microproject.exchange.serialize.CustomFieldsMapper.getInstance().taskMaps, durationDefaults);
		if (task instanceof NormalTask) {
			NormalTask normalTask=(NormalTask)task;
			normalTask.setEffortDriven(mpxTask.getEffortDriven());
			if (mpxTask.getType() != null)
				normalTask.setSchedulingType(((Integer)new MpxSchedulingTypeConverter().from(mpxTask.getType())).intValue());
			if (mpxTask.getPriority() != null)
				normalTask.setPriority(((Integer)new MpxPriorityConverter().from(mpxTask.getPriority())).intValue());
			if (mpxTask.getFixedCost() != null)
				normalTask.setFixedCost(mpxTask.getFixedCost().doubleValue());
			if (mpxTask.getFixedCostAccrual() != null)
				normalTask.setFixedCostAccrual(((Integer)new MpxAccrueTypeConverter().from(mpxTask.getFixedCostAccrual())).intValue());
		}

		task.setInactiveTask(!mpxTask.getActive());
		task.setIgnoreWarnings(Boolean.TRUE.equals(mpxTask.get(org.mpxj.TaskField.IGNORE_WARNINGS)));
		task.setManuallyScheduled(mpxTask.getTaskMode() == TaskMode.MANUALLY_SCHEDULED);

	}

	/**
	 * Imports task gaps exposed by MPXJ as gaps in the union of assignment work
	 * periods. Calendar nonworking time between those periods is excluded by
	 * moving the gap start to the next working instant.
	 */
	public static void importWorkSplits(org.mpxj.Task source, NormalTask target, MpxImportState state) {
		List<org.mpxj.LocalDateTimeRange> workPeriods = source.getWorkSplits();
		if (workPeriods == null || workPeriods.size() < 2)
			return;
		long taskStart = target.getStart();
		long sourceFinish = toLong(source.getFinish());
		WorkCalendar calendar = target.getEffectiveWorkCalendar();
		List<TaskSplitInterval> gaps = new ArrayList<>();
		for (int index = 1; index < workPeriods.size(); index++) {
			long previousFinish = toLong(workPeriods.get(index - 1).getEnd());
			long nextStart = toLong(workPeriods.get(index).getStart());
			long gapStart = calendar.adjustInsideCalendar(previousFinish, false);
			if (nextStart > gapStart)
				gaps.add(new TaskSplitInterval(gapStart - taskStart, nextStart - taskStart));
		}
		if (!gaps.isEmpty()) {
			target.restoreImportedTaskSplitIntervals(gaps);
			long sourceStart = taskStart;
			state.addPostScheduleValidation(() -> {
				target.getCurrentSchedule().setStart(sourceStart);
				target.getCurrentSchedule().setFinish(sourceFinish);
			});
		}
	}

	private static long toLong(java.time.LocalDateTime d) {
		return toLong(MpxDates.toDate(d));
	}

	private static long toLong(Date d) {
		return DateUTCConverter.toModelTime(d);
	}

	private static double toRatio(Number percentage) {
		return percentage == null ? 0.0 : percentage.doubleValue() / 100.0;
	}

	private static long toLong(Duration d) {
		return MpxUtils.toMillis(d);
	}
}
