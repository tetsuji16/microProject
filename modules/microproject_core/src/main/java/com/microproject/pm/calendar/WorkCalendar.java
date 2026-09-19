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

import com.microproject.configuration.NamedItem;
import com.microproject.server.data.DataObject;
import com.microproject.strings.Messages;

public interface WorkCalendar extends Cloneable, NamedItem, DataObject {
	static final long MILLIS_IN_DAY = (long) (1000L * 60 * 60 * 24);
	static final long MILLIS_IN_WEEK = (long) (7L * MILLIS_IN_DAY);
	static final long MILLIS_IN_MINUTE = (long) (1000L * 60);
	static final long MILLIS_IN_HOUR = (long) (1000L * 60 * 60);
	static final String CALENDAR_CATEGORY="Calendar";
	
// Status values in modify working time dialog
	static final String WORKING=Messages.getString("Calendar.Working");
	static final String NONWORKING=Messages.getString("Calendar.Nonworking");
	static final String EDITED_WORKING_HOURS=Messages.getString("Calendar.EditedWorkingHours");

// additional status values for "derived" calendar	
	static final String EDITS_TO_A_DAY_OF_THE_WEEK=Messages.getString("Calendar.EditsToADayOfTheWeek");
	static final String EDITS_TO_AN_INDIVIDUAL_DAY=Messages.getString("Calendar.EditsToAnIndividualDay");
	static final String UNMODIFIED=Messages.getString("Calendar.Unmodified");
	
	long adjustInsideCalendar(long date, boolean useSooner);	
    long add(long date, long duration, boolean useSooner);
    long compare(long laterDate, long earlierDate, boolean elapsed);
    Object clone() throws CloneNotSupportedException;
    void setName(String name);
	CalendarDefinition getConcreteInstance();
	WorkCalendar getBaseCalendar();
	boolean dependsOn(WorkCalendar cal);
	void invalidate();
	boolean isInvalid();

}
