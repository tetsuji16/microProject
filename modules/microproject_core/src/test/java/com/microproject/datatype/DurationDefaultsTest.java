/*******************************************************************************
 * MIT License
 *
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
package com.microproject.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.ParsePosition;

import org.junit.jupiter.api.Test;

class DurationDefaultsTest {
	private static final double EPSILON = 0.000001D;

	@Test
	void independentDefaultsKeepSameUnitDurationsProjectSpecific() {
		DurationDefaults sixHourDay = new DurationDefaults(360.0D, 1800.0D, 20.0D);
		DurationDefaults eightHourDay = new DurationDefaults(480.0D, 2400.0D, 20.0D);

		long oneDay = Duration.getInstance(1.0D, TimeUnit.DAYS, sixHourDay);
		long oneWeek = Duration.getInstance(1.0D, TimeUnit.WEEKS, sixHourDay);

		assertEquals(TimeUnit.DAYS, Duration.getType(oneDay));
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(oneDay));
		assertEquals(1.0D, Duration.getValue(oneDay, sixHourDay), EPSILON);
		assertEquals(0.75D, Duration.getValue(oneDay, eightHourDay), EPSILON);
		assertEquals(30L * 60L * 60L * 1000L, Duration.millis(oneWeek));
		assertEquals(1.0D, Duration.getValue(oneWeek, sixHourDay), EPSILON);
		assertEquals(0.75D, Duration.getValue(oneWeek, eightHourDay), EPSILON);
	}

	@Test
	void monthYearFactorsUseProjectDayAndMonthDefaults() {
		DurationDefaults defaults = new DurationDefaults(360.0D, 1800.0D, 22.0D);
		long oneMonth = Duration.getInstance(1.0D, TimeUnit.MONTHS, defaults);
		long oneYear = Duration.getInstance(1.0D, TimeUnit.YEARS, defaults);

		assertEquals(22L * 6L * 60L * 60L * 1000L, Duration.millis(oneMonth));
		assertEquals(365L * 6L * 60L * 60L * 1000L, Duration.millis(oneYear));
		assertEquals(1.0D, Duration.getValue(oneMonth, defaults), EPSILON);
		assertEquals(1.0D, Duration.getValue(oneYear, defaults), EPSILON);
	}

	@Test
	void elapsedUnitsRemainWallClockAcrossProjectDefaults() {
		DurationDefaults sixHourDay = new DurationDefaults(360.0D, 1800.0D, 20.0D);
		long oneElapsedDay = Duration.getInstance(1.0D, TimeUnit.ELAPSED_DAYS, sixHourDay);

		assertEquals(24L * 60L * 60L * 1000L, Duration.millis(oneElapsedDay));
		assertEquals(1.0D, Duration.getValue(oneElapsedDay, sixHourDay), EPSILON);
		assertEquals(1.0D, Duration.getValue(oneElapsedDay,
			new DurationDefaults(480.0D, 2400.0D, 20.0D)), EPSILON);
	}

	@Test
	void specialFlagsAndZeroNegativeValuesArePreserved() {
		DurationDefaults sixHourDay = new DurationDefaults(360.0D, 1800.0D, 20.0D);
		long zero = Duration.getInstance(0.0D, TimeUnit.DAYS, sixHourDay);
		long negative = Duration.getInstance(-0.5D, TimeUnit.DAYS, sixHourDay);
		long percent = Duration.getInstance(0.25D, TimeUnit.PERCENT, sixHourDay);
		long nonTemporal = Duration.getInstance(3.0D, TimeUnit.NON_TEMPORAL, sixHourDay);
		long estimated = Duration.setAsEstimated(Duration.getInstance(2.0D, TimeUnit.DAYS, sixHourDay), true);

		assertEquals(0L, Duration.millis(zero));
		assertEquals(-3L * 60L * 60L * 1000L, Duration.millis(negative));
		assertEquals(-0.5D, Duration.getValue(negative, sixHourDay), EPSILON);
		assertEquals(0.25D, Duration.getValue(percent, sixHourDay), EPSILON);
		assertEquals(TimeUnit.NON_TEMPORAL, Duration.getType(nonTemporal));
		assertEquals(3.0D, Duration.getValue(nonTemporal, sixHourDay), EPSILON);
		assertEquals(true, Duration.isEstimated(estimated));
		assertEquals(2.0D, Duration.getValue(estimated, sixHourDay), EPSILON);
	}

	@Test
	void invalidDefaultsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> new DurationDefaults(0.0D, 2400.0D, 20.0D));
		assertThrows(IllegalArgumentException.class, () -> new DurationDefaults(480.0D, Double.NaN, 20.0D));
		assertThrows(IllegalArgumentException.class, () -> new DurationDefaults(480.0D, 2400.0D, Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class, () -> Duration.getInstance(Double.NaN, TimeUnit.DAYS,
			new DurationDefaults(480.0D, 2400.0D, 20.0D)));
		assertThrows(IllegalArgumentException.class, () -> Duration.getInstance(1.0D, TimeUnit.DAYS,
			new DurationDefaults(Double.MAX_VALUE, 2400.0D, 20.0D)));
		assertThrows(IllegalArgumentException.class, () -> Duration.getInstance(Double.POSITIVE_INFINITY,
			TimeUnit.PERCENT, new DurationDefaults(480.0D, 2400.0D, 20.0D)));
	}

	@Test
	void contextualFormattingAndParsingDoNotChangeGlobalOptions() {
		DurationDefaults sixHourDay = new DurationDefaults(360.0D, 1800.0D, 20.0D,
			TimeUnit.HOURS, TimeUnit.MINUTES);
		DurationFormat format = DurationFormat.getProjectInstance();
		double globalDayMillis = Duration.timeUnitFactor(TimeUnit.DAYS);

		long oneSourceDay = Duration.getInstance(1.0D, TimeUnit.DAYS, sixHourDay);
		assertEquals("1 day", format.format(new Duration(oneSourceDay), sixHourDay));
		assertEquals("0.75 days", format.format(new Duration(oneSourceDay),
			new DurationDefaults(480.0D, 2400.0D, 20.0D)));

		String input = "1 day";
		Duration parsed = (Duration) format.parseObject(input, new ParsePosition(0), sixHourDay);
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(parsed.getEncodedMillis()));
		assertEquals(globalDayMillis, Duration.timeUnitFactor(TimeUnit.DAYS));
	}

	@Test
	void omittedDurationAndWorkUnitsUseEachProjectsDefaults() {
		DurationDefaults durationHoursWorkMinutes = new DurationDefaults(360.0D, 1800.0D, 20.0D,
			TimeUnit.HOURS, TimeUnit.MINUTES);
		DurationDefaults durationDaysWorkHours = new DurationDefaults(480.0D, 2400.0D, 20.0D,
			TimeUnit.DAYS, TimeUnit.HOURS);
		DurationFormat format = DurationFormat.getProjectInstance();

		Duration oneUnit = (Duration) format.parseObject("2", new ParsePosition(0), durationHoursWorkMinutes);
		assertEquals(TimeUnit.HOURS, Duration.getType(oneUnit.getEncodedMillis()));
		assertEquals(2.0D, Duration.getValue(oneUnit.getEncodedMillis(), durationHoursWorkMinutes), EPSILON);
		assertEquals(7_200_000L, Duration.millis(oneUnit.getEncodedMillis()));

		Duration oneUnitOtherProject = (Duration) format.parseObject("2", new ParsePosition(0), durationDaysWorkHours);
		assertEquals(TimeUnit.DAYS, Duration.getType(oneUnitOtherProject.getEncodedMillis()));
		assertEquals(16L * 60L * 60L * 1000L, Duration.millis(oneUnitOtherProject.getEncodedMillis()));

		Duration work = new Work(60_000L);
		assertEquals("1 minute", format.format(work, durationHoursWorkMinutes));
		assertEquals("0.017 hours", format.format(work, durationDaysWorkHours));
	}
}
