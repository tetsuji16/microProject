/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.assignment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-MSP-POOL-RECOVERY-01: unavailable pools visibly disable assignment edits. */
class UnresolvedSharedResourcePoolGuiAcceptanceTest {
	private TestFrame frame;

	@AfterEach
	void closeFrame() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
	}

	@Test
	void unresolvedPoolDisablesAssignmentButtonsAndExplainsRecovery() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = project();
		project.setSharedResourcePoolFile("C:/plans/missing-shared-resource-pool.mpo");
		SwingUtilities.invokeAndWait(() -> {
			frame = new TestFrame(project);
			AssignmentDialog dialog = new AssignmentDialog(frame.manager.documentFrame);
			JComponent content = dialog.createContentPanel();
			frame.add(content);
			frame.setSize(1_050, 460);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> frame != null && frame.isShowing(), "assignment view did not open");
		for (String name : new String[] { "assignResources", "removeResources", "replaceResources" }) {
			AbstractButton button = findButton(frame, name);
			assertTrue(button != null, "missing assignment button: " + name);
			assertFalse(button.isEnabled(), "unresolved pool must disable " + name);
			assertTrue(button.getToolTipText().contains("resource-pool"), "button must explain pool recovery");
		}
		capture(new Robot());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("Unresolved pool client", undo), undo);
		project.initialize(false, false);
		project.setName("Unresolved pool client");
		return project;
	}

	private static AbstractButton findButton(Component component, String name) {
		if (component instanceof AbstractButton button && name.equals(button.getName())) return button;
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				AbstractButton found = findButton(child, name);
				if (found != null) return found;
			}
		return null;
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getLocationOnScreen(), frame.getSize()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				"unresolved-shared-resource-pool-assignment-disabled.png");
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private static final class TestFrame extends JFrame implements FrameHolder {
		private static final long serialVersionUID = 1L;
		private final TestGraphicManager manager;
		TestFrame(Project project) {
			super("Unresolved shared resource pool GUI acceptance");
			manager = new TestGraphicManager(this, project);
			manager.getMenuManager();
		}
		@Override public FrameManager getFrameManager() { return null; }
		@Override public GraphicManager getGraphicManager() { return manager; }
		@Override public void setGraphicManager(GraphicManager value) { }
	}

	private static final class TestGraphicManager extends GraphicManager {
		private final DocumentFrame documentFrame;
		TestGraphicManager(TestFrame frame, Project project) {
			super(frame);
			documentFrame = new DocumentFrame(this, project, "unresolved-pool") {
				private static final long serialVersionUID = 1L;
				@Override public void activateGanttView() { }
				@Override public void activateResourceView() { }
			};
		}
		@Override public DocumentFrame getCurrentFrame() { return documentFrame; }
	}
}
