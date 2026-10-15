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
package com.microproject.server.data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mpxj.AccrueType;
import net.sf.mpxj.ConstraintType;
import net.sf.mpxj.DateRange;
import net.sf.mpxj.Day;
import net.sf.mpxj.DayType;
import net.sf.mpxj.EarnedValueMethod;
import net.sf.mpxj.FieldContainer;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.Priority;
import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectCalendarException;
import net.sf.mpxj.ProjectCalendarHours;
import net.sf.mpxj.ProjectCalendarWeek;
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.RecurringData;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.ScheduleFrom;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskMode;
import net.sf.mpxj.TaskType;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.TimephasedWork;
import net.sf.mpxj.common.DefaultTimephasedWorkContainer;
import net.sf.mpxj.WorkContour;
import net.sf.mpxj.mspdi.MSPDITimephasedWorkNormaliser;

import com.microproject.exchange.ImportedCalendarService;
import com.microproject.exchange.mpxj.MpxjApi;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.Rate;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.field.CustomFields;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.options.CalendarOption;
import com.microproject.options.ScheduleOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.costing.CostRate;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkWeekPeriod;
import com.microproject.pm.calendar.RecurringCalendarException;
import com.microproject.core.pm.exchange.converters.mpx.MpxCalendarRecurrenceConverter;
import com.microproject.core.time.TimeUtil;
import com.microproject.core.pm.exchange.converters.mpx.MpxCalendarDateConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxResourceAvailabilityConverter;
import com.microproject.core.pm.exchange.converters.mpx.MpxUtils;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.util.DateTime;
import com.microproject.util.MathUtils;
/**
 *
 */
public class MPXConverter {
	private static final Logger logger = Logger.getLogger(MPXConverter.class.getName());

	public static int nameFieldWidth = Configuration.getFieldFromId("Field.name").getTextWidth();
	private static final ThreadLocal<ExportIdAllocator> EXPORT_IDS = new ThreadLocal<ExportIdAllocator>();

	public static void beginExport(com.microproject.pm.task.Project project) {
		ExportIdAllocator allocator = new ExportIdAllocator();
		// Reserve persisted UIDs before assigning IDs to new, negative-ID tasks.
		for (com.microproject.pm.task.Task task : project.getTaskList()) {
			if (task.getUniqueId() > 0 && task.getUniqueId() <= Integer.MAX_VALUE) {
				allocator.get(IdNamespace.TASK, task.getUniqueId());
			}
		}
		EXPORT_IDS.set(allocator);
	}

	public static void endExport() {
		EXPORT_IDS.remove();
	}

	private static int exportId(IdNamespace namespace, long sourceId) {
		ExportIdAllocator allocator = EXPORT_IDS.get();
		if (allocator == null) {
			allocator = new ExportIdAllocator();
			EXPORT_IDS.set(allocator);
		}
		return allocator.get(namespace, sourceId);
	}


	public static void toMPXOptions(Project project, ProjectProperties projectHeader) {

		CalendarOption calendarOption = CalendarOption.getInstance();
		com.microproject.datatype.DurationDefaults defaults = project.getDurationDefaults();
		projectHeader.setMinutesPerDay(Integer.valueOf(mspdiInteger(defaults.minutesPerDay(), "MinutesPerDay")));
		projectHeader.setMinutesPerWeek(Integer.valueOf(mspdiInteger(defaults.minutesPerWeek(), "MinutesPerWeek")));
		projectHeader.setDaysPerMonth(Integer.valueOf(mspdiInteger(defaults.daysPerMonth(), "DaysPerMonth")));
		projectHeader.setDefaultDurationUnits(MpxUtils.toMpxTimeUnit(defaults.durationEnteredIn()));
		projectHeader.setDefaultWorkUnits(MpxUtils.toMpxTimeUnit(defaults.workUnit()));

		projectHeader.setDefaultStartTime(calendarOption.getDefaultStartTime().getTime());
		projectHeader.setDefaultEndTime(calendarOption.getDefaultEndTime().getTime());
	}

	private static int mspdiInteger(double value, String name) {
		if (!Double.isFinite(value) || value < 1.0D || value > Integer.MAX_VALUE || value != Math.rint(value))
			throw new IllegalArgumentException("Project " + name + " must be a positive whole number for MSPDI export");
		return (int) value;
	}

	public static void toMPXProject(Project project,ProjectProperties projectHeader) {
		projectHeader.setName(project.getName());
		projectHeader.setProjectTitle(project.getName());
		projectHeader.setComments(project.getNotes());
		projectHeader.setManager(project.getManager());
		projectHeader.setComments(removeInvalidChars(project.getNotes()));
		// MSPDI requires this flag to state which boundary is used to schedule
		// the project.  Writing both dates without it causes readers to assume a
		// forward schedule and loses a finish-scheduled project's semantics.
		projectHeader.setScheduleFrom(project.isForward() ? ScheduleFrom.START : ScheduleFrom.FINISH);
		projectHeader.setStartDate(toMpxDate(project.getStartDate()));
		projectHeader.setFinishDate(toMpxDate(project.getFinishDate()));
		projectHeader.setDefaultStartTime(CalendarOption.getInstance().getDefaultStartTime().getTime());
		projectHeader.setDefaultEndTime(CalendarOption.getInstance().getDefaultEndTime().getTime());

	}


