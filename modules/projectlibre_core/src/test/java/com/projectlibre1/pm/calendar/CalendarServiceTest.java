package com.projectlibre1.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CalendarServiceTest {
	@Test
	void calendarServiceHandlesScratchCopyAndApplyBoundaries() {
		CalendarService service = CalendarService.getInstance();
		WorkingCalendar original = WorkingCalendar.getInstance();
		original.setName("CalendarServiceTest-original");
		original.getConcreteInstance();

		assertEquals(null, service.getBaseCalendar(null));
		WorkingCalendar scratch = service.makeScratchCopy(original);
		scratch.setName("CalendarServiceTest-scratch");
		service.assignCalendar(original, scratch);
		assertEquals("CalendarServiceTest-scratch", original.getName());

		service.saveAndUpdate(original);
		assertTrue(original.isInvalid());
		service.invalidate(null);
		assertThrows(IllegalArgumentException.class, () -> service.makeScratchCopy(null));
		assertThrows(IllegalArgumentException.class, () -> service.assignCalendar(original, null));
	}
}
