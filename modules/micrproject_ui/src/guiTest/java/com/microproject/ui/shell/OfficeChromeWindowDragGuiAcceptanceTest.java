/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Verifies physical dragging on the custom title row of an undecorated frame. */
class OfficeChromeWindowDragGuiAcceptanceTest {
	private MainRibbonFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
	}

	@Test
	void draggingTheDocumentTitleMovesTheUndecoratedWindow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for this acceptance test.");
		final OfficeChromePanel[] panel = new OfficeChromePanel[1];
		final javax.swing.JLabel[] title = new javax.swing.JLabel[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Office chrome drag acceptance", "", "");
			panel[0] = new OfficeChromePanel(frame, MenuManager.getInstance(MenuActionMapSupport.noopActionMap()),
				new JPanel(), () -> { }, AutoSaveControl.DISABLED);
			frame.setRibbonPanel(panel[0]);
			title[0] = findTitle(panel[0]);
			frame.setSize(900, 180);
			frame.setLocation(120, 120);
			frame.setVisible(true);
			frame.toFront();
		});
		GuiAcceptanceSupport.await(() -> title[0].isShowing(), "document title was not visible");

		Point start = title[0].getLocationOnScreen();
		Point before = frame.getLocation();
		Robot robot = new Robot();
		robot.setAutoDelay(25);
		robot.mouseMove(start.x + Math.max(4, title[0].getWidth() / 2), start.y + title[0].getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(start.x + 60, start.y + 35);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !before.equals(frame.getLocation()), "dragging the title did not move the window");
		assertNotEquals(before, frame.getLocation());
	}

	private static javax.swing.JLabel findTitle(java.awt.Container root) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof javax.swing.JLabel label && OfficeChromePanel.DOCUMENT_TITLE_NAME.equals(label.getName())) return label;
			if (child instanceof java.awt.Container container) {
				try { return findTitle(container); } catch (IllegalArgumentException ignored) { }
			}
		}
		throw new IllegalArgumentException("document title was not found");
	}
}
