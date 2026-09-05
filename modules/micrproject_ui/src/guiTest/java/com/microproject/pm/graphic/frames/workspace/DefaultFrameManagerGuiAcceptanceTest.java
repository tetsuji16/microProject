/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames.workspace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-NC-11: switch between two open project frames with real keyboard/mouse input. */
class DefaultFrameManagerGuiAcceptanceTest {
	private JFrame window;
	private DefaultFrameManager desktopWindowManager;

	@AfterEach
	void closeWindow() throws Exception {
		if (desktopWindowManager != null)
			SwingUtilities.invokeAndWait(() -> desktopWindowManager.cleanUp());
		if (window != null)
			SwingUtilities.invokeAndWait(() -> window.dispose());
	}

	@Test
	void mainApplicationUsesIndependentDesktopWindowsAndPromotesTheRemainingProjectOnClose() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DocumentFrame[] frames = new DocumentFrame[2];
		DesktopWindowGraphicManager[] graphicManagers = new DesktopWindowGraphicManager[1];
		SwingUtilities.invokeAndWait(() -> {
			MainRibbonFrame mainWindow = new MainRibbonFrame("Multiple-project desktop acceptance", null, null);
			window = mainWindow;
			DesktopWindowGraphicManager graphicManager = new DesktopWindowGraphicManager(mainWindow);
			graphicManagers[0] = graphicManager;
			mainWindow.setGraphicManager(graphicManager);
			desktopWindowManager = new DefaultFrameManager(mainWindow, new JPanel(), graphicManager);
			graphicManager.setFrameManager(desktopWindowManager);
			frames[0] = createDocumentFrame(graphicManager, "Desktop Alpha");
			frames[1] = createDocumentFrame(graphicManager, "Desktop Beta");
			desktopWindowManager.addFrame(frames[0]);
			graphicManager.activateDocumentWindow(frames[0]);
			mainWindow.setSize(800, 500);
			mainWindow.setVisible(true);
			desktopWindowManager.addFrame(frames[1]);
		});
		GuiAcceptanceSupport.await(() -> desktopWindowManager.getIndependentWindowCount() == 1
				&& desktopWindowManager.getIndependentWindow(frames[1]).isShowing(), "second project did not receive its own desktop window");
		JFrame secondary = desktopWindowManager.getIndependentWindow(frames[1]);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		Point location = secondary.getLocationOnScreen();
		robot.mouseMove(location.x + secondary.getWidth() / 2, location.y + secondary.getHeight() / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> graphicManagers[0].getCurrentFrame() == frames[1], "desktop focus did not activate the second project");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(frames[0].isShowing());
			assertTrue(frames[1].isShowing());
			assertTrue(frames[1].isActive());
			assertFalse(frames[0].isActive());
			assertTrue(window.getTitle().contains("Desktop Alpha"),
					"The primary window title must continue to identify the document it contains");
			assertTrue(secondary.getTitle().contains("Desktop Beta"));
			assertFalse(secondary.getTitle().contains("desktop-beta.projectlibre"),
					"window titles must not expose the absolute project path");
			desktopWindowManager.activateFrame(frames[0]);
			assertTrue(frames[1].isShowing(), "switching back must not blank or hide the secondary window");
			desktopWindowManager.arrangeAll(FrameManager.WindowArrangement.TILE);
			assertEquals(FrameManager.WindowArrangement.TILE, desktopWindowManager.getCurrentArrangement());
		});
		captureDesktopWindows(robot, window, secondary, "msp-independent-project-windows-tiled.png");
		SwingUtilities.invokeAndWait(() -> {
			secondary.toFront();
			secondary.requestFocus();
		});
		Rectangle secondaryBounds = secondary.getBounds();
		robot.mouseMove(secondaryBounds.x + secondaryBounds.width - 22, secondaryBounds.y + 15);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> desktopWindowManager.getIndependentWindowCount() == 0,
			"physical secondary window title-bar close did not remove the document window");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(0, desktopWindowManager.getIndependentWindowCount());
			assertSame(frames[0], desktopWindowManager.getSelectedFrame());
			assertFalse(secondary.isShowing(), "the closed secondary window must no longer be visible");
		});
	}

	private static DocumentFrame createDocumentFrame(GraphicManager graphicManager, String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name + " pool", undo), undo);
		project.setName(name);
		project.setFileName("C:/fixtures/" + name.toLowerCase().replace(' ', '-') + ".projectlibre");
		TestDocumentFrame frame = new TestDocumentFrame(graphicManager, project, name.toLowerCase().replace(' ', '-'));
		project.setDirty(false);
		project.setGroupDirty(false);
		return frame;
	}

	private static final class DesktopWindowGraphicManager extends GraphicManager {
		private DocumentFrame currentFrame;

		DesktopWindowGraphicManager(JFrame frame) {
			super(frame);
		}

		@Override public void activateDocumentWindow(DocumentFrame frame) {
			currentFrame = frame;
			getFrameManager().activateFrame(frame);
		}

		@Override public DocumentFrame getCurrentFrame() {
			return currentFrame;
		}
	}

	private static final class TestDocumentFrame extends DocumentFrame {
		private static final long serialVersionUID = 1L;

		TestDocumentFrame(GraphicManager manager, Project project, String id) {
			super(manager, project, id);
			removeAll();
			setLayout(new BorderLayout());
			add(new JLabel(project.getName() + " independent project window", SwingConstants.CENTER), BorderLayout.CENTER);
		}

		@Override public void activateGanttView() {
			// The window-management test deliberately does not construct a full ribbon toolbar.
		}
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

		click(robot, selector[0]);
		robot.keyPress(KeyEvent.VK_END);
		robot.keyRelease(KeyEvent.VK_END);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> manager[0].getSelectedFrame() == second, "Robot selection did not activate the second project");
		SwingUtilities.invokeAndWait(() -> {
			assertSame(second, container[0].getComponent(0));
			assertTrue(second.isActive() && second.isVisible());
			assertFalse(first.isActive() || first.isVisible());
		});

		SwingUtilities.invokeAndWait(() -> manager[0].arrangeAll(FrameManager.WindowArrangement.HORIZONTAL));
		GuiAcceptanceSupport.await(() -> manager[0].getCurrentArrangement() == FrameManager.WindowArrangement.HORIZONTAL,
				"horizontal arrangement was not applied");
		capture(robot, "msp-window-switch-horizontal.png");

		SwingUtilities.invokeAndWait(() -> manager[0].arrangeAll(FrameManager.WindowArrangement.VERTICAL));
		GuiAcceptanceSupport.await(() -> manager[0].getCurrentArrangement() == FrameManager.WindowArrangement.VERTICAL,
				"vertical arrangement was not applied");
		capture(robot, "msp-window-switch-vertical.png");

		SwingUtilities.invokeAndWait(() -> manager[0].arrangeAll(FrameManager.WindowArrangement.CASCADE));
		GuiAcceptanceSupport.await(() -> manager[0].getCurrentArrangement() == FrameManager.WindowArrangement.CASCADE,
				"cascade arrangement was not applied");
		capture(robot, "msp-window-switch-cascade.png");

		// MSP's maximized active-project view returns from every multi-window
		// arrangement without closing or losing either project frame.
		SwingUtilities.invokeAndWait(() -> manager[0].arrangeAll(FrameManager.WindowArrangement.SINGLE));
		GuiAcceptanceSupport.await(() -> manager[0].getCurrentArrangement() == FrameManager.WindowArrangement.SINGLE,
				"active-project-only arrangement was not applied");
		SwingUtilities.invokeAndWait(() -> {
			assertSame(second, container[0].getComponent(0));
			assertTrue(second.isActive() && second.isVisible());
			assertFalse(first.isActive() || first.isVisible());
		});
		capture(robot, "msp-window-switch-active-project-only.png");
	}

	private void capture(Robot robot, String fileName) throws Exception {
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), fileName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(robot.createScreenCapture(window.getBounds()), "png", artifact.toFile());
	}

	private static void captureDesktopWindows(Robot robot, JFrame primary, JFrame secondary, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[2];
		SwingUtilities.invokeAndWait(() -> {
			bounds[0] = primary.getBounds();
			bounds[1] = secondary.getBounds();
		});
		Rectangle captureArea = bounds[0].union(bounds[1]);
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), fileName);
		Files.createDirectories(artifact.getParent());
		ImageIO.write(robot.createScreenCapture(captureArea), "png", artifact.toFile());
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
			setLayout(new BorderLayout());
			setBackground("first".equals(id) ? new Color(230, 242, 255) : new Color(236, 250, 232));
			setOpaque(true);
			setBorder(BorderFactory.createTitledBorder(title + " — GUI fixture"));
			add(new JLabel(title + " project window", SwingConstants.CENTER), BorderLayout.CENTER);
		}
	}
}
