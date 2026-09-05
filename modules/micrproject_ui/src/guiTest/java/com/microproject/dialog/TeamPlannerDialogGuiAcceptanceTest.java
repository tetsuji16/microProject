/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.Window;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.SharedResourcePoolService;
import com.microproject.pm.resource.TeamPlannerService;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI acceptance coverage for shared-pool over-allocation visibility. */
class TeamPlannerDialogGuiAcceptanceTest {
	@AfterEach
	void closeDialogs() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof TeamPlannerDialogBox || window instanceof ResourceLevelingDialogBox)
					window.dispose();
		});
	}

	@Test
	void sharedPoolOverloadIsVisibleInTeamPlanner() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = fixture();
		assertTrue(new TeamPlannerService().slots(fixture.owner).stream()
			.anyMatch(TeamPlannerService.Slot::overallocated));

		SwingUtilities.invokeLater(() -> TeamPlannerDialogBox.getInstance(null, fixture.owner).setVisible(true));
		GuiAcceptanceSupport.await(() -> findDialog() != null, "Team Planner dialog did not open");
		TeamPlannerDialogBox dialog = findDialog();
		Robot robot = new Robot();
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.requestFocus();
			java.awt.Point location = dialog.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, dialog.getWidth(), dialog.getHeight());
		});
		robot.waitForIdle();
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(robot.createScreenCapture(bounds), "png", directory.resolve("team-planner-shared-overload.png").toFile());
		TeamPlannerDialogBox.TeamPlannerCanvas canvas = findCanvas(dialog);
		assertTrue(canvas != null, "Team Planner canvas was not installed");
		GuiAcceptanceSupport.await(() -> canvas.getWidth() > 200 && canvas.getHeight() > 80, "Team Planner canvas was not laid out");
		BufferedImage rendered = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Graphics2D graphics = rendered.createGraphics();
			try { canvas.printAll(graphics); } finally { graphics.dispose(); }
		});
		assertTrue(redPixels(rendered) > 50, "The visible Team Planner canvas did not render the shared-resource overload bar");
		ImageIO.write(rendered, "png", directory.resolve("team-planner-shared-overload-render.png").toFile());
		Point sourceSlot = findSlot(canvas, "Project: shared-sharer");
		assertTrue(sourceSlot != null, "The visible shared assignment must identify its source project in the tooltip");
		SwingUtilities.invokeAndWait(() -> {
			Point location = canvas.getLocationOnScreen();
			robot.mouseMove(location.x + sourceSlot.x, location.y + sourceSlot.y);
		});
		Thread.sleep(900L);
		ImageIO.write(robot.createScreenCapture(bounds), "png", directory.resolve("team-planner-shared-overload-source-project.png").toFile());
	}

	private static Point findSlot(TeamPlannerDialogBox.TeamPlannerCanvas canvas, String expected) throws Exception {
		Point[] result = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < canvas.getHeight() && result[0] == null; y += 5)
				for (int x = 0; x < canvas.getWidth(); x += 5) {
					String tooltip = canvas.getToolTipText(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED,
							System.currentTimeMillis(), 0, x, y, 0, false));
					if (tooltip != null && tooltip.contains(expected)) {
						result[0] = new Point(x, y);
						break;
					}
				}
		});
		return result[0];
	}

	private static TeamPlannerDialogBox.TeamPlannerCanvas findCanvas(Component component) {
		if (component instanceof TeamPlannerDialogBox.TeamPlannerCanvas canvas) return canvas;
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				TeamPlannerDialogBox.TeamPlannerCanvas found = findCanvas(child);
				if (found != null) return found;
			}
		return null;
	}

	private static long redPixels(BufferedImage image) {
		long count = 0;
		for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
			int rgb = image.getRGB(x, y);
			if (((rgb >> 16) & 0xff) > 180 && ((rgb >> 8) & 0xff) < 100 && (rgb & 0xff) < 100) count++;
		}
		return count;
	}

	private static TeamPlannerDialogBox findDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof TeamPlannerDialogBox dialog && dialog.isVisible()) return dialog;
		return null;
	}

	private static Fixture fixture() {
		Project owner = newProject("shared-owner");
		Project sharer = newProject("shared-sharer");
		ResourceImpl ownerResource = resource(owner, "Shared engineer");
		ResourceImpl sharerResource = resource(sharer, "Shared engineer");
		ownerResource.setUniqueId(9001L);
		sharerResource.setUniqueId(9001L);
		NormalTask ownerTask = task(owner, "Owner work");
		NormalTask sharerTask = task(sharer, "Sharer work");
		AssignmentService.getInstance().newAssignment(ownerTask, ownerResource, 1D, 0L, TeamPlannerDialogGuiAcceptanceTest.class);
		AssignmentService.getInstance().newAssignment(sharerTask, sharerResource, 1D, 0L, TeamPlannerDialogGuiAcceptanceTest.class);
		SharedResourcePoolService.getInstance().share(sharer, owner,
			SharedResourcePoolService.ConflictPolicy.POOL_TAKES_PRECEDENCE);
		return new Fixture(owner, sharer);
	}

	private static Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		project.setName(name);
		project.setFileName("C:/gui-fixtures/" + name + ".mpo");
		return project;
	}

	private static ResourceImpl resource(Project project, String name) {
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setName(name);
		return resource;
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(name);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private record Fixture(Project owner, Project sharer) { }
}
