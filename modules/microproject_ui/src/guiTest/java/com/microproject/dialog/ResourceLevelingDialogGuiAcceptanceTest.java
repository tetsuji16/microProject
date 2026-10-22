/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;

/** GUI acceptance coverage for the same-resource conflict preview workflow. */
class ResourceLevelingDialogGuiAcceptanceTest {
	private MainRibbonFrame applicationFrame;
	private GraphicManager applicationManager;

	@AfterEach
	void closeDialogs() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof ResourceLevelingDialogBox) window.dispose();
			if (applicationManager != null) applicationManager.cleanUp();
			if (applicationFrame != null) applicationFrame.dispose();
		});
		applicationManager = null;
		applicationFrame = null;
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
		AbstractButton allowSplitting = findButton(dialog, UsabilityStrings.text("leveling.splits"));
		assertTrue(allowSplitting != null && allowSplitting.isShowing(), "Allow task splitting option must be visible");
		assertTrue(isSelected(allowSplitting), "Allow task splitting defaults on before the physical workflow");
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
		long targetTaskId = targetTask.getUniqueId();
		java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
		com.microproject.exchange.MpoFileImporter importer = new com.microproject.exchange.MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected assignment split state");
		Project reloaded = importer.loadProject(new java.io.ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedTask = taskWithUniqueId(reloaded, targetTaskId, targetTask);
		assertEquals(targetTask.getName(), reloadedTask.getName(), "MPO reload changed the target task identity");
		Assignment reloadedTarget = (Assignment) reloadedTask.getAssignments().iterator().next();
		assertTrue(reloadedTarget.getLevelingSplitState() != null, "MPO reload lost generated assignment split ownership");
		assertEquals(target.getWorkContour().toString(target.getDurationMillis()),
			reloadedTarget.getWorkContour().toString(reloadedTarget.getDurationMillis()),
			"MPO reload changed the physical route's assignment contour");
		capture(robot, dialog);
	}

	@Test
	void clearLevelingKeepsUserAssignmentSplitAndSupportsPhysicalUndoRedoAndMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-leveling-clear-user-split-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl shared = pool.newResourceInstance();
		shared.setName("Shared committed resource");
		shared.setBookingTypeKind(BookingType.Kind.COMMITTED);
		NormalTask targetTask = task(project, "User split target", 5L);
		NormalTask blockerTask = task(project, "Higher priority blocker", 1L);
		blockerTask.setPriority(900);
		blockerTask.setStart(project.getEffectiveWorkCalendar().add(project.getStart(),
			CalendarOption.getInstance().getMillisPerDay(), false));
		Assignment target = AssignmentService.getInstance().newAssignment(targetTask, shared, 1D, 0L, this);
		long userGapStart = target.getStart() + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		long userGapEnd = userGapStart + com.microproject.pm.calendar.WorkCalendar.MILLIS_IN_HOUR;
		ResourceImpl other = pool.newResourceInstance();
		other.setName("Unrelated resource");
		Assignment unrelated = AssignmentService.getInstance().newAssignment(targetTask, other, 1D, 0L, this);
		Assignment blocker = AssignmentService.getInstance().newAssignment(blockerTask, shared, 1D, 0L, this);
		project.recalculate();
		target.split(this, userGapStart, userGapEnd);
		long targetTaskStart = targetTask.getStart();
		long blockerTaskStart = blockerTask.getStart();
		long blockerTaskEnd = blockerTask.getEnd();
		long blockerStart = blocker.getStart();
		long blockerEnd = blocker.getEnd();
		long unrelatedStart = unrelated.getStart();
		long userContourDuration = target.getDurationMillis();
		String userContour = target.getWorkContour().toString(userContourDuration);
		assertTrue(userGapStart < userGapEnd, "the pre-existing user split must have a stable non-empty gap");
		long contourWork = 0L;
		for (var bucket : target.getWorkContour().getContourBuckets())
			if (bucket != null) contourWork += (long) (bucket.getUnits() * bucket.getBucketDuration(target.getDurationMillis()));
		assertEquals(target.getWork(null), contourWork, "MPO's contour work total must match assignment work; actual="
			+ target.getActualWork(null) + " remaining=" + target.getRemainingWork() + " contour=" + userContour);
		java.io.ByteArrayOutputStream beforeLevelingArchive = new java.io.ByteArrayOutputStream();
		com.microproject.exchange.MpoFileImporter importer = new com.microproject.exchange.MpoFileImporter();
		assertTrue(importer.saveProject(project, beforeLevelingArchive), "MPO save rejected the original user split");
		Project beforeLevelingReload = importer.loadProject(new java.io.ByteArrayInputStream(beforeLevelingArchive.toByteArray()));
		Assignment beforeLevelingTarget = (Assignment) taskNamed(beforeLevelingReload, "User split target").getAssignments().stream()
			.filter(value -> "Shared committed resource".equals(((Assignment) value).getResource().getName()))
			.findFirst().orElseThrow();
		assertEquals(userContour, beforeLevelingTarget.getWorkContour().toString(beforeLevelingTarget.getDurationMillis()),
			"MPO reload before leveling must preserve the original user-created contour and gap");

		MainRibbonFrame owner = new MainRibbonFrame("Clear leveling user split acceptance", null);
		GraphicManager manager = new GraphicManager(owner);
		applicationFrame = owner;
		applicationManager = manager;
		SwingUtilities.invokeAndWait(() -> {
			owner.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			owner.setSize(1100, 700);
			owner.setLocationByPlatform(true);
			owner.setVisible(true);
			manager.installDocumentShortcuts(owner);
		});
		GuiAcceptanceSupport.await(owner::isShowing, "owner application frame did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(owner, owner.getContentPane(), robot, true,
			"clear-leveling owner application frame");
		// Adding the live document frame may settle schedules after the user split was
		// created. Settle once more before the dialog captures its preview source state.
		project.recalculate();
		assertEquals(userContour, target.getWorkContour().toString(userContourDuration),
			"opening the project frame must retain the pre-existing user contour before leveling");
		ResourceLevelingDialogBox dialog = openDialog(project, owner);
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton apply = findButton(dialog, UsabilityStrings.text("leveling.apply"));
		AbstractButton clear = findButton(dialog, UsabilityStrings.text("leveling.clear"));
		assertTrue(preview != null && preview.isShowing(), "Preview must be visible");
		assertTrue(apply != null && apply.isShowing(), "Apply must be visible");
		assertTrue(clear != null && clear.isShowing(), "Clear Leveling must be visible");
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"physical preview must show the conflict eligible for an assignment split");
		click(robot, apply);
		GuiAcceptanceSupport.await(() -> target.getLevelingSplitState() != null,
			"physical Apply must create a leveling-owned split on the already split assignment");
		String leveledContour = target.getWorkContour().toString(target.getDurationMillis());
		assertTrue(!userContour.equals(leveledContour), "Apply must add a distinct leveling-owned contour change");
		assertEquals(targetTaskStart, targetTask.getStart(), "assignment leveling must preserve target task start");
		assertEquals(blockerTaskStart, blockerTask.getStart(), "leveling must not move the competing task");
		assertEquals(blockerTaskEnd, blockerTask.getEnd(), "leveling must not resize the competing task");
		assertEquals(blockerStart, blocker.getStart(), "leveling must not move the blocking assignment");
		assertEquals(blockerEnd, blocker.getEnd(), "leveling must not resize the blocking assignment");
		assertEquals(unrelatedStart, unrelated.getStart(), "leveling must not change the target's other assignment");

		click(robot, clear);
		GuiAcceptanceSupport.await(() -> target.getLevelingSplitState() == null,
			"physical Clear Leveling must remove ownership of the generated split");
		assertEquals(userContourDuration, target.getDurationMillis(), "clear must restore the pre-leveling assignment duration");
		assertEquals(userContour, target.getWorkContour().toString(userContourDuration),
			"clear must preserve the exact pre-existing user-created contour and gap");
		assertEquals(targetTaskStart, targetTask.getStart(), "clear must preserve the target task start");
		assertEquals(blockerTaskStart, blockerTask.getStart(), "clear must preserve the competing task start");
		assertEquals(blockerTaskEnd, blockerTask.getEnd(), "clear must preserve the competing task finish");
		assertEquals(blockerStart, blocker.getStart(), "clear must preserve the blocking assignment start");
		assertEquals(blockerEnd, blocker.getEnd(), "clear must preserve the blocking assignment finish");
		assertEquals(unrelatedStart, unrelated.getStart(), "clear must preserve the unrelated assignment");
		capture(robot, dialog);
		click(robot, findButton(dialog, UsabilityStrings.text("common.close")));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "leveling dialog did not close after Clear");
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(owner, owner.getContentPane(), robot, true,
			"owner application frame after closing clear-leveling dialog");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> target.getLevelingSplitState() != null,
			"physical Ctrl+Z must restore the leveling-owned split");
		assertEquals(leveledContour, target.getWorkContour().toString(target.getDurationMillis()),
			"Undo must restore the leveled contour without losing the user's prior split");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> target.getLevelingSplitState() == null,
			"physical Ctrl+Y must restore the cleared leveling state");
		assertEquals(userContour, target.getWorkContour().toString(userContourDuration),
			"Redo must preserve the exact pre-existing user-created contour and gap");
		java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the cleared leveling state");
		Project reloaded = importer.loadProject(new java.io.ByteArrayInputStream(saved.toByteArray()));
		NormalTask reloadedTargetTask = taskNamed(reloaded, "User split target");
		Assignment reloadedTarget = (Assignment) reloadedTargetTask.getAssignments().stream()
			.filter(value -> "Shared committed resource".equals(((Assignment) value).getResource().getName()))
			.findFirst().orElseThrow();
		assertNull(reloadedTarget.getLevelingSplitState(), "MPO reload must not resurrect a leveling-owned split");
		assertEquals(userContourDuration, reloadedTarget.getDurationMillis(), "MPO reload changed the user-split assignment duration");
		assertEquals(userContour, reloadedTarget.getWorkContour().toString(userContourDuration),
			"MPO reload must preserve the exact user-created contour and gap");
	}

	@Test
	void proposedResourcesStayOutOfPreviewUntilRobotEnablesThem() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = newConflictProject();
		for (Object value : project.getTaskList()) ((NormalTask) value).setManuallyScheduled(true);
		MainRibbonFrame owner = new MainRibbonFrame("Proposed resource leveling acceptance", null);
		GraphicManager manager = new GraphicManager(owner);
		applicationFrame = owner;
		applicationManager = manager;
		SwingUtilities.invokeAndWait(() -> {
			owner.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			owner.setSize(1100, 700);
			owner.setLocationByPlatform(true);
			owner.setVisible(true);
			manager.installDocumentShortcuts(owner);
		});
		GuiAcceptanceSupport.await(owner::isShowing, "owner application frame did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(owner, owner.getContentPane(), robot, true,
			"proposed-resource owner application frame");
		ResourceLevelingDialogBox dialog = openDialog(project, owner);
		AbstractButton preview = findButton(dialog, UsabilityStrings.text("leveling.preview"));
		AbstractButton includeProposed = findButton(dialog, UsabilityStrings.text("leveling.includeProposed"));
		AbstractButton includeManual = findButton(dialog, UsabilityStrings.text("leveling.includeManual"));
		JComboBox<?> order = findComboBox(dialog, 0);
		JComboBox<?> sensitivity = findComboBox(dialog, 1);
		assertTrue(preview != null && preview.isShowing(), "Preview button must be visible");
		assertTrue(includeProposed != null && includeProposed.isShowing(), "Proposed-resource option must be visible");
		assertTrue(sensitivity != null && sensitivity.isShowing(), "sensitivity period must be visible");
		assertTrue(order != null && isBelow(sensitivity, order),
			"sensitivity must occupy a separate form row below leveling order");
		assertTrue(selectedIndex(sensitivity, 2), "day must be the document-derived product default");
		assertTrue(!isSelected(includeProposed), "Proposed resources must be excluded by default");
		assertTrue(includeManual != null && !isSelected(includeManual), "manual tasks must be excluded by default");
		click(robot, includeManual);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) == 0,
			"default preview must leave proposed-resource conflicts untouched even when manual tasks are enabled");

		java.util.Map<NormalTask, Long> originalTaskStarts = taskStarts(project);
		java.util.Map<NormalTask, Long> originalTaskEnds = taskEnds(project);
		java.util.Map<Assignment, Long> originalAssignmentStarts = assignmentStarts(project);
		java.util.Map<Assignment, Long> originalAssignmentDelays = assignmentDelays(project);
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
		String changedTaskName = tableCell(dialog, 0, 0);
		assertTrue("Conflict A".equals(changedTaskName) || "Conflict B".equals(changedTaskName),
			"preview must identify one of the stable fixture tasks");
		Assignment changedAssignment = (Assignment) taskNamed(project, changedTaskName).getAssignments().iterator().next();
		assertTrue(originalAssignmentDelays.containsKey(changedAssignment), "preview assignment must belong to the original model");
		click(robot, findButton(dialog, UsabilityStrings.text("leveling.apply")));
		GuiAcceptanceSupport.await(() -> !assignmentDelays(project).equals(originalAssignmentDelays),
			"physical Apply must level a proposed-resource assignment after opt-in");
		assertEquals(originalTaskStarts, taskStarts(project), "assignment leveling must preserve both task starts");
		assertTrue(com.microproject.datatype.Duration.millis(changedAssignment.getLevelingDelay()) > 0L,
			"the assignment named by the preview must receive the leveling delay");
		java.util.Map<NormalTask, Long> leveledTaskEnds = taskEnds(project);
		NormalTask unchangedTask = taskNamed(project,
			"Conflict A".equals(changedTaskName) ? "Conflict B" : "Conflict A");
		assertEquals(originalTaskEnds.get(unchangedTask), leveledTaskEnds.get(unchangedTask),
			"the unrelated task finish must remain unchanged");
		assertEquals(originalTaskEnds.get(taskNamed(project, changedTaskName)),
			leveledTaskEnds.get(taskNamed(project, changedTaskName)),
			"assignment-level leveling must preserve the manual task finish");
		java.util.Map<Assignment, Long> leveledStarts = assignmentStarts(project);
		java.util.Map<Assignment, Long> leveledDelays = assignmentDelays(project);
		assertEquals(1, changedEntryCount(originalAssignmentDelays, leveledDelays),
			"only the previewed proposed-resource assignment may change");
		assertEquals(1, changedEntryCount(originalAssignmentStarts, leveledStarts),
			"only the previewed proposed-resource assignment may move");
		assertEquals(BookingType.Kind.PROPOSED,
			((ResourceImpl) changedAssignment.getResource()).getBookingTypeKind(),
			"leveling must not change the resource booking type");
		capture(robot, dialog);
		click(robot, findButton(dialog, UsabilityStrings.text("common.close")));
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "leveling dialog did not close after Apply");
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(owner, owner.getContentPane(), robot, true,
			"owner application frame after closing leveling dialog");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> assignmentStarts(project).equals(originalAssignmentStarts)
				&& assignmentDelays(project).equals(originalAssignmentDelays),
			"physical Ctrl+Z must undo proposed-resource assignment leveling");
		assertEquals(originalTaskStarts, taskStarts(project), "Undo must preserve both task starts");
		assertEquals(originalTaskEnds, taskEnds(project), "Undo must preserve both task finishes");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> assignmentStarts(project).equals(leveledStarts)
				&& assignmentDelays(project).equals(leveledDelays),
			"physical Ctrl+Y must restore proposed-resource assignment leveling");
		assertEquals(leveledTaskEnds, taskEnds(project), "Redo must restore the expected leveled task finish");
		long changedTaskId = taskNamed(project, changedTaskName).getUniqueId();
		java.io.ByteArrayOutputStream saved = new java.io.ByteArrayOutputStream();
		com.microproject.exchange.MpoFileImporter importer = new com.microproject.exchange.MpoFileImporter();
		assertTrue(importer.saveProject(project, saved), "MPO save rejected the redone proposed-resource leveling");
		Project reloaded = importer.loadProject(new java.io.ByteArrayInputStream(saved.toByteArray()));
		NormalTask restoredChangedTask = taskWithUniqueId(reloaded, changedTaskId, taskNamed(project, changedTaskName));
		Assignment restoredChangedAssignment = (Assignment) restoredChangedTask.getAssignments().iterator().next();
		assertEquals(changedTaskName, restoredChangedTask.getName(), "MPO reload changed the leveled task identity");
		assertEquals("Shared resource", restoredChangedAssignment.getResource().getName(),
			"MPO reload changed the redone assignment's resource association");
		assertEquals(leveledDelays.get(changedAssignment), Long.valueOf(restoredChangedAssignment.getLevelingDelay()),
			"MPO reload changed the redone assignment delay");
		assertEquals(leveledStarts.get(changedAssignment), Long.valueOf(restoredChangedAssignment.getStart()),
			"MPO reload changed the redone assignment start");
		assertEquals(originalTaskStarts.get(taskNamed(project, "Conflict A")), Long.valueOf(taskNamed(reloaded, "Conflict A").getStart()),
			"MPO reload changed Conflict A start");
		assertEquals(originalTaskStarts.get(taskNamed(project, "Conflict B")), Long.valueOf(taskNamed(reloaded, "Conflict B").getStart()),
			"MPO reload changed Conflict B start");
		assertEquals(leveledTaskEnds.get(taskNamed(project, "Conflict A")), Long.valueOf(taskNamed(reloaded, "Conflict A").getEnd()),
			"MPO reload changed Conflict A finish");
		assertEquals(leveledTaskEnds.get(taskNamed(project, "Conflict B")), Long.valueOf(taskNamed(reloaded, "Conflict B").getEnd()),
			"MPO reload changed Conflict B finish");
	}

	private static int changedEntryCount(java.util.Map<Assignment, Long> before, java.util.Map<Assignment, Long> after) {
		int changed = 0;
		for (Assignment assignment : before.keySet())
			if (!before.get(assignment).equals(after.get(assignment))) changed++;
		return changed;
	}

	private static java.util.Map<NormalTask, Long> taskEnds(Project project) {
		java.util.Map<NormalTask, Long> result = new java.util.HashMap<>();
		runOnEdt(() -> {
			for (Object value : project.getTaskList()) {
				NormalTask task = (NormalTask) value;
				result.put(task, Long.valueOf(task.getEnd()));
			}
		});
		return result;
	}

	private static String tableCell(ResourceLevelingDialogBox dialog, int row, int column) throws Exception {
		Object[] value = new Object[1];
		SwingUtilities.invokeAndWait(() -> value[0] = findTable(dialog).getValueAt(row, column));
		return String.valueOf(value[0]);
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
		java.util.Map<NormalTask, Long> originalStarts = taskStarts(project);
		java.util.Map<Assignment, Long> originalAssignmentStarts = assignmentStarts(project);
		java.util.Map<Assignment, Long> originalAssignmentDelays = assignmentDelays(project);
		click(robot, preview);
		assertTrue(tableRowCount(dialog) == 0, "default preview must leave manual assignments fixed");
		assertEquals(originalStarts, taskStarts(project), "default preview must not change manual task dates");
		assertEquals(originalAssignmentStarts, assignmentStarts(project), "default preview must not change assignment dates");
		assertEquals(originalAssignmentDelays, assignmentDelays(project), "default preview must not change assignment delays");
		click(robot, includeManual);
		click(robot, preview);
		GuiAcceptanceSupport.await(() -> tableRowCountUnchecked(dialog) > 0,
			"explicitly included manual assignments must appear in the preview");
		assertTrue(isSelected(includeManual), "physical opt-in must remain selected while the plan is shown");
		click(robot, apply);
		GuiAcceptanceSupport.await(() -> anyAssignmentDelayDiffers(project, originalAssignmentDelays),
			"physical Apply must change at least one manual assignment's leveling delay");
		assertEquals(originalStarts, taskStarts(project), "assignment-level leveling must leave manual task dates unchanged");
		assertTrue(anyAssignmentStartDiffers(project, originalAssignmentStarts),
			"physical Apply must change a leveled manual assignment's start");
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
		GuiAcceptanceSupport.await(() -> assignmentStarts(project).equals(originalAssignmentStarts)
				&& assignmentDelays(project).equals(originalAssignmentDelays),
			"Undo restores the manual assignment dates and delays changed by the physical route");
		assertEquals(originalStarts, taskStarts(project), "Undo must preserve manual task dates");
		SwingUtilities.invokeAndWait(() -> project.getUndoController().redo());
		GuiAcceptanceSupport.await(() -> anyAssignmentDelayDiffers(project, originalAssignmentDelays)
				&& anyAssignmentStartDiffers(project, originalAssignmentStarts),
			"Redo restores the manual assignment leveling result");
		assertEquals(originalStarts, taskStarts(project), "Redo must preserve manual task dates");
		capture(robot, dialog);
	}

	private static java.util.Map<NormalTask, Long> taskStarts(Project project) {
		java.util.Map<NormalTask, Long> result = new java.util.HashMap<>();
		runOnEdt(() -> {
			for (Object value : project.getTaskList()) {
				NormalTask task = (NormalTask) value;
				result.put(task, Long.valueOf(task.getStart()));
			}
		});
		return result;
	}

	private static java.util.Map<Assignment, Long> assignmentStarts(Project project) {
		java.util.Map<Assignment, Long> result = new java.util.HashMap<>();
		runOnEdt(() -> {
			for (Object value : project.getTaskList())
				for (Object assignmentValue : ((NormalTask) value).getAssignments()) {
					Assignment assignment = (Assignment) assignmentValue;
					result.put(assignment, Long.valueOf(assignment.getStart()));
				}
		});
		return result;
	}

	private static java.util.Map<Assignment, Long> assignmentDelays(Project project) {
		java.util.Map<Assignment, Long> result = new java.util.HashMap<>();
		runOnEdt(() -> {
			for (Object value : project.getTaskList())
				for (Object assignmentValue : ((NormalTask) value).getAssignments()) {
					Assignment assignment = (Assignment) assignmentValue;
					result.put(assignment, Long.valueOf(assignment.getLevelingDelay()));
				}
		});
		return result;
	}

	private static boolean anyAssignmentDelayDiffers(Project project, java.util.Map<Assignment, Long> originalDelays) {
		return !assignmentDelays(project).equals(originalDelays);
	}

	private static boolean anyAssignmentStartDiffers(Project project, java.util.Map<Assignment, Long> originalStarts) {
		return !assignmentStarts(project).equals(originalStarts);
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
		return openDialog(project, null);
	}

	private static ResourceLevelingDialogBox openDialog(Project project, MainRibbonFrame owner) throws Exception {
		ResourceLevelingDialogBox[] dialog = new ResourceLevelingDialogBox[1];
		SwingUtilities.invokeAndWait(() -> dialog[0] = ResourceLevelingDialogBox.getInstance(owner, project));
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
		NormalTask first = task(project, "Conflict A", 1L);
		NormalTask second = task(project, "Conflict B", 1L);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Shared resource");
		resource.setBookingTypeKind(com.microproject.pm.resource.BookingType.Kind.PROPOSED);
		AssignmentService.getInstance().newAssignment(first, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		AssignmentService.getInstance().newAssignment(second, resource, 1.0, 0L, ResourceLevelingDialogGuiAcceptanceTest.class);
		project.recalculate();
		return project;
	}

	private static NormalTask task(Project project, String name, long days) {
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName(name);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(days * CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private static NormalTask taskNamed(Project project, String name) {
		return (NormalTask) project.getTaskList().stream().filter(task -> name.equals(((NormalTask) task).getName()))
			.findFirst().orElseThrow();
	}

	private static NormalTask taskWithUniqueId(Project project, long uniqueId, NormalTask savedTask) {
		for (java.util.Iterator<com.microproject.pm.task.Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = tasks.next();
			if (task.getUniqueId() == uniqueId) return (NormalTask) task;
		}
		java.util.List<String> outline = new java.util.ArrayList<>();
		for (java.util.Iterator<com.microproject.pm.task.Task> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			com.microproject.pm.task.Task task = tasks.next();
			outline.add("id=" + task.getUniqueId() + ",name=" + task.getName());
		}
		java.util.List<String> taskList = new java.util.ArrayList<>();
		for (Object value : project.getTaskList()) {
			com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) value;
			taskList.add("id=" + task.getUniqueId() + ",name=" + task.getName());
		}
		throw new AssertionError("MPO reload omitted target task id=" + uniqueId + ", name=" + savedTask.getName()
			+ "; reloaded outline=" + outline + "; reloaded task list=" + taskList);
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
