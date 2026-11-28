/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.calendar;

import com.microproject.configuration.NamedItem;
import com.microproject.persistence.data.DataObject;
import com.microproject.i18n.Messages;

/**
 * A named calendar that defines working-time adjustment and scheduling operations.
 * Date arguments are scheduler timestamps; durations use the scheduler's
 * millisecond-based duration representation, including its elapsed-time flag.
 */
public interface WorkCalendar extends Cloneable, NamedItem, DataObject {
	static final long MILLIS_IN_DAY = (1000L * 60 * 60 * 24);
	static final long MILLIS_IN_WEEK = (7L * MILLIS_IN_DAY);
	static final long MILLIS_IN_MINUTE = (1000L * 60);
	static final long MILLIS_IN_HOUR = (1000L * 60 * 60);
	static final String CALENDAR_CATEGORY="Calendar";
	
// Status values in modify working time dialog
	static final String WORKING=Messages.getString("Calendar.Working");
	static final String NONWORKING=Messages.getString("Calendar.Nonworking");
	static final String EDITED_WORKING_HOURS=Messages.getString("Calendar.EditedWorkingHours");

// additional status values for "derived" calendar	
	static final String EDITS_TO_A_DAY_OF_THE_WEEK=Messages.getString("Calendar.EditsToADayOfTheWeek");
	static final String EDITS_TO_AN_INDIVIDUAL_DAY=Messages.getString("Calendar.EditsToAnIndividualDay");
	static final String UNMODIFIED=Messages.getString("Calendar.Unmodified");
	
	/** Moves a timestamp to an allowed working-time boundary in the selected direction. */
	long adjustInsideCalendar(long date, boolean useSooner);
	/** Adds a duration according to this calendar and the requested boundary direction. */
    long add(long date, long duration, boolean useSooner);
    /** Returns the signed elapsed or scheduled working duration from earlier to later. */
    long compare(long laterDate, long earlierDate, boolean elapsed);
    /** Creates an independent calendar copy. */
    Object clone() throws CloneNotSupportedException;
	/** Changes this calendar's display name. */
    void setName(String name);
	/** Returns the concrete calendar definition used for calculations. */
	CalendarDefinition getConcreteInstance();
	/** Returns the calendar from which this one inherits. */
	WorkCalendar getBaseCalendar();
	/** Returns whether this calendar depends on the supplied calendar. */
	boolean dependsOn(WorkCalendar cal);
	/** Invalidates cached schedule values after a calendar change. */
	void invalidate();
	/** Returns whether the concrete calculation state needs rebuilding. */
	boolean isInvalid();

}
