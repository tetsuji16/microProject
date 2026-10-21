/*
 * Copyright (c) 2026 microProject
 * SPDX-License-Identifier: MIT
 */
package com.microproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.LookAndFeel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.microproject.testsupport.GuiRobot;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class FlatUiSupportDialogBindingsGuiAcceptanceTest {
	@Test
	void parentLookAndFeelRefreshPreservesOwnedDialogActions() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for window-tree UI refresh.");
		JFrame[] owner = new JFrame[1];
		JDialog[] dialog = new JDialog[1];
		JPanel[] inputTarget = new JPanel[1];
		JLabel[] result = new JLabel[1];
		LookAndFeel[] originalLookAndFeel = new LookAndFeel[1];
		AtomicInteger escapeCount = new AtomicInteger();
		AtomicInteger enterCount = new AtomicInteger();
		AtomicInteger helpCount = new AtomicInteger();
		Robot robot = new GuiRobot();
		robot.setAutoDelay(80);

		try {
			SwingUtilities.invokeAndWait(() -> {
				originalLookAndFeel[0] = UIManager.getLookAndFeel();
				owner[0] = new JFrame("dialog binding refresh owner");
				dialog[0] = new JDialog(owner[0], "dialog binding refresh", false);
				inputTarget[0] = new JPanel();
				inputTarget[0].setFocusable(true);
				result[0] = new JLabel("ready");
				inputTarget[0].add(result[0]);
				dialog[0].setContentPane(inputTarget[0]);
				dialog[0].setSize(320, 100);
				owner[0].setSize(320, 160);
				owner[0].setLocation(80, 80);
				dialog[0].setLocation(420, 80);
				assertTrue(java.util.List.of(owner[0].getOwnedWindows()).contains(dialog[0]),
					"the test dialog must be an owned window of the refreshed frame");
				Map<KeyStroke, Action> expected = new LinkedHashMap<>();
				bind(dialog[0], expected, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialog.escape", () -> {
					escapeCount.incrementAndGet();
					dialog[0].setVisible(false);
				});
				bind(dialog[0], expected, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dialog.enter", () -> {
					enterCount.incrementAndGet();
					result[0].setText("entered");
				});
				bind(dialog[0], expected, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "dialog.help", () -> {
					helpCount.incrementAndGet();
					result[0].setText("help shown");
				});
				bind(dialog[0], expected, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "dialog.undo");

				setLookAndFeel(originalLookAndFeel[0] instanceof FlatDarkLaf
					? new FlatLightLaf() : new FlatDarkLaf());
				FlatUiSupport.updateComponentTreeUI(owner[0]);

				for (Map.Entry<KeyStroke, Action> binding : expected.entrySet()) {
					String actionKey = (String)dialog[0].getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
						.get(binding.getKey());
					assertTrue(actionKey.startsWith("dialog."), "the root-pane key mapping must survive for " + binding.getKey());
					assertSame(binding.getValue(), dialog[0].getRootPane().getActionMap().get(actionKey),
						"the exact dialog action must survive for " + binding.getKey());
				}
				owner[0].setVisible(true);
				dialog[0].setVisible(true);
			});

			activateDialog(robot, dialog[0], inputTarget[0]);
			SwingUtilities.invokeAndWait(() -> assertFocused(dialog[0], inputTarget[0]));
			press(robot, KeyEvent.VK_ENTER);
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(1, enterCount.get(), "Enter must dispatch the dialog-owned action exactly once");
				assertEquals("entered", result[0].getText(), "Enter must update visible dialog content");
			});
			press(robot, KeyEvent.VK_F1);
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(1, helpCount.get(), "F1 must dispatch the dialog-owned action exactly once");
				assertEquals("help shown", result[0].getText(), "F1 must update visible dialog content");
			});
			press(robot, KeyEvent.VK_ESCAPE);
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(1, escapeCount.get(), "Escape must dispatch the dialog-owned action exactly once");
				assertTrue(!dialog[0].isVisible(), "Escape must close the dialog");
			});
		} catch (Exception | AssertionError failure) {
			if (originalLookAndFeel[0] != null) {
				try {
					SwingUtilities.invokeAndWait(() -> setLookAndFeel(originalLookAndFeel[0]));
				} catch (Exception restoreFailure) {
					failure.addSuppressed(restoreFailure);
				}
			}
			throw failure;
		}
		SwingUtilities.invokeAndWait(() -> {
			setLookAndFeel(originalLookAndFeel[0]);
			FlatUiSupport.updateComponentTreeUI(owner[0]);
			dialog[0].dispose();
			owner[0].dispose();
		});
	}

	private static void assertFocused(JDialog dialog, JPanel inputTarget) {
		assertTrue(dialog.isActive(), "the test dialog must be the active window before key dispatch");
		assertSame(inputTarget, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
			"the dialog content panel must be the actual focused input target");
	}

	private static void setLookAndFeel(LookAndFeel lookAndFeel) {
		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (javax.swing.UnsupportedLookAndFeelException exception) {
			throw new AssertionError("Could not install test Look & Feel " + lookAndFeel.getName(), exception);
		}
	}

	private static void bind(JDialog dialog, Map<KeyStroke, Action> expected, KeyStroke keyStroke, String actionKey,
			Runnable handler) {
		Action action = new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) { handler.run(); }
		};
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
		dialog.getRootPane().getActionMap().put(actionKey, action);
		expected.put(keyStroke, action);
	}

	private static void bind(JDialog dialog, Map<KeyStroke, Action> expected, KeyStroke keyStroke, String actionKey) {
		bind(dialog, expected, keyStroke, actionKey, () -> { });
	}

	private static void press(Robot robot, int keyCode) {
		robot.waitForIdle();
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		robot.waitForIdle();
	}

	private static void activateDialog(Robot robot, JDialog dialog, JPanel inputTarget) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
			inputTarget.requestFocusInWindow();
			Point location = dialog.getLocationOnScreen();
			center[0] = new Point(location.x + dialog.getWidth() / 2, location.y + dialog.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(inputTarget::requestFocusInWindow);
	}
}
