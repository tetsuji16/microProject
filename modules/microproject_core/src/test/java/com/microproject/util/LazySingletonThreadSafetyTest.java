/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.RateFormat;
import com.microproject.pm.calendar.CalendarCatalog;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingHours;

/** Issue #1041: shared lazily created singletons must be safe to request from several threads. */
class LazySingletonThreadSafetyTest {
	private static final int THREADS = 8;

	@Test
	void calendarServiceReturnsOneInstance() throws Exception {
		assertSingleInstance(CalendarService::getInstance);
	}

	@Test
	void calendarCatalogReturnsOneInstance() throws Exception {
		assertSingleInstance(CalendarCatalog::getInstance);
	}

	@Test
	void workDaysReturnOneInstance() throws Exception {
		assertSingleInstance(WorkDay::getDefaultWorkDay);
		assertSingleInstance(WorkDay::getNonStopWorkDay);
		assertSingleInstance(WorkDay::getNonWorkingDay);
	}

	@Test
	void workingHoursReturnOneInstance() throws Exception {
		assertSingleInstance(WorkingHours::getDefault);
		assertSingleInstance(WorkingHours::getNonStop);
	}

	@Test
	void durationFormatsReturnOneInstance() throws Exception {
		assertSingleInstance(DurationFormat::getInstance);
		assertSingleInstance(DurationFormat::getProjectInstance);
		assertSingleInstance(DurationFormat::getSignedInstance);
		assertSingleInstance(DurationFormat::getWorkInstance);
		assertSingleInstance(DurationFormat::getNonTemporalWorkInstance);
	}

	@Test
	void rateFormatsReturnOneInstance() throws Exception {
		assertSingleInstance(() -> RateFormat.getInstance(null, false, false, true));
		assertSingleInstance(() -> RateFormat.getInstance(null, true, false, true));
		assertSingleInstance(() -> RateFormat.getInstance(null, false, true, true));
		assertSingleInstance(() -> RateFormat.getInstance(null, false, false, false));
	}

	@Test
	void sharedDatesReturnOneInstance() throws Exception {
		assertSingleInstance(DateTime::getZeroDate);
		assertSingleInstance(DateTime::getMaxDate);
	}

	@Test
	void modifyingMaxCalendarDoesNotAffectLaterCalls() {
		long expected = DateTime.getMaxCalendar().getTimeInMillis();
		Calendar first = DateTime.getMaxCalendar();
		first.add(Calendar.YEAR, 10);

		assertNotSame(first, DateTime.getMaxCalendar());
		assertEquals(expected, DateTime.getMaxCalendar().getTimeInMillis());
		assertEquals(expected, DateTime.getMaxDate().getTime());
	}

	@Test
	void modifyingZeroCalendarDoesNotAffectLaterCalls() {
		Calendar first = DateTime.getZeroCalendar();
		first.add(Calendar.YEAR, 10);

		assertEquals(0L, DateTime.getZeroCalendar().getTimeInMillis());
	}

	private static <T> void assertSingleInstance(Supplier<T> getter) throws Exception {
		CyclicBarrier start = new CyclicBarrier(THREADS);
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		try {
			List<Future<T>> futures = new ArrayList<>();
			for (int i = 0; i < THREADS; i++) {
				Callable<T> task = () -> {
					start.await(10, TimeUnit.SECONDS);
					return getter.get();
				};
				futures.add(pool.submit(task));
			}
			Set<T> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
			for (Future<T> future : futures) {
				T value = future.get(30, TimeUnit.SECONDS);
				assertNotNull(value);
				distinct.add(value);
			}
			assertEquals(1, distinct.size());
		} finally {
			pool.shutdownNow();
		}
	}
}
