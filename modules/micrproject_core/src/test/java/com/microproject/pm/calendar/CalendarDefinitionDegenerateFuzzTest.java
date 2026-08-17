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

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Issue #175 adversarial hardening: CalendarDefinition.add() must terminate -
 * no ArithmeticException and no infinite fine-tuning walk - for any combination
 * of working / non-working / null weekdays, positive and negative dates, and
 * arbitrary durations. The hard JUnit timeout makes a future regression fail
 * loudly instead of hanging the suite.
 */
class CalendarDefinitionDegenerateFuzzTest {

	private static final long DAY_MS = 24L * 60 * 60 * 1000;

	@Test
	@Timeout(20)
	void addTerminatesForRandomizedDegenerateCalendars() {
		Random random = new Random(175L);
		for (int iteration = 0; iteration < 300; iteration++) {
			CalendarDefinition calendar = randomCalendar(random);
			for (int i = 0; i < 20; i++) {
				long date = randomDate(random);
				long duration = (1 + random.nextInt(365)) * DAY_MS;
				boolean useSooner = random.nextBoolean();
				calendar.add(date, duration, useSooner);
			}
		}
	}

	private static CalendarDefinition randomCalendar(Random random) {
		CalendarDefinition calendar = new CalendarDefinition();
		for (int day = 0; day < WorkWeek.DAYS_IN_WEEK; day++) {
			WorkDay workDay = null;
			switch (random.nextInt(3)) {
				case 1:
					workDay = new WorkDay(0L); // explicit non-working day
					break;
				case 2:
					workDay = new WorkDay(0L);
					workDay.setWorkingHours((WorkingHours) WorkingHours.getDefault().clone());
					break;
				default:
					// null weekday: resolves to the default working day
			}
			calendar.week.setWeekDay(day, workDay);
		}
		calendar.addSentinelsAndMakeArray();
		return calendar;
	}

	private static long randomDate(Random random) {
		long base = (long) random.nextInt(1_000_000_000) * DAY_MS;
		return random.nextBoolean() ? base : -base;
	}
}
