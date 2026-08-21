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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.microproject.configuration.CircularDependencyException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.options.CalendarOption;
import com.microproject.pm.key.HasCommonKeyImpl;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;
/**
 * Calendar functions
 */
public class WorkingCalendar implements WorkCalendar,  Serializable, Comparable {
	static final long serialVersionUID = 27738049223431L;
	private static final Logger logger = Logger.getLogger(WorkingCalendar.class.getName());
	private int fixedId = 0;
	public static final WorkingCalendar INVALID_INTERSECTION_CALENDAR = new WorkingCalendar();

// the objects that use this calendar
	private transient HashSet objectsUsing = null;

	public final HashSet getObjectsUsing() {
		if (objectsUsing == null)
			objectsUsing = new HashSet();
		return objectsUsing;
	}
	public void addObjectUsing(HasCalendar cal) {
		getObjectsUsing().add(cal);
	}
	public void removeObjectUsing(HasCalendar cal) {
		getObjectsUsing().remove(cal);
	}

	private void invalidateConcreteInstance() {
		concrete = null;
	}

	private transient HasCommonKeyImpl hasKey = new HasCommonKeyImpl(true,this); //true if calendars aren't internal
	private WorkingCalendar() { // for static
		super();
	}
	public boolean equals(Object arg0) {
		return (this == arg0);
	}

	@Override
	public int hashCode() {
		// equals() is identity-based (this == arg0), so the hash code is the
		// identity hash as well (issue #177).
		return System.identityHashCode(this);
	}
	public static WorkingCalendar getInstance() {
		return new WorkingCalendar();
	}

	public static WorkingCalendar getInstanceBasedOn(WorkCalendar base) {
		WorkingCalendar cal = getInstance();
		try {
			cal.setBaseCalendar(base);
		} catch (CircularDependencyException e) {
			throw impossibleCircularDependency(e);
		}
		return cal;
	}
	public static WorkingCalendar getStandardBasedInstance() {
		WorkingCalendar cal = getInstance();
		try {
			cal.setBaseCalendar(WorkingCalendar.getStandardInstance());
		} catch (CircularDependencyException e) {
			throw impossibleCircularDependency(e);
		}
		return cal;
	}

	public Object clone() throws CloneNotSupportedException {
		WorkingCalendar cal = (WorkingCalendar)super.clone();
	//	cal.hasKey = new HasCommonKeyImpl(this);
		cal.setName(getName());
		return cal;
	}


	public WorkingCalendar makeScratchCopy() {
		WorkingCalendar newOne = null;
		try {
			newOne = new WorkingCalendar();
			newOne.baseCalendar = baseCalendar;
			newOne.setName(getName());
			newOne.differences = (CalendarDefinition) differences.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Calendar definition should be cloneable", e);
		}
		return newOne;
	}


    public long getId() {
        return hasKey.getId();
    }
    public void setId(long id) {
        hasKey.setId(id);
    }
    public long getUniqueId() {
        return hasKey.getUniqueId();
    }
    public void setUniqueId(long id) {
        hasKey.setUniqueId(id);
    }


//	public boolean isNew() {
//		return hasKey.isNew();
//	}
//	public void setNew(boolean isNew) {
//		hasKey.setNew(isNew);
//	}
	public void assignFrom(WorkingCalendar source) {
		baseCalendar = source.baseCalendar;
		if (!source.getName().equals(getName())) {
			setName(source.getName());
//			CalendarService.getInstance().add(this);
		}
		differences = source.differences;
		invalidateConcreteInstance();
	}

	private WorkCalendar baseCalendar = null;
	private CalendarDefinition differences = new CalendarDefinition();

	public WorkDay[] getExceptionDays() { // get day exceptions in derived cal
		return differences.getExceptions();
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return hasKey.getName();
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		hasKey.setName(name);
	}
	public void addOrReplaceException(WorkDay exceptionDay) {
		exceptionDay.initialize(); // make sure cached duration is set
		differences.addOrReplaceException(exceptionDay);
		invalidateConcreteInstance();
	}

	public void removeException(WorkDay exceptionDay) {
		differences.dayExceptions.remove(exceptionDay); // remove any existing
		invalidateConcreteInstance();
	}


/**
 * This function will return a concrete calendar instance.  That is, one for which the days are already merged
 * @return concrete instance
 */
	private transient CalendarDefinition concrete = null;

