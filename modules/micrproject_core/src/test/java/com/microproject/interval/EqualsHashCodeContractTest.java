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
package com.microproject.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.microproject.core.time.DefaultTimeInterval;
import com.microproject.core.time.DefaultTimeIntervals;
import com.microproject.datatype.Duration;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.key.HasCommonKeyImpl;
import com.microproject.pm.key.HasKeyImpl;
import com.microproject.pm.key.HasUniqueIdImpl;
import com.microproject.pm.time.Interval;

/**
 * Locks the equals/hashCode contract for the value classes that previously
 * overrode equals() without hashCode() (issue #157): objects that are equal
 * must have equal hashCodes, and equals must tolerate foreign types.
 */
class EqualsHashCodeContractTest {

	@Test
	void durationEqualsImpliesSameHashCode() {
		Duration a = new Duration(1000L);
		Duration b = new Duration(1000L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(new Duration(2000L), a);
	}

	@Test
	void intervalsEqualsImpliesSameHashCode() {
		DefaultTimeInterval a = new DefaultTimeInterval(100L, 200L);
		DefaultTimeInterval b = new DefaultTimeInterval(100L, 200L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Interval ia = new StubInterval(100L, 200L);
		Interval ib = new StubInterval(100L, 200L);
		assertEquals(ia, ib);
		assertEquals(ia.hashCode(), ib.hashCode());
	}

	@Test
	void timeIntervalsEqualsImpliesSameHashCode() {
		DefaultTimeIntervals a = new DefaultTimeIntervals();
		a.inter(new DefaultTimeInterval(100L, 200L));
		DefaultTimeIntervals b = new DefaultTimeIntervals();
		b.inter(new DefaultTimeInterval(100L, 200L));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void workDayEqualsUsesStartOnlyAndHashCodeMatches() {
		WorkDay a = new WorkDay(123L, 123L);
		WorkDay b = new WorkDay(123L, 124L); // same start, different end -> equal by contract
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void workingHoursEqualsImpliesSameHashCode() throws WorkRangeException {
		WorkingHours a = new WorkingHours();
		WorkingHours b = new WorkingHours();
		a.setInterval(0, 100L, 200L);
		b.setInterval(0, 100L, 200L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		// WorkRange (Interval subclass) hashCode must be value-based too
		WorkRange r1 = new WorkRange(100L, 200L);
		WorkRange r2 = new WorkRange(100L, 200L);
		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
	}

	@Test
	void valueObjectForIntervalEqualsDoesNotThrowForForeignTypes() {
		ValueObjectForInterval a = new ValueObjectForInterval(null, 100L);
		assertFalse(a.equals("not an interval"));
		assertFalse(a.equals(null));
		ValueObjectForInterval b = new ValueObjectForInterval(null, 100L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void uniqueIdBasedClassesHaveConsistentHashCode() {
		HasUniqueIdImpl a = new HasUniqueIdImpl(null, 42L);
		HasUniqueIdImpl b = new HasUniqueIdImpl(null, 42L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		HasKeyImpl ka = new HasKeyImpl(null, 42L);
		HasKeyImpl kb = new HasKeyImpl(null, 42L);
		assertEquals(ka, kb);
		assertEquals(ka.hashCode(), kb.hashCode());
	}

	@Test
	void hasCommonKeyEqualsImpliesSameHashCode() {
		// Issue #177: HasCommonKeyImpl overrides equals() by uniqueId and now
		// implements hashCode() consistently.
		HasCommonKeyImpl a = new HasCommonKeyImpl(null, 42L);
		HasCommonKeyImpl b = new HasCommonKeyImpl(null, 42L);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		a.setUniqueId(7L);
		assertNotEquals(a, b);
	}

	@Test
	void workingCalendarIdentityEqualsHasMatchingHashCode() {
		// Issue #177: WorkingCalendar.equals() is identity-based, so its hash
		// code must be the identity hash as well.
		WorkingCalendar a = WorkingCalendar.getInstance();
		WorkingCalendar b = WorkingCalendar.getInstance();
		assertFalse(a.equals(b));
		assertEquals(a, a);
		assertEquals(a.hashCode(), a.hashCode());
	}

	/** Minimal concrete Interval for testing the abstract base class. */
	private static final class StubInterval extends Interval {
		StubInterval(long start, long end) {
			super(start, end);
		}

		@Override
		public long getStart() {
			return start;
		}

		@Override
		public long getEnd() {
			return end;
		}
	}
}
