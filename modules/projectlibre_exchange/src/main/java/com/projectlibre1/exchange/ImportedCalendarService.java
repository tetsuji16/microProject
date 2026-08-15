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

package com.projectlibre1.exchange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.calendar.WorkingCalendar;

import net.sf.mpxj.ProjectCalendar;

/**
 * Facade for manipulating calendars via a dialog or web interface
 */
public class ImportedCalendarService {
	private static ImportedCalendarService instance = null;

	public static ImportedCalendarService getInstance() {
		if (instance == null)
			instance = new ImportedCalendarService();
		return instance;
	}
	private final Map<ProjectCalendar, WorkCalendar> importedCalendarMap = new HashMap<ProjectCalendar, WorkCalendar>();
	public void addImportedCalendar(WorkingCalendar cal, ProjectCalendar mpxCal) {
		importedCalendarMap.put(mpxCal,cal);
		CalendarService calendarService=CalendarService.getInstance();
		if (cal.isBaseCalendar()) {
			if (calendarService.findBaseCalendar(cal.getName(),true) != null)
				return;
// cal.setName(cal.getName() + PLACE_HOLDER_NAME);
		}
		calendarService.add(cal);
	}

	public WorkCalendar findImportedCalendar(ProjectCalendar mpxCal) {
		return importedCalendarMap.get(mpxCal);
	}
	public ProjectCalendar findImportedMPXCalendar(String name) {
		Iterator<ProjectCalendar> i = importedCalendarMap.keySet().iterator();
		ProjectCalendar cal;
		if (name == null)
			return null;
		while (i.hasNext()) {
			cal = i.next();
			if (name.equals(cal.getName()))
				return cal;
		}
		return null;
	}

	private final Map<WorkCalendar, ProjectCalendar> exportedCalendarMap = new HashMap<WorkCalendar, ProjectCalendar>();
	public void addExportedCalendar(ProjectCalendar mpxCal,WorkingCalendar cal) {
		exportedCalendarMap.put(cal,mpxCal);
	}

	public ProjectCalendar findExportedCalendar(WorkCalendar cal) {
		return exportedCalendarMap.get(cal);
	}

	public static void cleanUp(){
		instance=null;
	}

}
