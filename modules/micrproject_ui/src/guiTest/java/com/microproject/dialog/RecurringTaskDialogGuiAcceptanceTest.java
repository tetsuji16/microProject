/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.strings.Messages;

/** Robot coverage for the recurring-task form's scrollable layout and Cancel route. */
class RecurringTaskDialogGuiAcceptanceTest {
	private RecurringTaskDialog dialog;

	@AfterEach
	void closeDialog() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (dialog != null)
				dialog.dispose();
		});
	}

	@Test
	void robotRecurringTaskFormKeepsContentReachableAndCancelCloses() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		SwingUtilities.invokeAndWait(() -> {
			dialog = RecurringTaskDialog.getInstance(null);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isShowing(),
				"recurring-task dialog did not become visible");
		assertTrue(findScrollPane(dialog) != null, "recurring-task form must use a scrollable content viewport");
		assertTrue(dialog.getButtonPanel().getHeight() > 0, "recurring-task buttons must remain laid out");

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		AbstractButton cancel = cancelButton(dialog);
		Rectangle bounds = new Rectangle(cancel.getLocationOnScreen(), cancel.getSize());
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "Cancel did not close recurring-task dialog");
	}

	private static JScrollPane findScrollPane(java.awt.Container root) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof JScrollPane pane)
				return pane;
			if (child instanceof java.awt.Container container) {
				JScrollPane pane = findScrollPane(container);
				if (pane != null)
					return pane;
			}
		}
		return null;
	}

	private static AbstractButton cancelButton(RecurringTaskDialog value) throws Exception {
		AbstractButton[] result = new AbstractButton[1];
		String text = Messages.getString("ButtonText.Cancel");
		SwingUtilities.invokeAndWait(() -> result[0] = findButton(value, text));
		if (result[0] == null)
			throw new AssertionError("Cancel button not found: " + text);
		return result[0];
	}

	private static AbstractButton findButton(java.awt.Container root, String text) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing())
				return button;
			if (child instanceof java.awt.Container container) {
				AbstractButton button = findButton(container, text);
				if (button != null)
					return button;
			}
		}
		return null;
	}
}
