/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.ribbon.SwingRibbonModel.RibbonTab;

class SwingRibbonFactoryCustomTabKeyTipTest {

	private static final String STANDARD_RIBBON = "StandardRibbon";

	@Test
	void customTabsReceiveDeterministicKeyTipsStartingWithY1() {
		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(new RibbonCustomization.CustomTab("custom-1", "Custom Tab 1", List.of(
				new RibbonCustomization.CustomGroup("g1", "Group 1", List.of("RibbonFind")))),
			new RibbonCustomization.CustomTab("custom-2", "Custom Tab 2", List.of(
				new RibbonCustomization.CustomGroup("g2", "Group 2", List.of("RibbonCopy")))),
			new RibbonCustomization.CustomTab("custom-3", "Custom Tab 3", List.of(
				new RibbonCustomization.CustomGroup("g3", "Group 3", List.of("RibbonPaste"))))),
			Map.of());

		SwingRibbonModel model = createModelWithCustomization(customization);

		List<RibbonTab> customTabs = model.getTabs().stream()
			.filter(t -> t.getId().startsWith("custom-"))
			.toList();

		assertEquals(3, customTabs.size());
		// First custom tab gets Y1
		assertEquals("Y1", customTabs.get(0).getAccessKey());
		// Second gets Y2
		assertEquals("Y2", customTabs.get(1).getAccessKey());
		// Third gets Y3
		assertEquals("Y3", customTabs.get(2).getAccessKey());
	}

	@Test
	void customTabKeyTipsDoNotConflictWithDefaultTabKeyTips() {
		List<String> defaultKeyTips = createModelWithCustomization(emptyCustomization()).getTabs().stream()
			.map(RibbonTab::getAccessKey)
			.filter(key -> key != null && !key.isBlank())
			.map(key -> key.toUpperCase(Locale.ROOT))
			.toList();

		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(new RibbonCustomization.CustomTab("custom-a", "Custom A", List.of(
				new RibbonCustomization.CustomGroup("g1", "Group 1", List.of("RibbonFind")))),
			new RibbonCustomization.CustomTab("custom-b", "Custom B", List.of(
				new RibbonCustomization.CustomGroup("g2", "Group 2", List.of("RibbonCopy")))),
			new RibbonCustomization.CustomTab("custom-c", "Custom C", List.of(
				new RibbonCustomization.CustomGroup("g3", "Group 3", List.of("RibbonPaste")))),
			new RibbonCustomization.CustomTab("custom-d", "Custom D", List.of(
				new RibbonCustomization.CustomGroup("g4", "Group 4", List.of("RibbonUndo")))),
			new RibbonCustomization.CustomTab("custom-e", "Custom E", List.of(
				new RibbonCustomization.CustomGroup("g5", "Group 5", List.of("RibbonRedo")))),
			new RibbonCustomization.CustomTab("custom-f", "Custom F", List.of(
				new RibbonCustomization.CustomGroup("g6", "Group 6", List.of("RibbonSaveProject")))),
			new RibbonCustomization.CustomTab("custom-g", "Custom G", List.of(
				new RibbonCustomization.CustomGroup("g7", "Group 7", List.of("RibbonPrint")))),
			new RibbonCustomization.CustomTab("custom-h", "Custom H", List.of(
				new RibbonCustomization.CustomGroup("g8", "Group 8", List.of("RibbonNewProject"))))),
			Map.of());

		SwingRibbonModel model = createModelWithCustomization(customization);

		// The default tabs keep the KeyTips the bundle gives them.
		assertEquals(defaultKeyTips, model.getTabs().stream()
			.filter(tab -> !tab.getId().startsWith("custom-"))
			.map(RibbonTab::getAccessKey)
			.filter(key -> key != null && !key.isBlank())
			.map(key -> key.toUpperCase(Locale.ROOT))
			.toList());

		// Custom tabs take the free Y slots in display order.
		List<String> customKeyTips = model.getTabs().stream()
			.filter(tab -> tab.getId().startsWith("custom-"))
			.map(RibbonTab::getAccessKey)
			.toList();
		assertEquals(List.of("Y1", "Y2", "Y3", "Y4", "Y5", "Y6", "Y7", "Y8"), customKeyTips);

		// No custom tab reuses a KeyTip a default tab already owns.
		for (String customKeyTip : customKeyTips) {
			assertFalse(defaultKeyTips.contains(customKeyTip.toUpperCase(Locale.ROOT)),
				"custom tab KeyTip " + customKeyTip + " collides with a default tab");
		}
	}

	@Test
	void customGroupsInDefaultTabsRetainCommandKeyTips() {
		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(),
			Map.of("TaskRibbonTask", List.of(
				new RibbonCustomization.CustomGroup("my-group", "My Group", List.of("RibbonPaste", "RibbonCopy", "RibbonCut")))));

		SwingRibbonModel model = createModelWithCustomization(customization);

		// Find the Task tab
		RibbonTab taskTab = model.getTabs().stream()
			.filter(t -> "TaskRibbonTask".equals(t.getId()))
			.findFirst().orElseThrow();

		// Find the custom group band
		SwingRibbonModel.RibbonBand customBand = taskTab.getBands().stream()
			.filter(b -> "my-group".equals(b.getId()))
			.findFirst().orElseThrow();

		// The commands in the custom group should have their KeyTips from the resource bundle
		List<String> commandKeyTips = new ArrayList<>();
		for (SwingRibbonModel.RibbonButton button : customBand.getButtons()) {
			if (button.getKeyTip() != null) {
				commandKeyTips.add(button.getKeyTip().toUpperCase(Locale.ROOT));
			}
		}

		// RibbonPaste=V, RibbonCopy=C, RibbonCut=X (from menuInternal.properties)
		assertTrue(commandKeyTips.contains("V"), "RibbonPaste KeyTip missing");
		assertTrue(commandKeyTips.contains("C"), "RibbonCopy KeyTip missing");
		assertTrue(commandKeyTips.contains("X"), "RibbonCut KeyTip missing");
	}

	@Test
	void customGroupsInCustomTabsRetainCommandKeyTips() {
		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(new RibbonCustomization.CustomTab("custom-tab", "Custom Tab", List.of(
				new RibbonCustomization.CustomGroup("my-group", "My Group", List.of("RibbonPaste", "RibbonCopy", "RibbonCut"))))),
			Map.of());

		SwingRibbonModel model = createModelWithCustomization(customization);

		// Find the custom tab
		RibbonTab customTab = model.getTabs().stream()
			.filter(t -> "custom-tab".equals(t.getId()))
			.findFirst().orElseThrow();

		// Custom tab should have a KeyTip
		assertNotNull(customTab.getAccessKey(), "Custom tab should have an accessKey");
		assertTrue(customTab.getAccessKey().startsWith("Y"), "Custom tab KeyTip should start with Y");

		// Find the custom group band
		SwingRibbonModel.RibbonBand customBand = customTab.getBands().stream()
			.filter(b -> "my-group".equals(b.getId()))
			.findFirst().orElseThrow();

		// The commands in the custom group should have their KeyTips from the resource bundle
		List<String> commandKeyTips = new ArrayList<>();
		for (SwingRibbonModel.RibbonButton button : customBand.getButtons()) {
			if (button.getKeyTip() != null) {
				commandKeyTips.add(button.getKeyTip().toUpperCase(Locale.ROOT));
			}
		}

		// RibbonPaste=V, RibbonCopy=C, RibbonCut=X
		assertTrue(commandKeyTips.contains("V"), "RibbonPaste KeyTip missing");
		assertTrue(commandKeyTips.contains("C"), "RibbonCopy KeyTip missing");
		assertTrue(commandKeyTips.contains("X"), "RibbonCut KeyTip missing");
	}

	@Test
	void sameCustomizationAlwaysProducesSameKeyTips() {
		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(new RibbonCustomization.CustomTab("tab-1", "Tab 1", List.of(
				new RibbonCustomization.CustomGroup("g1", "Group 1", List.of("RibbonFind")))),
			new RibbonCustomization.CustomTab("tab-2", "Tab 2", List.of(
				new RibbonCustomization.CustomGroup("g2", "Group 2", List.of("RibbonCopy"))))),
			Map.of());

		SwingRibbonModel model1 = createModelWithCustomization(customization);
		SwingRibbonModel model2 = createModelWithCustomization(customization);

		List<RibbonTab> tabs1 = model1.getTabs().stream().filter(t -> t.getId().startsWith("tab-")).toList();
		List<RibbonTab> tabs2 = model2.getTabs().stream().filter(t -> t.getId().startsWith("tab-")).toList();

		assertEquals(tabs1.get(0).getAccessKey(), tabs2.get(0).getAccessKey());
		assertEquals(tabs1.get(1).getAccessKey(), tabs2.get(1).getAccessKey());
	}

	private static RibbonCustomization emptyCustomization() {
		return new RibbonCustomization(List.of(), Set.of(), List.of(), Map.of());
	}

	private SwingRibbonModel createModelWithCustomization(RibbonCustomization customization) {
		ResourceBundle[] bundles = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
		ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(), bundles);
		SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons), bundles);
		return factory.createModel(STANDARD_RIBBON, null, customization);
	}
}