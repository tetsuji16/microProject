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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.configuration.Settings;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/**
 * 
 */
public class WorkingHours implements Cloneable, Serializable {
	static final long serialVersionUID = 83888849333431L;
	//private static Log log = LogFactory.getLog(WorkingHours.class);
	private static long MS_PER_MINUTE = 60000L;
	private static final Logger logger = Logger.getLogger(WorkingHours.class.getName());
	/**
	 * milliseconds of work time in the day
	 */
	long duration = 0;

	WorkRange workRange[] = new WorkRange[Settings.CALENDAR_INTERVALS];
	private static GregorianCalendar helper = DateTime.calendarInstance();;
	
	
	public Object clone() {
		WorkingHours newOne = new WorkingHours();
		for (int i = 0; i < workRange.length; i++) {
			if (workRange[i] == null)
				newOne.workRange[i] = null;
			else
				newOne.workRange[i] = (WorkRange) workRange[i].clone();
		}
		newOne.duration = duration;
		return newOne;
	}
	
/** Create a new working hours by intersecting this one's ranges with another's.  Not that it's possible that the result has
 * up to twice as many ranges as normal in a worst case
 * @param other
 * @return
 */	
	WorkingHours intersectWith(WorkingHours other) {
		int thisIndex = 0;
		int otherIndex = 0;
		
		long start;
		long end;
		WorkRange thisRange;
		WorkRange otherRange;
		ArrayList list = new ArrayList();
		for(;;) {
			// check boundary conditions.  if one of the working hours is exhausted, then no more intersection
			if (thisIndex == workRange.length)
				break;
			if (otherIndex == other.workRange.length)
				break;
			
			thisRange = workRange[thisIndex];
			otherRange = other.workRange[otherIndex];
			if (thisRange == null || otherRange == null)
				break;
			
			
			// the start is always the maximum of the current ranges
			start = Math.max(thisRange.getStart(), otherRange.getStart());
			
			// the end is the minimum of the current ranges
			if (thisRange.getEnd() < otherRange.getEnd()) {
				end = thisRange.getEnd();
				thisIndex++;
			} else {
				end = otherRange.getEnd();
				otherIndex++;
			}
			if (end > start) //if the range is not degenerate, then there is an overlap
				try {
					list.add(new WorkRange(start,end));
				} catch (WorkRangeException e) {
					logger.log(Level.WARNING, "Failed to create intersected work range", e);
				}
		}
		// make a new working hours and use the work ranges that were generated
		WorkingHours result = new WorkingHours();
		result.workRange = new WorkRange[list.size()];
		list.toArray(result.workRange);
		result.initialize();
		return result;
		
	}
	
	synchronized private static long getHoursAndMinutes(Date date) {
		helper.setTime(date);
		// the date needs to be normalized for GMT.  because can wrap around, use modulus
		long timeZoneOffsetMinutes = helper.getTimeZone().getOffset(date.getTime()) / 60000L;
		long minutes = 60L *  (24 + helper.get(GregorianCalendar.HOUR_OF_DAY)) + helper.get(GregorianCalendar.MINUTE) - timeZoneOffsetMinutes;
		minutes = minutes % (24 * 60);
		return 60000L * minutes;
	}
	synchronized public static long hourTime(int hour) {
		helper.setTimeInMillis(0);
		helper.set(GregorianCalendar.HOUR_OF_DAY,hour);
		return helper.getTimeInMillis();
	}

	
	/**
	 * Used by importing, not the dialog box
	 * @param number
	 * @param start
	 * @param end
	 * @return
	 */public boolean setInterval(int number, Date start, Date end) {
		if (start == null || end == null)
			return false;
		
		try {
			setInterval(number,getHoursAndMinutes(start),getHoursAndMinutes(end));
			return true;
		} catch (WorkRangeException e) {
			logger.log(Level.WARNING, "Failed to set working interval from dates", e);
			return false;
		}
		
	}
	/**
	 * Set an interval.  Start and end must be on 1/1/70 and must have their hours set using a GregorianCalendar
	 * to avoid daylight savings issues.
	 * @param number - Range number 0 up to Settings.CALENDAR_INTERVALS -1.  Currently range is 0-4
	 * @param start - start time.  A value of -1 signifies that this range is null
	 * @param end - end time.  If end time is 0, it is treated as midnight the next day
	 * @throws WorkRangeException
	 */
	public void setInterval(int number, long start, long end) throws WorkRangeException {
		if (start == -1 || end == -1)
			workRange[number] = null;
		else {
			GregorianCalendar cal = DateTime.calendarInstance();
			cal.setTimeInMillis(end);
			if (cal.get(GregorianCalendar.HOUR_OF_DAY) == 0 && cal.get(GregorianCalendar.MINUTE) == 0) { // test for midnight end next day
				cal.add(GregorianCalendar.HOUR_OF_DAY,24);
				end = cal.getTimeInMillis();
			}
			workRange[number] = new WorkRange(start,end);			
		}
			
		initialize();
	}
	
	
	public WorkRange getInterval(int number){
	    return workRange[number];
	}
	
	public List getIntervals(){
	    return Arrays.asList(workRange);
	}
	
	
	/**
	 * 
	 */
	public WorkingHours() {
		super();
		for (int i = 0; i < workRange.length; i++)
			workRange[i] = null;
	}
	
