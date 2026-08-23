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
package com.microproject.pm.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

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

	/**
	 * Issue #355 / upstream SF #112: editing a base calendar's content must be
	 * reflected by the derived (e.g. resource) calendars built on top of it. The
	 * derived calendars cache a merged {@code concrete} CalendarDefinition; the
	 * model must clear that cache when a base they depend on is invalidated,
	 * otherwise they keep serving stale working-time and tasks scheduled against
	 * the derived calendar ignore the base edit.
	 */
	@Test
	void derivedCalendarConcreteReflectsBaseEdit() throws WorkRangeException {
		CalendarService service = CalendarService.getInstance();

		// Isolated base calendar: weekdays working, weekends non-working.
		WorkingCalendar base = WorkingCalendar.getInstance();
		base.setName("Issue355-base");
		base.setUniqueId(-9000001L);
		WorkDay working = new WorkDay();
		try {
			working.getWorkingHours().setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
			working.getWorkingHours().setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		} catch (WorkRangeException e) {
			throw new IllegalStateException("Failed to build working day for test", e);
		}
		WorkDay nonWorking = new WorkDay();
		nonWorking.getWorkingHours().setNonWorking();
		base.setWeekDays(working);
		base.setWeekends(nonWorking);
		service.add(base);

		// Derived calendar built on top of the base (e.g. a resource calendar).
		WorkingCalendar derived = WorkingCalendar.getInstanceBasedOn(base);
		derived.setName("Issue355-derived");
		derived.setUniqueId(-9000002L);
		service.add(derived);

		// Populate the derived concrete cache.
		derived.getConcreteInstance();
		assertFalse(derived.isInvalid(), "derived concrete should be cached before the base edit");
		boolean saturdayBefore = derived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking();
		assertFalse(saturdayBefore, "Saturday must be non-working in the derived calendar before the base edit");

		// Edit the base: make Saturday a working day, then apply (invalidates the base).
		WorkingHours saturdayHours = new WorkingHours();
		saturdayHours.setInterval(0, WorkingHours.hourTime(8), WorkingHours.hourTime(12));
		saturdayHours.setInterval(1, WorkingHours.hourTime(13), WorkingHours.hourTime(17));
		base.setWeekDayWorkingHours(Calendar.SATURDAY, saturdayHours);
		base.invalidate();

		// The derived concrete cache must be cleared and recomputed to reflect the base edit.
		assertTrue(derived.isInvalid(), "base edit must invalidate the derived concrete cache (issue #355)");
		boolean saturdayAfter = derived.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking();
		assertTrue(saturdayAfter, "derived calendar must reflect the base edit (Saturday now working)");
		assertTrue(base.getConcreteInstance().getWeekDay(Calendar.SATURDAY - 1).isWorking(),
				"base concrete must reflect its own edit");
	}
}
