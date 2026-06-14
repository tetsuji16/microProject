package com.projectlibre1.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeInputParserTest {
	@Test
	void parsesLeadingHour() {
		assertEquals(9, TimeInputParser.parseHour("9:30", 0));
	}

	@Test
	void returnsFallbackForBlankOrInvalidInput() {
		assertEquals(7, TimeInputParser.parseHour(null, 7));
		assertEquals(7, TimeInputParser.parseHour("   ", 7));
		assertEquals(7, TimeInputParser.parseHour("not-a-number", 7));
	}
}
