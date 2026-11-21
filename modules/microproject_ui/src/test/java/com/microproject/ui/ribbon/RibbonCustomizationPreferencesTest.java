/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.ui.ribbon.RibbonCustomization.CustomGroup;
import com.microproject.ui.ribbon.RibbonCustomization.CustomTab;

class RibbonCustomizationPreferencesTest {
	private static final String KEY = "ribbonCustomization";
	private static final String QAT_KEY = "ribbonQuickAccessCommands";
	private final Preferences preferences = Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
	private String previousValue;
	private String previousQuickAccess;

	@BeforeEach
	void isolatePreferences() {
		previousValue = preferences.get(KEY, null);
		previousQuickAccess = preferences.get(QAT_KEY, null);
		preferences.remove(KEY);
		preferences.remove(QAT_KEY);
	}

	@AfterEach
	void restorePreferences() {
		restore(KEY, previousValue);
		restore(QAT_KEY, previousQuickAccess);
	}

	private void restore(String key, String value) {
		if (value == null) preferences.remove(key);
		else preferences.put(key, value);
	}

	private static RibbonCustomization sample() {
		CustomGroup onTask = new CustomGroup("g-task", "My group \u65e5\u672c:\u8a9e, x", List.of("RibbonFind", "RibbonUndo"));
		CustomGroup inCustom = new CustomGroup("g-custom", "Quick", List.of("RibbonSaveProject"));
		return new RibbonCustomization(List.of("ViewRibbonTask", "TaskRibbonTask"), Set.of("HelpRibbonTask"),
			List.of(new CustomTab("my-tab", "My Tab", List.of(inCustom))),
			Map.of("TaskRibbonTask", List.of(onTask)));
	}

	@Test
	void defaultsToNoCustomization() {
		assertTrue(RibbonDisplayPreferences.loadRibbonCustomization().isEmpty());
	}

	@Test
	void roundTripsThroughPreferencesAndAFreshLoad() {
		RibbonCustomization saved = sample();
		RibbonDisplayPreferences.saveRibbonCustomization(saved);

		assertEquals(saved, RibbonDisplayPreferences.loadRibbonCustomization());
		assertEquals(saved, RibbonCustomization.parse(preferences.get(KEY, null)));
		assertTrue(preferences.get(KEY, "").startsWith("microProject.ribbonCustomization.v1\n"));
	}

	@Test
	void fileTabCannotBeHiddenOrMoved() {
		RibbonCustomization customization = new RibbonCustomization(
			List.of("FileRibbonTask", "ViewRibbonTask"), Set.of("FileRibbonTask", "HelpRibbonTask"), List.of(), Map.of());

		assertEquals(List.of("ViewRibbonTask"), customization.defaultTabOrder());
		assertEquals(Set.of("HelpRibbonTask"), customization.hiddenDefaultTabs());
	}

	@Test
	void defaultGroupsAndCommandsAreNotPartOfTheModel() {
		// Only custom groups exist in the model; default groups/commands cannot be expressed, hence not changed.
		assertEquals(Set.of(), RibbonCustomization.none().defaultTabGroups().keySet());
		RibbonCustomization parsed = RibbonCustomization.parse(
			"microProject.ribbonCustomization.v1\ngroup:TaskRibbonTask:RibbonFind:Renamed:RibbonFind\n");
		assertEquals(1, parsed.defaultTabGroups().get("TaskRibbonTask").size());
		assertEquals("Renamed", parsed.defaultTabGroups().get("TaskRibbonTask").get(0).name());
		assertThrows(UnsupportedOperationException.class, () -> parsed.defaultTabGroups().clear());
	}

	@Test
	void unknownTabAndCommandIdsAreDroppedWithoutRejectingTheRest() {
		CustomGroup group = new CustomGroup("g", "Group", List.of("NoSuchCommand", "RibbonFind"));
		RibbonCustomization customization = new RibbonCustomization(
			List.of("NoSuchTab", "ViewRibbonTask"), Set.of("NoSuchTab2"), List.of(),
			Map.of("NoSuchTab3", List.of(group), "TaskRibbonTask", List.of(group)));

		assertEquals(List.of("RibbonFind"), group.commandIds());
		assertEquals(List.of("ViewRibbonTask"), customization.defaultTabOrder());
		assertTrue(customization.hiddenDefaultTabs().isEmpty());
		assertEquals(Set.of("TaskRibbonTask"), customization.defaultTabGroups().keySet());
	}