	public static void toMpxCalendar(WorkingCalendar workCalendar,ProjectCalendar mpx) {
		mpx.setName(workCalendar.getName());
		mpx.setUniqueID(Integer.valueOf(exportId(IdNamespace.CALENDAR, workCalendar.getUniqueId())));

		for (int i = 0; i < 7; i++) {// MPX days go from SUNDAY=1 to SATURDAY=7
			WorkDay day= workCalendar.getDerivedWeekDay(i);
			ProjectCalendarHours mpxDay = null;
			Day d = Day.getInstance(i+1);
			if (day == null) {
				mpx.setCalendarDayType(d,DayType.DEFAULT); // claur
			} else {
				mpx.setWorkingDay(d,day.isWorking());
				if (day.isWorking()) {
					mpxDay = mpx.addCalendarHours(Day.getInstance(i+1));
					toMpxCalendarDay(day,mpxDay);
				}
			}
		}
		for (WorkWeekPeriod period : workCalendar.getEffectiveWorkWeekPeriods()) {
			ProjectCalendarWeek mpxWeek = mpx.addWorkWeek();
			mpxWeek.setName(period.getName());
			mpxWeek.setDateRange(new DateRange(
				MpxCalendarDateConverter.toMpxDate(period.getStart()),
				MpxCalendarDateConverter.toMpxDate(period.getEnd())));
			for (int i = 0; i < 7; i++) {
				Day mpxDay = Day.getInstance(i + 1);
				WorkDay workDay = period.getWeekDay(i);
				if (workDay == null) {
					mpxWeek.setCalendarDayType(mpxDay, DayType.DEFAULT);
				} else if (!workDay.isWorking()) {
					mpxWeek.setCalendarDayType(mpxDay, DayType.NON_WORKING);
				} else {
					mpxWeek.setCalendarDayType(mpxDay, DayType.WORKING);
					ProjectCalendarHours mpxHours = mpxWeek.addCalendarHours(mpxDay);
					toMpxCalendarDay(workDay, mpxHours);
				}
			}
		}

		MpxCalendarRecurrenceConverter recurrenceConverter = new MpxCalendarRecurrenceConverter();
		for (RecurringCalendarException recurring : workCalendar.getEffectiveRecurringExceptions()) {
			WorkDay template = recurring.getTemplate();
			RecurringData recurrence = recurrenceConverter.to(recurring.getRecurrence());
			ProjectCalendarException exception = mpx.addCalendarException(recurrence);
			if (template.getDescription() != null) exception.setName(template.getDescription());
			toMpxExceptionDay(template, exception);
		}

		WorkDay[] workDays=workCalendar.getExceptionDays();
		if (workDays!=null)
			for (int i=0;i<workDays.length;i++){
				if (workDays[i]==null||workDays[i].getStart()==0L||workDays[i].getStart()==Long.MAX_VALUE)
					continue;
				Date start = MpxCalendarDateConverter.toMpxDate(workDays[i].getStart());
				// Keep both bounds as calendar dates; MPXJ treats the exception end
				// date as inclusive and normalizes its time component.
				Date end = MpxCalendarDateConverter.toMpxDate(workDays[i].getEnd());
				ProjectCalendarException exception=mpx.addCalendarException(start,end); //claur
				if (workDays[i].getDescription() != null)
					exception.setName(workDays[i].getDescription());
				
				toMpxExceptionDay(workDays[i],exception);
				//exception.setWorking(workDays[i].isWorking()); //claur exception is working once it has at least one range
			}
		// Write effective working time into each calendar instead of creating
		// MPXJ parent links. This preserves calendar behavior and prevents a
		// malformed source hierarchy from forming a recursive derived-calendar graph.
	}

	public static  void toMpxCalendarDay(WorkDay day,ProjectCalendarHours mpxDay) {
		if  (day==null)
			return;
		WorkingHours workingHours=day.getWorkingHours();

		for (WorkRange range : workingHours.getIntervals()) { //claur
			if (range!=null)
				mpxDay.add(new DateRange(toMpxDate(range.getNormalizedStartTime().getTime()),
						toMpxDate(range.getNormalizedEndTime().getTime())));
		}
	}

	public static  void toMpxExceptionDay(WorkDay day,ProjectCalendarException mpxDay) {
		if  (day==null)
			return;
		WorkingHours workingHours=day.getWorkingHours();
		for (WorkRange range : workingHours.getIntervals()) {
			if (range != null)
				mpxDay.add(new DateRange(toMpxDate(range.getNormalizedStartTime().getTime()),
						toMpxDate(range.getNormalizedEndTime().getTime())));
		}
	}


	public static void toMPXResource(ResourceImpl projectlibreResource,Resource mpxResource) {
		toMPXResource(null, projectlibreResource, mpxResource);
	}

