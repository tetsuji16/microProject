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

	@Test
	void providersMayReturnNullWithoutChangingLegacyCancellationSemantics() {
		UiServices.FileChooserProvider provider = new UiServices.FileChooserProvider() {
			@Override
			public String chooseFileName(boolean save, String selected, Object parent) {
				return null;
			}

			@Override
			public List<String> chooseFileNames(boolean save, String selected, Object parent) {
				return null;
			}
		};

		assertTrue(provider.chooseFileNames(false, null, null) == null,
			"custom providers may return null; callers must treat it as cancellation");
	}
}
