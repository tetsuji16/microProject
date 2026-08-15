package com.microproject.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FitToSettingsTest {
	@Test
	void autoRowsArePreservedForMsProjectStyleWidthOnlyScaling() {
		FitToSettings settings = new FitToSettings();

		assertTrue(settings.isRowsAutomatic());
		assertEquals(FitToSettings.AUTOMATIC, settings.getRows());
		assertEquals(1, settings.getEffectiveRows());
	}

	@Test
	void explicitRowsRemainAvailableForOlderSavedSettings() {
		FitToSettings settings = new FitToSettings();

		settings.setRows(4);

		assertEquals(4, settings.getRows());
		assertEquals(4, settings.getEffectiveRows());
	}
}