	/**
	 * Validate that the work ranges do not overlap
	 * @param range
	 * @return
	 */
	void validate() throws WorkRangeException {
		boolean foundNull = false;
		
		// check for gaps first
		for (int i = 0; i < workRange.length - 1; i++) {
			if (workRange[i] == null) {
				foundNull = true;
			} else {
				if (foundNull)
					throw new WorkRangeException(Messages.getString("WorkRangeException.RangeIncomplete"));
			}
			
		}
		for (int i = 0; i < workRange.length - 1; i++) {
			for (int j = i+1; j < workRange.length; j++) {
				if (workRange[i] != null && !workRange[i].isBefore(workRange[j]))
					throw new WorkRangeException(Messages.getString("WorkRangeException.RangesMustBeOrdered"));
			}
		}
		initialize();
	}
	
	/**
	 * @return Returns the workTime.
	 */
	public long getDuration() {
		return duration;
	}

	
	void initialize() {
		duration = 0;
		for (int i =0; i <workRange.length; i++) {
			if (workRange[i] != null)
				duration += workRange[i].calcWorkingHours();
		}
	}
 	
	public void setNonWorking() {
		duration = 0;
	}


	/**
	 * Calculates the time of day when there is still x time left to do
	 * @param duration The x time of work left after the return value
	 * @return Time of day 
	 */
	public long calcTimeAtRemainingWork(long duration) {
		long work = 0;
		for (int i = workRange.length-1; i >=0; i--) {
			if (workRange[i] != null) {
				work += workRange[i].calcWorkingHours();
				if (work >= duration) {
					return (workRange[i].getStart() + (work - duration));
				}
			}
		}
//		log.error("calcTimeAtRemainingWork didn't finish");
		return -1; // error, return day start
	}	


	/**
	 * @param duration
	 * @return
	 */
	public long calcTimeAtWork(long duration) {
		long work = 0;
		for (int i =0; i <workRange.length; i++) {
			if (workRange[i] != null) {
				work += workRange[i].calcWorkingHours();
				if (work >= duration) {
					return (workRange[i].getEnd() - (work - duration));
				}
			}
		}
//		log.error("calcTimeAtWork didn't finish");
		return -1;//24L*60*60*1000; // error return midnight next day
	}	
	

	/**
	 * Calculate how much work time is remaining after the given time. 
	 * @param time
	 * @return
	 */
	public long calcWorkTimeAfter(long time) {
		long work = 0;
		for (int i =0; i <workRange.length; i++) {
			if (workRange[i] != null) {
				if (workRange[i].getEnd() > time)
					work += (workRange[i].getEnd() - Math.max(time,workRange[i].getStart()));
			}
		}
		return work;
	}
	
	/**
	 * @param date
	 * @return
	 */
	public long calcWorkTimeBefore(long time) {
		return duration - calcWorkTimeAfter(time);
	}
	
	public long calcWorkTime(long time, boolean after) {
		return after ? calcWorkTimeAfter(time) : calcWorkTimeBefore(time);
	}

	public String toString() {
		String result = "WorkingHours\n";
		for (int i =0; i <workRange.length; i++) {
			if (workRange[i] != null) {
				if (result.length() != 0)
					result += "\n";
				result +="Range " + i + " - " + workRange[i]; 
			}
		}
		return result;
	}
	private static WorkingHours defaultWorkingHours = null;
	
	public static WorkingHours getDefault() {
		if (defaultWorkingHours == null) {
			defaultWorkingHours = new WorkingHours();
			try {
				Calendar cal = DateTime.calendarInstance();
				cal.setTimeInMillis(0);
				cal.set(Calendar.HOUR_OF_DAY,8);
				long start = cal.getTimeInMillis();
				cal.set(Calendar.HOUR_OF_DAY,12);
				long end = cal.getTimeInMillis();
				
				defaultWorkingHours.setInterval(0,start, end);
				
				cal.set(Calendar.HOUR_OF_DAY,13);
				start = cal.getTimeInMillis();
				cal.set(Calendar.HOUR_OF_DAY,17);
				end = cal.getTimeInMillis();
				defaultWorkingHours.setInterval(1,start, end);
				
				
				
			} catch (WorkRangeException e) {
				logger.log(Level.WARNING, "Failed to build default working hours", e);
			}
		}
		return defaultWorkingHours;
	}
	private static WorkingHours nonStopWorkingHours = null;
	
	public static WorkingHours getNonStop() {
		if (nonStopWorkingHours == null) {
			nonStopWorkingHours = new WorkingHours();
			try {
				Calendar cal = DateTime.calendarInstance();
				cal.setTimeInMillis(0);
				cal.set(Calendar.HOUR_OF_DAY,0);
				long start = cal.getTimeInMillis();
				
				nonStopWorkingHours.setInterval(0,start, start);
				
				
			} catch (WorkRangeException e) {
				logger.log(Level.WARNING, "Failed to build nonstop working hours", e);
			}
		}
		return nonStopWorkingHours;
	}

	
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof WorkingHours))
			return false;
		if (this == arg0)
			return true;
		WorkingHours to = (WorkingHours)arg0;
		for (int i = 0; i < workRange.length; i++) {
			if (workRange[i] != null) {
				if (!workRange[i].equals(to.workRange[i]))
					return false;
			} else if (to.workRange[i] != null) {
					return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return java.util.Arrays.hashCode(workRange);
	}
	
	public boolean hasHours() {
		// Query only: must not mutate duration (see issue #156). Callers rely on
		// duration to decide whether a day is working; zeroing it here corrupted
		// the cached working time and, via the shared default WorkDay singleton,
		// the app-wide default calendar.
		for (int i =0; i <workRange.length; i++) {
			if (workRange[i] != null)
				return true;
		}
		return false;
	}
}