	public static void toMPXResource(Project project, ResourceImpl projectlibreResource, Resource mpxResource) {
		DurationDefaults defaults = project == null ? currentDurationDefaults() : project.getDurationDefaults();
		// Validate and install the full interval table before writing any other target fields.
		MpxResourceAvailabilityConverter.to(projectlibreResource.getAvailabilityTable(), mpxResource);
		mpxResource.setName(removeInvalidChars(projectlibreResource.getName()));
		mpxResource.setNotes(removeInvalidChars(projectlibreResource.getNotes()));
		mpxResource.setAccrueAt(AccrueType.getInstance(projectlibreResource.getAccrueAt()));
		for (int tableIndex = 0; tableIndex < net.sf.mpxj.CostRateTable.MAX_TABLES; tableIndex++) {
			com.microproject.pm.costing.CostRateTable sourceTable = projectlibreResource.getCostRateTableIfPresent(tableIndex);
			net.sf.mpxj.CostRateTable targetTable = new net.sf.mpxj.CostRateTable();
			if (sourceTable != null) {
				for (ValueObjectForInterval value : sourceTable.getList()) {
					CostRate rate = (CostRate) value;
					Date start = value.isFirst() ? net.sf.mpxj.common.DateHelper.START_DATE_NA
						: new Date(value.getStart());
					Date end = value.getEnd() >= com.microproject.util.DateTime.getMaxDate().getTime()
						? net.sf.mpxj.common.DateHelper.END_DATE_NA : new Date(value.getEnd());
					targetTable.add(new net.sf.mpxj.CostRateTableEntry(start, end,
						Double.valueOf(rate.getCostPerUse()), toMPXRate(rate.getStandardRate(), defaults),
						toMPXRate(rate.getOvertimeRate(), defaults)));
				}
			}
			// MPXJ and the domain both number tables A-E as 0-4.
			mpxResource.setCostRateTable(tableIndex, targetTable);
		}
		mpxResource.setGroup(projectlibreResource.getGroup());
		mpxResource.setEmailAddress(projectlibreResource.getEmailAddress());
		mpxResource.setGeneric(projectlibreResource.isGeneric()); // fix for 2024492

		mpxResource.setInitials(projectlibreResource.getInitials());
		int resourceId = projectlibreResource.isDefault() ? 0
			: exportId(IdNamespace.RESOURCE_ROW, projectlibreResource.getId());
		mpxResource.setID(resourceId);
		long uid = projectlibreResource.getExternalId(); // try using external id of one set
		if (uid <= 0)
			uid = projectlibreResource.getUniqueId();
		if (uid <= 0)
			uid = projectlibreResource.getId();
		// The unassigned singleton is a sentinel, not a new resource requiring an ID.
		mpxResource.setUniqueID(projectlibreResource.isDefault()
			? com.microproject.pm.resource.EnterpriseResource.UNASSIGNED_ID
			: exportId(IdNamespace.RESOURCE, uid));
		mpxResource.setMaxUnits(projectlibreResource.getMaximumUnits()*100);

		WorkingCalendar projectlibreCalendar = (WorkingCalendar)projectlibreResource.getWorkCalendar();
		if (projectlibreCalendar != null) { // there should be a calendar, except for the unassigned instance
			ProjectCalendar mpxCalendar = ImportedCalendarService.getInstance().findExportedCalendar(projectlibreCalendar);
			if (mpxCalendar == null) {
				try {
					mpxCalendar = mpxResource.addCalendar();
				} catch (MPXJException e) {
					logger.log(Level.WARNING, "Failed to add resource calendar", e);
					return;
				}
				toMpxCalendar(projectlibreCalendar,mpxCalendar);
			}
			mpxResource.setCalendar(mpxCalendar);
		}
		toMpxCustomFields(projectlibreResource.getCustomFields(),mpxResource, CustomFieldsMapper.getInstance().resourceMaps, defaults);


	}


	public static void toMpxCustomFields(CustomFields projectlibreFields,FieldContainer mpx, CustomFieldsMapper.Maps maps) {
		toMpxCustomFields(projectlibreFields, mpx, maps, currentDurationDefaults());
	}

	private static void toMpxCustomFields(CustomFields projectlibreFields, FieldContainer mpx,
		CustomFieldsMapper.Maps maps, DurationDefaults defaults) {
		for (int i = 0; i < maps.costMap.length; i++) {
			double cost = projectlibreFields.getCustomCost(i);
			if (cost != 0.0D)
				mpx.set(maps.costMap[i],Double.valueOf(cost));
		}
		for (int i = 0; i < maps.dateMap.length; i++) {
			long d = projectlibreFields.getCustomDate(i);
			if (d != 0)
				mpx.set(maps.dateMap[i],new Date(d));
		}
		for (int i = 0; i < maps.durationMap.length; i++) {
			long d = projectlibreFields.getCustomDuration(i);
			if (Duration.millis(d) != 0)
				mpx.set(maps.durationMap[i],toMPXDuration(d, defaults));
		}
		for (int i = 0; i < maps.finishMap.length; i++) {
			long d = projectlibreFields.getCustomFinish(i);
			if (d != 0)
				mpx.set(maps.finishMap[i],new Date(d));
		}
		for (int i = 0; i < maps.flagMap.length; i++) {
			boolean b = projectlibreFields.getCustomFlag(i);
			if (b)
				mpx.set(maps.flagMap[i],Boolean.TRUE);
		}
		for (int i = 0; i < maps.numberMap.length; i++) {
			double n = projectlibreFields.getCustomNumber(i);
			if (n != 0.0D)
				mpx.set(maps.numberMap[i],Double.valueOf(n));
		}
		for (int i = 0; i < maps.startMap.length; i++) {
			long d = projectlibreFields.getCustomStart(i);
			if (d != 0)
				mpx.set(maps.startMap[i],new Date(d));
		}
		for (int i = 0; i < maps.textMap.length; i++) {
			String s = projectlibreFields.getCustomText(i);
			if (s != null) {
				mpx.set(maps.textMap[i],MPXConverter.removeInvalidChars(s));
			}
		}
	}

