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
package com.microproject.timescale;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.microproject.strings.Messages;

public class ExtendedDateFormat extends SimpleDateFormat {
	protected boolean quarter,half,normal;
	protected String text;

	public ExtendedDateFormat() {
		super();
	}

	public ExtendedDateFormat(String pattern, DateFormatSymbols formatSymbols) {
		super(pattern, formatSymbols);
	}

	public ExtendedDateFormat(String pattern, Locale locale) {
		super(pattern, locale);
	}

	public ExtendedDateFormat(String pattern) {
		super(pattern);
	}

	
	@Override
	public void applyPattern(String pattern) {
		quarter=false;
		half=false;
		normal=false;
		if (pattern.startsWith("Q")){
			int l=pattern.startsWith("QQ")?2:1;
			text=Messages.getString("Date.Quarter"+l);
			if (l==2) text+=" ";
			pattern=pattern.substring(l);
			quarter=true;
		}else if (pattern.startsWith("L")){
			int l=pattern.startsWith("LL")?2:1;
			text=Messages.getString("Date.Half"+l);
			if (l==2) text+=" ";
			pattern=pattern.substring(l);
			half=true;
		}
		if (pattern.length()>0){
			super.applyPattern(pattern);
			normal=true;
		}
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo,
			FieldPosition pos) {
		if (quarter||half){
			calendar.setTime(date);
			int month=calendar.get(Calendar.MONTH);
			toAppendTo.append(text).append(month/(quarter?3:6)+1);
		}
		if (normal) super.format(date, toAppendTo, pos);
		return toAppendTo;
	}


}
