package com.microproject.core.pm.exchange.converters.mpx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.sf.mpxj.Duration;
import net.sf.mpxj.TimeUnit;

/**
 * Locks the MPXJ TimeUnit value mapping used by MpxTaskConverter / MpxAssignmentConverter
 * (see issue #155). MPXJ TimeUnit is 0-based: MINUTES=0, HOURS=1, DAYS=2, WEEKS=3,
 * MONTHS=4, PERCENT=5, YEARS=6, ELAPSED_*=7..13, NULL=14.
 */
public class MpxDurationConversionTest {

	@Test
	public void minutesPerUnitMapsAllNonElapsedUnits() {
		assertEquals(1.0, MpxUtils.minutesPerUnit(TimeUnit.MINUTES.getValue()), 0.0001);
		assertEquals(60.0, MpxUtils.minutesPerUnit(TimeUnit.HOURS.getValue()), 0.0001);
		assertEquals(1440.0, MpxUtils.minutesPerUnit(TimeUnit.DAYS.getValue()), 0.0001);
		assertEquals(10080.0, MpxUtils.minutesPerUnit(TimeUnit.WEEKS.getValue()), 0.0001);
		assertEquals(43200.0, MpxUtils.minutesPerUnit(TimeUnit.MONTHS.getValue()), 0.0001);
		assertEquals(518400.0, MpxUtils.minutesPerUnit(TimeUnit.YEARS.getValue()), 0.0001);
	}

	@Test
	public void elapsedUnitsUseSameMultipliersAsNonElapsed() {
		assertEquals(1.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_MINUTES.getValue()), 0.0001);
		assertEquals(60.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_HOURS.getValue()), 0.0001);
		assertEquals(1440.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_DAYS.getValue()), 0.0001);
		assertEquals(10080.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_WEEKS.getValue()), 0.0001);
		assertEquals(43200.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_MONTHS.getValue()), 0.0001);
		assertEquals(518400.0, MpxUtils.minutesPerUnit(TimeUnit.ELAPSED_YEARS.getValue()), 0.0001);
	}

	@Test
	public void percentAndUnknownUnitsDoNotIndexOutOfBounds() {
		// must not throw ArrayIndexOutOfBoundsException (see issue #155)
		assertEquals(1.0, MpxUtils.minutesPerUnit(TimeUnit.PERCENT.getValue()), 0.0001);
		// unknown/out-of-range values (e.g. a legacy NULL sentinel) fall back to minutes
		assertEquals(1.0, MpxUtils.minutesPerUnit(14), 0.0001);
	}

	@Test
	public void toMillisConvertsHoursAndDaysCorrectly() {
		Duration twoHours = Duration.getInstance(2, TimeUnit.HOURS);
		assertEquals(2L * 60L * 60L * 1000L, MpxUtils.toMillis(twoHours));

		Duration twoDays = Duration.getInstance(2, TimeUnit.DAYS);
		assertEquals(2L * 24L * 60L * 60L * 1000L, MpxUtils.toMillis(twoDays));

		Duration twoElapsedHours = Duration.getInstance(2, TimeUnit.ELAPSED_HOURS);
		assertEquals(2L * 60L * 60L * 1000L, MpxUtils.toMillis(twoElapsedHours));
	}

	@Test
	public void toMillisHandlesNull() {
		assertEquals(0L, MpxUtils.toMillis(null));
	}

	@Test
	public void everyMpxTimeUnitConvertsWithoutOutOfBounds() {
		// Regression for issue #155: the old minutesPerUnit[] array was indexed
		// by TimeUnit.getValue() and threw ArrayIndexOutOfBoundsException for
		// every elapsed unit (values >= 7). No TimeUnit value may throw.
		for (TimeUnit unit : TimeUnit.values()) {
			long millis = MpxUtils.toMillis(Duration.getInstance(1, unit));
			org.junit.Assert.assertTrue(
					"unit " + unit + " (value=" + unit.getValue() + ") produced invalid result " + millis,
					millis >= 0L);
		}
	}

	@Test
	public void elapsedUnitsConvertWithNonElapsedMultipliers() {
		assertEquals(1440L * 60000L, MpxUtils.toMillis(Duration.getInstance(1, TimeUnit.ELAPSED_DAYS)));
		assertEquals(10080L * 60000L, MpxUtils.toMillis(Duration.getInstance(1, TimeUnit.ELAPSED_WEEKS)));
		assertEquals(43200L * 60000L, MpxUtils.toMillis(Duration.getInstance(1, TimeUnit.ELAPSED_MONTHS)));
		assertEquals(518400L * 60000L, MpxUtils.toMillis(Duration.getInstance(1, TimeUnit.ELAPSED_YEARS)));
	}
}