	public static void toMPXAssignment(Assignment assignment, ResourceAssignment mpxAssignment) {
//		long work = assignment.isDefault() ? 0 : assignment.getWork(null); // microsoft considers no work on default assignments
		long work = assignment.getWork(null); // microsoft considers no work on default assignments
		if (assignment.getStart() != 0L)
			mpxAssignment.setStart(toMpxDate(assignment.getStart()));
		if (assignment.getEnd() != 0L)
			mpxAssignment.setFinish(toMpxDate(assignment.getEnd()));
		DurationDefaults defaults = assignment.getTask() == null || assignment.getTask().getProject() == null
			? currentDurationDefaults() : assignment.getTask().getProject().getDurationDefaults();
		mpxAssignment.setWork(MPXConverter.toMPXDuration(work, defaults));
		mpxAssignment.setActualWork(MPXConverter.toMPXDuration(assignment.getActualWork(null), defaults));
		long assignmentWork = Duration.millis(assignment.getWork(null));
		double percentWorkComplete = assignmentWork == 0L ? 0.0d
				: ((double) Duration.millis(assignment.getActualWork(null))) / assignmentWork;
		mpxAssignment.setPercentageWorkComplete(percentWorkComplete * 100.0d);
		if (assignment.getActualStart() != 0L)
			mpxAssignment.setActualStart(toMpxDate(assignment.getActualStart()));
		if (assignment.getActualFinish() != 0L)
			mpxAssignment.setActualFinish(toMpxDate(assignment.getActualFinish()));
    	mpxAssignment.setUnits(MathUtils.roundToDecentPrecision(assignment.getUnits()*100.0D));
		mpxAssignment.setRemainingWork(MPXConverter.toMPXDuration(assignment.getRemainingWork(), defaults)); //2007
    	long delay = Duration.millis(assignment.getDelay());
    	if (delay != 0) {
			mpxAssignment.setDelay(MPXConverter.toMPXDuration(assignment.getDelay(), defaults));
    	}

		long levelingDelay = Duration.millis(assignment.getLevelingDelay());
		if (levelingDelay != 0) {
			mpxAssignment.setLevelingDelay(MPXConverter.toMPXLevelingDelay(assignment.getLevelingDelay()));
		}


		mpxAssignment.setWorkContour(MpxjApi.workContour(assignment.getWorkContourType()));
		if (assignment.getWorkContour().isPersonal())
			toMPXTimephasedWork(assignment, mpxAssignment);
		for (int snapshotId = 0; snapshotId <= 10; snapshotId++) {
			Assignment.BaselineValues baseline = assignment.getBaselineValues(snapshotId);
			Assignment baselineAssignment = assignment.getBaselineAssignment(Integer.valueOf(snapshotId), false);
			if (baseline == null && baselineAssignment == null)
				continue;
			Date baselineStart = baseline == null || baseline.start() == null ? null
				: toMpxDate(baseline.start().longValue());
			Date baselineFinish = baseline == null || baseline.finish() == null ? null
				: toMpxDate(baseline.finish().longValue());
			net.sf.mpxj.Duration baselineWork = baseline == null || baseline.work() == null
				? null : toMPXDuration(baseline.work().longValue(), defaults);
			if (snapshotId == 0) {
				mpxAssignment.setBaselineStart(baselineStart);
				mpxAssignment.setBaselineFinish(baselineFinish);
				mpxAssignment.setBaselineWork(baselineWork);
			} else {
				mpxAssignment.setBaselineStart(snapshotId, baselineStart);
				mpxAssignment.setBaselineFinish(snapshotId, baselineFinish);
				mpxAssignment.setBaselineWork(snapshotId, baselineWork);
			}
		}


	}

	static List<TimephasedWork> baselineTimephasedWorkRows(Assignment baseline) {
		List<TimephasedWork> intervals = new ArrayList<>();
		long total = appendProfileTimephasedWork(intervals, baseline.getWorkContour().getContourBuckets(),
			baseline.getStart(), baseline.getDuration(), baseline.getEffectiveWorkCalendar());
		if (total != baseline.getWork(null))
			throw new IllegalArgumentException("Assignment baseline contour does not match its work total");
		return List.copyOf(intervals);
	}

