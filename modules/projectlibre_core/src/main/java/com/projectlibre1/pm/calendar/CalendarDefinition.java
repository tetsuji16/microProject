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
package com.projectlibre1.pm.calendar;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.projectlibre1.datatype.Duration;
import com.projectlibre1.pm.criticalpath.CriticalPath;
import com.projectlibre1.server.access.ErrorLogger;
import com.projectlibre1.util.DateTime;

/**
 * This class holds specific calendar informatin either for a base calendar or a concrete one, as well as date math functions
 */
public class CalendarDefinition implements WorkCalendar, Cloneable {
	static final long serialVersionUID = 73883742020831L;
	private static final Logger logger = Logger.getLogger(CalendarDefinition.class.getName());
	TreeSet<WorkDay> dayExceptions = null;
	WorkDay[] exceptions = null;
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
			week = (WorkWeek)  base.week.clone(); // copy the week days
		}
		week.addDaysFrom(differences.week); // Now replace any special weekdays

		@SuppressWarnings("unchecked")
		TreeSet<WorkDay> clonedExceptions = (TreeSet<WorkDay>) differences.dayExceptions.clone();
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
		exceptions = new WorkDay[dayExceptions.size()];
		dayExceptions.toArray(exceptions);

	}
	public WorkDay[] getExceptions() {
		return exceptions;
	}

	public WorkDay getWeekDay(int d) {
		return week.getWeekDay(d);
	}
	void addOrReplaceException(WorkDay exceptionDay) {
		dayExceptions.remove(exceptionDay); // remove any existing
		dayExceptions.add(exceptionDay);
		exceptions = new WorkDay[dayExceptions.size()];
		dayExceptions.toArray(exceptions);
	}


	public Object clone() throws CloneNotSupportedException {
		CalendarDefinition newOne = (CalendarDefinition) super.clone();
		newOne.week = (WorkWeek) week.clone();
		newOne.dayExceptions = new TreeSet<WorkDay>();

		Iterator<WorkDay> i = dayExceptions.iterator();
		while (i.hasNext())
			newOne.dayExceptions.add((WorkDay) i.next().clone());
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
	static final class AddCacheKey {
		final long date;
		final long duration;
		final boolean useSooner;

		AddCacheKey(long date, long duration, boolean useSooner) {
			this.date = date;
			this.duration = duration;
			this.useSooner = useSooner;
		}

		@Override
		public int hashCode() {
			return (int) (date ^ (date >>> 32) ^ duration ^ (duration >>> 32) ^ (useSooner ? 1 : 0));
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof AddCacheKey) {
				AddCacheKey o = (AddCacheKey) obj;
				return date == o.date && duration == o.duration && useSooner == o.useSooner;
			}
			return false;
		}
	}

	/**
	 * Clear the add() result cache. Called after each scheduling pass to prevent stale results.
	 */
	public void clearAddCache() {
		addCache.clear();
	}

	// Track all CalendarDefinition instances that have been used for caching.
	// WeakHashMap ensures no memory leak - entries are removed when CalendarDefinition is GC'd.
	private static final java.util.concurrent.ConcurrentHashMap<CalendarDefinition, Boolean> cachedInstances =
		new java.util.concurrent.ConcurrentHashMap<>();

	private void markCacheUsed() {
		cachedInstances.put(this, Boolean.TRUE);
	}

	/**
	 * Clear add() result caches on all tracked CalendarDefinition instances.
	 * Called after each scheduling pass to free memory and prevent stale results.
	 */
	public static void clearAllAddCaches() {
		for (CalendarDefinition cd : cachedInstances.keySet()) {
			cd.addCache.clear();
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
		long result = elapsed ? addElapsedTime(date, duration, useSooner) : addScheduledTime(date, duration, useSooner, forward);
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
		long numWeeks;
		while ((numWeeks = (duration / weekDuration)) != 0) {
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

		long numWeeks = (iterator.dayOf(laterDate) - currentDay) / WorkWeek.MS_IN_WEEK;
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
			if (day == exceptionDay) {
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
		int i = findExceptionIndex(date);
		if (i >= 0) {
			return exceptions[i];
		}
		return getWeekDay(date);
	}

	private int findExceptionIndex(long date) {
		Date searchDate = new Date(DateTime.dayFloor(date));
		return Arrays.binarySearch(getConcreteInstance().exceptions, searchDate);
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
