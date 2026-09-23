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
package com.microproject.pm.calendar;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.microproject.datatype.Duration;
import com.microproject.pm.criticalpath.CriticalPath;
import com.microproject.server.access.ErrorLogger;
import com.microproject.util.DateTime;

/**
 * This class holds specific calendar informatin either for a base calendar or a concrete one, as well as date math functions
 */
public class CalendarDefinition implements WorkCalendar, Cloneable {
	static final long serialVersionUID = 73883742020831L;
	private static final Logger logger = Logger.getLogger(CalendarDefinition.class.getName());
	TreeSet<WorkDay> dayExceptions = null;
	WorkDay[] exceptions = null;
	private List<WorkWeekPeriod> workWeekPeriods;
	private List<RecurringCalendarException> recurringExceptions;
	WorkWeek week = new WorkWeek();
	protected long id=-1L;
	private String name = "CalendarDefinition";

	// Cache for add() results during scheduling passes. Cleared after each pass.
	ConcurrentHashMap<AddCacheKey, Long> addCache = new ConcurrentHashMap<>(256);

	/**
	 *
	 */
	public CalendarDefinition() {
		super();
		dayExceptions = new TreeSet<WorkDay>();
	}

	public CalendarDefinition(CalendarDefinition base, CalendarDefinition differences) {
		if (base == null) {
			week = new WorkWeek();
		} else {
			week = base.week.clone(); // copy the week days
			for (WorkWeekPeriod period : base.getWorkWeekPeriods())
				addOrReplaceWorkWeekPeriod(period);
			for (RecurringCalendarException exception : base.getRecurringExceptions())
				addOrReplaceRecurringException(exception);
		}
		week.addDaysFrom(differences.week); // Now replace any special weekdays
		for (WorkWeekPeriod difference : differences.getWorkWeekPeriods()) addOrReplaceWorkWeekPeriod(difference);
		for (RecurringCalendarException exception : differences.getRecurringExceptions())
			addOrReplaceRecurringException(exception);

		TreeSet<WorkDay> clonedExceptions = new TreeSet<>(differences.dayExceptions);
		dayExceptions = clonedExceptions; // copy from differences
		if (base != null)
			dayExceptions.addAll( base.dayExceptions); // add in base days. If day is already present it will not be added
		addSentinelsAndMakeArray();

		if (!testValid())
			logger.warning("calendar is invalid " + this.getName());
	}

	public boolean testValid() {
		if (week == null)
			return false;
		for (int i = 0; i < 7; i++)
			if (week.getWeekDay(i) == null)
				return false;
		return true;

	}
	void addSentinelsAndMakeArray() {
		// Add endpoint sentinels.  This facilitates algorithms which will no longer need to check for boundary conditions
		dayExceptions.add(WorkDay.MINIMUM);
		dayExceptions.add(WorkDay.MAXIMUM);
		exceptions = buildExceptionArray();
	}

	private WorkDay[] buildExceptionArray() {
		TreeSet<WorkDay> allExceptions = new TreeSet<>();
		if (recurringExceptions != null)
			for (RecurringCalendarException recurring : recurringExceptions)
				for (WorkDay occurrence : recurring.getOccurrences()) allExceptions.add(occurrence);
		// An explicitly dated exception overrides a recurring exception on the same day.
		for (WorkDay exception : dayExceptions) {
			allExceptions.remove(exception);
			allExceptions.add(exception);
		}
		return allExceptions.toArray(WorkDay[]::new);

	}

	/**
	 * Total working time of the week, treating null weekdays as the default
	 * working day (matching the {@link CalendarIterator} behavior). Used when
	 * {@link WorkWeek#getDuration()} is zero so that an uninitialized week is
	 * not mistaken for a calendar without working time (issue #175).
	 */
	private long effectiveWeekDuration() {
		long total = 0;
		for (int i = 0; i < WorkWeek.DAYS_IN_WEEK; i++) {
			WorkDay day = week.getWeekDay(i);
			total += (day == null ? WorkDay.getDefaultWorkDay() : day).getDuration();
		}
		return total;
	}

