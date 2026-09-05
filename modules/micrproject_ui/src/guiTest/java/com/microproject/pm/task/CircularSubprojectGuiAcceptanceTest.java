/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** GUI-MSP-CYCLE-01: a circular child insertion is rejected with its reference chain. */
class CircularSubprojectGuiAcceptanceTest {
	private JFrame frame;
	private boolean previousClientSide;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
		Environment.setClientSide(previousClientSide);
	}

	@Test
	void robotShowsCycleChainAndDoesNotAttachTheCandidate() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Fixture fixture = createFixture();
		show();
		AtomicBoolean completed = new AtomicBoolean();
		AtomicReference<Project> result = new AtomicReference<Project>();
		SwingUtilities.invokeLater(() -> { result.set(fixture.factory.openSubproject(fixture.master, fixture.attemptNode, false)); completed.set(true); });
		GuiAcceptanceSupport.await(() -> findCycleDialog() != null || completed.get(), "circular insertion did not show an error or return");
		assertFalse(completed.get(), "circular insertion returned before warning the user");
		Dialog dialog = findCycleDialog();
		String message = dialogText(dialog);
		assertTrue(message.contains("circular master/subproject reference"), message);
		assertTrue(message.contains("Cycle master -> Cycle candidate -> Cycle master"), message);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		capture(robot, dialog);
		Rectangle buttonBounds = buttonBounds(dialog);
		robot.mouseMove(buttonBounds.x + buttonBounds.width / 2, buttonBounds.y + buttonBounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(completed::get, "OK did not dismiss circular-reference warning");
		assertNull(result.get());
		assertEquals(SubProj.LoadStatus.CYCLE, fixture.attempt.getLoadStatus());
		assertFalse(fixture.master.getTasks().contains(fixture.candidate), "candidate must not be attached after a cycle rejection");
	}

	private Fixture createFixture() {
		Project master = project("Cycle master");
		Project candidate = project("Cycle candidate");
		DefaultSubProj backReference = new DefaultSubProj(candidate, master.getUniqueId());
		backReference.setName(master.getName());
		candidate.connectTask(backReference);
		candidate.addToDefaultOutline(null, NodeFactory.getInstance().createNode(backReference));
		DefaultSubProj attempt = new DefaultSubProj(master, candidate.getUniqueId());
		attempt.setName(candidate.getName());
		Node attemptNode = NodeFactory.getInstance().createNode(attempt);
		master.addToDefaultOutline(null, attemptNode);
		ProjectFactory factory = ProjectFactory.createInstance();
		factory.addProject(candidate, false, true);
		return new Fixture(factory, master, candidate, attempt, attemptNode);
	}

	private void show() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("microProject — Circular subproject GUI acceptance");
			frame.add(new JLabel("Cycle insertion is rejected without changing the master", SwingConstants.CENTER), BorderLayout.CENTER);
			frame.setSize(760, 280);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private static Dialog findCycleDialog() {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof Dialog dialog && dialog.isShowing()) return dialog;
		return null;
	}

	private static String dialogText(java.awt.Container container) {
		StringBuilder text = new StringBuilder();
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof javax.swing.JLabel label) text.append(label.getText());
			if (component instanceof JTextComponent textComponent) text.append(textComponent.getText());
			if (component instanceof java.awt.Container child) text.append(dialogText(child));
		}
		return text.toString();
	}

	private void capture(Robot robot, Dialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getBounds()).union(dialog.getBounds()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"), "circular-subproject-rejection.png");
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static Rectangle buttonBounds(Dialog dialog) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			JButton button = findButton(dialog);
			if (button == null) throw new AssertionError("cycle warning has no dismissal button");
			java.awt.Point point = button.getLocationOnScreen();
			bounds[0] = new Rectangle(point.x, point.y, button.getWidth(), button.getHeight());
		});
		return bounds[0];
	}

	private static JButton findButton(java.awt.Container container) {
		for (java.awt.Component component : container.getComponents()) {
			if (component instanceof JButton button) return button;
			if (component instanceof java.awt.Container child) {
				JButton found = findButton(child);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static Project project(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		return project;
	}

	private record Fixture(ProjectFactory factory, Project master, Project candidate, DefaultSubProj attempt, Node attemptNode) { }
}
