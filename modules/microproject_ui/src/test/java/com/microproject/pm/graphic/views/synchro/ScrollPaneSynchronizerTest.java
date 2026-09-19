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
package com.microproject.pm.graphic.views.synchro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Point;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.Test;

import com.microproject.timescale.TimeScale;
import com.microproject.util.DateTime;

class ScrollPaneSynchronizerTest {
	@Test
	void synchronizerDoesNotRegisterTheSamePairTwice() throws ReflectiveOperationException {
		Synchronizer synchronizer = new Synchronizer();
		JScrollPane first = new JScrollPane(new JPanel());
		JScrollPane second = new JScrollPane(new JPanel());

		synchronizer.addSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);
		synchronizer.addSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);

		Field field = Synchronizer.class.getDeclaredField("scrollPaneSynchronizers");
		field.setAccessible(true);
		assertEquals(1, ((List<?>) field.get(synchronizer)).size());

		synchronizer.removeSynchro(first, second, ScrollPaneSynchronizer.HORIZONTAL);
		assertEquals(0, ((List<?>) field.get(synchronizer)).size());
	}
	@Test
	void resolveViewportLeftEdgeDateUsesCurrentViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 1_000.0d + x * 2.0d, new Point(125, 0));

		assertEquals(1_250.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void resolveViewportLeftEdgeDateClampsNegativeViewportOffset() {
		double leftEdgeDate = ScrollPaneSynchronizer.resolveViewportLeftEdgeDate(x -> 500.0d + x, new Point(-25, 0));

		assertEquals(500.0d, leftEdgeDate, 0.00001d);
	}

	@Test
	void restoreViewportXRoundsConvertedCoordinate() {
		assertEquals(384, ScrollPaneSynchronizer.restoreViewportX(t -> t / 2.0d, 767.6d));
	}

	@Test
	void chooseZoomLeftDatePrefersKeptDateWhenItIsLaterThanMinimum() {
		assertEquals(2_000.0d, ScrollPaneSynchronizer.chooseZoomLeftDate(2_000.0d, 1_500.0d), 0.00001d);
	}

	@Test
	void chooseZoomLeftDateUsesMinimumWhenKeptDateIsTooFarLeft() {
		assertEquals(1_500.0d, ScrollPaneSynchronizer.chooseZoomLeftDate(1_200.0d, 1_500.0d), 0.00001d);
	}

	@Test
	void resolveMinimumLeftDateUsesPreviousDayForDailyScale() {
		TimeScale scale = scale(Calendar.DAY_OF_WEEK, 1);
		long earliestTaskDate = date(2025, Calendar.JUNE, 21);

		assertEquals(date(2025, Calendar.JUNE, 20),
				(long) ScrollPaneSynchronizer.resolveMinimumLeftDate(scale, earliestTaskDate, -1.0d));
	}

	@Test
	void resolveMinimumLeftDateUsesPreviousMonthBoundaryForMonthlyScale() {
		TimeScale scale = scale(Calendar.MONTH, 1);
		long earliestTaskDate = date(2025, Calendar.JUNE, 21);

		assertEquals(date(2025, Calendar.MAY, 1),
				(long) ScrollPaneSynchronizer.resolveMinimumLeftDate(scale, earliestTaskDate, -1.0d));
	}

	@Test
	void clampTargetScaleIndexKeepsInRangeValue() {
		assertEquals(3, ScrollPaneSynchronizer.clampTargetScaleIndex(3, 6));
	}

	@Test
	void clampTargetScaleIndexClampsNegativeValueToZero() {
		assertEquals(0, ScrollPaneSynchronizer.clampTargetScaleIndex(-4, 6));
	}

	@Test
	void clampTargetScaleIndexClampsLargeValueToLastScale() {
		assertEquals(5, ScrollPaneSynchronizer.clampTargetScaleIndex(12, 6));
	}

	@Test
	void chooseZoomLeftDateWithResolvedMinimumKeepsVisibleLeftDateWhenAllowed() {
		TimeScale scale = scale(Calendar.DAY_OF_WEEK, 1);
		long earliestTaskDate = date(2025, Calendar.JUNE, 21);
		double keptLeftDate = date(2025, Calendar.JUNE, 25);
		double minimumLeftDate = ScrollPaneSynchronizer.resolveMinimumLeftDate(scale, earliestTaskDate, keptLeftDate);

		assertEquals(keptLeftDate, ScrollPaneSynchronizer.chooseZoomLeftDate(keptLeftDate, minimumLeftDate), 0.00001d);
	}

	@Test
	void chooseZoomLeftDateWithResolvedMinimumClampsWhenVisibleLeftDateIsTooEarly() {
		TimeScale scale = scale(Calendar.DAY_OF_WEEK, 1);
		long earliestTaskDate = date(2025, Calendar.JUNE, 21);
		double keptLeftDate = date(2025, Calendar.JUNE, 18);
		double minimumLeftDate = ScrollPaneSynchronizer.resolveMinimumLeftDate(scale, earliestTaskDate, keptLeftDate);

		assertEquals(minimumLeftDate, ScrollPaneSynchronizer.chooseZoomLeftDate(keptLeftDate, minimumLeftDate), 0.00001d);
	}

	private static TimeScale scale(int calendarField1, int number1) {
		TimeScale scale = new TimeScale();
		scale.setCalendarField1(calendarField1);
		scale.setNumber1(number1);
		scale.setNormalMinWidth(1);
		return scale;
	}

	private static long date(int year, int month, int dayOfMonth) {
		var calendar = DateTime.calendarInstance();
		calendar.clear();
		calendar.set(year, month, dayOfMonth, 0, 0, 0);
		return calendar.getTimeInMillis();
	}
}