	@Test
	void emptyNamesAndDuplicatesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> new CustomGroup("g", "  ", List.of()));
		assertThrows(IllegalArgumentException.class, () -> new CustomTab("t", "", List.of()));
		assertThrows(IllegalArgumentException.class, () -> new CustomGroup("g", "G", List.of("RibbonFind", "RibbonFind")));
		CustomGroup group = new CustomGroup("g", "G", List.of());
		assertThrows(IllegalArgumentException.class, () -> new CustomTab("t", "T", List.of(group, group)));
		assertThrows(IllegalArgumentException.class, () -> new RibbonCustomization(List.of(), Set.of(),
			List.of(new CustomTab("TaskRibbonTask", "Clash", List.of())), Map.of()));
	}

	@Test
	void resetClearsEverythingButLeavesQuickAccessAlone() {
		RibbonDisplayPreferences.saveQuickAccessCommands(List.of("RibbonFind"));
		RibbonDisplayPreferences.saveRibbonCustomization(sample());

		RibbonDisplayPreferences.resetRibbonCustomization();

		assertTrue(RibbonDisplayPreferences.loadRibbonCustomization().isEmpty());
		assertEquals(List.of("RibbonFind"), RibbonDisplayPreferences.loadQuickAccessCommands());
	}

	@Test
	void resetTabRemovesOnlyThatTabsCustomization() {
		RibbonDisplayPreferences.saveRibbonCustomization(sample());

		RibbonDisplayPreferences.resetRibbonTab("TaskRibbonTask");
		RibbonCustomization afterTask = RibbonDisplayPreferences.loadRibbonCustomization();
		assertEquals(List.of("ViewRibbonTask"), afterTask.defaultTabOrder());
		assertTrue(afterTask.defaultTabGroups().isEmpty());
		assertEquals(Set.of("HelpRibbonTask"), afterTask.hiddenDefaultTabs());
		assertEquals(1, afterTask.customTabs().size());

		RibbonDisplayPreferences.resetRibbonTab("HelpRibbonTask");
		assertTrue(RibbonDisplayPreferences.loadRibbonCustomization().hiddenDefaultTabs().isEmpty());

		RibbonDisplayPreferences.resetRibbonTab("my-tab");
		assertTrue(RibbonDisplayPreferences.loadRibbonCustomization().customTabs().isEmpty());
	}

	@Test
	void malformedOrUnknownVersionValuesMeanNoCustomization() {
		for (String bad : List.of("garbage", "", "microProject.ribbonCustomization.v2\norder:TaskRibbonTask\n",
			"microProject.ribbonCustomization.v1\ngroup:TaskRibbonTask\n",
			"microProject.ribbonCustomization.v1\nbogus:x\n",
			"microProject.ribbonCustomization.v1\ntab:t:%ZZ\n",
			"microProject.ribbonCustomization.v1\ntab:t:%20\n")) {
			preferences.put(KEY, bad);
			assertTrue(RibbonDisplayPreferences.loadRibbonCustomization().isEmpty(), bad);
		}
		assertFalse(sample().isEmpty());
	}

	@Test
	void savingNoCustomizationRemovesTheKey() {
		RibbonDisplayPreferences.saveRibbonCustomization(sample());
		RibbonDisplayPreferences.saveRibbonCustomization(RibbonCustomization.none());
		assertEquals(null, preferences.get(KEY, null));
	}

	@Test
	void recordsWithTheWrongFieldCountAreMalformed() {
		for (String bad : List.of("tab:t:Name:extra", "tab:t", "order:TaskRibbonTask:x", "hidden:HelpRibbonTask:x",
			"group:TaskRibbonTask:g:Name:RibbonFind:extra")) {
			assertTrue(RibbonCustomization.parse("microProject.ribbonCustomization.v1\n" + bad + "\n").isEmpty(), bad);
		}
	}

	@Test
	void duplicateTabAndGroupDeclarationsAreMalformed() {
		assertTrue(RibbonCustomization.parse("microProject.ribbonCustomization.v1\n" + "tab:t:First\ntab:t:Second\n").isEmpty());
		assertTrue(RibbonCustomization.parse("microProject.ribbonCustomization.v1\n"
			+ "group:TaskRibbonTask:g:A:RibbonFind\ngroup:ViewRibbonTask:g:B:RibbonFind\n").isEmpty());
	}

	@Test
	void oversizedCustomizationIsRejectedAndKeepsThePreviousValue() {
		RibbonCustomization small = sample();
		RibbonDisplayPreferences.saveRibbonCustomization(small);
		List<CustomTab> tabs = new java.util.ArrayList<>();
		for (int i = 0; i < 400; i++) tabs.add(new CustomTab("tab-" + i, "Tab number " + i, List.of()));
		RibbonCustomization huge = new RibbonCustomization(List.of(), Set.of(), tabs, Map.of());

		assertThrows(IllegalArgumentException.class, () -> RibbonDisplayPreferences.saveRibbonCustomization(huge));

		assertEquals(small, RibbonDisplayPreferences.loadRibbonCustomization());
	}
}
