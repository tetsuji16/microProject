/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;
import com.microproject.util.FlatLafSupport;

/** Verifies that Windows caption movement is supplied by FlatLaf/Windows. */
class WindowShellNativeDecorationGuiAcceptanceTest {
	private MainRibbonFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
	}

	@Test
	void physicalCaptionDragUsesNativeWindowShell() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for this acceptance test.");
		Assumptions.assumeTrue(Environment.isWindows(), "FlatLaf native window shell is Windows-specific.");
		FlatLafSupport.initialize();
		final JLabel[] title = new JLabel[1];
		final JComponent[] brand = new JComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Native window shell acceptance", "", "");
			OfficeChromePanel panel = new OfficeChromePanel(frame,
				MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> { },
				AutoSaveControl.DISABLED);
			frame.setRibbonPanel(panel);
			title[0] = findTitle(panel);
			brand[0] = findComponent(panel, OfficeChromePanel.BRAND_ICON_NAME);
			frame.setSize(900, 240);
			frame.setLocation(120, 120);
			frame.setVisible(true);
			frame.toFront();
		});
		GuiAcceptanceSupport.await(() -> title[0].isShowing(), "document title was not visible");
		assertFalse(frame.isUndecorated(), "FlatLaf must own the native-capable decoration layer");
		assertEquals(Boolean.TRUE, frame.getRootPane().getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
		assertEquals(18, brand[0].getPreferredSize().width);
		assertTrue(brand[0] instanceof JLabel label && label.getIcon() != null,
			"Windows full-content header must show the application icon");

		Point start = title[0].getLocationOnScreen();
		Point before = frame.getLocation();
		Robot robot = new Robot();
		robot.setAutoDelay(25);
		robot.mouseMove(start.x + Math.max(4, title[0].getWidth() / 2), start.y + title[0].getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(start.x + 60, start.y + 35);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !before.equals(frame.getLocation()), "native caption drag did not move the window");
		assertNotEquals(before, frame.getLocation());
	}

	private static JLabel findTitle(java.awt.Container root) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof JLabel label && OfficeChromePanel.DOCUMENT_TITLE_NAME.equals(label.getName())) return label;
			if (child instanceof java.awt.Container container) {
				try { return findTitle(container); } catch (IllegalArgumentException ignored) { }
			}
		}
		throw new IllegalArgumentException("document title was not found");
	}

	private static JComponent findComponent(java.awt.Container root, String name) {
		for (java.awt.Component child : root.getComponents()) {
			if (child instanceof JComponent component && name.equals(component.getName())) return component;
			if (child instanceof java.awt.Container container) {
				try { return findComponent(container, name); } catch (IllegalArgumentException ignored) { }
			}
		}
		throw new IllegalArgumentException("component was not found: " + name);
	}
}
