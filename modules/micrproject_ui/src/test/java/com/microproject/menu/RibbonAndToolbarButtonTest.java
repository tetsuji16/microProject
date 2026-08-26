/*
 * MIT License
 *
 * Copyright (c) 2026 microProject
 */
package com.microproject.menu;

import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonTaskIds;
import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonUiButtonIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.common.JCommandToggleButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;

import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ui.ribbon.CustomRibbonBandGenerator;
import com.microproject.ui.ribbon.FlamingoRibbonPanel;

class RibbonAndToolbarButtonTest {
	@Test
	void standardRibbonUsesFlamingoTasksBandsAndContextualGroups() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			FlamingoRibbonPanel panel = ribbonPanel(manager);
			JRibbon ribbon = panel.getRibbon();

			assertEquals(ribbonTaskIds().size() - 1, ribbon.getTaskCount());
			assertEquals(1, ribbon.getContextualTaskGroupCount());
			assertFalse(panel.isContextualTabVisible("FormatRibbonTask"));
			panel.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			assertTrue(panel.isContextualTabVisible("FormatRibbonTask"));
			assertEquals("R", ribbon.getTask(3).getKeyTip(), "Report must have a keyboard key tip");
			for (int i = 0; i < ribbon.getTaskCount(); i++) {
				assertFalse(ribbon.getTask(i).getTitle().isBlank());
				assertFalse(ribbon.getTask(i).getBands().isEmpty());
			}
		});
	}

	@Test
	void contextualTitleAndCustomBandUseFlamingoWithoutASecondRibbonModel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			CustomRibbonBandGenerator generator = bandId -> "FormatLayoutRibbonBand".equals(bandId)
					? customBand("Layout from generator") : null;
			FlamingoRibbonPanel panel = assertInstanceOf(FlamingoRibbonPanel.class,
					manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, generator, null));

			panel.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			panel.setContextualTabTitles(Map.of("FormatRibbonTask", "Gantt Chart Format"));
			assertEquals("Gantt Chart Format",
					panel.getRibbon().getContextualTaskGroup(0).getTask(0).getTitle());
			assertTrue(UiComponentWalker.flatten(panel).stream()
					.anyMatch(component -> component instanceof JLabel label
							&& "Layout from generator".equals(label.getText())));
		});
	}

	@Test
	void everyRibbonButtonDispatchesItsActionExactlyOnce() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ClickRecordingActionMap actions = new ClickRecordingActionMap();
			MenuManager manager = MenuManager.getInstance(actions);
			ribbonPanel(manager);

			for (String buttonId : ribbonUiButtonIds()) {
				String actionId = manager.getRibbonFactory().getActionStringFromId(buttonId);
				AbstractCommandButton button = ribbonButton(manager, buttonId);
				int before = actions.clickCount(actionId);
				button.doActionClick();
				assertEquals(before + 1, actions.clickCount(actionId),
						() -> buttonId + " must dispatch " + actionId + " once");
			}
		});
	}

	@Test
	void ribbonCommandStateIsUpdatedThroughTheFlamingoModel() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
			ribbonPanel(manager);
			JCommandToggleButton toggle = assertInstanceOf(JCommandToggleButton.class,
					ribbonButton(manager, "RibbonToggleProgressLine"));

			manager.setActionSelected("ToggleProgressLine", true);
			manager.setActionEnabled("ToggleProgressLine", false);
			manager.setActionVisible("ToggleProgressLine", false);

			assertTrue(toggle.getActionModel().isSelected());
			assertFalse(toggle.isEnabled());
			assertFalse(toggle.isVisible());
		});
	}

	@Test
	void ribbonResourceInventoryHasNoDuplicateCommandOwnership() {
		for (String taskId : ribbonTaskIds()) {
			var commandsInTask = new java.util.HashSet<String>();
			for (String bandId : MenuDefinitionSupport.ribbonBandIds(taskId)) {
				for (String buttonId : ribbonButtonIds(bandId)) {
					assertTrue(commandsInTask.add(buttonId),
							() -> buttonId + " is duplicated within " + taskId);
				}
			}
		}
	}

	@Test
	void japaneseRibbonBuildsAllCommands() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtRibbonFactory factory = new ExtRibbonFactory(MenuActionMapSupport.noopActionMap(),
					MenuDefinitionSupport.ribbonBundles(Locale.JAPANESE));
			FlamingoRibbonPanel panel = new FlamingoRibbonPanel(factory, MenuManager.STANDARD_RIBBON, null, null);
			assertNotNull(panel.getRibbon().getSelectedTask());
			for (String id : ribbonUiButtonIds()) assertNotNull(factory.getButtonsFromId(id), id);
		});
	}

	private static FlamingoRibbonPanel ribbonPanel(MenuManager manager) {
		return assertInstanceOf(FlamingoRibbonPanel.class,
				manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null));
	}

	private static AbstractCommandButton ribbonButton(MenuManager manager, String id) {
		return manager.getToolButtonsFromId(id).stream()
				.filter(AbstractCommandButton.class::isInstance)
				.map(AbstractCommandButton.class::cast)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Ribbon button not found: " + id));
	}

	private static JPanel customBand(String text) {
		JPanel panel = new JPanel();
		panel.add(new JLabel(text));
		return panel;
	}

	private static final class ClickRecordingActionMap implements ProjectMenuActionMap {
		private final Map<String, Integer> clickCounts = new HashMap<>();
		private final Map<String, Action> actions = new HashMap<>();

		@Override
		public Action getAction(String key) {
			return actions.computeIfAbsent(key, actionId -> new AbstractAction(actionId) {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent event) {
					clickCounts.merge(actionId, 1, Integer::sum);
				}
			});
		}

		@Override
		public String getStringFromAction(Action action) {
			return String.valueOf(action.getValue(Action.NAME));
		}

		int clickCount(String actionId) {
			return clickCounts.getOrDefault(actionId, 0);
		}
	}
}
