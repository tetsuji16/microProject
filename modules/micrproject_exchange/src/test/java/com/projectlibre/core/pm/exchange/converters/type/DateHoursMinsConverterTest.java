package com.projectlibre.core.pm.exchange.converters.type;

import java.util.Date;

import junit.framework.TestCase;

public class DateHoursMinsConverterTest extends TestCase {
	public void testToConvertsLongBackToDate() {
		DateHoursMinsConverter converter = new DateHoursMinsConverter();
		Date expected = new Date(1_725_000_000_000L);

		assertEquals(expected, converter.to(Long.valueOf(expected.getTime())));
	}

	public void testToRejectsUnexpectedInputTypes() {
		DateHoursMinsConverter converter = new DateHoursMinsConverter();

		try {
			converter.to("not a long");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException expected) {
			assertTrue(expected.getMessage().contains("Expected Long value"));
		}
	}
}
