package com.projectlibre.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class CalendarManagerTest {
	@Test
	void removalCleansNameIndexEvenWhenCalendarWasRenamed() throws Exception {
		CalendarManager manager = new CalendarManager();
		DefaultWorkCalendar calendar = new DefaultWorkCalendar();
		calendar.setId(new CalendarId(7L));
		calendar.setName("Original");
		manager.addBaseCalendar(calendar);
		calendar.setName("Renamed");

		assertSame(calendar, manager.removeBaseCalendar(calendar));
		assertNull(manager.getCalendar(new CalendarId(7L)));
		assertNull(manager.getCalendar("Original"));
	}
}
