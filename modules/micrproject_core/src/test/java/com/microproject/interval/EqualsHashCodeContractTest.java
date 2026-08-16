package com.microproject.interval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.microproject.core.time.DefaultTimeInterval;
import com.microproject.core.time.DefaultTimeIntervals;
import com.microproject.datatype.Duration;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.pm.calendar.WorkRange;
import com.microproject.pm.calendar.WorkRangeException;
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
