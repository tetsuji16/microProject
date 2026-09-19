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
package com.microproject.options;
import java.util.GregorianCalendar;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.microproject.datatype.Duration;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.util.DateTime;

/**
 *
 */
public class CalendarOption implements java.io.Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6714103946319228798L;
	private static CalendarOption instance = null;
	private static CalendarOption defaultInstance = null;
	private boolean showTimeInDates = false;
	
	public final boolean isAddedCalendarTimeIsNonStop() {
		return addedCalendarTimeIsNonStop;
	}
	public final void setAddedCalendarTimeIsNonStop(boolean addedCalendarTimeIsNonStop) {
		this.addedCalendarTimeIsNonStop = addedCalendarTimeIsNonStop;
	}
	public static CalendarOption getInstance() {
		if (instance == null)
			instance = new CalendarOption();
		return instance;
	}
	public static CalendarOption getNewInstance() {
		return new CalendarOption();
	}
	public static CalendarOption getDefaultInstance() {
		if (defaultInstance == null)
			defaultInstance = new CalendarOption();
		return defaultInstance;
	}
	// allow setting of options so that default options can be toggled on and off for import export
	public static final void setInstance(CalendarOption instance) {
		CalendarOption.instance = instance;
	}
	private CalendarOption() {
		// note that the mpxj library uses the default values when importing or exporting
		syncDefaultStartTime();
		syncDefaultEndTime();
	}
	
	int weekStartsOn = GregorianCalendar.SUNDAY;
	int fiscalYearStartsIn = GregorianCalendar.JANUARY;
	int defaultStartHour = 8;
	int defaultEndHour = 17;
 
	GregorianCalendar defaultStartTime = DateTime.calendarInstance();
	GregorianCalendar defaultEndTime = DateTime.calendarInstance();
	double hoursPerDay = 8.0D;
	double hoursPerWeek = 40.0D;
	double daysPerMonth = 20.0D;
	
	public double getFractionOfDayThatIsWorking() {
		return hoursPerDay / 24.0;
	}
	long defaultDuration = Duration.setAsEstimated((long) (WorkCalendar.MILLIS_IN_HOUR * hoursPerDay),true);
	// when typing values on a non work day, the day is added to the assignment calendar.  If it is non stop, then a 24 hour exception
	// is used (MSP does this). If false, a default day is used.  The MSP behaviour is the default.  I don't think it is all that logical
	// so i provide the option to use a default day instead

	boolean addedCalendarTimeIsNonStop = false; 
	/**
	 * @return Returns the daysPerMonth.
	 */
	public double getDaysPerMonth() {
		return daysPerMonth;
	}
	/**
	 * @param daysPerMonth The daysPerMonth to set.
	 */
	public void setDaysPerMonth(double daysPerMonth) {
		this.daysPerMonth = daysPerMonth;
	}

	public long getMillisPerDay() {
		return (long) (WorkCalendar.MILLIS_IN_HOUR * hoursPerDay);
	}
	/**
	 * @return Returns the fiscalYearStartsIn.
	 */
	public int getFiscalYearStartsIn() {
		return fiscalYearStartsIn;
	}
	/**
	 * @param fiscalYearStartsIn The fiscalYearStartsIn to set.
	 */
	public void setFiscalYearStartsIn(int fiscalYearStartsIn) {
		this.fiscalYearStartsIn = fiscalYearStartsIn;
	}
	/**
	 * @return Returns the hoursPerDay.
	 */
	public double getHoursPerDay() {
		return hoursPerDay;
	}
	/**
	 * @param hoursPerDay The hoursPerDay to set.
	 */
	public void setHoursPerDay(double hoursPerDay) {
		this.hoursPerDay = hoursPerDay;
		defaultDuration = Duration.setAsEstimated((long) (WorkCalendar.MILLIS_IN_HOUR * hoursPerDay),true);

	}
	/**
	 * @return Returns the hoursPerWeek.
	 */
	public double getHoursPerWeek() {
		return hoursPerWeek;
	}
	/**
	 * @param hoursPerWeek The hoursPerWeek to set.
	 */
	public void setHoursPerWeek(double hoursPerWeek) {
		this.hoursPerWeek = hoursPerWeek;
	}
	/**
	 * @return Returns the weekStartsOn.
	 */
	public int getWeekStartsOn() {
		return weekStartsOn;
	}
	/**
	 * @param weekStartsOn The weekStartsOn to set.
	 */
	public void setWeekStartsOn(int weekStartsOn) {
		this.weekStartsOn = weekStartsOn;
	}
	
	public double hoursPerMonth() {
		return hoursPerDay * daysPerMonth;
	}
	/**
	 * @return Returns the defaultEndTime.
	 */
	public GregorianCalendar getDefaultEndTime() {
		return defaultEndTime;
	}
	/**
	 * @param defaultEndTime The defaultEndTime to set.
	 */
	public void setDefaultEndTime(GregorianCalendar defaultEndTime) {
		this.defaultEndTime = defaultEndTime;
		if (defaultEndTime != null) {
			this.defaultEndHour = defaultEndTime.get(GregorianCalendar.HOUR_OF_DAY);
		}
	}
	/**
	 * @return Returns the defaultStartTime.
	 */
	public GregorianCalendar getDefaultStartTime() {
		return defaultStartTime;
	}
	/**
	 * @param defaultStartTime The defaultStartTime to set.
	 */
	public void setDefaultStartTime(GregorianCalendar defaultStartTime) {
		this.defaultStartTime = defaultStartTime;
		if (defaultStartTime != null) {
			this.defaultStartHour = defaultStartTime.get(GregorianCalendar.HOUR_OF_DAY);
		}
	}
	
	public long makeValidStart(long start, boolean force) {
		
		start = DateTime.minuteFloor(start);
		GregorianCalendar cal = DateTime.calendarInstance();
		cal.setTimeInMillis(start);
		int year = cal.get(GregorianCalendar.YEAR);
		int dayOfYear = cal.get(GregorianCalendar.DAY_OF_YEAR);
		if (force || cal.get(GregorianCalendar.HOUR_OF_DAY) == 0 && cal.get(GregorianCalendar.MINUTE) == 0) {
			cal.set(GregorianCalendar.HOUR_OF_DAY,getDefaultStartTime().get(GregorianCalendar.HOUR_OF_DAY));
			cal.set(GregorianCalendar.MINUTE,getDefaultStartTime().get(GregorianCalendar.MINUTE));
			cal.set(GregorianCalendar.YEAR,year);
			cal.set(GregorianCalendar.DAY_OF_YEAR,dayOfYear);
		}
		return cal.getTimeInMillis();
	}
		
	public long makeValidEnd(long end, boolean force) {
		end =DateTime.minuteFloor(end);		
		GregorianCalendar cal = DateTime.calendarInstance();
		cal.setTimeInMillis(end);
		if (force || cal.get(GregorianCalendar.HOUR_OF_DAY) == 0 && cal.get(GregorianCalendar.MINUTE) == 0) {
			cal.set(GregorianCalendar.HOUR_OF_DAY,getDefaultEndTime().get(GregorianCalendar.HOUR_OF_DAY));
			cal.set(GregorianCalendar.MINUTE,getDefaultEndTime().get(GregorianCalendar.MINUTE));
		}
		return cal.getTimeInMillis();
	}
	
	
	
	/**
	 * @return Returns the defaultDuration.
	 */
	public long getDefaultDuration() {
		return defaultDuration;
	}
	/**
	 * @param defaultDuration The defaultDuration to set.
	 */
	public void setDefaultDuration(long defaultDuration) {
		this.defaultDuration = defaultDuration;
	}
	public final int getDefaultEndHour() {
		return defaultEndHour;
	}
	public final void setDefaultEndHour(int defaultEndHour) {
		this.defaultEndHour = defaultEndHour;
		syncDefaultEndTime();
	}
	public final int getDefaultStartHour() {
		return defaultStartHour;
	}
	public final void setDefaultStartHour(int defaultStartHour) {
		this.defaultStartHour = defaultStartHour;
		syncDefaultStartTime();
	}

	public boolean isShowTimeInDates() {
		return showTimeInDates;
	}

	public void setShowTimeInDates(boolean showTimeInDates) {
		this.showTimeInDates = showTimeInDates;
	}
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	private void syncDefaultStartTime() {
		defaultStartTime.set(GregorianCalendar.HOUR_OF_DAY, defaultStartHour);
		defaultStartTime.set(GregorianCalendar.MINUTE, 0);
		defaultStartTime.set(GregorianCalendar.SECOND, 0);
		defaultStartTime.set(GregorianCalendar.MILLISECOND, 0);
	}

	private void syncDefaultEndTime() {
		defaultEndTime.set(GregorianCalendar.HOUR_OF_DAY, defaultEndHour);
		defaultEndTime.set(GregorianCalendar.MINUTE, 0);
		defaultEndTime.set(GregorianCalendar.SECOND, 0);
		defaultEndTime.set(GregorianCalendar.MILLISECOND, 0);
	}
}
