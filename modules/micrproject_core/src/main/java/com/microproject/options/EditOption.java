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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.microproject.options.CalendarOption;
import com.microproject.util.DateTime;

/**
 *
 */
public class EditOption {
	private static EditOption instance = null;
	public static EditOption getInstance() {
		if (instance == null)
			instance = new EditOption();
		return instance;
	}
	private SimpleDateFormat dateFormat = null;
	private DateFormat shortDateFormat = null;
	

	/**
	 * 
	 */
	public EditOption() {
		super();
		// DateTime delegates patterns to the active JVM locale and pins only the time zone.
		dateFormat = DateTime.utcDateFormatInstance();
		shortDateFormat = DateTime.utcShortDateFormatInstance();
	}
	
	private boolean addSpaceBeforeLabel = true;
	
	/**
	 * @return Returns the addSpaceBeforeLabel.
	 */
	public boolean isAddSpaceBeforeLabel() {
		return addSpaceBeforeLabel;
	}
	/**
	 * @param addSpaceBeforeLabel The addSpaceBeforeLabel to set.
	 */
	public void setAddSpaceBeforeLabel(boolean addSpaceBeforeLabel) {
		this.addSpaceBeforeLabel = addSpaceBeforeLabel;
	}
	
	private int[] viewAs = new int[]{2,2,2,2,2,2,2}; // index into resource string of types array.  See DurationFormat
	public int getViewAs(int index) {
		if (index < 0)
			return 2;
		return viewAs[index % (viewAs.length)];
	}
	public void setViewAs(int index, int value) {
		viewAs[index % (viewAs.length)] = value;
	}
	
	
	/**
	 * @return Returns the dateFormat.
	 */
	public final DateFormat getDateFormat() {
		return CalendarOption.getInstance().isShowTimeInDates() ? dateFormat : shortDateFormat;
	}
	/**
	 * @param dateFormat The dateFormat to set.
	 */
	public final void setDateFormat(SimpleDateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}
	public DateFormat getShortDateFormat() {
		return shortDateFormat;
	}
	public void setShortDateFormat(DateFormat shortDateFormat) {
		this.shortDateFormat = shortDateFormat;
	}
}