	public CalendarDefinition getConcreteInstance() {
		if (concrete == null) {
			WorkCalendar base = baseCalendar;
			concrete = new CalendarDefinition(base == null ? null : base.getConcreteInstance(),differences);
		}
		return concrete;
	}

	public void invalidate() {
		invalidateConcreteInstance();
		CalendarService.getInstance().invalidate(this);
	}


	/**
	 * @return Returns the baseCalendar.
	 */
	public WorkCalendar getBaseCalendar() {
		return baseCalendar;
	}

/**
 * Test for ciruclar dependency
 */
	public boolean dependsOn(WorkCalendar cal) {
		if (this == cal)
			return true;
		WorkCalendar base = getBaseCalendar();
		if (base == null)
			return false;
		return base.dependsOn(cal);
	}
	/**
	 * @param baseCalendar The baseCalendar to set.
	 */
	public void setBaseCalendar(WorkCalendar baseCalendar) throws CircularDependencyException {
		if (baseCalendar != null && baseCalendar.dependsOn(this)) // avoid circular
			throw new CircularDependencyException(Messages.getString("Calendar.ExceptionCircular"));
		this.baseCalendar = baseCalendar;
		invalidateConcreteInstance();
	}

	public void changeBaseCalendar(WorkCalendar baseCalendar) throws CircularDependencyException {
		setBaseCalendar(baseCalendar);
	}


	/**
	 * @param dayNum
	 * @return
	 */
	public WorkDay getWeekDay(int dayNum) {
		return differences.week.getWeekDay(dayNum);
	}

	public WorkDay getDerivedWeekDay(int dayNum) {
		WorkDay day = differences.week.getWeekDay(dayNum);
		if (day == null && baseCalendar != null)
			day = ((WorkingCalendar)baseCalendar).getDerivedWeekDay(dayNum);
		return day;
	}

	/**
	 * @param dayNum
	 * @param day
	 */
	public void setWeekDay(int dayNum, WorkDay day) {
		differences.week.setWeekDay(dayNum, day);
	}

	/**
	 * @param day
	 */
	public void setWeekDays(WorkDay day) {
		differences.week.setWeekDays(day);
	}

	/**
	 * @param day
	 */
	public void setWeekends(WorkDay day) {
		differences.week.setWeekends(day);
	}



	public long add(long date, long duration, boolean useSooner) {
//		if (date == 0)
//			DebugUtils.dumpStack("0 date");
		return getConcreteInstance().add(date,duration,useSooner);
	}
	public long compare(long laterDate, long earlierDate, boolean elapsed) {
		return getConcreteInstance().compare(laterDate,earlierDate,elapsed);
	}

	public long adjustInsideCalendar(long date, boolean useSooner) {
		return getConcreteInstance().adjustInsideCalendar(date,useSooner);
	}


	public String getCategory() {
		return CALENDAR_CATEGORY;
	}
	public String toString() {
		return getName();
	}

	private static WorkingCalendar standardInstance = null;
	private static WorkingCalendar defaultInstance = null;

	// Issue #227/#268: the standard calendar singletons (base/default/24h/nightshift) are
	// created via HasCommonKeyImpl(local, this), which mints from LocalSession.localSeed
	// (a persistent per-JVM counter). That minted id was serialized into the .pod and
	// changed on every load, causing non-deterministic (drifting) round-trip output.
	// These singletons already carry a fixedId (0..3); assign a deterministic negative
	// uniqueId range so their identity never drifts and never collides with real
	// (positive) object ids.
	private static final long STANDARD_CALENDAR_UNIQUE_ID_BASE = -1_000_000L;

