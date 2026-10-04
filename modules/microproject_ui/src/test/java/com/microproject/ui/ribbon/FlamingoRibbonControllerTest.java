/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Action;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;

class FlamingoRibbonControllerTest {
	@Test
	void commandActionNameChangesUpdateNativeButtonTextAndAccessibilityName() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			MenuRibbonCommandSource commands = new MenuRibbonCommandSource(buttons);
			SwingRibbonFactory factory = new SwingRibbonFactory(commands, MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
			AbstractCommandButton button = findCommand(host, "RibbonTaskInformation");
			assertNotNull(button);
			assertFalse(button.getText().equals(button.getName()),
				"ribbon labels must use the resource text instead of leaking internal command IDs");
			Action action = commands.createAction("RibbonTaskInformation");
			action.putValue(Action.NAME, "Updated command name");
			assertEquals("Updated command name", button.getText());
		});
	}

	@Test
	void autoHideAltBindingRevealsTheRibbonAndIsRemovedWhenModeChanges() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
			JRootPane root = new JRootPane();
			root.setContentPane(host);
			FlamingoRibbonController ribbon = (FlamingoRibbonController) host
				.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.AUTO_HIDE);
			assertFalse(ribbon.isVisible());
			assertFalse(ribbon.isCommandSurfaceVisible());
			assertEquals(FlamingoRibbonController.AUTO_HIDE_REVEAL_ACTION,
				root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(FlamingoRibbonController.AUTO_HIDE_REVEAL_KEY));
			var reveal = root.getActionMap().get(FlamingoRibbonController.AUTO_HIDE_REVEAL_ACTION);
			assertNotNull(reveal, "Alt reveal binding must have one root-pane action");
			reveal.actionPerformed(new java.awt.event.ActionEvent(root, java.awt.event.ActionEvent.ACTION_PERFORMED,
				FlamingoRibbonController.AUTO_HIDE_REVEAL_ACTION));
			assertTrue(ribbon.isVisible());
			assertTrue(ribbon.isCommandSurfaceVisible());
			assertFalse(findRibbon(host).isMinimized());

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode());
			assertTrue(ribbon.isCommandSurfaceVisible());
			assertTrue(root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.get(FlamingoRibbonController.AUTO_HIDE_REVEAL_KEY) == null,
				"leaving auto-hide must remove only the binding owned by this ribbon");
		});
	}

	@Test
	void displayModesKeepTheSelectedTabAndCommandRegistrationWhileChangingOnlyChromeVisibility() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			ExtToolBarFactory buttons = new ExtToolBarFactory(MenuActionMapSupport.noopActionMap(),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons),
				MenuDefinitionSupport.ribbonBundles(Locale.ROOT));
			JPanel host = factory.createPanel(MenuManager.STANDARD_RIBBON, () -> { });
			RibbonController ribbon = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			JRibbon nativeRibbon = findRibbon(host);
			var taskTab = nativeRibbon.getTask(1);
			nativeRibbon.setSelectedTask(taskTab);
			assertEquals("Task", taskTab.getTitle());

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.TABS_ONLY);
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode());
			assertTrue(nativeRibbon.isMinimized(), "tabs-only mode uses Flamingo's minimized layout");
			assertNotNull(findCommand(host, "RibbonInsert"),
				"hiding chrome must not unregister commands");

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
			assertEquals(taskTab, nativeRibbon.getSelectedTask(), "visibility changes must not replace the active tab");
			assertFalse(nativeRibbon.isMinimized());

			ribbon.setRibbonDisplayMode(RibbonDisplayMode.AUTO_HIDE);
			assertFalse(((javax.swing.JComponent) ribbon).isVisible(), "auto-hide must remove the ribbon surface");
			assertEquals(RibbonDisplayMode.AUTO_HIDE, ribbon.getRibbonDisplayMode());
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from auto-hide must restore the full ribbon");
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.TABS_ONLY, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from full mode must collapse command bands");
			ribbon.toggleRibbonCollapseMode();
			assertEquals(RibbonDisplayMode.ALWAYS_SHOW, ribbon.getRibbonDisplayMode(),
				"Ctrl+F1 from tabs-only mode must restore command bands");

		});
	}

	private static JRibbon findRibbon(JPanel root) {
		return UiComponentWalker.flatten(root).stream().filter(JRibbon.class::isInstance)
			.map(JRibbon.class::cast).findFirst().orElseThrow();
	}

	private static AbstractCommandButton findCommand(JPanel root, String command) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractCommandButton.class::isInstance)
			.map(AbstractCommandButton.class::cast).filter(button -> command.equals(button.getName()))
			.findFirst().orElse(null);
	}
}
