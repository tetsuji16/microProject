/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.ribbon.SwingRibbonModel;
import com.microproject.ui.ribbon.RibbonCustomization.CustomGroup;
import com.microproject.ui.ribbon.RibbonCustomization.CustomTab;

/** Locks how a saved {@link RibbonCustomization} is rendered by the ribbon. */
class RibbonCustomizationRenderTest {
	private static final String KEY = "ribbonCustomization";
	private final Preferences preferences = Preferences.userNodeForPackage(RibbonDisplayPreferences.class);
	private String previousValue;

	@BeforeEach
	void isolatePreferences() {
		previousValue = preferences.get(KEY, null);
		preferences.remove(KEY);
	}

	@AfterEach
	void restorePreferences() {
		if (previousValue == null) preferences.remove(KEY);
		else preferences.put(KEY, previousValue);
	}

	private static SwingRibbonFactory factory() {
		return MenuManager.getInstance(MenuActionMapSupport.noopActionMap()).getRibbonFactory();
	}

	private static List<String> tabIds(SwingRibbonModel model) {
		return model.getTabs().stream().map(SwingRibbonModel.RibbonTab::getId).toList();
	}

	private static String signature(SwingRibbonModel model) {
		StringBuilder text = new StringBuilder();
		for (SwingRibbonModel.RibbonTab tab : model.getTabs()) {
			text.append(tab.getId()).append('|').append(tab.getTitle()).append('|').append(tab.isContextual())
				.append('|').append(tab.getAccessKey()).append('\n');
			for (SwingRibbonModel.RibbonBand band : tab.getBands()) {
				text.append("  ").append(band.getId()).append('|').append(band.getTitle()).append('\n');
				for (SwingRibbonModel.RibbonButton button : band.getButtons()) {
					text.append("    ").append(button.getId()).append('|').append(button.getKeyTip()).append('|')
						.append(button.getPriority()).append('\n');
				}
			}
		}
		return text.toString();
	}

	private static RibbonCustomization custom(List<String> order, Set<String> hidden, List<CustomTab> tabs,
		Map<String, List<CustomGroup>> groups) {
		return new RibbonCustomization(order, hidden, tabs, groups);
	}

	@Test
	void noCustomizationLeavesTheDefaultRibbonUnchanged() {
		SwingRibbonModel saved = factory().createModel(MenuManager.STANDARD_RIBBON, null);
		SwingRibbonModel none = factory().createModel(MenuManager.STANDARD_RIBBON, null, RibbonCustomization.none());
		assertEquals(signature(none), signature(saved));
		assertEquals("FileRibbonTask", tabIds(none).get(0));
	}

	@Test
	void customTabsFollowTheDefaultTabsInListOrder() {
		SwingRibbonModel base = factory().createModel(MenuManager.STANDARD_RIBBON, null, RibbonCustomization.none());
		RibbonCustomization customization = custom(List.of(), Set.of(), List.of(
			new CustomTab("first-tab", "First", List.of(new CustomGroup("g1", "Edit", List.of("RibbonFind")))),
			new CustomTab("second-tab", "Second", List.of())), Map.of());
		SwingRibbonModel model = factory().createModel(MenuManager.STANDARD_RIBBON, null, customization);
		List<String> expected = new ArrayList<>(tabIds(base));
		expected.addAll(List.of("first-tab", "second-tab"));
		assertEquals(expected, tabIds(model));
		SwingRibbonModel.RibbonTab first = tab(model, "first-tab");
		assertEquals("First", first.getTitle());
		assertEquals("Edit", first.getBands().get(0).getTitle());
		assertEquals("RibbonFind", first.getBands().get(0).getButtons().get(0).getId());
	}

	@Test
	void customGroupOnADefaultTabFollowsItsDefaultGroups() {
		SwingRibbonModel base = factory().createModel(MenuManager.STANDARD_RIBBON, null, RibbonCustomization.none());
		SwingRibbonModel model = factory().createModel(MenuManager.STANDARD_RIBBON, null, custom(List.of(), Set.of(),
			List.of(), Map.of("ViewRibbonTask", List.of(new CustomGroup("g-view", "Mine", List.of("RibbonFind"))))));
		SwingRibbonModel.RibbonTab before = tab(base, "ViewRibbonTask");
		SwingRibbonModel.RibbonTab after = tab(model, "ViewRibbonTask");
		assertEquals(before.getBands().size() + 1, after.getBands().size());
		assertEquals("g-view", after.getBands().get(after.getBands().size() - 1).getId());
		assertEquals(tabIds(base), tabIds(model));
	}

