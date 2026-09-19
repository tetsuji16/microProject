package com.microproject.core.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeTypeBridgeTest {
	@Test
	void mapsLegacyUnitsWithDifferentYearsAndPercentIds() {
		assertEquals(com.microproject.datatype.TimeUnit.YEARS,
				TimeTypeBridge.toDomainUnit(TimeUnit.YEARS));
		assertEquals(com.microproject.datatype.TimeUnit.PERCENT,
				TimeTypeBridge.toDomainUnit(TimeUnit.PERCENT));
		assertEquals(com.microproject.datatype.TimeUnit.ELAPSED_DAYS,
				TimeTypeBridge.toDomainUnit(TimeUnit.ELAPSED_DAYS));
		assertEquals(TimeUnit.YEARS,
				TimeTypeBridge.fromDomainUnit(com.microproject.datatype.TimeUnit.YEARS));
		assertEquals(TimeUnit.PERCENT,
				TimeTypeBridge.fromDomainUnit(com.microproject.datatype.TimeUnit.PERCENT));
	}

	@Test
	void convertsLegacyDurationAndRateWithoutAliasing() {
		com.microproject.datatype.Duration duration = TimeTypeBridge.toDomainDuration(
				new Duration(2D, TimeUnit.DAYS));
		com.microproject.datatype.Rate rate = TimeTypeBridge.toDomainRate(
				new Rate(3D, TimeUnit.HOURS));
		assertEquals(2D, com.microproject.datatype.Duration.getValue(duration.getEncodedMillis()), 0.0001D);
		assertEquals(3D, rate.getValue(), 0.0001D);
		assertEquals(com.microproject.datatype.TimeUnit.DAYS, com.microproject.datatype.Duration.getType(duration.getEncodedMillis()));
		assertEquals(com.microproject.datatype.TimeUnit.HOURS, rate.getTimeUnit());
		Duration legacyDuration = TimeTypeBridge.fromDomainDuration(duration);
		Rate legacyRate = TimeTypeBridge.fromDomainRate(rate);
		assertEquals(2D, legacyDuration.getValue(), 0.0001D);
		assertEquals(TimeUnit.DAYS, legacyDuration.getUnit());
		assertEquals(3D, legacyRate.getValue(), 0.0001D);
		assertEquals(TimeUnit.HOURS, legacyRate.getUnit());
	}
}
