package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.junit.jupiter.api.Test;

import net.sf.mpxj.DateRange;
import net.sf.mpxj.ProjectCalendarHours;

class MpxCalendarWorkingHoursTest {
	@Test
	void importsEveryMspWorkingRangeWithoutCollapsingToTheDefaultDay() {
		ProjectCalendarHours mspHours = new ProjectCalendarHours();
		mspHours.add(new DateRange(localTime(7), localTime(11)));
		mspHours.add(new DateRange(localTime(12), localTime(16)));

		var hours = new MpxRangeConverter().from(mspHours);

		assertEquals(2, hours.getIntervals().stream().filter(java.util.Objects::nonNull).count());
		assertEquals(8L * 60L * 60L * 1000L, hours.getDuration());
	}

	private static Date localTime(int hour) {
		java.util.GregorianCalendar calendar = new java.util.GregorianCalendar();
		calendar.clear();
		calendar.set(1970, java.util.Calendar.JANUARY, 1, hour, 0);
		return calendar.getTime();
	}
}
