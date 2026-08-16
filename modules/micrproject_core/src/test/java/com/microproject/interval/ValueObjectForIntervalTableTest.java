package com.microproject.interval;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.microproject.pm.availability.AvailabilityTable;

/**
 * Issue #167: findActive() used to call valueObjects.get(-1) when the table
 * was empty or the date preceded the first entry, throwing
 * IndexOutOfBoundsException. Callers (e.g. ResourceAvailabilityFunctor)
 * already handle a null result.
 */
class ValueObjectForIntervalTableTest {

	@Test
	void findActiveReturnsNullInsteadOfThrowingForEmptyTable() {
		AvailabilityTable table = new AvailabilityTable();
		assertNull(table.findActive(System.currentTimeMillis()));
		assertNull(table.findCurrent());
	}
}