	/**
	 * Whether the week contributes no working time at all (after resolving null
	 * weekdays to the default working day). Such a week is invalid - the
	 * codebase rejects zero-working-time weeks elsewhere (intersectWith throws
	 * InvalidCalendarIntersectionException) - so scheduling against it falls
	 * back to elapsed-time arithmetic in calculateAddition() (issue #175).
	 * Exception days are deliberately not consulted: they cannot rescue a
	 * degenerate week when the schedule direction never reaches them, which
	 * would let the per-day fine-tuning walk in addScheduledTime loop forever.
	 */
	private boolean hasNoWorkingTime() {
		if (week.getDuration() > 0) {
			return false;
		}
		return effectiveWeekDuration() <= 0;
	}
	public WorkDay[] getExceptions() {
		return exceptions;
	}

	WorkDay[] getLocalExceptionDays() {
		return dayExceptions.stream()
			.filter(day -> day != WorkDay.MINIMUM && day != WorkDay.MAXIMUM)
			.map(WorkDay::clone)
			.toArray(WorkDay[]::new);
	}

	/** Canonicalize empty lists written by transitional builds and legacy fixtures. */
	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		if (workWeekPeriods != null && workWeekPeriods.isEmpty()) workWeekPeriods = null;
	}

	public List<WorkWeekPeriod> getWorkWeekPeriods() {
		return workWeekPeriods == null ? Collections.emptyList() : Collections.unmodifiableList(workWeekPeriods);
	}

	public List<RecurringCalendarException> getRecurringExceptions() {
		return recurringExceptions == null ? Collections.emptyList() : Collections.unmodifiableList(recurringExceptions);
	}

	void addOrReplaceRecurringException(RecurringCalendarException exception) {
		if (recurringExceptions == null) recurringExceptions = new ArrayList<>();
		WorkDay template = exception.getTemplate();
		recurringExceptions.removeIf(existing -> existing.getTemplate().getStart() == template.getStart()
			&& java.util.Objects.equals(existing.getTemplate().getDescription(), template.getDescription()));
		recurringExceptions.add(exception.clone());
	}

	void removeRecurringException(RecurringCalendarException exception) {
		if (recurringExceptions == null) return;
		WorkDay template = exception.getTemplate();
		recurringExceptions.removeIf(existing -> existing.getTemplate().getStart() == template.getStart()
			&& java.util.Objects.equals(existing.getTemplate().getDescription(), template.getDescription()));
		exceptions = buildExceptionArray();
	}

	void addOrReplaceWorkWeekPeriod(WorkWeekPeriod period) {
		if (workWeekPeriods == null) workWeekPeriods = new ArrayList<>();
		workWeekPeriods.removeIf(existing -> existing.hasSameRange(period));
		workWeekPeriods.add(period.clone());
	}

	void removeWorkWeekPeriod(WorkWeekPeriod period) {
		if (workWeekPeriods != null)
			workWeekPeriods.removeIf(existing -> existing.hasSameRange(period));
	}

	public WorkDay getWeekDay(int d) {
		return week.getWeekDay(d);
	}
	void addOrReplaceException(WorkDay exceptionDay) {
		dayExceptions.remove(exceptionDay); // remove any existing
		dayExceptions.add(exceptionDay);
		exceptions = buildExceptionArray();
	}


	@Override
	public CalendarDefinition clone() throws CloneNotSupportedException {
		CalendarDefinition newOne = (CalendarDefinition) super.clone();
		newOne.week = week.clone();
		newOne.dayExceptions = new TreeSet<WorkDay>();

		for (WorkDay dayException : dayExceptions)
			newOne.dayExceptions.add(dayException.clone());
		newOne.workWeekPeriods = null;
		if (workWeekPeriods != null) {
			newOne.workWeekPeriods = new ArrayList<>();
			for (WorkWeekPeriod period : workWeekPeriods) newOne.workWeekPeriods.add(period.clone());
		}
		newOne.recurringExceptions = null;
		if (recurringExceptions != null) {
			newOne.recurringExceptions = new ArrayList<>();
			for (RecurringCalendarException exception : recurringExceptions)
				newOne.recurringExceptions.add(exception.clone());
		}
		newOne.exceptions = newOne.buildExceptionArray();
		return newOne;
	}


	/**
	 * This method adjusts the given time to a working time in the calendar.
	 * The algorithm just subtracts a tick and adds it back for sooner or vice versa for later
	 * @param date
	 * @param useSooner
	 * @return
	 */
	public long adjustInsideCalendar(long date, boolean useSooner) {
		long result;
		if (date < 0) {
			date = -date;
			useSooner = !useSooner;
		}
		if (useSooner) {
			long backOne = add(date,-MILLIS_IN_MINUTE,useSooner);
			result =add(backOne,MILLIS_IN_MINUTE,useSooner);
		} else {
			long aheadOne = add(date,MILLIS_IN_MINUTE,useSooner);
			result =add(aheadOne,-MILLIS_IN_MINUTE,useSooner);
		}
		return result;
	}



