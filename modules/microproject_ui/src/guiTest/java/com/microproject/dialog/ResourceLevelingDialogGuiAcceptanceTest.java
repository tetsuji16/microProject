/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.Assignment;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.BookingType;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** GUI acceptance coverage for the same-resource conflict preview workflow. */
class ResourceLevelingDialogGuiAcceptanceTest {
	@AfterEach
	void closeDialogs() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof ResourceLevelingDialogBox) window.dispose();
		});
	}

	@Test
	void assignmentLevelingIsThePhysicalDefaultAndMovesOnlyTheAssignment() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		for (Object value : project.getResourcePool().getResourceList())
			((ResourceImpl) value).setBookingTypeKind(com.microproject.pm.resource.BookingType.Kind.COMMITTED);
		NormalTask target = (NormalTask) project.getTaskList().get(1);
		Assignment assignment = (Assignment) target.getAssignments().iterator().next();
		ResourceLevelingDialogBox dialog = openDialog(project);
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton levelAssignments = findButton(dialog, UsabilityStrings.text("leveling.assignments"));
		AbstractButton apply = findButton(dialog, UsabilityStrings.text("leveling.apply"));
		assertTrue(levelAssignments != null && levelAssignments.isShowing(), "Level Assignments option must be visible");
		assertTrue(isSelected(levelAssignments), "global Level Assignments defaults on");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"the default assignment-level preview must show the contested assignment");
		click(robot, apply);
		GuiAcceptanceSupport.await(() -> com.microproject.datatype.Duration.millis(assignment.getLevelingDelay()) > 0L,
			"physical Apply must move the assignment under the default setting");
		assertEquals(0L, target.getLevelingDelay(), "assignment-level leveling leaves the task-wide delay untouched");
		assertTrue(com.microproject.datatype.Duration.millis(assignment.getLevelingDelay()) > 0L);
		capture(robot, dialog);
	}

	@Test
	void physicalAssignmentSplitPreservesTaskAndPersistsItsContour() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-leveling-assignment-split-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Shared resource");
		resource.setBookingTypeKind(com.microproject.pm.resource.BookingType.Kind.COMMITTED);
		NormalTask targetTask = task(project, "Split target", 5L);
		NormalTask blockerTask = task(project, "Interior blocker", 1L);
		blockerTask.setPriority(900);
		blockerTask.setStart(project.getEffectiveWorkCalendar().add(project.getStart(),
			CalendarOption.getInstance().getMillisPerDay(), false));
		Assignment target = AssignmentService.getInstance().newAssignment(targetTask, resource, 1.0, 0L, this);
		AssignmentService.getInstance().newAssignment(blockerTask, resource, 1.0, 0L, this);
		project.recalculate();
		long taskStart = targetTask.getStart();
		long taskEndBefore = targetTask.getEnd();
		String contourBefore = target.getWorkContour().toString(target.getDurationMillis());
		ResourceLevelingDialogBox dialog = openDialog(project);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, findButton(dialog, UsabilityStrings.text("leveling.preview")));
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"physical preview must identify the assignment that can split around the blocker");
		click(robot, findButton(dialog, UsabilityStrings.text("leveling.apply")));
		GuiAcceptanceSupport.await(() -> target.getLevelingSplitState() != null,
			"physical Apply must generate an assignment-owned split around the interior blocker");
		assertEquals(taskStart, targetTask.getStart(), "assignment splitting must preserve task start");
		assertTrue(targetTask.getEnd() >= target.getEnd(), "task finish must cover the split assignment's new finish");
		assertTrue(target.getEnd() > taskEndBefore, "the assignment contour must extend beyond the original finish");
		assertTrue(!contourBefore.equals(target.getWorkContour().toString(target.getDurationMillis())),
			"the physical route must change the assignment work contour");
		java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
		com.microproject.exchange.MpoFileImporter importer = new com.microproject.exchange.MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected assignment split state");
		Project reloaded = importer.loadProject(new java.io.ByteArrayInputStream(saved.toByteArray()));
		Assignment reloadedTarget = (Assignment) taskNamed(reloaded, "Split target").getAssignments().iterator().next();
		assertTrue(reloadedTarget.getLevelingSplitState() != null, "MPO reload lost generated assignment split ownership");
		assertEquals(target.getWorkContour().toString(target.getDurationMillis()),
			reloadedTarget.getWorkContour().toString(reloadedTarget.getDurationMillis()),
			"MPO reload changed the physical route's assignment contour");
		capture(robot, dialog);
	}

	@Test
	void proposedResourcesStayOutOfPreviewUntilRobotEnablesThem() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		ResourceLevelingDialogBox dialog = openDialog(project);
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton includeProposed = findButton(dialog, UsabilityStrings.text("leveling.includeProposed"));
		JComboBox<?> order = findComboBox(dialog, 0);
		JComboBox<?> sensitivity = findComboBox(dialog, 1);
		assertTrue(preview != null && preview.isShowing(), "Preview button must be visible");
		assertTrue(includeProposed != null && includeProposed.isShowing(), "Proposed-resource option must be visible");
		assertTrue(sensitivity != null && sensitivity.isShowing(), "sensitivity period must be visible");
		assertTrue(order != null && isBelow(sensitivity, order),
			"sensitivity must occupy a separate form row below leveling order");
		assertTrue(selectedIndex(sensitivity, 2), "day must be the document-derived product default");
		assertTrue(!isSelected(includeProposed), "Proposed resources must be excluded by default");
		assertTrue(tableRowCount(dialog) == 0, "default preview must leave proposed-resource conflicts untouched");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, includeProposed);
		click(robot, preview);

		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = preview.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, preview.getWidth(), preview.getHeight());
		});
		GuiAcceptanceSupport.await(() -> findTable(dialog) != null && findTable(dialog).getRowCount() > 0,
			"same-resource conflict preview must show at least one proposed change");
		assertTrue(isSelected(includeProposed), "physical opt-in must stay selected while showing the plan");
		for (int index : new int[] {0, 1}) {
			GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, index);
			click(robot, preview);
			GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
				"minute and hour sensitivity must show the same full-day conflict");
		}
		GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, 3);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) == 0,
			"weekly sensitivity must accept two one-day assignments within weekly capacity");
		GuiAcceptanceSupport.selectComboBoxItem(robot, sensitivity, 2);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"daily sensitivity must detect the full-day conflict");
		capture(robot, dialog);
	}

	@Test
	void manualTasksStayFixedUntilRobotEnablesTheirLeveling() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		for (Object value : project.getTaskList()) ((NormalTask) value).setManuallyScheduled(true);
		for (Object value : project.getResourcePool().getResourceList())
			((ResourceImpl) value).setBookingTypeKind(BookingType.Kind.COMMITTED);
		ResourceLevelingDialogBox dialog = openDialog(project);
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton includeManual = findButton(dialog, UsabilityStrings.text("leveling.includeManual"));
		AbstractButton apply = findButton(dialog, UsabilityStrings.text("leveling.apply"));
		assertTrue(preview != null && preview.isShowing(), "Preview button must be visible");
		assertTrue(includeManual != null && includeManual.isShowing(), "Manual-task option must be visible");
		assertTrue(apply != null && apply.isShowing(), "Apply button must be visible");
		assertTrue(!isSelected(includeManual), "manual tasks must remain fixed by default");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, preview);
		assertTrue(tableRowCount(dialog) == 0, "default preview must leave manual assignments fixed");
		click(robot, includeManual);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"explicitly included manual assignments must appear in the preview");
		assertTrue(isSelected(includeManual), "physical opt-in must remain selected while the plan is shown");
		java.util.Map<NormalTask, Long> originalStarts = taskStarts(project);
		click(robot, apply);
		GuiAcceptanceSupport.await(() -> anyTaskStartDiffers(project, originalStarts),
			"physical Apply must change at least one manual task's visible start");
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
		GuiAcceptanceSupport.await(() -> taskStarts(project).equals(originalStarts),
			"Undo restores the manual task dates changed by the physical route");
		SwingUtilities.invokeAndWait(() -> project.getUndoController().redo());
		GuiAcceptanceSupport.await(() -> anyTaskStartDiffers(project, originalStarts),
			"Redo restores the manual leveling result");
		capture(robot, dialog);
	}

	private static java.util.Map<NormalTask, Long> taskStarts(Project project) {
		java.util.Map<NormalTask, Long> result = new java.util.HashMap<>();
		runOnEdt(() -> {
			for (Object value : project.getTaskList()) {
				NormalTask task = (NormalTask) value;
				result.put(task, Long.valueOf(visibleLevelingPosition(task)));
			}
		});
		return result;
	}

	/**
	 * Start plus assignment leveling delays: a manually scheduled task keeps its user-owned start while leveling
	 * stores the delay on its assignments (Level Assignments), so the delay is the observable leveling result.
	 */
	private static long visibleLevelingPosition(NormalTask task) {
		long total = task.getStart();
		for (Object value : task.getAssignments())
			total += com.microproject.datatype.Duration.millis(((Assignment) value).getLevelingDelay());
		return total;
	}

	private static boolean anyTaskStartDiffers(Project project, java.util.Map<NormalTask, Long> originalStarts) {
		boolean[] changed = new boolean[1];
		runOnEdt(() -> {
			for (var entry : originalStarts.entrySet())
				if (visibleLevelingPosition(entry.getKey()) != entry.getValue().longValue()) changed[0] = true;
		});
		return changed[0];
	}

	private static void runOnEdt(Runnable operation) {
		try {
			SwingUtilities.invokeAndWait(operation);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while reading project state on the EDT", exception);
		} catch (java.lang.reflect.InvocationTargetException exception) {
			throw new IllegalStateException("Project state inspection failed on the EDT", exception.getCause());
		}
	}

	private static ResourceLevelingDialogBox openDialog(Project project) throws Exception {
		ResourceLevelingDialogBox[] dialog = new ResourceLevelingDialogBox[1];
		SwingUtilities.invokeAndWait(() -> dialog[0] = ResourceLevelingDialogBox.getInstance(null, project));
		AtomicReference<Throwable> showFailure = new AtomicReference<>();
		SwingUtilities.invokeLater(() -> {
			try { dialog[0].setVisible(true); }
			catch (Throwable failure) { showFailure.set(failure); }
		});
		GuiAcceptanceSupport.await(() -> {
			Throwable failure = showFailure.get();
			if (failure != null) throw new IllegalStateException("Resource leveling dialog failed while showing", failure);
			boolean[] visible = new boolean[1];
			try { SwingUtilities.invokeAndWait(() -> visible[0] = dialog[0].isVisible()); }
			catch (Exception invocationFailure) { throw new IllegalStateException("Could not inspect dialog visibility", invocationFailure); }
			return visible[0];
		}, "Resource leveling dialog did not become visible");
		return dialog[0];
	}

	private static void click(Robot robot, Component component) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = component.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, component.getWidth(), component.getHeight());
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static boolean isSelected(AbstractButton button) throws Exception {
		boolean[] selected = new boolean[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = button.isSelected());
		return selected[0];
	}

	private static boolean selectedIndex(JComboBox<?> combo, int index) throws Exception {
		boolean[] selected = new boolean[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = combo.getSelectedIndex() == index);
		return selected[0];
	}

	private static boolean isBelow(Component component, Component other) throws Exception {
		boolean[] below = new boolean[1];
		SwingUtilities.invokeAndWait(() -> below[0] = component.getY() >= other.getY() + other.getHeight());
		return below[0];
	}

	private static int tableRowCount(ResourceLevelingDialogBox dialog) throws Exception {
		int[] rowCount = new int[1];
		SwingUtilities.invokeAndWait(() -> rowCount[0] = findTable(dialog).getRowCount());
		return rowCount[0];
	}

	private static int tableRowCountUnchecked(ResourceLevelingDialogBox dialog) {
		try { return tableRowCount(dialog); }
		catch (Exception exception) { throw new IllegalStateException(exception); }
	}

	private static JComboBox<?> findComboBox(java.awt.Container container, int index) {
		int[] visited = {0};
		return findComboBox(container, index, visited);
	}

	private static JComboBox<?> findComboBox(java.awt.Container container, int index, int[] visited) {
		for (Component child : container.getComponents()) {
			if (child instanceof JComboBox<?> combo && visited[0]++ == index) return combo;
			if (child instanceof java.awt.Container nested) {
				JComboBox<?> combo = findComboBox(nested, index, visited);
				if (combo != null) return combo;
			}
		}
		return null;
	}

	private static AbstractButton findButton(java.awt.Container container, String text) {
		for (Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static JTable findTable(java.awt.Container container) {
		for (Component child : container.getComponents()) {
			if (child instanceof JTable table) return table;
			if (child instanceof java.awt.Container nested) {
				JTable table = findTable(nested);
				if (table != null) return table;
			}
		}
		return null;
	}

	private static Project newConflictProject() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-leveling-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Resource leveling GUI acceptance");
		project.initialize(false, false);
		NormalTask first = new NormalTask(project);
		first.setName("Conflict A");
		first.setManuallyScheduled(false);
		project.connectTask(first);
		project.getSchedulingAlgorithm().addObject(first);
		first.getCurrentSchedule().setStart(project.getStart());
		first.setDuration(CalendarOption.getInstance().getMillisPerDay());
		NormalTask second = new NormalTask(project);
		second.setName("Conflict B");
		second.setManuallyScheduled(false);
		project.connectTask(second);
		project.getSchedulingAlgorithm().addObject(second);
		second.getCurrentSchedule().setStart(project.getStart());
		second.setDuration(CalendarOption.getInstance().getMillisPerDay());
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Shared resource");
		resource.setBookingTypeKind(com.microproject.pm.resource.BookingType.Kind.PROPOSED);
		AssignmentService.getInstance().newAssignment(first, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		AssignmentService.getInstance().newAssignment(second, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		project.recalculate();
		return project;
	}

	private static NormalTask task(Project project, String name, long days) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private static NormalTask taskNamed(Project project, String name) {
		return (NormalTask) project.getTaskList().stream().filter(task -> name.equals(((NormalTask) task).getName()))
			.findFirst().orElseThrow();
	}

	private static void capture(Robot robot, ResourceLevelingDialogBox dialog) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			dialog.toFront();
			dialog.requestFocus();
			java.awt.Point location = dialog.getRootPane().getLocationOnScreen();
			bounds.setBounds(location.x, location.y, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
		});
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		javax.imageio.ImageIO.write(robot.createScreenCapture(bounds), "png",
			directory.resolve("resource-leveling-conflict-preview.png").toFile());
	}
}
