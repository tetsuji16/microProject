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
import java.util.Objects;

import com.microproject.core.pm.exchange.converters.type.DateUTCConverter;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.TimeUnit;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.task.Project;

import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.ScheduleFrom;

/**
 * Converts MPXJ ProjectProperties into a microproject Project header.
 * Only fields carried by the microproject Project model are mapped; fields the
 * model does not expose are intentionally skipped (see issue #154).
 * @author Laurent Chretienneau
 */
public class MpxProjectConverter {

	public void from(ProjectProperties mpxProjectHeader, Iterable<ProjectCalendar> sourceCalendars, Project project,
		MpxImportState state) {
		project.setDurationDefaults(durationDefaults(mpxProjectHeader));
		if (mpxProjectHeader.getName() != null)
			project.setName(mpxProjectHeader.getName());
		if (mpxProjectHeader.getManager() != null)
			project.setManager(mpxProjectHeader.getManager());
		if (mpxProjectHeader.getComments() != null)
			project.setNotes(mpxProjectHeader.getComments());
		// MSPDI makes ScheduleFromStart the discriminator for the project boundary:
		// StartDate is authoritative for a forward schedule and FinishDate is
		// authoritative for a backward schedule.  Do not silently turn a
		// finish-scheduled MSP project into a start-scheduled project on import.
		boolean scheduleFromStart = mpxProjectHeader.getScheduleFrom() != ScheduleFrom.FINISH;
		project.setForward(scheduleFromStart);
		if (scheduleFromStart)
			project.setStartDate(toLong(mpxProjectHeader.getStartDate()));
		else
			project.setFinishDate(toLong(mpxProjectHeader.getFinishDate()));
		project.setStatusDate(toLong(mpxProjectHeader.getStatusDate()));

		WorkCalendar calendar = sourceDefaultCalendar(mpxProjectHeader, sourceCalendars, state);
		try {
			project.setBaseCalendar(calendar);
		} catch (com.microproject.configuration.CircularDependencyException e) {
			// ignore: a self-referential base calendar is not expected here
		}
	}

	public void from(ProjectProperties mpxProjectHeader, Project project, MpxImportState state) {
		from(mpxProjectHeader, null, project, state);
	}

	/** Resolves the source project's default calendar consistently before and after task import. */
	public static WorkCalendar sourceDefaultCalendar(ProjectProperties source,
		Iterable<ProjectCalendar> sourceCalendars, MpxImportState state) {
		ProjectCalendar sourceCalendar = source == null ? null : source.getDefaultCalendar();
		// Earlier serialized projects can contain duplicate calendar UIDs. Do not
		// guess by object or name in that ambiguous case; retain the established
		// imported standard/base calendar choice for backward compatibility.
		if (sourceCalendar != null && hasDuplicateUid(sourceCalendar, sourceCalendars))
			return state.getProjectBaseCalendar();
		WorkCalendar calendar = sourceCalendar == null ? null : state.getImportedCalendar(sourceCalendar);
		if (calendar == null && sourceCalendar != null)
			calendar = state.getMappedBaseCalendar(sourceCalendar.getName());
		return calendar == null ? state.getProjectBaseCalendar() : calendar;
	}

	private static boolean hasDuplicateUid(ProjectCalendar sourceCalendar, Iterable<ProjectCalendar> sourceCalendars) {
		Integer uid = sourceCalendar.getUniqueID();
		if (uid == null || sourceCalendars == null)
			return false;
		int matches = 0;
		for (ProjectCalendar calendar : sourceCalendars) {
			if (calendar != null && Objects.equals(uid, calendar.getUniqueID()) && ++matches > 1)
				return true;
		}
		return false;
	}

	public static DurationDefaults durationDefaults(ProjectProperties source) {
		DurationDefaults product = DurationDefaults.PRODUCT_DEFAULTS;
		if (source == null)
			return product;
		int durationUnit = source.getDefaultDurationUnits() == null ? product.durationEnteredIn()
			: MpxUtils.toProjectTimeUnit(source.getDefaultDurationUnits());
		int workUnit = source.getDefaultWorkUnits() == null ? product.workUnit()
			: MpxUtils.toProjectTimeUnit(source.getDefaultWorkUnits());
		return new DurationDefaults(
			source.getMinutesPerDay() == null ? product.minutesPerDay() : source.getMinutesPerDay().doubleValue(),
			source.getMinutesPerWeek() == null ? product.minutesPerWeek() : source.getMinutesPerWeek().doubleValue(),
			source.getDaysPerMonth() == null ? product.daysPerMonth() : source.getDaysPerMonth().doubleValue(),
			durationUnit, workUnit);
	}

	private static long toLong(Date d) {
		return DateUTCConverter.toModelTime(d);
	}
}
