package com.microproject.graphic.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Issue #186: initZoomX/initZoomY used Double.parseDouble on config tokens, so
 * a non-numeric zoom ratio threw NumberFormatException when zooming.
 */
class BarStylesMalformedZoomTest {

	@Test
	void malformedZoomRatioDegradesInsteadOfThrowing() {
		BarStyles styles = new BarStyles();
		styles.setZoomX("2,*,abc");
		styles.setZoomY("1.5,abc,3");
		assertDoesNotThrow(() -> styles.getRatioX(0, true));
		assertDoesNotThrow(() -> styles.getRatioY(0, true));
		// the malformed token degrades to a 1.0 ratio instead of throwing, and it must not
		// skip an array slot (index++ inside the throwing parse expression used to
		// double-increment, leaving a 0.0 hole and overflowing on the trailing token)
		assertEquals(2.0, styles.zoomRatioX[0], 1e-9);
		assertEquals(1.0, styles.zoomRatioX[1], 1e-9);
		assertEquals(1.5, styles.zoomRatioY[0], 1e-9);
		assertEquals(1.0, styles.zoomRatioY[1], 1e-9);
		assertEquals(3.0, styles.zoomRatioY[2], 1e-9);
		assertEquals(1.0, styles.getRatioX(0, true), 1e-9);
		assertEquals(1.5, styles.getRatioY(0, true), 1e-9);
	}

	@Test
	void validZoomConfigStillResolves() {
		BarStyles styles = new BarStyles();
		styles.setZoomX("2,*,1");
		styles.setZoomY("1.5,*,3");
		assertDoesNotThrow(() -> styles.getRatioX(0, true));
		assertDoesNotThrow(() -> styles.getRatioY(0, true));
		assertEquals(2.0, styles.zoomRatioX[0], 1e-9);
		assertEquals(1.0, styles.zoomRatioX[1], 1e-9);
		assertEquals(1.5, styles.zoomRatioY[0], 1e-9);
		assertEquals(3.0, styles.zoomRatioY[1], 1e-9);
		assertEquals(1, styles.defaultZoomIndexX);
		assertEquals(1, styles.defaultZoomIndexY);
	}
}
