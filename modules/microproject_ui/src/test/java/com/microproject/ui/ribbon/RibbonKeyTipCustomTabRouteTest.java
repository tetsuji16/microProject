/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.menu.testsupport.MenuDefinitionSupport;

/** The Alt key sequence must reach a command that sits in a user-defined tab. */
class RibbonKeyTipCustomTabRouteTest {

	@Test
	void theAltSequenceRunsACommandOfACustomTab() throws Exception {
		RibbonCustomization customization = new RibbonCustomization(List.of(), Set.of(),
			List.of(new RibbonCustomization.CustomTab("custom-tab", "Custom Tab", List.of(
				new RibbonCustomization.CustomGroup("custom-group", "Custom Group", List.of("RibbonCut"))))),
			Map.of());
		List<String> executed = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			ResourceBundle[] bundles = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
			ExtToolBarFactory buttons = new ExtToolBarFactory(countingActions(executed), bundles);
			SwingRibbonFactory factory = new SwingRibbonFactory(new MenuRibbonCommandSource(buttons), bundles);
			JPanel host = factory.createPanel(factory.createModel("StandardRibbon", null, customization), null);
			ModernRibbonPanel panel = (ModernRibbonPanel) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
			host.setSize(1000, 300);
			host.doLayout();

			RibbonKeyTipSession session = panel.keyTipSession();
			session.toggle();
			assertEquals(RibbonKeyTipSession.Level.TABS, session.getLevel(), "Alt must show the tab KeyTips");
			session.type('Y');
			session.type('1');
			assertTrue(session.visibleTargets().containsKey("X"),
				"the custom tab must expose the Cut KeyTip, found: " + session.visibleTargets().keySet());
			session.type('X');
			assertFalse(executed.isEmpty(), "the KeyTip X must run the Cut command of the custom tab");
		});
	}

	/** An action map that records which commands ran, so the route is observable without a window. */
	private static ProjectMenuActionMap countingActions(List<String> executed) {
		return new ProjectMenuActionMap() {
			@Override
			public Action getAction(String key) {
				return new AbstractAction(key) {
					@Override
					public void actionPerformed(ActionEvent event) {
						executed.add(key);
					}
				};
			}

			@Override
			public String getStringFromAction(Action action) {
				Object name = action.getValue(Action.NAME);
				return name == null ? "" : name.toString();
			}
		};
	}
}
