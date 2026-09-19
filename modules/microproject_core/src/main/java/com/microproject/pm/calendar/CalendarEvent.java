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

import com.microproject.pm.time.ImmutableInterval;

/**
 * An immutable calendar event 
 */
public class CalendarEvent extends ImmutableInterval implements Serializable {
	static final long serialVersionUID = 26373991911119L;
	private static String DEFAULT_DESCRIPTION = "Default";
	private String description;
	/**
	 * Constructor taking two dates 
	 */
	public CalendarEvent(long start, long end, String description)  {
		super(start,end);
		this.description = description;
	}

	public CalendarEvent(long start, long end) {
		this(start,end,DEFAULT_DESCRIPTION);
	}
	
/**
 * Constructor for an event which takes place on one day
 * @param date
 */
	public CalendarEvent(long date, String description) {
		this(date,date, description);
	}

	public CalendarEvent(long date) {
		this(date, DEFAULT_DESCRIPTION);
	}
	
	public boolean equals(Object e) {
		if (! (e instanceof CalendarEvent))
			return false;
		if (! super.equals(e))
			return false;
		String otherDescription = ((CalendarEvent)e).description;
		return description == null ? otherDescription == null : description.equals(otherDescription);
	}

	@Override
	public int hashCode() {
		return 31 * super.hashCode() + (description == null ? 0 : description.hashCode());
	}

	public int compare(Object event1, Object event2) {
		if (! (event1 instanceof CalendarEvent) || ! (event2 instanceof CalendarEvent))
			return 0;
		return super.compare(event1,event2);
	}


	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		return description;
	}

}
