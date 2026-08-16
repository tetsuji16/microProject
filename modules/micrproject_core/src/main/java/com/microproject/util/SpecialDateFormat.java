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
package com.microproject.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import com.microproject.strings.Messages;

/**
 *
 */
public class SpecialDateFormat extends SimpleDateFormat {
	private static final String NO_END=Messages.getString("Date.NoEnd");
	private static final String NO_START=Messages.getString("Date.NoStart");
	
	public static String DATE_FORMAT="EEE MM/dd/yy H:mm";

	private static DateFormat instance = null;
	private static DateFormat defaultInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT);
	public static DateFormat getSpecialInstance() {
		if (instance == null) {
			instance = new SpecialDateFormat();
			instance.setTimeZone(DateUtils.UTC_TIME_ZONE);
			defaultInstance.setTimeZone(DateUtils.UTC_TIME_ZONE);
		}
		return instance; 
	}
	protected SpecialDateFormat() {
		super(DATE_FORMAT);
	}
	
	public StringBuffer format(Date date, StringBuffer arg1, FieldPosition arg2) {
		if (date.equals(DateTime.getMaxDate())) {
			arg1.append(NO_END);
			return arg1;
		} else if (date.equals(DateTime.NA_TIME)) {
			arg1.append(NO_START);
			return arg1;
		}
		return defaultInstance.format(date,arg1,arg2);
//		return super.format(date, arg1, arg2);
	}
	public Date parse(String arg0, ParsePosition arg1) {
		if (arg0.equals(NO_END))
			return DateTime.getMaxDate();
		else if (arg0.equals(NO_START))
			return DateTime.NA_TIME;
		return defaultInstance.parse(arg0, arg1);
//		return super.parse(arg0, arg1);
	}


}