	@Test
	void hiddenDefaultTabIsOmittedAndFileStaysFirstEvenWhenOrdered() {
		RibbonCustomization customization = custom(List.of("ViewRibbonTask", "TaskRibbonTask"),
			Set.of("HelpRibbonTask"), List.of(), Map.of());
		SwingRibbonModel base = factory().createModel(MenuManager.STANDARD_RIBBON, null, RibbonCustomization.none());
		SwingRibbonModel model = factory().createModel(MenuManager.STANDARD_RIBBON, null, customization);
		List<String> ids = tabIds(model);
		assertEquals("FileRibbonTask", ids.get(0));
		assertFalse(ids.contains("HelpRibbonTask"));
		// Task and View swap their default positions; every other tab keeps its place.
		List<String> expected = new ArrayList<>(tabIds(base));
		expected.remove("HelpRibbonTask");
		int task = expected.indexOf("TaskRibbonTask");
		int view = expected.indexOf("ViewRibbonTask");
		expected.set(task, "ViewRibbonTask");
		expected.set(view, "TaskRibbonTask");
		assertEquals(expected, ids);
	}

	@Test
	void hiddenTabHasNoTabButtonAndFileCannotBeHidden() throws Exception {
		SwingRibbonModel model = factory().createModel(MenuManager.STANDARD_RIBBON, null,
			custom(List.of(), Set.of("HelpRibbonTask", "FileRibbonTask"), List.of(), Map.of()));
		assertFalse(tabIds(model).contains("HelpRibbonTask"));
		assertEquals("FileRibbonTask", tabIds(model).get(0));
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = factory().createPanel(model, null);
			assertNull(find(host, "microProjectRibbonTab-HelpRibbonTask"));
			assertNotNull(find(host, "microProjectRibbonTab-TaskRibbonTask"));
		});
	}

	@Test
	void customTabButtonSharesTheActionOfTheDefaultTabButton() throws Exception {
		RibbonCustomization customization = custom(List.of(), Set.of(), List.of(
			new CustomTab("my-tab", "Mine", List.of(new CustomGroup("g1", "Edit", List.of("RibbonFind"))))), Map.of());
		SwingUtilities.invokeAndWait(() -> {
			SwingRibbonFactory factory = factory();
			SwingRibbonModel model = factory.createModel(MenuManager.STANDARD_RIBBON, null, customization);
			JPanel host = factory.createPanel(model, null);
			RibbonController ribbon = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			assertTrue(ribbon.selectTab("TaskRibbonTask"));
			assertTrue(ribbon.selectTab("my-tab"));
			IdentityHashMap<Action, Boolean> actions = new IdentityHashMap<>();
			int buttons = 0;
			for (Object button : factory.getButtonsFromId("RibbonFind")) {
				if (button instanceof AbstractButton abstractButton && abstractButton.getAction() != null) {
					actions.put(abstractButton.getAction(), true);
					buttons++;
				}
			}
			assertTrue(buttons >= 2, "the command is shown on its default tab and in the custom tab");
			assertEquals(1, actions.size(), "one canonical Action serves every button of the command");
		});
	}

	@Test
	void selectTabKeepsExistingTabAndRejectsMissingOnes() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = factory().createPanel(factory().createModel(MenuManager.STANDARD_RIBBON, null,
				custom(List.of(), Set.of("HelpRibbonTask"), List.of(), Map.of())), null);
			RibbonController ribbon = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			assertTrue(ribbon.selectTab("ViewRibbonTask"));
			assertEquals("ViewRibbonTask", ribbon.getSelectedTabId());
			assertFalse(ribbon.selectTab("HelpRibbonTask"));
			assertEquals("ViewRibbonTask", ribbon.getSelectedTabId());
			assertFalse(ribbon.selectTab("FileRibbonTask"));
		});
	}

	@Test
	void customGroupsKeyedToTheFileTabAreRejectedRatherThanDropped() {
		RibbonCustomization customization = custom(List.of(), Set.of(), List.of(),
			Map.of("FileRibbonTask", List.of(new CustomGroup("g-file", "Mine", List.of("RibbonFind")))));
		assertTrue(customization.defaultTabGroups().isEmpty());
	}

	@Test
	void initialSelectionSkipsAnInvisibleContextualTabThatCustomOrderPlacedFirst() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JPanel host = factory().createPanel(factory().createModel(MenuManager.STANDARD_RIBBON, null,
				custom(List.of("FormatRibbonTask", "TaskRibbonTask"), Set.of(), List.of(), Map.of())), null);
			RibbonController ribbon = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			assertNotNull(ribbon.getSelectedTabId());
			assertFalse("FormatRibbonTask".equals(ribbon.getSelectedTabId()));
		});
	}

	private static SwingRibbonModel.RibbonTab tab(SwingRibbonModel model, String id) {
		return model.getTabs().stream().filter(tab -> tab.getId().equals(id)).findFirst().orElseThrow();
	}

	private static Component find(Container root, String name) {
		for (Component child : root.getComponents()) {
			if (name.equals(child.getName())) return child;
			if (child instanceof Container container) {
				Component found = find(container, name);
				if (found != null) return found;
			}
		}
		return null;
	}
}