	private static void toMPXTimephasedWork(Assignment assignment, ResourceAssignment mpxAssignment) {
		List<TimephasedWork> actual = new ArrayList<>();
		List<TimephasedWork> remaining = new ArrayList<>();
		if (assignment.hasImportedTimephasedWork()) {
			long actualTotal = appendProfileTimephasedWork(actual, assignment.getContour(Assignment.ACTUAL_WORK),
				assignment.getStart(), assignment.getDuration(), assignment.getEffectiveWorkCalendar());
			long remainingTotal = appendProfileTimephasedWork(remaining,
				assignment.getContour(Assignment.REMAINING_WORK), assignment.getStart(), assignment.getDuration(),
				assignment.getEffectiveWorkCalendar());
			if (actualTotal != assignment.getActualWork(null)
					|| remainingTotal != assignment.getRemainingWork()
					|| Math.addExact(actualTotal, remainingTotal) != assignment.getWork(null))
				throw new IllegalArgumentException("Imported timephased profiles do not match assignment totals");
			var normaliser = new MSPDITimephasedWorkNormaliser();
			mpxAssignment.setTimephasedActualWork(new DefaultTimephasedWorkContainer(mpxAssignment, normaliser, actual, false));
			mpxAssignment.setTimephasedWork(new DefaultTimephasedWorkContainer(mpxAssignment, normaliser, remaining, false));
			return;
		}
		long cursor = calendarStart(assignment.getStart(), assignment.getEffectiveWorkCalendar());
		long stop = assignment.getStop();
		long actualTotal = 0L;
		long remainingTotal = 0L;
		WorkCalendar calendar = assignment.getEffectiveWorkCalendar();
		for (var bucket : assignment.getWorkContour().getContourBuckets()) {
			long duration = bucket.getBucketDuration(assignment.getDuration());
			long end = calendar.add(cursor, duration, true);
			long work = (long) (bucket.getUnits() * duration);
			long actualDuration = stop <= cursor ? 0L : Math.min(duration, calendar.compare(Math.min(stop, end), cursor, false));
			long actualWork = (long) (bucket.getUnits() * actualDuration);
			long remainingWork = work - actualWork;
			if (duration > 0L && end <= cursor)
				throw new IllegalArgumentException("Cannot export a personal contour bucket without a calendar interval");
			if (actualDuration > 0L)
				appendDailyTimephasedWork(actual, cursor, calendar.add(cursor, actualDuration, true), actualWork, calendar);
			if (duration > actualDuration)
				appendDailyTimephasedWork(remaining, calendar.adjustInsideCalendar(calendar.add(cursor, actualDuration, true), false),
					end, remainingWork, calendar);
			actualTotal = Math.addExact(actualTotal, actualWork);
			remainingTotal = Math.addExact(remainingTotal, remainingWork);
			cursor = calendarStart(end, calendar);
		}
		if (actualTotal != assignment.getActualWork(null)
				|| Math.addExact(actualTotal, remainingTotal) != assignment.getWork(null))
			throw new IllegalArgumentException("Personal contour does not match assignment work/actual-work totals");
		var normaliser = new MSPDITimephasedWorkNormaliser();
		mpxAssignment.setTimephasedActualWork(new DefaultTimephasedWorkContainer(mpxAssignment, normaliser, actual, false));
		mpxAssignment.setTimephasedWork(new DefaultTimephasedWorkContainer(mpxAssignment, normaliser, remaining, false));
	}

	private static long appendProfileTimephasedWork(List<TimephasedWork> destination,
		com.microproject.pm.assignment.contour.AbstractContourBucket[] buckets, long start, long assignmentDuration,
		WorkCalendar calendar) {
		long cursor = calendarStart(start, calendar);
		long total = 0L;
		for (var bucket : buckets) {
			long duration = bucket.getBucketDuration(assignmentDuration);
			long end = calendar.add(cursor, duration, true);
			long work = Math.round(bucket.getUnits() * duration);
			if (duration > 0L)
				appendDailyTimephasedWork(destination, cursor, end, work, calendar);
			total = Math.addExact(total, work);
			cursor = calendarStart(end, calendar);
		}
		return total;
	}

	private static long calendarStart(long date, WorkCalendar calendar) {
		return calendar.adjustInsideCalendar(date, false);
	}

	static void appendDailyTimephasedWork(List<TimephasedWork> destination, long start, long end, long totalWork,
		WorkCalendar calendar) {
		long cursor = calendarStart(start, calendar);
		long totalDuration = calendar.compare(end, cursor, false);
		if (totalDuration <= 0L) {
			throw new IllegalArgumentException("Cannot export a personal-contour interval without working duration");
		}
		long cumulativeDuration = 0L;
		long previousWork = 0L;
		while (cursor < end) {
			long nextDay = DateTime.nextDay(DateTime.dayFloor(cursor));
			long segmentEnd = Math.min(end, nextDay);
			long workingEnd = segmentEnd == nextDay ? calendar.adjustInsideCalendar(segmentEnd, true) : segmentEnd;
			long duration = calendar.compare(workingEnd, cursor, false);
			if (duration > 0L) {
				cumulativeDuration = Math.addExact(cumulativeDuration, duration);
				long cumulativeWork = cumulativeDuration >= totalDuration ? totalWork
					: Math.round(totalWork * (cumulativeDuration / (double) totalDuration));
				long work = cumulativeWork - previousWork;
				destination.add(timephasedWork(cursor, workingEnd, work));
				previousWork = cumulativeWork;
			}
			if (segmentEnd <= cursor)
				throw new IllegalArgumentException("Cannot advance a personal-contour timephased interval");
			cursor = segmentEnd < end ? calendarStart(segmentEnd, calendar) : segmentEnd;
		}
		if (cumulativeDuration != totalDuration || previousWork != totalWork)
			throw new IllegalArgumentException("Cannot export a personal-contour interval without exact daily work totals");
	}

