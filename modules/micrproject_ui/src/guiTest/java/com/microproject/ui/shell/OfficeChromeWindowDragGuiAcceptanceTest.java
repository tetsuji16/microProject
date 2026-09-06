/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.FlatLafSupport;

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
		FlatLafSupport.initialize();
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

	@Test
	void physicalHoverUsesOfficeWindowButtonStates() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for this acceptance test.");
		FlatLafSupport.initialize();
		final OfficeChromePanel[] panel = new OfficeChromePanel[1];
		final JButton[] minimize = new JButton[1];
		final JButton[] maximize = new JButton[1];
		final JButton[] close = new JButton[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Office chrome hover acceptance", "", "");
			panel[0] = new OfficeChromePanel(frame, MenuManager.getInstance(MenuActionMapSupport.noopActionMap()),
				new JPanel(), () -> { }, AutoSaveControl.DISABLED);
			frame.setRibbonPanel(panel[0]);
			minimize[0] = findButton(panel[0], OfficeChromePanel.MINIMIZE_BUTTON_NAME);
			maximize[0] = findButton(panel[0], OfficeChromePanel.MAXIMIZE_BUTTON_NAME);
			close[0] = findButton(panel[0], OfficeChromePanel.CLOSE_BUTTON_NAME);
			frame.setSize(900, 180);
			frame.setLocation(120, 120);
			frame.setVisible(true);
			frame.toFront();
		});
		GuiAcceptanceSupport.await(() -> minimize[0].isShowing(), "window buttons were not visible");

		Robot robot = new Robot();
		robot.setAutoDelay(25);
		robot.mouseMove(20, 20);
		BufferedImage before = captureCorner(minimize[0], robot);
		moveOver(robot, minimize[0]);
		GuiAcceptanceSupport.await(() -> minimize[0].getModel().isRollover(), "minimize button did not receive physical hover");
		BufferedImage minHover = captureCorner(minimize[0], robot);
		moveOver(robot, maximize[0]);
		GuiAcceptanceSupport.await(() -> maximize[0].getModel().isRollover(), "maximize button did not receive physical hover");
		BufferedImage maxHover = captureCorner(maximize[0], robot);
		moveOver(robot, close[0]);
		GuiAcceptanceSupport.await(() -> close[0].getModel().isRollover(), "close button did not receive physical hover");
		BufferedImage closeHover = captureCorner(close[0], robot);

		Color officeHover = UIManager.getColor("TitlePane.buttonHoverBackground");
		assertEquals(officeHover.getRGB(), minHover.getRGB(1, 1));
		assertEquals(officeHover.getRGB(), maxHover.getRGB(1, 1));
		assertNotEquals(before.getRGB(1, 1), minHover.getRGB(1, 1));
		assertNotEquals(minHover.getRGB(1, 1), closeHover.getRGB(1, 1));
	}

	private static void moveOver(Robot robot, JButton button) {
		Point location = button.getLocationOnScreen();
		robot.mouseMove(location.x + button.getWidth() / 2, location.y + button.getHeight() / 2);
	}

	private static BufferedImage captureCorner(JButton button, Robot robot) {
		Point location = button.getLocationOnScreen();
		return robot.createScreenCapture(new java.awt.Rectangle(location.x, location.y, button.getWidth(), button.getHeight()));
	}

	private static JButton findButton(java.awt.Container root, String name) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof JButton button && name.equals(button.getName())) return button;
			if (child instanceof java.awt.Container container) {
				try { return findButton(container, name); } catch (IllegalArgumentException ignored) { }
			}
		}
		throw new IllegalArgumentException("button was not found: " + name);
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
