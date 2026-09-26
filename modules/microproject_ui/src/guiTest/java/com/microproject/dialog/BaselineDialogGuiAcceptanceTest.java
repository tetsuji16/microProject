/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.lang.reflect.Constructor;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.strings.Messages;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.microproject.ui.theme.MicroProjectTheme;

/** GUI regression for the Clear Baseline controls reported in #715. */
class BaselineDialogGuiAcceptanceTest {
	private BaselineDialog dialog;
	private Font originalDefaultFont;

	@AfterEach
	void closeDialog() throws Exception {
		if (dialog != null) SwingUtilities.invokeAndWait(dialog::dispose);
		SwingUtilities.invokeAndWait(() -> {
			FlatLightLaf.setup();
			MicroProjectTheme.installLight();
			if (originalDefaultFont != null) UIManager.put("defaultFont", originalDefaultFont);
		});
	}

	@Test
	void clearBaselineControlsRemainVisibleAndClickable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for baseline dialog layout acceptance coverage.");
		originalDefaultFont = UIManager.getFont("defaultFont");
		SwingUtilities.invokeAndWait(() -> {
			try {
				Constructor<BaselineDialog> constructor = BaselineDialog.class
					.getDeclaredConstructor(java.awt.Frame.class, BaselineDialog.Form.class, boolean.class);
				constructor.setAccessible(true);
				dialog = constructor.newInstance(null, new BaselineDialog.Form(), true);
				dialog.setTitle(Messages.getString("Text.ClearBaseline"));
				dialog.pack();
				dialog.setLocationRelativeTo(null);
			} catch (ReflectiveOperationException exception) {
				throw new AssertionError("Could not create the Clear Baseline dialog", exception);
			}
		});
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Clear Baseline dialog did not open");

		Container content = dialog.getContentPane();
		JComboBox<?> baseline = dialog.baseline;
		AbstractButton entire = dialog.entireProject;
		AbstractButton selected = dialog.selectedTasks;
		AbstractButton cancel = dialog.cancel;
		AbstractButton ok = dialog.ok;
		assertControlInsideDialog(dialog, baseline, "baseline selector");
		assertControlInsideDialog(dialog, entire, "Entire Project option");
		assertControlInsideDialog(dialog, selected, "Selected Tasks option");
		assertControlInsideDialog(dialog, cancel, "Cancel button");
		assertControlInsideDialog(dialog, ok, "OK button");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(content, "Clear Baseline dialog (#715)");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Clear Baseline dialog (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, content, 100, 40, "Clear Baseline dialog (#724)");
		assertControlInsideDialog(dialog, baseline, "resized baseline selector");
		assertControlInsideDialog(dialog, entire, "resized Entire Project option");
		assertControlInsideDialog(dialog, selected, "resized Selected Tasks option");
		assertControlInsideDialog(dialog, cancel, "resized Cancel button");
		assertControlInsideDialog(dialog, ok, "resized OK button");

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		click(robot, selected);
		assertTrue(selected.isSelected(), "Selected Tasks must respond to a physical click");
		click(robot, entire);
		assertTrue(entire.isSelected(), "Entire Project must respond to a physical click");
		click(robot, cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(),
			"Clear Baseline dialog did not close through its visible Cancel button");

		Font largerFont = (originalDefaultFont == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : originalDefaultFont)
			.deriveFont(16f);
		SwingUtilities.invokeAndWait(() -> {
			FlatDarkLaf.setup();
			UIManager.put("defaultFont", largerFont);
			SwingUtilities.updateComponentTreeUI(dialog);
			dialog.pack();
		});
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Clear Baseline dialog did not reopen after theme/font change");
		assertControlInsideDialog(dialog, dialog.baseline, "reopened baseline selector");
		assertControlInsideDialog(dialog, dialog.entireProject, "reopened Entire Project option");
		assertControlInsideDialog(dialog, dialog.selectedTasks, "reopened Selected Tasks option");
		assertControlInsideDialog(dialog, dialog.cancel, "reopened Cancel button");
		assertControlInsideDialog(dialog, dialog.ok, "reopened OK button");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(),
			"Clear Baseline dialog after reopen, FlatDarkLaf, and larger font (#723)");
		click(robot, dialog.cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "reopened Clear Baseline dialog did not close");
	}

	private static void assertControlInsideDialog(BaselineDialog dialog, Component control, String name) {
		assertTrue(control != null && control.isShowing() && control.getWidth() > 0 && control.getHeight() > 0,
			name + " must be visible with a non-empty hit area");
		Rectangle dialogBounds = new Rectangle(0, 0, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
		Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), dialog.getRootPane());
		assertTrue(dialogBounds.contains(bounds), name + " is clipped by the dialog content: " + bounds);
	}

	private static void click(Robot robot, Component component) throws Exception {
		java.awt.Point point = component.getLocationOnScreen();
		robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}
}
