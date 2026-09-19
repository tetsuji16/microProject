package com.microproject.reports.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Issue #186: report properties were parsed with an unguarded
 * Integer.parseInt; a null or non-numeric collectionType threw
 * NumberFormatException instead of degrading.
 */
class DataSourceProviderTest {

	@Test
	void parseCollectionTypeHandlesNullAndNonNumericValues() {
		assertEquals(-1, DataSourceProvider.parseCollectionType(null));
		assertEquals(-1, DataSourceProvider.parseCollectionType("abc"));
		assertEquals(3, DataSourceProvider.parseCollectionType("3"));
		assertEquals(15, DataSourceProvider.parseCollectionType("15"));
	}
}
