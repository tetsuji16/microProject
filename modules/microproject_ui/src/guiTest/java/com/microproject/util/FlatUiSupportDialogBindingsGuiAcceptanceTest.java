/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class FlatUiSupportDialogBindingsGuiAcceptanceTest {
	@Test
	void parentLookAndFeelRefreshPreservesOwnedDialogActions() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for window-tree UI refresh.");
		SwingUtilities.invokeAndWait(() -> {
			LookAndFeel originalLookAndFeel = UIManager.getLookAndFeel();
			JFrame owner = new JFrame("dialog binding refresh owner");
			JDialog dialog = new JDialog(owner, "dialog binding refresh", false);
			try {
				assertTrue(java.util.List.of(owner.getOwnedWindows()).contains(dialog),
					"the test dialog must be an owned window of the refreshed frame");
				Map<KeyStroke, Action> expected = new LinkedHashMap<>();
				bind(dialog, expected, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialog.escape");
				bind(dialog, expected, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dialog.enter");
				bind(dialog, expected, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "dialog.help");
				bind(dialog, expected, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "dialog.undo");

				setLookAndFeel(originalLookAndFeel instanceof FlatDarkLaf
					? new FlatLightLaf() : new FlatDarkLaf());
				FlatUiSupport.updateComponentTreeUI(owner);

				for (Map.Entry<KeyStroke, Action> binding : expected.entrySet()) {
					String actionKey = (String)dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
						.get(binding.getKey());
					assertTrue(actionKey.startsWith("dialog."), "the root-pane key mapping must survive for " + binding.getKey());
					assertSame(binding.getValue(), dialog.getRootPane().getActionMap().get(actionKey),
						"the exact dialog action must survive for " + binding.getKey());
				}
			} finally {
				setLookAndFeel(originalLookAndFeel);
				FlatUiSupport.updateComponentTreeUI(owner);
				dialog.dispose();
				owner.dispose();
			}
		});
	}

	private static void setLookAndFeel(LookAndFeel lookAndFeel) {
		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (javax.swing.UnsupportedLookAndFeelException exception) {
			throw new AssertionError("Could not install test Look & Feel " + lookAndFeel.getName(), exception);
		}
	}

	private static void bind(JDialog dialog, Map<KeyStroke, Action> expected, KeyStroke keyStroke, String actionKey) {
		Action action = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) { }
		};
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
		dialog.getRootPane().getActionMap().put(actionKey, action);
		expected.put(keyStroke, action);
	}
}
