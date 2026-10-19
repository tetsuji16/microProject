/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComponentInputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.plaf.UIResource;

import org.junit.jupiter.api.Test;

class FlatUiSupportRootPaneBindingsTest {
	@Test
	void preservesAllApplicationRootPaneActionsWithoutFreezingLookAndFeelBindings() {
		JRootPane rootPane = new JRootPane();
		List<KeyStroke> keys = List.of(
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
			KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		List<Action> actions = new ArrayList<>();
		for (int index = 0; index < keys.size(); index++) {
			String actionKey = "dialog.test." + index;
			Action action = new AbstractAction() {
				private static final long serialVersionUID = 1L;
				@Override public void actionPerformed(ActionEvent event) { }
			};
			actions.add(action);
			rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keys.get(index), actionKey);
			rootPane.getActionMap().put(actionKey, action);
		}
		KeyStroke lookAndFeelKey = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
		Action oldLookAndFeelAction = new LookAndFeelAction();
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(lookAndFeelKey, "old.lookAndFeel.help");
		rootPane.getActionMap().put("old.lookAndFeel.help", oldLookAndFeelAction);
		InputMap initialLookAndFeelBindings = new ComponentInputMap(rootPane);
		initialLookAndFeelBindings.put(lookAndFeelKey, "old.lookAndFeel.help");
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).setParent(initialLookAndFeelBindings);

		List<FlatUiSupport.RootPaneActionBinding> snapshot = FlatUiSupport.captureRootPaneActionBindings(rootPane);
		assertEquals(keys.size(), snapshot.size(), "only bindings with application-owned Actions should be captured");

		InputMap refreshedLookAndFeelBindings = new ComponentInputMap(rootPane);
		refreshedLookAndFeelBindings.put(lookAndFeelKey, "new.lookAndFeel.help");
		rootPane.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, refreshedLookAndFeelBindings);
		rootPane.getActionMap().clear();
		Action newLookAndFeelAction = new LookAndFeelAction();
		rootPane.getActionMap().put("new.lookAndFeel.help", newLookAndFeelAction);
		FlatUiSupport.restoreRootPaneActionBindings(rootPane, snapshot);

		for (int index = 0; index < keys.size(); index++) {
			String actionKey = "dialog.test." + index;
			assertEquals(actionKey,
				rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(keys.get(index)));
			assertSame(actions.get(index), rootPane.getActionMap().get(actionKey));
		}
		assertEquals("new.lookAndFeel.help", rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(lookAndFeelKey),
			"Look-and-Feel key bindings must be allowed to refresh");
		assertSame(newLookAndFeelAction, rootPane.getActionMap().get("new.lookAndFeel.help"),
			"the refreshed Look-and-Feel Action must not be replaced by the captured one");
	}

	private static final class LookAndFeelAction extends AbstractAction implements UIResource {
		private static final long serialVersionUID = 1L;
		@Override public void actionPerformed(ActionEvent event) { }
	}
}