	static WorkingCalendar getStandardInstance() {
		if (standardInstance != null)
			return standardInstance;

		standardInstance = WorkingCalendar.getInstance();
		WorkDay nonWorking = null;
		WorkDay working = null;
		nonWorking = new WorkDay();
		working = new WorkDay();
		nonWorking.getWorkingHours().setNonWorking();

		try {
			working.getWorkingHours().setInterval(0,hourTime(8), hourTime(12));
			working.getWorkingHours().setInterval(1,hourTime(13), hourTime(17));
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to build standard working calendar", e);
		}


		standardInstance.setWeekends(nonWorking);
		standardInstance.setWeekDays(working); // 8 hours

		standardInstance.setName("default base");
		standardInstance.setUniqueId(STANDARD_CALENDAR_UNIQUE_ID_BASE); // deterministic identity (issue #227/#268)
		return standardInstance;

	}
	static WorkingCalendar getDefaultInstance() {
		if (defaultInstance != null)
			return defaultInstance;


		defaultInstance = getStandardBasedInstance();
		defaultInstance.setName(Messages.getString("Calendar.Standard"));
		defaultInstance.setFixedId(1);
		defaultInstance.setUniqueId(STANDARD_CALENDAR_UNIQUE_ID_BASE - 1); // deterministic identity (issue #227/#268)
		CalendarService.getInstance().add(defaultInstance);

		get24HoursInstance();
		getNightShiftInstance();

		return defaultInstance;
	}
	private static WorkingCalendar _24HoursInstance = null;
	static WorkingCalendar get24HoursInstance() {
		if (_24HoursInstance != null)
			return _24HoursInstance;
		_24HoursInstance = getStandardBasedInstance();
		WorkDay working = null;
		working = new WorkDay();
		try {
			working.getWorkingHours().setInterval(0,hourTime(0), hourTime(0));
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to build 24 hour working calendar", e);
		}


		_24HoursInstance.setWeekends(working);
		_24HoursInstance.setWeekDays(working);

		_24HoursInstance.setName(Messages.getString("Calendar.24Hours"));
		_24HoursInstance.setFixedId(2);
		_24HoursInstance.setUniqueId(STANDARD_CALENDAR_UNIQUE_ID_BASE - 2); // deterministic identity (issue #227/#268)
		CalendarService.getInstance().add(_24HoursInstance); // put standard calendar in list
		return _24HoursInstance;
	}

	private static WorkingCalendar nightShiftInstance = null;
	static WorkingCalendar getNightShiftInstance() {
		if (nightShiftInstance != null)
			return nightShiftInstance;
		nightShiftInstance = WorkingCalendar.getStandardBasedInstance();
		WorkDay nonWorking = null;
		WorkDay working = null;
		nonWorking = new WorkDay();
		working = new WorkDay();
		nonWorking.getWorkingHours().setNonWorking();

		nightShiftInstance.setWeekDay(Calendar.SUNDAY-1,null); // will revert to overall default for sunday which is not working

		WorkDay monday = new WorkDay();
		try {
			monday.getWorkingHours().setInterval(0,hourTime(23), hourTime(0));
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to build night shift Monday calendar", e);
		}
		nightShiftInstance.setWeekDay(Calendar.MONDAY-1,monday);

		try {
			working.getWorkingHours().setInterval(0,hourTime(0), hourTime(3));
			working.getWorkingHours().setInterval(1,hourTime(4), hourTime(8));
			working.getWorkingHours().setInterval(2,hourTime(23), hourTime(0));
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to build night shift weekday calendar", e);
		}
		nightShiftInstance.setWeekDay(Calendar.TUESDAY-1,working);
		nightShiftInstance.setWeekDay(Calendar.WEDNESDAY-1,working);
		nightShiftInstance.setWeekDay(Calendar.THURSDAY-1,working);
		nightShiftInstance.setWeekDay(Calendar.FRIDAY-1,working);

		WorkDay saturday = new WorkDay();
		try {
			saturday.getWorkingHours().setInterval(0,hourTime(0), hourTime(3));
			saturday.getWorkingHours().setInterval(1,hourTime(4), hourTime(8));
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to build night shift Saturday calendar", e);
		}
		nightShiftInstance.setWeekDay(Calendar.SATURDAY-1,saturday);

		nightShiftInstance.setName(Messages.getString("Calendar.NightShift"));
		nightShiftInstance.setFixedId(3);
		nightShiftInstance.setUniqueId(STANDARD_CALENDAR_UNIQUE_ID_BASE - 3); // deterministic identity (issue #227/#268)

		CalendarService.getInstance().add(nightShiftInstance); // put night shift calendar in list
		return nightShiftInstance;
	}

	private static long hourTime(int hour) {
		return WorkingHours.hourTime(hour);
	}

	public String dump() {
		String result = "Calendar " + getName() + "\n";
		result += "weekdays\n";
		for (int i = 0; i < 7; i++) {
			result += "day[" + i +"]" + getWeekDay(i) +  "\n";
		}
		result += "There are " + differences.dayExceptions + " exceptions\n";
		Iterator i = differences.dayExceptions.iterator();
		while (i.hasNext()) {
			result += "exception" + i.next().toString();
		}
		return result;

	}



	private static WorkDay getDay(Collection collection, long day) {
		Iterator i = collection.iterator();
		Date date = new Date(day);
		WorkDay current = null;
		while (i.hasNext()) {
			current = (WorkDay)i.next();
			if (current.compareTo(date) == 0) {
				return current;
			}
		}
		return null;
	}