	private static TimephasedWork timephasedWork(long start, long end, long work) {
		TimephasedWork result = new TimephasedWork();
		result.setStart(toMpxDate(start));
		result.setFinish(toMpxDate(end));
		net.sf.mpxj.Duration amount = net.sf.mpxj.Duration.getInstance(
			work / (double) (60L * 60L * 1000L), TimeUnit.HOURS);
		result.setTotalAmount(amount);
		result.setAmountPerDay(amount);
		return result;
	}

private static int autoId = 0;



	public static String removeInvalidChars(String in) { // had case of user with newlines in task names
		if (in == null)
			return null;
		StringBuilder inBuf = new StringBuilder(in);
		for (int i = 0; i <inBuf.length(); i++) {
			char c = inBuf.charAt(i);
			if ((c < 0x20 && c != '\r' && c != '\n' && c != '\t') || c == 0xFFFE || c == 0xFFFF) {
				inBuf.setCharAt(i, ' ');
			}
		}
		return inBuf.toString();

	}
	public static  void toMPXTask(NormalTask projectlibreTask, Task mpxTask) {
		mpxTask.setName(removeInvalidChars(projectlibreTask.getName()));
		if (projectlibreTask.getWbs() != null)
			mpxTask.setWBS(removeInvalidChars(projectlibreTask.getWbs()));
		mpxTask.setNotes(removeInvalidChars(projectlibreTask.getNotes()));
		int taskId = exportId(IdNamespace.TASK_ROW, projectlibreTask.getId());
		mpxTask.setID(taskId);
		mpxTask.setUniqueID(exportId(IdNamespace.TASK, projectlibreTask.getUniqueId()));
		mpxTask.setCreateDate(projectlibreTask.getCreated());
		DurationDefaults defaults = projectlibreTask.getProject().getDurationDefaults();
		mpxTask.setDuration(toMPXDuration(projectlibreTask.getDuration(), defaults)); // set duration without controls
		mpxTask.setWork(toMPXDuration((long) projectlibreTask.getWork(), defaults));
		mpxTask.setActualWork(toMPXDuration(projectlibreTask.getActualWork(null), defaults));
		mpxTask.setRemainingWork(toMPXDuration(projectlibreTask.getRemainingWork(null), defaults));
		// The model stores local wall time encoded in a UTC epoch. MPXJ formats
		// these MSPDI fields in the host zone, so remove that encoding first.
		mpxTask.setStart(toMpxDate(projectlibreTask.getStart()));
		mpxTask.setFinish(toMpxDate(projectlibreTask.getEnd()));
		mpxTask.setCritical(Boolean.valueOf(projectlibreTask.isCritical()));
		mpxTask.setEstimated(projectlibreTask.isEstimated());
		mpxTask.setEffortDriven(projectlibreTask.isEffortDriven());
		mpxTask.setType(MpxjApi.taskType(projectlibreTask.getSchedulingType()));
		mpxTask.setConstraintType(ConstraintType.getInstance(projectlibreTask.getConstraintType()));
		mpxTask.setConstraintDate(toMpxDate(projectlibreTask.getConstraintDate()));
		mpxTask.setPriority(Priority.getInstance(projectlibreTask.getPriority()));
		mpxTask.setFixedCost(projectlibreTask.getFixedCost());
		mpxTask.setFixedCostAccrual(AccrueType.getInstance(projectlibreTask.getFixedCostAccrual()));
		mpxTask.setMilestone(projectlibreTask.isMarkTaskAsMilestone());
		toMPXTaskTracking(projectlibreTask, mpxTask);
		// MSPDI stores leveling delay as a count of minutes. Unlike task
		// duration, the scheduling engine keeps this value as an unencoded
		// millisecond offset. Routing it through toMPXDuration() can therefore
		// serialize milliseconds as minutes and create months of artificial delay
		// after an import/recalculation.
		mpxTask.setLevelingDelay(toMPXLevelingDelay(projectlibreTask.getLevelingDelay()));
		if (projectlibreTask.getDeadline() != 0)
			mpxTask.setDeadline(toMpxDate(projectlibreTask.getDeadline()));
		mpxTask.setEarnedValueMethod(EarnedValueMethod.getInstance(projectlibreTask.getEarnedValueMethod()));
		mpxTask.setIgnoreResourceCalendar(projectlibreTask.isIgnoreResourceCalendar());
		toMpxTaskBaselines(projectlibreTask, mpxTask);

		//2007
		mpxTask.setTotalSlack(toMPXDuration(projectlibreTask.getTotalSlack(), defaults));
		mpxTask.setRemainingDuration(toMPXDuration(projectlibreTask.getRemainingDuration(), defaults));
		if (projectlibreTask.getStop() != 0)
			mpxTask.setStop(toMpxDate(projectlibreTask.getStop()));
		
		if (projectlibreTask.getResume() != 0)
			mpxTask.setResume(toMpxDate(projectlibreTask.getResume()));

		WorkCalendar cal = projectlibreTask.getWorkCalendar();

		if (cal != null)
			mpxTask.setCalendar(ImportedCalendarService.getInstance().findExportedCalendar(cal));
		if (projectlibreTask instanceof SubProj subproject) {
			net.sf.mpxj.SubProject mpxSubproject = new net.sf.mpxj.SubProject();
			mpxSubproject.setFileName(projectlibreTask.getSubprojectFile());
			mpxSubproject.setFullPath(projectlibreTask.getSubprojectFile());
			mpxSubproject.setTaskUniqueID(Integer.valueOf((int) projectlibreTask.getUniqueId()));
			mpxTask.setSubProject(mpxSubproject);
			mpxTask.setSubprojectFile(projectlibreTask.getSubprojectFile());
			mpxTask.setSubprojectTaskID(Integer.valueOf((int) projectlibreTask.getId()));
			mpxTask.setSubprojectReadOnly(projectlibreTask.isSubprojectReadOnly());
		}

//	Not needed - it will be set when hierarchy is done		mpxTask.setOutlineLevel(new Integer(projectlibreTask.getOutlineLevel()));

		toMpxCustomFields(projectlibreTask.getCustomFields(),mpxTask, CustomFieldsMapper.getInstance().taskMaps, defaults);
	}

