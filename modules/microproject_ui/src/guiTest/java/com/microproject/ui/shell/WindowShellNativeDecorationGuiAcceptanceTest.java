/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatLaf;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

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
		final JLabel[] title = new JLabel[1];
		final JComponent[] brand = new JComponent[1];
		final JComponent[] content = new JComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Native window shell acceptance", "");
			OfficeChromePanel panel = new OfficeChromePanel(frame,
				MenuManager.getInstance(MenuActionMapSupport.noopActionMap()), new JPanel(), () -> { },
				AutoSaveControl.DISABLED);
			content[0] = panel;
			frame.setRibbonPanel(panel);
			title[0] = findTitle(panel);
			brand[0] = findComponent(panel, OfficeChromePanel.BRAND_ICON_NAME);
			frame.setSize(900, 240);
			frame.setLocation(120, 120);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
		});
		GuiAcceptanceSupport.await(() -> title[0].isShowing() && frame.isActive(),
			"document title was not visible in the active window");
		assertEquals("com.formdev.flatlaf.FlatLightLaf", UIManager.getLookAndFeel().getClass().getName(),
			"a standalone ribbon frame must install FlatLaf before it becomes displayable");
		assertEquals("com.formdev.flatlaf.ui.FlatRootPaneUI", frame.getRootPane().getUI().getClass().getName(),
			"the root pane created by JFrame must be refreshed to FlatLaf");
		assertFalse(frame.isUndecorated(), "FlatLaf must own the native-capable decoration layer");
		assertTrue(FlatLaf.isUseNativeWindowDecorations(),
			"Windows must enable FlatLaf native decorations before creating the shell frame");
		assertEquals(Boolean.TRUE, frame.getRootPane().getClientProperty(WindowShellInstaller.USE_WINDOW_DECORATIONS));
		assertEquals(FlatUiSupport.ribbonChromeHeight(),
			frame.getRootPane().getClientProperty(WindowShellInstaller.TITLE_BAR_HEIGHT),
			"FlatLaf's native caption hit-test band must cover the Office chrome row");
		assertEquals(18, brand[0].getPreferredSize().width);
		assertTrue(brand[0] instanceof JLabel label && label.getIcon() != null,
			"Windows full-content header must show the application icon");
		assertTrue(frame.getIconImage() != null,
			"full-window-content must retain the application icon for native window surfaces");
		assertEquals(Boolean.TRUE, brand[0].getClientProperty("JComponent.titleBarCaption"),
			"the non-interactive brand icon must remain part of the draggable caption");
		assertEquals(Boolean.FALSE, findComponent(content[0], OfficeChromePanel.SEARCH_BOX_NAME)
			.getClientProperty("JComponent.titleBarCaption"),
			"the interactive search box must not be treated as a caption hit target");

		// In FlatLaf full-window-content mode the draggable caption is the
		// document-title component marked as titleBarCaption, not an arbitrary
		// point over the interactive search box.
		Point start = title[0].getLocationOnScreen();
		start.translate(Math.max(4, title[0].getWidth() / 2), Math.max(4, title[0].getHeight() / 2));
		Point before = frame.getLocation();
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(25);
		robot.waitForIdle();
		robot.mouseMove(start.x, start.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(start.x + 60, start.y + 35);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> !before.equals(frame.getLocation()), "native caption drag did not move the window: before="
			+ before + ", after=" + frame.getLocation() + ", dragTarget=" + start
			+ ", windowBounds=" + frame.getBounds());
		assertNotEquals(before, frame.getLocation());

		Rectangle normalBounds = frame.getBounds();
		WindowAcceptanceAssertions.hoverNativeTitleButton(robot, frame, "Maximize", 1_200);
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		// Snap Layouts is a native hover popup; Escape alone does not dismiss it reliably.
		// Move into the current window's content and allow the popup to close before testing the button.
		Rectangle movedWindow = frame.getBounds();
		Point popupDismiss = WindowAcceptanceAssertions.robotPointFor(
			new Rectangle(movedWindow.x + movedWindow.width / 2, movedWindow.y + movedWindow.height / 2, 2, 2));
		robot.mouseMove(popupDismiss.x, popupDismiss.y);
		robot.delay(500);
		robot.waitForIdle();
		WindowAcceptanceAssertions.clickNativeTitleButton(robot, frame, "Maximize");
		GuiAcceptanceSupport.await(() -> isMaximized(frame),
			"physical native maximize-button click did not maximize the window");
		WindowAcceptanceAssertions.clickNativeTitleButton(robot, frame, "Restore");
		GuiAcceptanceSupport.await(() -> !isMaximized(frame),
			"physical native restore-button click did not restore the window");
		GuiAcceptanceSupport.await(() -> normalBounds.equals(frame.getBounds()),
			"native caption-button Restore did not recover the pre-maximize bounds: expected=" + normalBounds
				+ ", actual=" + frame.getBounds());
		WindowAcceptanceAssertions.assertWithinUsableWorkArea(frame, "caption-button-restored primary native window");

		assertTrue(frame.isActive(), "the restored native window must remain active for the physical Alt+Space route");
		pressAltSpace(robot);
		pressKey(robot, KeyEvent.VK_X); // Windows system-menu accelerator for Maximize.
		GuiAcceptanceSupport.await(() -> isMaximized(frame),
			"physical Alt+Space system-menu Maximize did not maximize the native window");
		rightClick(robot, title[0]);
		pressKey(robot, KeyEvent.VK_R); // Windows system-menu accelerator for Restore.
		GuiAcceptanceSupport.await(() -> !isMaximized(frame),
			"physical title-area context-menu Restore did not restore the native window");
		GuiAcceptanceSupport.await(() -> normalBounds.equals(frame.getBounds()),
			"native system-menu Restore did not recover the pre-maximize bounds: expected=" + normalBounds
				+ ", actual=" + frame.getBounds());
		WindowAcceptanceAssertions.assertWithinUsableWorkArea(frame, "restored primary native window");

		Rectangle beforeResize = frame.getBounds();
		int edgeX = beforeResize.x + beforeResize.width - 1;
		int edgeY = beforeResize.y + beforeResize.height / 2;
		robot.mouseMove(edgeX, edgeY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(edgeX - 60, edgeY);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> frame.getWidth() < beforeResize.width,
			"physical right-edge drag did not resize the native window: before=" + beforeResize
				+ ", after=" + frame.getBounds());
		WindowAcceptanceAssertions.assertWithinUsableWorkArea(frame, "resized primary native window before close");
	}

	private static boolean isMaximized(MainRibbonFrame target) {
		return (target.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
	}

	private static void pressAltSpace(Robot robot) {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_ALT);
		robot.waitForIdle();
	}

	private static void pressKey(Robot robot, int keyCode) {
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		robot.waitForIdle();
	}

	private static void rightClick(Robot robot, Component component) throws Exception {
		Rectangle bounds = WindowAcceptanceAssertions.boundsOnScreen(component);
		Point point = WindowAcceptanceAssertions.robotPointFor(bounds);
		robot.waitForIdle();
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		robot.waitForIdle();
		robot.delay(500);
		robot.waitForIdle();
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
