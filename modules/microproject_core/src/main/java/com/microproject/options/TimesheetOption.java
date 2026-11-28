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
package com.microproject.options;

import com.microproject.configuration.Dictionary;
import com.microproject.view.configuration.SpreadSheetCategories;
import com.microproject.view.configuration.SpreadSheetFieldArray;
import com.microproject.i18n.Messages;

/**
 * Timesheet integration options.
 * This class manages automatic timecard data integration and the field array
 * used for timesheet spreadsheets.
 * <p>
 * The class follows a singleton pattern via {@link #getInstance()}, but the public constructor
 * allows creating independent instances for testing or isolated configurations.
 * </p>
 */
public class TimesheetOption {
	/** Whether timecard data is automatically integrated (default true). */
	private boolean automaticallyIntegrateTimecardData = true;

	/** The name of the timesheet field array (default from Messages "Spreadsheet.Timesheet.completion"). */
	private String timesheetFieldArrayName = Messages.getString("Spreadsheet.Timesheet.completion");

	private static TimesheetOption instance = null;

	/**
	 * Returns the name of the timesheet field array.
	 *
	 * @return the field array name
	 */
	public final String getTimesheetFieldArrayName() {
		return timesheetFieldArrayName;
	}

	/**
	 * Sets the name of the timesheet field array.
	 *
	 * @param timesheetFieldArrayName the field array name to use for lookup
	 */
	public final void setTimesheetFieldArrayName(String timesheetFieldArrayName) {
		this.timesheetFieldArrayName = timesheetFieldArrayName;
	}

	/**
	 * Returns whether timecard data is automatically integrated.
	 *
	 * @return true if automatic integration is enabled, false otherwise
	 */
	public final boolean isAutomaticallyIntegrateTimecardData() {
		return automaticallyIntegrateTimecardData;
	}

	/**
	 * Sets whether timecard data is automatically integrated.
	 *
	 * @param automaticallyIntegrateTimecardData true to enable automatic integration
	 */
	public final void setAutomaticallyIntegrateTimecardData(boolean automaticallyIntegrateTimecardData) {
		this.automaticallyIntegrateTimecardData = automaticallyIntegrateTimecardData;
	}

	/**
	 * Returns the shared singleton instance. Creates it on first call.
	 *
	 * @return the singleton instance
	 */
	public static TimesheetOption getInstance() {
		if (instance == null)
			instance = new TimesheetOption();
		return instance;
	}

	/**
	 * Creates a new instance with default values. This constructor is public to allow
	 * independent instances for testing or isolated configurations, separate from the
	 * singleton returned by {@link #getInstance()}.
	 */
	public TimesheetOption() {
		super();
	}

	/**
	 * Returns the timesheet field array by looking it up in the {@link Dictionary}
	 * using the category {@link SpreadSheetCategories#timesheetSpreadsheetCategory}
	 * and the current {@link #timesheetFieldArrayName}.
	 *
	 * @return the {@link SpreadSheetFieldArray} for timesheets, or null if not found
	 */
	public SpreadSheetFieldArray getTimesheetFieldArray() {
		return (SpreadSheetFieldArray) Dictionary.get(
				SpreadSheetCategories.timesheetSpreadsheetCategory, getTimesheetFieldArrayName());

	}

}
