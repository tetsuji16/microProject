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
package com.microproject.pm.assignment.timesheet;

import java.util.Collection;
import java.util.Iterator;

import com.microproject.strings.Messages;

public class TimesheetHelper {
	public static boolean applyTimesheet(Collection children, Collection fieldArray, long timesheetUpdateDate) {
		Iterator i = children.iterator();
		boolean changed = false;
		while (i.hasNext()) {
			if (((UpdatesFromTimesheet)i.next()).applyTimesheet(fieldArray,timesheetUpdateDate))
				changed = true;
		}
		return changed;
	}
	
	public static long getLastTimesheetUpdate(Collection children) {
		long last = 0;
		Iterator i = children.iterator();
		while (i.hasNext()) {
			last = Math.max(last,((UpdatesFromTimesheet)i.next()).getLastTimesheetUpdate());
		}
		return last;
	}
	public static boolean isPendingTimesheetUpdate(Collection children) {
		Iterator i = children.iterator();
		while (i.hasNext()) {
			if (((UpdatesFromTimesheet)i.next()).isPendingTimesheetUpdate())
			return true;
		}
		return false;
	}

	public static int getTimesheetStatus(Collection children) {
		Iterator i = children.iterator();
		int status = TimesheetStatus.NO_DATA;
		while (i.hasNext()) {
			int curStatus = ((UpdatesFromTimesheet)i.next()).getTimesheetStatus();
			if (curStatus == TimesheetStatus.NO_DATA) // ignore if no data
				continue;
			if (status == TimesheetStatus.NO_DATA) // if currently no value, use this 
				status = curStatus;
			else
				return TimesheetStatus.MIXED; // differing statuses. return mixed
		}
		return status;
	}

	public static String getTimesheetStatusName(int status) { // used for display style in web, that's why I use underscores instead of dots for CSS compatibility
		return Messages.getString(getTimesheetStatusStyle(status));
	}

	public static String getTimesheetStatusStyle(int status) { // used for display style in web, that's why I don't use dots - CSS wouldn't like it
		switch (status) {
		case TimesheetStatus.ENTERED:
			return "timesheetEntered"; //$NON-NLS-1$
		case TimesheetStatus.INTEGRATED:
			return "timesheetIntegrated"; //$NON-NLS-1$
		case TimesheetStatus.NO_DATA:
			return "timesheetNoData"; //$NON-NLS-1$
		case TimesheetStatus.REJECTED:
			return "timesheetRejected"; //$NON-NLS-1$
		case TimesheetStatus.VALIDATED:
			return "timesheetValidated"; //$NON-NLS-1$
		case TimesheetStatus.MIXED:
			return "timesheetMixed"; //$NON-NLS-1$
		}
		return "timesheetNoData"; //$NON-NLS-1$
	}
	
	public static String getStatusName(int status) { // used for display style in web, that's why I don't use dots - CSS wouldn't like it
		switch (status) {
		case TimesheetStatus.ENTERED:
			return Messages.getString("TimesheetHelper.Entered"); //$NON-NLS-1$
		case TimesheetStatus.INTEGRATED:
			return Messages.getString("TimesheetHelper.Integrated"); //$NON-NLS-1$
		case TimesheetStatus.NO_DATA:
			return Messages.getString("TimesheetHelper.New"); //$NON-NLS-1$
		case TimesheetStatus.REJECTED:
			return Messages.getString("TimesheetHelper.Rejected"); //$NON-NLS-1$
		case TimesheetStatus.VALIDATED:
			return Messages.getString("TimesheetHelper.Validated"); //$NON-NLS-1$
		case TimesheetStatus.MIXED:
			return Messages.getString("TimesheetHelper.Mixed"); //$NON-NLS-1$
		case TimesheetStatus.SAVED:
			return Messages.getString("TimesheetHelper.Saved"); //$NON-NLS-1$
		}
		return Messages.getString("TimesheetHelper.New"); //$NON-NLS-1$
	}

	public static boolean isReadOnly(int status) {
		return status == TimesheetStatus.ENTERED 
		|| status == TimesheetStatus.INTEGRATED
		|| status == TimesheetStatus.VALIDATED;
	}

}