	private static void toMpxTaskBaselines(NormalTask task, Task mpxTask) {
		for (int snapshotId = 0; snapshotId <= 10; snapshotId++) {
			TaskSnapshot snapshot = (TaskSnapshot) task.getSnapshot(Integer.valueOf(snapshotId));
			if (snapshot == null || snapshot.getCurrentSchedule() == null)
				continue;
			var schedule = snapshot.getCurrentSchedule();
			if (schedule.getStart() != 0L) {
				Date start = toMpxDate(schedule.getStart());
				if (snapshotId == 0)
					mpxTask.setBaselineStart(start);
				else
					mpxTask.setBaselineStart(snapshotId, start);
			}
			if (schedule.getFinish() != 0L) {
				Date finish = toMpxDate(schedule.getFinish());
				if (snapshotId == 0)
					mpxTask.setBaselineFinish(finish);
				else
					mpxTask.setBaselineFinish(snapshotId, finish);
			}
			// A present snapshot with zero duration is a valid milestone baseline.
			net.sf.mpxj.Duration baselineDuration = toMPXDuration(schedule.getRawDuration(), task.getProject().getDurationDefaults());
			if (snapshotId == 0)
				mpxTask.setBaselineDuration(baselineDuration);
			else
				mpxTask.setBaselineDuration(snapshotId, baselineDuration);
		}
	}

	public static void toMPXTaskTracking(NormalTask projectlibreTask, Task mpxTask) {
		DurationDefaults defaults = projectlibreTask.getProject().getDurationDefaults();
		mpxTask.setPercentageComplete(projectlibreTask.getPercentComplete()*100.0D);
		mpxTask.setPercentageWorkComplete(projectlibreTask.getPercentWorkComplete()*100.0D);
		mpxTask.setPhysicalPercentComplete(projectlibreTask.getPhysicalPercentComplete()*100.0D);
		if (projectlibreTask.getActualStart() != 0L)
			mpxTask.setActualStart(toMpxDate(projectlibreTask.getActualStart()));
		if (projectlibreTask.getActualFinish() != 0L)
			mpxTask.setActualFinish(toMpxDate(projectlibreTask.getActualFinish()));
		mpxTask.setActualDuration(toMPXDuration(projectlibreTask.getActualDuration(), defaults));
		mpxTask.setActive(!projectlibreTask.isInactiveTask());
		mpxTask.setTaskMode(projectlibreTask.isManuallyScheduled() ? TaskMode.MANUALLY_SCHEDULED : TaskMode.AUTO_SCHEDULED);
		if (projectlibreTask.isManuallyScheduled())
			mpxTask.setManualDuration(toMPXDuration(projectlibreTask.getDuration(), defaults));
	}

	public static void toMPXVoid(VoidNodeImpl projectlibreVoid, Task mpxTask) {
		int taskId = exportId(IdNamespace.TASK_ROW, projectlibreVoid.getId());
		mpxTask.setID(taskId);
		mpxTask.setUniqueID(exportId(IdNamespace.TASK, Long.MIN_VALUE + projectlibreVoid.getId()));
		mpxTask.setNull(true);
		// below is for mpxj 2007. These values need to be set
		mpxTask.setCritical(false);
		mpxTask.setTotalSlack(toMPXDuration(0, currentDurationDefaults()));

	}







	public static net.sf.mpxj.Rate toMPXRate(Rate rate) {
		return toMPXRate(rate, currentDurationDefaults());
	}

	public static net.sf.mpxj.Rate toMPXRate(Rate rate, DurationDefaults defaults) {
		double value = rate.getValue() * Duration.timeUnitFactor(rate.getTimeUnit(), defaults);
		return new net.sf.mpxj.Rate(value, MpxUtils.toMpxTimeUnit(rate.getTimeUnit()));
	}

	public static net.sf.mpxj.Duration toMPXDuration(long duration) {
		return toMPXDuration(duration, currentDurationDefaults());
	}

