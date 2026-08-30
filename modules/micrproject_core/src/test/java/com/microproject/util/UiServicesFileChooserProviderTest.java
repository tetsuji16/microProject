/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class UiServicesFileChooserProviderTest {
	@Test
	void legacyProviderGetsSingleSelectionCompatibility() {
		UiServices.FileChooserProvider provider = (save, selected, parent) -> "C:\\projects\\one.mpo";

		assertEquals(List.of("C:\\projects\\one.mpo"),
			provider.chooseFileNames(false, null, null));
	}

	@Test
	void legacyProviderCancellationBecomesEmptySelection() {
		UiServices.FileChooserProvider provider = (save, selected, parent) -> null;

		assertTrue(provider.chooseFileNames(false, null, null).isEmpty());
	}
}