	DayDescriptor getMonthDayDescriptor(long date) {
		DayDescriptor descriptor = new DayDescriptor();
		int dayNum = CalendarDefinition.getDayOfWeek(date);
		descriptor.workDay = getDay(differences.dayExceptions,date); // is this day modified in derived calendar?
		descriptor.modified = descriptor.workDay != null;

		if (descriptor.workDay == null)
			descriptor.workDay = differences.week.getWeekDay(dayNum); // try difference week day

		if (descriptor.workDay == null) // if not overrideen in derived calendar, see if base calendar has a special day
			descriptor.workDay = getConcreteInstance().getWorkDay(date);

		if (descriptor.workDay == null) // return week day
			descriptor.workDay = getConcreteInstance().week.getWeekDay(CalendarDefinition.getDayOfWeek(date));
		return descriptor;
	}

	DayDescriptor getWeekDayDescriptor(int dayNum) {
		dayNum -=1; // SUNDAY is 1, so need to subtract 1
		DayDescriptor descriptor = new DayDescriptor();
		descriptor.workDay = differences.week.getWeekDay(dayNum);

		descriptor.modified = descriptor.workDay != null;
		descriptor.workDay = getConcreteInstance().week.getWeekDay(dayNum);
//		if (isBaseCalendar()) {
//			// for base calendars, the notion of modified is based on the default work week
//			WorkDay baseDay = WorkingCalendar.getDefaultInstance(null).getWeekDay(dayNum);
//			descriptor.modified = !(baseDay.hasSameWorkHours(descriptor.workDay));
//		} else {
//			if (descriptor.workDay == null)
//				descriptor.workDay = getConcreteInstance().week.getWeekDay(dayNum);
//		}
		return descriptor;
	}

	void makeDefaultDay(long date) {
		WorkDay day = getDay(differences.dayExceptions,date);
		if (day != null)
			differences.dayExceptions.remove(day);
		invalidateConcreteInstance();
	}

	void makeDefaultWeekDay(int dayNum) {
		dayNum -=1; // SUNDAY is 1, so need to subtract 1
		differences.week.setWeekDay(dayNum,null);
		invalidateConcreteInstance();
	}

	void setDayNonWorking(long date) {
		WorkDay day = new WorkDay(date,date);
		addOrReplaceException(day);
	}

	void setWeekDayNonWorking(int dayNum) {
		dayNum -=1; // SUNDAY is 1, so need to subtract 1
		WorkDay day = new WorkDay();
		differences.week.setWeekDay(dayNum,day);
		invalidateConcreteInstance();
	}

	void setDayWorkingHours(long date, WorkingHours workingHours) throws WorkRangeException{
		workingHours.validate();
		WorkDay day = new WorkDay(date,date);
		day.setWorkingHours(workingHours);
		addOrReplaceException(day);
	}
	void setWeekDayWorkingHours(int dayNum, WorkingHours workingHours) throws WorkRangeException{
		dayNum -=1; // SUNDAY is 1, so need to subtract 1
		workingHours.validate();
		WorkDay day = new WorkDay();
		day.setWorkingHours(workingHours);
		differences.week.setWeekDay(dayNum,day);
		invalidateConcreteInstance();
	}

	String serializedName=""; // non transient version of name for serialization
	private void writeObject(ObjectOutputStream s) throws IOException {
		serializedName = getName();
		if (baseCalendar == CalendarService.getInstance().getStandardInstance()) // don't serialize default base. treat it as null
			baseCalendar = null;
	    s.defaultWriteObject();
	    hasKey.serialize(s);
	    if (baseCalendar == null)
	    	baseCalendar = CalendarService.getInstance().getStandardInstance(); // put it back so program will work
	}
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException  {
	    s.defaultReadObject();
	    hasKey=HasCommonKeyImpl.deserialize(s,this);
	    if (serializedName == null)
	    	serializedName = "";
	    setName(serializedName);
	    if (baseCalendar == null)
	    	baseCalendar = CalendarService.getInstance().getStandardInstance();
	    CalendarService.getInstance().add(this);
	}

	public boolean isBaseCalendar() {
		return baseCalendar == getStandardInstance();
	}

