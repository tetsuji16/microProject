/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.timescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.DateTime;

class TimeScaleLocaleTest {
	private final Locale originalLocale = Locale.getDefault();

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(originalLocale);
	}

	@Test
	void builtInLabelsUseJapaneseMonthAndWeekdayNames() {
		Locale.setDefault(Locale.JAPAN);
		TimeScale scale = scale("E d MMM", "MMM y");
		long date = DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 10).getTimeInMillis();

		assertEquals("木 10", scale.getText1(date));
		assertEquals("9月 2026年", scale.getText2(date));
	}

	@Test
	void anExistingScaleFollowsAChangedApplicationLocale() {
		Locale.setDefault(Locale.ENGLISH);
		TimeScale scale = scale("E d MMM", "MMM y");
		long date = DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 10).getTimeInMillis();
		assertTrue(scale.getText1(date).contains("Thu"));

		Locale.setDefault(Locale.JAPAN);
		assertEquals("木 10", scale.getText1(date));
		assertEquals("9月 2026年", scale.getText2(date));
	}

	@Test
	void nonBuiltInPatternsRemainLocaleAwareAndAreNotRewritten() {
		Locale.setDefault(Locale.JAPAN);
		TimeScale scale = scale("yyyy/MM/dd", "yyyy年MM月");
		long date = DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 10).getTimeInMillis();

		assertEquals("2026/09/10", scale.getText1(date));
		assertEquals("2026年09月", scale.getText2(date));
	}

	private static TimeScale scale(String pattern1, String pattern2) {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(Calendar.DAY_OF_MONTH);
		scale.setNumber1(1);
		scale.setPattern1(pattern1);
		scale.setPattern2(pattern2);
		return scale;
	}
}
