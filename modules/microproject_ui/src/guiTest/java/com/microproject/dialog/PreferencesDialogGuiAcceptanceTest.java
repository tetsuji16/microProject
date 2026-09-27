/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.preference.GlobalPreferences;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.shell.AutoSaveControl;

/** Visible coverage for the user preference controls added to the desktop dialog. */
class PreferencesDialogGuiAcceptanceTest {
	@AfterEach
	void closeDialogs() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof PreferencesDialogBox)
					window.dispose();
		});
	}

	@Test
	void preferencesDialogVisiblyOffersThemeAutomaticGridColorAndReset() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		AtomicInteger appliedInterval = new AtomicInteger(-1);
		SwingUtilities.invokeLater(() -> PreferencesDialogBox.showDialog(null, new GlobalPreferences(), null,
			new AutoSaveControl() {
				private int intervalMinutes = DEFAULT_INTERVAL_MINUTES;
				public boolean isEnabled() { return true; }
				public void setEnabled(boolean enabled) { }
				public int getIntervalMinutes() { return intervalMinutes; }
				public void setIntervalMinutes(int minutes) { intervalMinutes = minutes; appliedInterval.set(minutes); }
			}));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Preferences dialog did not open");
		PreferencesDialogBox dialog = findDialog();
		SwingUtilities.invokeAndWait(() -> {
			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.requestFocus();
		});
		Robot robot = new Robot();
		robot.delay(300);
		robot.waitForIdle();
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.gridColorAutomatic")));
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.ganttBarColorAutomatic")));
		assertTrue(hasButton(dialog, UsabilityStrings.text("preferences.reset")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextResourceNames")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextTaskName")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionAutomatic")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionRight")));
		assertTrue(hasComboItem(dialog, UsabilityStrings.text("preferences.ganttBarTextPositionLeft")));
		JSpinner interval = findNamedComponent(dialog, JSpinner.class, "preferencesAutoSaveInterval");
		assertTrue(interval != null, "Preferences must expose the auto-recovery interval");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(), "Preferences dialog");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Preferences dialog");
		capture(robot, dialog);
		JTextField intervalEditor = ((JSpinner.DefaultEditor) interval.getEditor()).getTextField();
		click(robot, intervalEditor);
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_1); robot.keyRelease(KeyEvent.VK_1);
		robot.keyPress(KeyEvent.VK_2); robot.keyRelease(KeyEvent.VK_2);
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
		click(robot, findButton(dialog, UsabilityStrings.text("preferences.apply")));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Preferences dialog did not close after Apply");
		assertEquals(12, appliedInterval.get(), "Apply must persist the entered recovery interval");
	}

	private static PreferencesDialogBox findDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof PreferencesDialogBox dialog && dialog.isVisible())
				return dialog;
		return null;
	}

	private static boolean hasButton(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return true;
			if (child instanceof java.awt.Container nested && hasButton(nested, text)) return true;
		}
		return false;
	}

	private static AbstractButton findButton(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton found = findButton(nested, text);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = component.getLocationOnScreen();
			center[0] = new Point(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static <T extends java.awt.Component> T findNamedComponent(java.awt.Container container, Class<T> type,
			String name) {
		for (java.awt.Component child : container.getComponents()) {
			if (type.isInstance(child) && name.equals(child.getName())) return type.cast(child);
			if (child instanceof java.awt.Container nested) {
				T found = findNamedComponent(nested, type, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static boolean hasComboItem(java.awt.Container container, String text) {
		for (java.awt.Component child : container.getComponents()) {
			if (child instanceof javax.swing.JComboBox<?> combo)
				for (int index = 0; index < combo.getItemCount(); index++)
					if (text.equals(combo.getItemAt(index))) return true;
			if (child instanceof java.awt.Container nested && hasComboItem(nested, text)) return true;
		}
		return false;
	}

	private static void capture(Robot robot, JDialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(dialog.getRootPane().getLocationOnScreen(), dialog.getRootPane().getSize()));
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds[0]), "png",
			directory.resolve("preferences-grid-color.png").toFile());
	}
}
