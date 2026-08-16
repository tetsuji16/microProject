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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.Date;

import com.microproject.core.time.TimeUtil;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.task.Task;

import net.sf.mpxj.Duration;
import net.sf.mpxj.TaskMode;

/**
 * Converts an MPXJ Task into a microproject Task.
 * Only fields that exist on the microproject Task model are mapped; fields that
 * the microproject model does not carry (estimated, effortDriven, schedulingType,
 * priority, cost, fixedCost, fixedCostAccrual, external, ...) are intentionally
 * skipped. See issue #154 for the model-extension discussion.
 * @author Laurent Chretienneau
 */
public class MpxTaskConverter {
	private MpxCalendarConverter calendarConverter = new MpxCalendarConverter();

	public void from(net.sf.mpxj.Task mpxTask, Task task, MpxImportState state) {
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
		if (mpxTask.getConstraintType() != null) {
			try {
				task.setConstraintType(mpxTask.getConstraintType().getValue());
			} catch (com.microproject.field.FieldParseException e) {
				// leave default constraint type
			}
		}
		if (mpxTask.getEarnedValueMethod() != null)
			task.setEarnedValueMethod(mpxTask.getEarnedValueMethod().getValue());
		if (mpxTask.getMilestone())
			task.setMarkTaskAsMilestone(true);

		task.setCreated(mpxTask.getCreateDate());
		task.setDeadline(toLong(mpxTask.getDeadline()));
		task.setConstraintDate(toLong(mpxTask.getConstraintDate()));
		task.setLevelingDelay(toLong(mpxTask.getLevelingDelay()));

		task.setStart(toLong(mpxTask.getStart()));
		task.setEnd(toLong(mpxTask.getFinish()));
		task.setActualStart(toLong(mpxTask.getActualStart()));
		task.setActualFinish(toLong(mpxTask.getActualFinish()));
		task.setActualDuration(toLong(mpxTask.getActualDuration()));
		task.setRemainingDuration(toLong(mpxTask.getRemainingDuration()));
		task.setDuration(toLong(mpxTask.getDuration()));
		task.setPercentComplete(toRatio(mpxTask.getPercentageComplete()));
		if (mpxTask.getPhysicalPercentComplete() != null)
			task.setPhysicalPercentComplete(toRatio(mpxTask.getPhysicalPercentComplete()));

		task.setInactiveTask(!mpxTask.getActive());
		task.setManuallyScheduled(mpxTask.getTaskMode() == TaskMode.MANUALLY_SCHEDULED);

		// convert calendar
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
	}

	private static long toLong(Date d) {
		if (d == null)
			return 0L;
		return TimeUtil.addTimeZoneOffset(d.getTime());
	}

	private static double toRatio(Number percentage) {
		return percentage == null ? 0.0 : percentage.doubleValue() / 100.0;
	}

	private static long toLong(Duration d) {
		return MpxUtils.toMillis(d);
	}
}