/**
	 * Cache key for add() results. Uses three primitive fields for minimal overhead.
	 */
	private record AddCacheKey(long date, long duration, boolean useSooner) {}

	/**
	 * Clear the add() result cache. Called after each scheduling pass to prevent stale results.
	 */
	public void clearAddCache() {
		addCache.clear();
	}

	// Track all CalendarDefinition instances that have been used for caching.
	// WeakHashMap ensures no memory leak - entries are removed when CalendarDefinition is GC'd.
	private static final WeakHashMap<CalendarDefinition, Boolean> cachedInstances = new WeakHashMap<>();

	private void markCacheUsed() {
		synchronized (cachedInstances) {
			cachedInstances.put(this, Boolean.TRUE);
		}
	}

	/**
	 * Clear add() result caches on all tracked CalendarDefinition instances.
	 * Called after each scheduling pass to free memory and prevent stale results.
	 */
	public static void clearAllAddCaches() {
		synchronized (cachedInstances) {
			for (CalendarDefinition calendar : cachedInstances.keySet()) {
				calendar.addCache.clear();
			}
		}
	}

	/**
	 * Add a duration to a date, following the calendar.  The duration is in milliseconds.  The date can be positive or negative.
	 * The time required by the algorithm is determined by the number of exceptions encountered and not the duration itself.
	 * To handle reverse scheduling, the date can be negative.  In this case, the date is converted to a positive value, but the duration
	 * is negated.
	 */
	public long add(long date, long duration, boolean useSooner) {
		if (date == 0) // don't bother treating null dates since they will never be valid for calculations
			return 0;
		AddCacheKey key = new AddCacheKey(date, duration, useSooner);
		Long cached = addCache.get(key);
		if (cached != null) {
			return cached.longValue();
		}
		long result = calculateAddition(date, duration, useSooner);
		addCache.put(key, result);
		markCacheUsed();
		return result;
	}

	private long calculateAddition(long date, long duration, boolean useSooner) {
		boolean negative = date < 0;
		boolean forward = true;
		boolean elapsed = Duration.isElapsed(duration);
		duration = Duration.millis(duration);
		if (negative) {
			date = -date;
			duration = -duration;
			useSooner = !useSooner;
			if (duration == 0) {
				forward = false;
			}
		}
		long result;
		if (elapsed) {
			result = addElapsedTime(date, duration, useSooner);
		} else if (hasNoWorkingTime()) {
			// Issue #175: the calendar has no working time at all. Schedule as
			// elapsed time - the same date + duration arithmetic addElapsedTime
			// performs, without the adjustInsideCalendar normalization which
			// would recurse into add(). This keeps degenerate calendars from
			// dividing by a zero week duration or walking non-working days
			// forever, and is sign-consistent with the elapsed path.
			result = date + duration;
		} else {
			result = addScheduledTime(date, duration, useSooner, forward);
		}
		return negative ? -result : result;
	}

	private long addElapsedTime(long date, long duration, boolean useSooner) {
		return adjustInsideCalendar(date + duration, useSooner);
	}

	private long addScheduledTime(long date, long duration, boolean useSooner, boolean forward) {
		if (duration < 0) {
			forward = false;
			duration = -duration;
		}
		CalendarIterator iterator = CalendarIteratorFactory.getInstance();
		if (iterator == null) {
			return date;
		}
		try {
			return addScheduledTime(date, duration, useSooner, forward, iterator);
		} finally {
			CalendarIteratorFactory.recycle(iterator);
		}
	}

	private long addScheduledTime(long date, long duration, boolean useSooner, boolean forward, CalendarIterator iterator) {
		long currentDay = iterator.dayOf(date);
		iterator.initialize(this, forward, currentDay);
		WorkingHours current = iterator.getNext(currentDay);
		duration -= current.calcWorkTime(iterator.timeOf(date), forward);

		/*
		 * First, do a rough tuning to get within a week of destination day.
		 * This part of the algorithm sees how many weeks there are in the duration,
		 * subtracts the normal working time for each week, and then adjusts for exceptions.
		 */
		int weekTries = 0;
		long weekDuration = week.getDuration();
		if (weekDuration <= 0) {
			// Uninitialized or degenerate week: resolve null weekdays as default
			// working days so the weekly tuning below divides by the effective
			// duration instead of the cached zero (issue #175). Calendars with no
			// working time at all were already routed to elapsed-time arithmetic
			// in calculateAddition().
			weekDuration = effectiveWeekDuration();
		}
		// Week skipping only adjusts for exception start dates. A ranged
		// exception changes each covered day, so walk those dates explicitly.
		if (hasMultiDayException())
			weekDuration = 0;
		long numWeeks;
		while (weekDuration > 0 && (numWeeks = (duration / weekDuration)) != 0) {
			if (weekTries++ == 4) {
				break;
			}
			currentDay = iterator.nextDay(currentDay);
			currentDay = iterator.moveNumberOfDays((int) (WorkWeek.DAYS_IN_WEEK * (forward ? numWeeks : -numWeeks)), currentDay);
			duration -= (numWeeks * weekDuration);
			duration -= iterator.exceptionDurationDifference(currentDay);

			if (duration <= 0) {
				iterator.reverseDirection();
				duration = -duration;
				forward = !forward;
			} else {
				currentDay = iterator.prevDay(currentDay);
			}
		}

		/*
		 * Fine tuning: walk the remaining days one by one.
		 * This is guaranteed to go through at most six days.
		 */
		while (duration >= 0) {
			if (duration == 0 && (forward == useSooner)) {
				break;
			}
			currentDay = iterator.nextDay(currentDay);
			current = iterator.getNext(currentDay);
			duration -= current.getDuration();
		}

		long time = -1;
		while (true) {
			if (forward) {
				time = current.calcTimeAtRemainingWork(-duration);
			} else {
				time = current.calcTimeAtWork(-duration);
			}
			if (time != -1) {
				break;
			}
			currentDay = iterator.nextDay(currentDay);
			current = iterator.getNext(currentDay);
		}
		return currentDay + time;
	}


	/**
	 * Get difference of two dates: laterDate - earlierDate according to calendar
	 */
	public long compare(long laterDate, long earlierDate, boolean elapsed) {
		boolean negative = laterDate < 0;
		if (negative) {
			laterDate = -laterDate;
			earlierDate = -earlierDate;
		}

		if (elapsed) { // if the desired duration is elapsed time, then just to a simple subtraction
			return laterDate - earlierDate;
		}

		long result = compareScheduledTime(laterDate, earlierDate);
		return negative ? -result : result;
	}

	private long compareScheduledTime(long laterDate, long earlierDate) {
		long swap = 0;
		if (laterDate < earlierDate) {
			swap = earlierDate;
			earlierDate = laterDate;
			laterDate = swap;
		}
		if (earlierDate == 0) {
			return laterDate;
		}

		CalendarIterator iterator = CalendarIteratorFactory.getInstance();
		if (iterator == null) {
			return laterDate - earlierDate;
		}
		try {
			return compareScheduledTime(laterDate, earlierDate, iterator, swap);
		} finally {
			CalendarIteratorFactory.recycle(iterator);
		}
	}

	private long compareScheduledTime(long laterDate, long earlierDate, CalendarIterator iterator, long swap) {
		long earlierDay = iterator.dayOf(earlierDate);
		long laterDay = iterator.dayOf(laterDate);
		iterator.initialize(this, true, earlierDay);
		WorkingHours current = iterator.getNext(earlierDay);
		long duration = current.calcWorkTimeAfter(iterator.timeOf(earlierDate));
		long currentDay = iterator.nextDay(earlierDay);

		long numWeeks = hasMultiDayException() ? 0
			: (iterator.dayOf(laterDate) - currentDay) / WorkWeek.MS_IN_WEEK;
		if (numWeeks != 0) {
			currentDay = iterator.moveNumberOfDays((int) (WorkWeek.DAYS_IN_WEEK * numWeeks), currentDay);
			duration += numWeeks * week.getDuration();
			duration += iterator.exceptionDurationDifference(currentDay);
		}

		for (; currentDay <= laterDay; currentDay = iterator.nextDay(currentDay)) {
			current = iterator.getNext(currentDay);
			duration += current.getDuration();
		}

		duration -= current.calcWorkTimeAfter(iterator.timeOf(laterDate));
		return (swap == 0) ? duration : -duration;
	}


