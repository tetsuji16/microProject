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
package com.microproject.core.time;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * @author Laurent Chretienneau
 *
 */
public class TimeUtil { //thread safe: no shared mutable state (issue #184)
	protected static long MINUTE=60000L;
	protected static long HOUR=60*MINUTE;
	protected static long DAY=24*HOUR;

	private static int getTimeZoneOffset(long t){ 
		Calendar c=Calendar.getInstance(); // local time zone
		c.setTimeInMillis(t);
		return c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET);
	}
	public static long removeTimeZoneOffset(long t){
		return t-getTimeZoneOffset(t);
	}
	public static long addTimeZoneOffset(long t){
		return t+getTimeZoneOffset(t);
	}
	
	public static String toUTCString(long t){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT);
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date(t));
	}
	
	public static long toHoursAndMinutes(long date) { //corrects the problem of mpx giving hours in local timezone not utc
		Calendar calendar=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(date);
		int tz=getTimeZoneOffset(date);
		long t=(60L * calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE)) * MINUTE + tz;
		//t can be negative because of timezone adjustment
		t=(t+DAY)%DAY; 
		return t;
	}
	
}
