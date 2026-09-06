/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.FlatLafSupport;

/** Verifies the real new-project form at the native desktop boundary. */
class ProjectDialogGuiAcceptanceTest {
	private ProjectDialog dialog;

	@AfterEach
	void closeDialog() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows()) {
				if (window instanceof ProjectDialog)
					window.dispose();
			}
		});
	}

	@Test
	void japaneseProjectFormKeepsLabelsAndControlsAtPreferredHeight() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for Robot acceptance coverage.");
		FlatLafSupport.initialize();
		SwingUtilities.invokeAndWait(() -> {
			ProjectDialog.Form form = new ProjectDialog.Form();
			dialog = ProjectDialog.getInstance(null, form);
			dialog.setModal(false);
			dialog.pack();
			dialog.setLocationByPlatform(true);
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);
			dialog.toFront();
			dialog.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(),
			"new-project dialog did not open");

		SwingUtilities.invokeAndWait(() -> {
			for (Component component : allComponents(dialog)) {
				if (!component.isVisible() || !isLayoutProbe(component))
					continue;
				assertTrue(component.getHeight() >= component.getPreferredSize().height,
					() -> component.getClass().getSimpleName() + " height=" + component.getHeight()
						+ " preferred=" + component.getPreferredSize().height);
			}
		});

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		Field cancelField = AbstractDialog.class.getDeclaredField("cancel");
		cancelField.setAccessible(true);
		AbstractButton cancel = (AbstractButton) cancelField.get(dialog);
		assertTrue(cancel.isShowing(), "Cancel button must be visible");
		robot.mouseMove(cancel.getLocationOnScreen().x + cancel.getWidth() / 2,
			cancel.getLocationOnScreen().y + cancel.getHeight() / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Cancel did not close new-project dialog");
	}

	private static boolean isLayoutProbe(Component component) {
		return component instanceof JLabel || component instanceof AbstractButton
			|| component instanceof JTextField || component instanceof JComboBox
			|| component instanceof JScrollPane;
	}

	private static java.util.List<Component> allComponents(Container root) {
		java.util.ArrayList<Component> result = new java.util.ArrayList<>();
		for (Component child : root.getComponents()) {
			result.add(child);
			if (child instanceof Container container)
				result.addAll(allComponents(container));
		}
		return result;
	}

}