/**
 * This class manages a pool of calendar iterators.
 *
 */	private static class CalendarIteratorFactory extends BasePoolableObjectFactory {
		private static GenericObjectPool pool =  new GenericObjectPool(new CalendarIteratorFactory());
		public Object makeObject(){ //claur
			return new CalendarIterator();
		}
		public static CalendarIterator getInstance() {
			try {
				return (CalendarIterator) pool.borrowObject();
			} catch (Exception e) {
				ErrorLogger.log("Failed to borrow CalendarIterator", e);
				return null;
			}
		}

		public static void recycle(CalendarIterator object) {
			try {
				pool.returnObject(object);
			} catch (Exception e) {
				ErrorLogger.log("Failed to return CalendarIterator", e);
			}
		}
	}

/**
 * This class is an iterator which is used to return week days or exception days
 *
 */
 	private static class CalendarIterator {
		WorkDay[] exceptions;
		WorkWeek week;
		Calendar scratchDate; // will get reused since this class is recycled

		long exceptionDay;
		int i;
		boolean forward;
		boolean rangeExceptions;
		CalendarDefinition calendar;
		int step;


		private CalendarIterator() {
			scratchDate = DateTime.calendarInstance(); // will get reused since this class is recycled
		}
		/**
		 *
		 */
		private void reverseDirection() {
			if (forward) {
				i -=1;
			} else {
				i += 1;
			}
			step = -step;
			if (isValidExceptionIndex(i)) {
				exceptionDay = exceptions[i].getStart();
			}
			forward = !forward;
		}

		private static SimpleDateFormat f = DateTime.dateFormatInstance();


		public long dayOf(long date) {
			scratchDate.setTimeInMillis(date);
			scratchDate.set(Calendar.HOUR_OF_DAY,0);
			scratchDate.set(Calendar.MINUTE,0);
			scratchDate.set(Calendar.SECOND,0); // Fixed rounding bug as we now go to seconds 8/2/07
//			scratchDate.set(Calendar.MILLISECOND,0);
			return scratchDate.getTimeInMillis();
		}

		public long timeOf(long date) {
			return date - dayOf(date);
		}


		private void initialize(CalendarDefinition cal, boolean forward, long day) {
			calendar = cal;
			rangeExceptions = cal.hasMultiDayException();
			exceptions = cal.exceptions;
			week = cal.week;
			this.forward = forward;
			scratchDate.setTimeInMillis(day);
			try {
				DateUtils.truncate(scratchDate,Calendar.DATE);
			} catch (Exception e) {
				ErrorLogger.logOnce("hugedate", "date value is garbage " + scratchDate + "\n" + CriticalPath.getTrace(), e);
			}
			step = (forward) ? 1 : -1;
			i = Arrays.binarySearch(exceptions, scratchDate);
			if (i < 0) {// First day not found
				i = -i-1; // set index for the future
				if (!forward)
					i -= 1;
			}
			if (isValidExceptionIndex(i)) {
				exceptionDay = exceptions[i].getStart();
			}

		}

		public String dump() {
			String result = "CalendarIterator ";
			result += "weekdays\n";
			for (int i = 0; i < 7; i++) {
				result += "day[" + i +"]" + week.getWeekDay(i) +  "\n";
			}
			result += "There are " + exceptions.length + " exceptions\n";
			for (int j = 0; j < exceptions.length; j++) {
				result += "exception" + exceptions[j].toString();
			}
			return result;

		}
		
		private WorkingHours getNext(long day) {
			WorkDay workDay;
			if (rangeExceptions) {
				workDay = calendar.getWorkDay(day);
			} else if (day == exceptionDay) {
				workDay = exceptions[i]; // move index, save off new value for exception day
				advanceExceptionIndex();
			} else {
				workDay = week.getWeekDay(dayOfWeek(day));
			}
			
			if (workDay==null)
				workDay=WorkDay.getDefaultWorkDay();
			
			return workDay.getWorkingHours();
		}

		private long exceptionDurationDifference(long endDay) {
			long difference = 0;
			if (hasOnlySentinelExceptions()) {
				return 0;
			}
			while (shouldTraverseException(endDay)) {
				difference = accumulateExceptionDurationDifference(difference);
				if (!advanceExceptionIndex()) {
					break;
				}
			}
			return difference;

		}

		private boolean hasOnlySentinelExceptions() {
			return exceptions.length == 2;
		}

		private boolean shouldTraverseException(long endDay) {
			return (forward && exceptionDay < endDay) || (!forward && exceptionDay > endDay);
		}

		private long accumulateExceptionDurationDifference(long difference) {
			difference -= week.getWeekDay(dayOfWeek(exceptionDay)).getDuration();
			difference += exceptions[i].getDuration();
			return difference;
		}

		private boolean advanceExceptionIndex() {
			i += step;
			if (!isValidExceptionIndex(i)) {
				logInvalidExceptionIndex();
				return false;
			}
			exceptionDay = exceptions[i].getStart();
			return true;
		}

		private boolean isValidExceptionIndex(int index) {
			return index >= 0 && index < exceptions.length;
		}

		private void logInvalidExceptionIndex() {
			logger.warning("invalid calendar iterator - index is negative or past bounds. avoiding");
			ErrorLogger.logOnce("CalendarIterator", "invalid calendar iterator i=" + i + "\n" + CriticalPath.getTrace(), null);
		}

		private int dayOfWeek(long day) {
			scratchDate.setTimeInMillis(day);
			return scratchDate.get(Calendar.DAY_OF_WEEK) -1 ;

		}
		private long moveNumberOfDays(int numberOfDays, long fromDay) {
			scratchDate.setTimeInMillis(fromDay);
			scratchDate.add(Calendar.DATE,numberOfDays);
			return scratchDate.getTimeInMillis();
		}

		private long nextDay(long day) {
			scratchDate.setTimeInMillis(day);
			scratchDate.add(Calendar.DATE,forward ? 1 : -1);
			return scratchDate.getTimeInMillis();
		}
		private long prevDay(long day) {
			scratchDate.setTimeInMillis(day);
			scratchDate.add(Calendar.DATE,forward ? -1 : 1);
			return scratchDate.getTimeInMillis();
		}

	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return WorkCalendar.CALENDAR_CATEGORY;
	}

	public void setName(String name) {
		if (name == null)
			throw new IllegalArgumentException("Calendar name must not be null");
		this.name = name;
	}

	public CalendarDefinition getConcreteInstance() {
		return this; // doesn't make sense to call this
	}
	public static final int getDayOfWeek(long date) {
		Calendar scratchDate = DateTime.calendarInstance();
		scratchDate.setTimeInMillis(date);
		return scratchDate.get(Calendar.DAY_OF_WEEK) -1 ;
	}


	public final WorkDay getWorkDay(long date) {
		WorkDay dateSpecific = getDateSpecificWorkDay(date);
		return dateSpecific == null ? getWeekDay(date) : dateSpecific;
	}

	/** Returns the effective exception/work-week override, or {@code null} if none applies. */
	final WorkDay getDateSpecificWorkDay(long date) {
		int i = findExceptionIndex(date);
		if (i >= 0) {
			return exceptions[i];
		}
		WorkWeekPeriod period = findWorkWeekPeriod(date);
		if (period != null) {
			WorkDay periodDay = period.workDayForDate(date);
			return periodDay == null ? getWeekDay(date) : periodDay;
		}
		return null;
	}

	private WorkWeekPeriod findWorkWeekPeriod(long date) {
		WorkWeekPeriod selected = null;
		for (WorkWeekPeriod period : getConcreteInstance().getWorkWeekPeriods()) {
			if (period.contains(date) && (selected == null || period.getStart() >= selected.getStart()))
				selected = period;
		}
		return selected;
	}

	private int findExceptionIndex(long date) {
		long day = DateTime.dayFloor(date);
		WorkDay[] concreteExceptions = getConcreteInstance().exceptions;
		int index = Arrays.binarySearch(concreteExceptions, new Date(day));
		if (index >= 0)
			return index;

		// MSP exceptions can cover date intervals. Find the most recently
		// starting interval that still contains this day, not only an exception
		// whose start exactly equals the requested date.
		for (int candidate = -index - 2; candidate >= 0; candidate--) {
			WorkDay exception = concreteExceptions[candidate];
			if (exception.getStart() <= day && exception.getEnd() >= day
					&& exception != WorkDay.MINIMUM && exception != WorkDay.MAXIMUM)
				return candidate;
		}
		return -1;
	}

	private boolean hasMultiDayException() {
		if (!getConcreteInstance().getWorkWeekPeriods().isEmpty()) return true;
		WorkDay[] concreteExceptions = getConcreteInstance().exceptions;
		for (int i = 1; i < concreteExceptions.length - 1; i++) {
			if (DateTime.dayFloor(concreteExceptions[i].getEnd())
					> DateTime.dayFloor(concreteExceptions[i].getStart()))
				return true;
		}
		return false;
	}

	private WorkDay getWeekDay(long date) {
		return week.getWeekDay(getDayOfWeek(date));
	}

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public long getUniqueId() {
        return id;
    }
    public void setUniqueId(long id) {
        this.id = id;
    }
    transient boolean newId=true;
    public boolean isNew(){
    	return newId;
    }
    public void setNew(boolean newId){
    	this.newId=newId;
    }
	public WorkCalendar getBaseCalendar() {
		return null;
	}

	public boolean dependsOn(WorkCalendar cal) {
		return this == cal;
	}

	public void invalidate() {
		clearAddCache();
	}

	public boolean isInvalid() {
		return !testValid();
	}


	CalendarDefinition intersectWith(CalendarDefinition other) throws InvalidCalendarIntersectionException {
		CalendarDefinition result = new CalendarDefinition();
		result.week = week.intersectWith(other.week);

		WorkDay exceptionDay;
		// merge exceptions
		for (int i = 0; i < exceptions.length; i++) {
			exceptionDay = exceptions[i];
			result.dayExceptions.add(exceptionDay.intersectWith(other.getWorkDay(exceptionDay.getStart())));
		}
		for (int i = 0; i < other.exceptions.length; i++) {
			exceptionDay = other.exceptions[i];
			result.dayExceptions.add(exceptionDay.intersectWith(getWorkDay(exceptionDay.getStart())));
		}
		result.addSentinelsAndMakeArray();
		return result;
	}

	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("CalendarDefinition _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
	}

	public String dump() {
		String result = "Calendar " + getName() + "\n";
		result += "weekdays\n";
		for (int i = 0; i < 7; i++) {
			result += "day[" + i +"]" + getWeekDay(i) +  "\n";
		}
		result += "There are " + exceptions.length + " exceptions\n";
		for (int j = 0; j < exceptions.length; j++) {
			result += "exception" + exceptions[j].toString();
		}
		return result;

	}

}
