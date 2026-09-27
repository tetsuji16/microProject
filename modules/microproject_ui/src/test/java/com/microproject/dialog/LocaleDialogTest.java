/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class LocaleDialogTest {
	@Test
	void countryEqualityPreservesNullTypeAndCodeBehavior() {
		LocaleDialog.Country us = new LocaleDialog.Country("US", "United States");

		assertEquals(us, new LocaleDialog.Country("US", "United States"));
		assertFalse(us.equals(new LocaleDialog.Country("CA", "Canada")));
		assertFalse(us.equals(null));
		assertFalse(us.equals("US"));
	}
}
