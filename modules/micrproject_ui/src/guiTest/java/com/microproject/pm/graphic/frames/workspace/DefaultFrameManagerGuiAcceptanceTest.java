/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames.workspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** GUI-NC-11: switch between two open project frames with real keyboard/mouse input. */
class DefaultFrameManagerGuiAcceptanceTest {
	private JFrame window;

	@AfterEach
	void closeWindow() throws Exception {
		if (window != null)
			SwingUtilities.invokeAndWait(() -> window.dispose());
	}

	@Test
	void robotSwitchesBetweenTwoOpenProjectsWithoutMixingFrames() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DefaultFrameManager[] manager = new DefaultFrameManager[1];
		TestNamedFrame first = new TestNamedFrame("first", "Project Alpha");
		TestNamedFrame second = new TestNamedFrame("second", "Project Beta");
		JComboBox<?>[] selector = new JComboBox<?>[1];
		JPanel[] container = new JPanel[1];
		SwingUtilities.invokeAndWait(() -> {
			container[0] = new JPanel(new BorderLayout());
			manager[0] = new DefaultFrameManager(container[0], new JPanel(), new GraphicManager(container[0]));
			manager[0].addFrame(first);
			manager[0].addFrame(second);
			selector[0] = (JComboBox<?>) manager[0].getProjectComboPanel().getComponent(0);
			window = new JFrame("Multiple project GUI acceptance");
			window.setAlwaysOnTop(true);
			window.add(manager[0].getProjectComboPanel(), BorderLayout.NORTH);
			window.add(container[0], BorderLayout.CENTER);
			window.setPreferredSize(new Dimension(800, 420));
			window.pack();
			window.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> window.isShowing() && selector[0].isShowing(), "project selector was not visible");
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		click(robot, selector[0]);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> manager[0].getSelectedFrame() == first, "Robot selection did not activate the first project");
		SwingUtilities.invokeAndWait(() -> {
			assertSame(first, container[0].getComponent(0));
			assertTrue(first.isActive() && first.isVisible());
			assertFalse(second.isActive() || second.isVisible());
		});
	}

	private static void click(Robot robot, JComboBox<?> combo) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = combo.getLocationOnScreen();
			center[0] = new Point(location.x + combo.getWidth() / 2, location.y + combo.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
	}

	private static final class TestNamedFrame extends NamedFrame {
		private TestNamedFrame(String id, String title) {
			super(id, new ImageIcon());
			setTabTitle(title);
		}
	}
}