	public final int getFixedId() {
		return fixedId;
	}
	public final void setFixedId(int fixedId) {
		this.fixedId = fixedId;
	}
	public static ArrayList extractCalendars(Collection collection) {
		ArrayList list = new ArrayList(collection.size());
		Iterator i = collection.iterator();
		WorkingCalendar cal;
		while (i.hasNext()) {
			cal = (WorkingCalendar) ((HasCalendar)i.next()).getWorkCalendar();
			if (cal != null)
				list.add(cal);
		}
		Collections.sort(list);
		return list;
	}

	public static ArrayList extractCalendars(NodeHierarchy hierarchy) {
		final ArrayList list = new ArrayList();
		hierarchy.visitAll(new Consumer<Object>() { public void accept(Object arg0) {
				if (arg0 != null) {
					Object impl = ((Node)arg0).getImpl();
					if (impl instanceof HasCalendar)
						list.add(impl);
				}
			}});
		return WorkingCalendar.extractCalendars(list);
	}

	public int compareTo(Object arg0) {
		if (arg0 == null)
			return 1;
		if (!(arg0 instanceof WorkingCalendar))
			return -1;
		return getName().compareTo(((WorkingCalendar)arg0).getName());
	}

	public void notifyChanged() {
		invalidateConcreteInstance();
	}
	public boolean isInvalid() {
		return concrete == null;
	}

	public WorkingCalendar intersectWith(WorkingCalendar other) throws InvalidCalendarIntersectionException {
		CalendarDefinition newDef = new CalendarDefinition();

		// do week
    	WorkWeek weekResult = new WorkWeek();
        for (int i = 0; i < WorkWeek.DAYS_IN_WEEK; i++)
        	weekResult.workDay[i] = getDerivedWeekDay(i).intersectWith(other.getDerivedWeekDay(i));
        weekResult.updateWorkingDuration();

        if (weekResult.getDuration() == 0) // a calendar cannot have no working time for its work week
        	throw new InvalidCalendarIntersectionException();

        // do exceptions
		CalendarDefinition thisDef = getConcreteInstance();
		CalendarDefinition otherDef = other.getConcreteInstance();
		WorkDay exceptionDay;
		// merge exceptions
		for (int i = 0; i < thisDef.exceptions.length; i++) {
			exceptionDay = thisDef.exceptions[i];
			newDef.dayExceptions.add(exceptionDay.intersectWith(otherDef.getWorkDay(exceptionDay.getStart())));
		}
		for (int i = 0; i < otherDef.exceptions.length; i++) {
			exceptionDay = otherDef.exceptions[i];
			newDef.dayExceptions.add(exceptionDay.intersectWith(thisDef.getWorkDay(exceptionDay.getStart())));
		}
		newDef.addSentinelsAndMakeArray();
        newDef.week = weekResult;

        WorkingCalendar intersection = new WorkingCalendar();
        intersection.concrete = newDef;
        intersection.setName("AssignCal: " + getName() + "/" + other.getName());
        return intersection;

    }
	public void addCalendarTime(long start, long end) {
		start = DateTime.dayFloor(start);
		end = DateTime.dayFloor(end);

		for (long day = start; day < end; day = DateTime.nextDay(day)) {
			WorkDay workDay = new WorkDay(day,day);
			WorkingHours hours = (WorkingHours) (CalendarOption.getInstance().isAddedCalendarTimeIsNonStop()
					 ? WorkingHours.getNonStop().clone()
					 : WorkingHours.getDefault().clone());

			workDay.setWorkingHours(hours);
			addOrReplaceException(workDay);
		}
		invalidate(); // the calendar needs to be reevaluated
	}

	private transient boolean dirty;
	public boolean isDirty() {
		return dirty;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("WorkingCalendar _setDirty("+dirty+"): "+getName());
		this.dirty = dirty;
	}

	public void removeEmptyDays() {
		int nullCount=0;
		for (int i = 0; i < 7; i++) {
			WorkDay w = getWeekDay(i);
			if (w == null)
				continue;
			if (w.getWorkingHours() == null || !w.getWorkingHours().hasHours())
				setWeekDay(i,null);
			if (getWeekDay(i) == null)
				nullCount++;
		}
//		if ( nullCount == 7) { // if all nulls, copy default cal
//			for (int i = 0; i < 7; i++) {
//				setWeekDay(i,(WorkDay) getDefaultInstance().getWeekDay(i).clone());
//			}
//		}

	}

	private static IllegalStateException impossibleCircularDependency(CircularDependencyException e) {
		return new IllegalStateException("Unexpected circular calendar dependency during instance creation", e);
	}

}
