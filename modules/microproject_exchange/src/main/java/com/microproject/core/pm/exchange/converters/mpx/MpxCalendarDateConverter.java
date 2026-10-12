/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import com.microproject.util.DateTime;

/**
 * Converts UTC model day keys to MPXJ's local-calendar-date representation.
 * MPXJ 11.5.4 normalizes calendar dates with DateHelper.getDayStartDate,
 * which uses the host-local Calendar; noon avoids crossing the local date
 * boundary before MPXJ applies that normalization.
 */
public final class MpxCalendarDateConverter {
	private MpxCalendarDateConverter() { }

	public static Date toMpxDate(long modelDay) {
		Calendar utc = DateTime.calendarInstance();
		utc.setTimeInMillis(DateTime.dayFloor(modelDay));
		Calendar localNoon = new GregorianCalendar();
		localNoon.clear();
		localNoon.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
		return localNoon.getTime();
	}

	public static long toModelDay(Date mpxDate) {
		Calendar local = new GregorianCalendar();
		local.setTime(mpxDate);
		Calendar utc = DateTime.calendarInstance();
		utc.clear();
		utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
		return utc.getTimeInMillis();
	}
}
