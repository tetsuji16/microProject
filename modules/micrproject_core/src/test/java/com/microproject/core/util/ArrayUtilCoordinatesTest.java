package com.microproject.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Issue #186: stringToCoordinates only validated the token count; a
 * non-numeric token used to escape as NumberFormatException instead of the
 * intended ArrayFormatException.
 */
class ArrayUtilCoordinatesTest {

	@Test
	void stringToCoordinatesParsesValidPairs() throws Exception {
		assertArrayEquals(new double[] { 1.5, 2.0 }, ArrayUtil.stringToCoordinates("1.5, 2.0"), 1e-9);
	}

	@Test
	void stringToCoordinatesRejectsNonNumericTokens() {
		assertThrows(ArrayFormatException.class, () -> ArrayUtil.stringToCoordinates("1, abc"));
		assertThrows(ArrayFormatException.class, () -> ArrayUtil.stringToCoordinates("x, y"));
	}

	@Test
	void stringToCoordinatesRejectsWrongTokenCount() {
		assertThrows(ArrayFormatException.class, () -> ArrayUtil.stringToCoordinates("1, 2, 3"));
	}
}
