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
import net.sf.mpxj.ProjectProperties;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceField;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;
import net.sf.mpxj.TaskMode;
import net.sf.mpxj.TaskType;
import net.sf.mpxj.TimeUnit;
import net.sf.mpxj.WorkContour;
import net.sf.mpxj.mspdi.DatatypeConverter;

import com.microproject.exchange.ImportedCalendarService;
import com.microproject.exchange.mpxj.MpxjApi;
import com.microproject.configuration.Configuration;
import com.microproject.contrib.util.Log;
import com.microproject.contrib.util.LogFactory;
import com.microproject.datatype.Duration;
import com.microproject.datatype.Rate;
import com.microproject.field.CustomFields;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.util.DateTime;
import com.microproject.util.MathUtils;
/**
 *
 */
public class MPXConverter {
	private static Log log = LogFactory.getLog(MPXConverter.class);
	private static final Logger logger = Logger.getLogger(MPXConverter.class.getName());

	public static int nameFieldWidth = Configuration.getFieldFromId("Field.name").getTextWidth();
	private static final ThreadLocal<ExportIdAllocator> EXPORT_IDS = new ThreadLocal<ExportIdAllocator>();

	public static void beginExport() {
		EXPORT_IDS.set(new ExportIdAllocator());
	}

	public static void endExport() {
		EXPORT_IDS.remove();
	}

	private static int exportId(long sourceId) {
		ExportIdAllocator allocator = EXPORT_IDS.get();
		if (allocator == null) {
			allocator = new ExportIdAllocator();
			EXPORT_IDS.set(allocator);
		}
		return allocator.get(sourceId);
	}


	public static void toMPXOptions(ProjectProperties projectHeader) {

		CalendarOption calendarOption = CalendarOption.getInstance();
//		projectHeader.setDefaultHoursInDay(new Float(calendarOption.getHoursPerDay()));
		projectHeader.setMinutesPerDay(Integer.valueOf((int) (60 * calendarOption.getHoursPerDay())));
//		projectHeader.setDefaultHoursInWeek(new Float(calendarOption.getHoursPerWeek()));
		projectHeader.setMinutesPerWeek(Integer.valueOf((int) (60 * calendarOption.getHoursPerWeek())));

		projectHeader.setDaysPerMonth(Integer.valueOf((int) Math.round(calendarOption.getDaysPerMonth())));

		projectHeader.setDefaultStartTime(calendarOption.getDefaultStartTime().getTime());
		projectHeader.setDefaultEndTime(calendarOption.getDefaultEndTime().getTime());
	}

	public static void toMPXProject(Project project,ProjectProperties projectHeader) {
		projectHeader.setName(project.getName());
		projectHeader.setProjectTitle(project.getName());
		projectHeader.setComments(project.getNotes());
		projectHeader.setManager(project.getManager());
		projectHeader.setComments(removeInvalidChars(project.getNotes()));
		projectHeader.setStartDate(DateTime.fromGmt(new Date(project.getStartDate())));
		projectHeader.setFinishDate(DateTime.fromGmt(new Date(project.getFinishDate())));
		projectHeader.setDefaultStartTime(CalendarOption.getInstance().getDefaultStartTime().getTime());
		projectHeader.setDefaultEndTime(CalendarOption.getInstance().getDefaultEndTime().getTime());

	}


