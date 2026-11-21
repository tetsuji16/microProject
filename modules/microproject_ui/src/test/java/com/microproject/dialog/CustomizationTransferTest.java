/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.ui.ribbon.RibbonCustomization;
import com.microproject.ui.ribbon.RibbonCustomization.CustomGroup;
import com.microproject.ui.ribbon.RibbonCustomization.CustomTab;
import com.microproject.ui.ribbon.RibbonCustomizationFile;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;

class CustomizationTransferTest {
	private final CustomizationTransfer.FileChoice previousChoice = CustomizationTransfer.fileChoice;
	private final java.util.function.BiConsumer<java.awt.Component, String> previousRefusal = CustomizationTransfer.importRefusal;

	@TempDir
	Path directory;

	@AfterEach
	void restoreSeams() {
		CustomizationTransfer.fileChoice = previousChoice;
		CustomizationTransfer.importRefusal = previousRefusal;
	}

	@Test
	void anImportThatApplyCouldNotStoreIsRefusedAndLeavesTheDraftsUnchanged() throws Exception {
		List<CustomTab> tabs = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			tabs.add(new CustomTab("tab-" + i, "A fairly long custom tab name number " + i,
				List.of(new CustomGroup("group-" + i, "Group " + i, List.of("RibbonFind")))));
		}
		RibbonCustomization oversized = new RibbonCustomization(List.of(), Set.of(), tabs, Map.of());
		assertFalse(RibbonDisplayPreferences.isStorable(oversized), "the fixture must exceed the storage limit");
		Path file = directory.resolve("big.mpui");
		RibbonCustomizationFile.write(file, oversized, List.of("RibbonFind"));

		List<String> refusals = new ArrayList<>();
		List<RibbonCustomization> replaced = new ArrayList<>();
		CustomizationTransfer.fileChoice = (parent, forSave) -> file;
		CustomizationTransfer.importRefusal = (parent, message) -> refusals.add(message);
		CustomizationTransfer transfer = new CustomizationTransfer(RibbonCustomization::none, List::of,
			(customization, commands) -> replaced.add(customization));

		transfer.importFrom(null);

		assertEquals(List.of(UsabilityStrings.text("ribbonCustomize.tooLarge")), refusals);
		assertEquals(List.of(), replaced, "neither draft may be replaced");
	}
}