	public static net.sf.mpxj.Duration toMPXDuration(long duration, DurationDefaults defaults) {
		//merge [ba296a] and [6fe45f] 
		double durationValue = Duration.getValue(duration, defaults);
		int type = Duration.getType(duration);
		if (type == com.microproject.datatype.TimeUnit.NONE)
			type = defaults.durationEnteredIn();
		TimeUnit timeUnit = projity2mpxTimeUnit(type);
		if (timeUnit == TimeUnit.PERCENT || timeUnit == TimeUnit.ELAPSED_PERCENT) {
			durationValue = durationValue * 100.0;
		}
		return net.sf.mpxj.Duration.getInstance(durationValue, timeUnit);


	}

	private static DurationDefaults currentDurationDefaults() {
		return DurationDefaults.from(CalendarOption.getInstance(), ScheduleOption.getInstance());
	}

	/** Converts a dependency lag without reinterpreting its milliseconds as the current display unit. */
	public static net.sf.mpxj.Duration toMPXDependencyLag(long lag) {
		if (Duration.isPercent(lag))
			return toMPXDuration(lag);
		boolean elapsed = Duration.isElapsed(lag);
		net.sf.mpxj.TimeUnit unit = elapsed
			? net.sf.mpxj.TimeUnit.ELAPSED_MINUTES : net.sf.mpxj.TimeUnit.MINUTES;
		return net.sf.mpxj.Duration.getInstance(Duration.millis(lag) / 60_000.0d, unit);
	}

	/** Converts the engine's millisecond leveling delay to MSPDI minutes. */
	public static net.sf.mpxj.Duration toMPXLevelingDelay(long levelingDelay) {
		return net.sf.mpxj.Duration.getInstance(
			(double) Duration.millis(levelingDelay) / (60.0d * 1000.0d), TimeUnit.MINUTES);
	}

	/**
	 * @param type
	 * @return
	 */
	private static TimeUnit projity2mpxTimeUnit(int type) {
		int mpxTimeUnit = switch (type) {
			case com.microproject.datatype.TimeUnit.MINUTES -> 0;
			case com.microproject.datatype.TimeUnit.HOURS -> 1;
			case com.microproject.datatype.TimeUnit.DAYS -> 2;
			case com.microproject.datatype.TimeUnit.WEEKS -> 3;
			case com.microproject.datatype.TimeUnit.MONTHS -> 4;
			case com.microproject.datatype.TimeUnit.PERCENT -> 5;
			case com.microproject.datatype.TimeUnit.YEARS -> 6;
			case com.microproject.datatype.TimeUnit.ELAPSED_MINUTES -> 7;
			case com.microproject.datatype.TimeUnit.ELAPSED_HOURS -> 8;
			case com.microproject.datatype.TimeUnit.ELAPSED_DAYS -> 9;
			case com.microproject.datatype.TimeUnit.ELAPSED_WEEKS -> 10;
			case com.microproject.datatype.TimeUnit.ELAPSED_MONTHS -> 11;
			case com.microproject.datatype.TimeUnit.ELAPSED_YEARS -> 12;
			case com.microproject.datatype.TimeUnit.ELAPSED_PERCENT -> 13;
			default -> 13;
		};
		return TimeUnit.getInstance(mpxTimeUnit);
	}

	private static final class ExportIdAllocator {
		private final Map<IdNamespace, Map<Long, Integer>> idsByNamespace = HashMap.newHashMap(IdNamespace.values().length);
		private final Map<IdNamespace, Integer> nextIds = HashMap.newHashMap(IdNamespace.values().length);

		int get(IdNamespace namespace, long sourceId) {
			Map<Long, Integer> ids = idsByNamespace.get(namespace);
			if (ids == null) {
				ids = new HashMap<Long, Integer>();
				idsByNamespace.put(namespace, ids);
			}
			int nextId = nextIds.getOrDefault(namespace, Integer.valueOf(1)).intValue();
			Long key = Long.valueOf(sourceId);
			Integer existing = ids.get(key);
			if (existing != null) {
				return existing.intValue();
			}
			int value;
			if (sourceId > 0 && sourceId <= Integer.MAX_VALUE && !ids.containsValue(Integer.valueOf((int) sourceId))) {
				value = (int) sourceId;
			} else {
				while (ids.containsValue(Integer.valueOf(nextId))) {
					++nextId;
				}
				value = nextId++;
			}
			nextIds.put(namespace, Integer.valueOf(nextId));
			ids.put(key, Integer.valueOf(value));
			return value;
		}
	}

	private enum IdNamespace {
		CALENDAR,
		RESOURCE,
		RESOURCE_ROW,
		TASK_ROW,
		TASK
	}



	public static final String dateToXMLString(long time) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
			.withZone(ZoneId.systemDefault())
			.format(Instant.ofEpochMilli(time));
	}

	/** Converts a model wall-time epoch into MPXJ's host-local date representation. */
	private static Date toMpxDate(long modelWallTime) {
		return new Date(TimeUtil.removeTimeZoneOffset(modelWallTime));
	}

	private static String truncName(String name) {
		if (name == null)
			return null;
		if (name.length() > nameFieldWidth)
			name = name.substring(0,nameFieldWidth);
		return name;
	}
}