	public static void toMpxCalendar(WorkingCalendar workCalendar,ProjectCalendar mpx) {
		mpx.setName(workCalendar.getName());
		mpx.setUniqueID(Integer.valueOf(exportId(workCalendar.getUniqueId())));

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

		WorkDay[] workDays=workCalendar.getExceptionDays();
		if (workDays!=null)
			for (int i=0;i<workDays.length;i++){
				if (workDays[i]==null||workDays[i].getStart()==0L||workDays[i].getStart()==Long.MAX_VALUE)
					continue;
				Date start = new Date(workDays[i].getStart());
				GregorianCalendar cal = DateTime.calendarInstance();
				// days go from 00:00 to 23:59
				cal.setTime(start);
				cal.set(Calendar.HOUR,23);
				cal.set(Calendar.MINUTE,59);
				ProjectCalendarException exception=mpx.addCalendarException(start,DateTime.fromGmt(cal.getTime())); //claur
				
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

		for (WorkRange range:(List<WorkRange>)workingHours.getIntervals()) { //claur
			if (range!=null)
				mpxDay.add(new DateRange(DateTime.fromGmt(range.getNormalizedStartTime()),DateTime.fromGmt(range.getNormalizedEndTime())));
		}
	}

	public static  void toMpxExceptionDay(WorkDay day,ProjectCalendarException mpxDay) {
		if  (day==null)
			return;
		WorkingHours workingHours=day.getWorkingHours();
		WorkRange range;

		range=workingHours.getInterval(0);
		if (range!=null){
			mpxDay.add(new DateRange(DateTime.fromGmt(range.getNormalizedStartTime()),DateTime.fromGmt(range.getNormalizedEndTime()))); //claur
		}

		range=workingHours.getInterval(1);
		if (range!=null){
			mpxDay.add(new DateRange(DateTime.fromGmt(range.getNormalizedStartTime()),DateTime.fromGmt(range.getNormalizedEndTime())));//claur
		}

		range=workingHours.getInterval(2);
		if (range!=null){
			mpxDay.add(new DateRange(DateTime.fromGmt(range.getNormalizedStartTime()),DateTime.fromGmt(range.getNormalizedEndTime())));//claur
		}
	}


	public static void toMPXResource(ResourceImpl projectlibreResource,Resource mpxResource) {
		mpxResource.setName(removeInvalidChars(projectlibreResource.getName()));
		mpxResource.setNotes(removeInvalidChars(projectlibreResource.getNotes()));
		mpxResource.setAccrueAt(AccrueType.getInstance(projectlibreResource.getAccrueAt()));
		mpxResource.set(ResourceField.COST_PER_USE, Double.valueOf(projectlibreResource.getCostPerUse()));
		mpxResource.set(ResourceField.STANDARD_RATE, toMPXRate(projectlibreResource.getStandardRate()));
		mpxResource.set(ResourceField.OVERTIME_RATE, toMPXRate(projectlibreResource.getOvertimeRate()));
		mpxResource.setGroup(projectlibreResource.getGroup());
		mpxResource.setEmailAddress(projectlibreResource.getEmailAddress());
		mpxResource.setGeneric(projectlibreResource.isGeneric()); // fix for 2024492

		mpxResource.setInitials(projectlibreResource.getInitials());
		int resourceId = exportId(projectlibreResource.getId());
		mpxResource.setID(resourceId);
		long uid = projectlibreResource.getExternalId(); // try using external id of one set
		if (uid <= 0)
			uid = projectlibreResource.getUniqueId();
		if (uid <= 0)
			uid = projectlibreResource.getId();
		mpxResource.setUniqueID(exportId(uid));
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
		toMpxCustomFields(projectlibreResource.getCustomFields(),mpxResource, CustomFieldsMapper.getInstance().resourceMaps);


	}


	public static void toMpxCustomFields(CustomFields projectlibreFields,FieldContainer mpx, CustomFieldsMapper.Maps maps) {
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
				mpx.set(maps.durationMap[i],toMPXDuration(d));
		}
		for (int i = 0; i < maps.finishMap.length; i++) {
			long d = projectlibreFields.getCustomFinish(i);
			if (d != 0)
				mpx.set(maps.finishMap[i],new Date(d));
		}
		for (int i = 0; i < maps.flagMap.length; i++) {
			boolean b = projectlibreFields.getCustomFlag(i);
			if (b == true)
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
    	mpxAssignment.setWork(MPXConverter.toMPXDuration(work));
		mpxAssignment.setActualWork(MPXConverter.toMPXDuration(assignment.getActualWork(null)));
		long assignmentWork = Duration.millis(assignment.getWork(null));
		double percentWorkComplete = assignmentWork == 0L ? 0.0d
				: ((double) Duration.millis(assignment.getActualWork(null))) / assignmentWork;
		mpxAssignment.setPercentageWorkComplete(percentWorkComplete * 100.0d);
		if (assignment.getActualStart() != 0L)
			mpxAssignment.setActualStart(DateTime.fromGmt(new Date(assignment.getActualStart())));
		if (assignment.getActualFinish() != 0L)
			mpxAssignment.setActualFinish(DateTime.fromGmt(new Date(assignment.getActualFinish())));
    	mpxAssignment.setUnits(MathUtils.roundToDecentPrecision(assignment.getUnits()*100.0D));
    	mpxAssignment.setRemainingWork(MPXConverter.toMPXDuration(assignment.getRemainingWork())); //2007
    	long delay = Duration.millis(assignment.getDelay());
    	if (delay != 0) {
    		// mpxj uses default options when dealing with assignment delay
    		CalendarOption oldOptions = CalendarOption.getInstance();
    		CalendarOption.setInstance(CalendarOption.getDefaultInstance());

        	mpxAssignment.setDelay(MPXConverter.toMPXDuration(assignment.getDelay()));
            CalendarOption.setInstance(oldOptions);
    	}

    	long levelingDelay = Duration.millis(assignment.getLevelingDelay());
    	if (levelingDelay != 0) {
    		// mpxj uses default options when dealing with assignment delay
    		CalendarOption oldOptions = CalendarOption.getInstance();
    		CalendarOption.setInstance(CalendarOption.getDefaultInstance());

        	mpxAssignment.setDelay(MPXConverter.toMPXDuration(assignment.getLevelingDelay()));
            CalendarOption.setInstance(oldOptions);
    	}


		mpxAssignment.setWorkContour(MpxjApi.workContour(assignment.getWorkContourType()));


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
		int taskId = exportId(projectlibreTask.getId());
		mpxTask.setID(taskId);
		mpxTask.setUniqueID(exportId(projectlibreTask.getUniqueId()));
		mpxTask.setCreateDate(projectlibreTask.getCreated());
		mpxTask.setDuration(toMPXDuration(projectlibreTask.getDuration())); // set duration without controls
		mpxTask.setWork(toMPXDuration((long) projectlibreTask.getWork()));
		mpxTask.setActualWork(toMPXDuration(projectlibreTask.getActualWork(null)));
		mpxTask.setRemainingWork(toMPXDuration(projectlibreTask.getRemainingWork(null)));
		mpxTask.setStart(DateTime.fromGmt(new Date(projectlibreTask.getStart())));
		mpxTask.setFinish(DateTime.fromGmt(new Date(projectlibreTask.getEnd())));
		mpxTask.setCritical(Boolean.valueOf(projectlibreTask.isCritical()));
		mpxTask.setEstimated(projectlibreTask.isEstimated());
		mpxTask.setEffortDriven(projectlibreTask.isEffortDriven());
		mpxTask.setType(MpxjApi.taskType(projectlibreTask.getSchedulingType()));
		mpxTask.setConstraintType(ConstraintType.getInstance(projectlibreTask.getConstraintType()));
		mpxTask.setConstraintDate(DateTime.fromGmt(new Date(projectlibreTask.getConstraintDate())));
		mpxTask.setPriority(Priority.getInstance(projectlibreTask.getPriority()));
		mpxTask.setFixedCost(projectlibreTask.getFixedCost());
		mpxTask.setFixedCostAccrual(AccrueType.getInstance(projectlibreTask.getFixedCostAccrual()));
		mpxTask.setMilestone(projectlibreTask.isMarkTaskAsMilestone());
		toMPXTaskTracking(projectlibreTask, mpxTask);
		mpxTask.setLevelingDelay(toMPXDuration(projectlibreTask.getLevelingDelay()));
		if (projectlibreTask.getDeadline() != 0)
			mpxTask.setDeadline(DateTime.fromGmt(new Date(projectlibreTask.getDeadline())));
		mpxTask.setEarnedValueMethod(EarnedValueMethod.getInstance(projectlibreTask.getEarnedValueMethod()));
		mpxTask.setIgnoreResourceCalendar(projectlibreTask.isIgnoreResourceCalendar());

		//2007
		mpxTask.setTotalSlack(toMPXDuration(projectlibreTask.getTotalSlack()));
		mpxTask.setRemainingDuration(toMPXDuration(projectlibreTask.getRemainingDuration()));
		if (projectlibreTask.getStop() != 0)
			mpxTask.setStop(DateTime.fromGmt(new Date(projectlibreTask.getStop())));
		
		if (projectlibreTask.getResume() != 0)
			mpxTask.setResume(DateTime.fromGmt(new Date(projectlibreTask.getResume())));

		WorkCalendar cal = projectlibreTask.getWorkCalendar();

		if (cal != null)
			mpxTask.setCalendar(ImportedCalendarService.getInstance().findExportedCalendar(cal));

//	Not needed - it will be set when hierarchy is done		mpxTask.setOutlineLevel(new Integer(projectlibreTask.getOutlineLevel()));

		toMpxCustomFields(projectlibreTask.getCustomFields(),mpxTask, CustomFieldsMapper.getInstance().taskMaps);
	}

	public static void toMPXTaskTracking(NormalTask projectlibreTask, Task mpxTask) {
		mpxTask.setPercentageComplete(projectlibreTask.getPercentComplete()*100.0D);
		mpxTask.setPercentageWorkComplete(projectlibreTask.getPercentWorkComplete()*100.0D);
		mpxTask.setPhysicalPercentComplete(projectlibreTask.getPhysicalPercentComplete()*100.0D);
		if (projectlibreTask.getActualStart() != 0L)
			mpxTask.setActualStart(DateTime.fromGmt(new Date(projectlibreTask.getActualStart())));
		if (projectlibreTask.getActualFinish() != 0L)
			mpxTask.setActualFinish(DateTime.fromGmt(new Date(projectlibreTask.getActualFinish())));
		mpxTask.setActualDuration(toMPXDuration(projectlibreTask.getActualDuration()));
		mpxTask.setActive(!projectlibreTask.isInactiveTask());
		mpxTask.setTaskMode(projectlibreTask.isManuallyScheduled() ? TaskMode.MANUALLY_SCHEDULED : TaskMode.AUTO_SCHEDULED);
		if (projectlibreTask.isManuallyScheduled())
			mpxTask.setManualDuration(toMPXDuration(projectlibreTask.getDuration()));
	}

	public static void toMPXVoid(VoidNodeImpl projectlibreVoid, Task mpxTask) {
		int taskId = exportId(projectlibreVoid.getId());
		mpxTask.setID(taskId);
		mpxTask.setUniqueID(taskId);
		mpxTask.setNull(true);
		// below is for mpxj 2007. These values need to be set
		mpxTask.setCritical(false);
		mpxTask.setTotalSlack(toMPXDuration(0));

	}







	public static net.sf.mpxj.Rate toMPXRate(Rate rate) {
		double value = rate.getValue() * Duration.timeUnitFactor(rate.getTimeUnit());
		return new net.sf.mpxj.Rate(value,TimeUnit.getInstance(rate.getTimeUnit()));
	}

	public static net.sf.mpxj.Duration toMPXDuration(long duration) {
		//merge [ba296a] and [6fe45f] 
		double durationValue = Duration.getValue(duration);
		int type = Duration.getEffectiveType(duration);
		TimeUnit timeUnit = projity2mpxTimeUnit(type);
		if (timeUnit == TimeUnit.PERCENT || timeUnit == TimeUnit.ELAPSED_PERCENT) {
			durationValue = durationValue * 100.0;
		}
		return net.sf.mpxj.Duration.getInstance(durationValue, timeUnit);


	}

	/**
	 * @param type
	 * @return
	 */
	private static TimeUnit projity2mpxTimeUnit(int type) {
		switch (type) {
			case com.microproject.datatype.TimeUnit.MINUTES:
				type = 0;
				break;
			case com.microproject.datatype.TimeUnit.HOURS:
				type = 1;
				break;
			case com.microproject.datatype.TimeUnit.DAYS:
				type = 2;
				break;
			case com.microproject.datatype.TimeUnit.WEEKS:
				type = 3;
				break;
			case com.microproject.datatype.TimeUnit.MONTHS:
				type = 4;
				break;
			case com.microproject.datatype.TimeUnit.PERCENT:
				type = 5;
				break;
			case com.microproject.datatype.TimeUnit.YEARS:
				type = 6;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_MINUTES:
				type = 7;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_HOURS:
				type = 8;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_DAYS:
				type = 9;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_WEEKS:
				type = 10;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_MONTHS:
				type = 11;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_YEARS:
				type = 12;
				break;
			case com.microproject.datatype.TimeUnit.ELAPSED_PERCENT:
				type = 13;
				break;
			default:
				type = 13;
				break;
		}
		return TimeUnit.getInstance(type);
	}

	private static final class ExportIdAllocator {
		private final Map<Long, Integer> ids = new HashMap<Long, Integer>();
		private int nextId = 1;

		int get(long sourceId) {
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
			ids.put(key, Integer.valueOf(value));
			return value;
		}
	}



	public static final String dateToXMLString(long time) {
		return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
			.withZone(ZoneId.systemDefault())
			.format(Instant.ofEpochMilli(time));
	}
	private static String truncName(String name) {
		if (name == null)
			return null;
		if (name.length() > nameFieldWidth)
			name = name.substring(0,nameFieldWidth);
		return name;
	}
}
