/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static com.microproject.menu.testsupport.MenuDefinitionSupport.ribbonUiButtonIds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.dialog.BaselineDialog;
import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.transform.grouping.NodeGroup;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.options.CalendarOption;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.pm.graphic.views.BaseView;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.util.Environment;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.pm.graphic.frames.workspace.Workspace;
import com.microproject.workspace.WorkspaceSetting;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ribbon.CommandId;
import com.microproject.ui.ribbon.ModernRibbonPanel;

class RibbonButtonBehaviorTest {
	private enum Strategy {
		ROUTE_DIALOG,
		ROUTE_CHOOSER,
		ROUTE_VIEW,
		VIEW_ONLY,
		ROUTE_EXTERNAL,
		STATE_TOGGLE,
		STRUCTURAL_ONLY
	}

	private static final Map<String, Strategy> COVERAGE = coverageTable();

	@Test
	void everyRibbonButtonFromTheDefinitionHasAnExplicitStrategy() {
		Set<String> inventory = new LinkedHashSet<>(ribbonUiButtonIds());
		assertTrue(COVERAGE.keySet().containsAll(inventory), () -> "Missing coverage for: " + missing(inventory, COVERAGE.keySet()));
	}

	@Test
	void statusDateTargetsAreDeduplicatedByIdentityAcrossRegistriesAndComponentTree() {
		JButton registered = equalButton("RibbonStatusDate");
		JButton toolbarCompatibility = equalButton("StatusDate");
		JButton transientRibbon = equalButton("RibbonStatusDate");
		JPanel nested = new JPanel();
		nested.add(transientRibbon);
		JPanel root = new JPanel();
		root.add(registered);
		root.add(nested);

		Set<AbstractButton> targets = StatusDateControlUpdater.collectStatusDateButtons(root,
				List.of(registered, toolbarCompatibility), List.of(registered));

		assertEquals(3, targets.size(), "one registered ribbon button, one legacy toolbar button, and one transient ribbon button are refreshed");
		assertTrue(targets.contains(registered));
		assertTrue(targets.contains(toolbarCompatibility));
		assertTrue(targets.contains(transientRibbon));
	}

	@Test
	void refreshUpdatesTransientStatusDateCommandsInCollapsedRibbonPopups() throws Exception {
		JButton overflow = new JButton("…");
		JPopupMenu popup = new JPopupMenu();
		JMenuItem transientStatusDate = new JMenuItem("stale Status Date");
		transientStatusDate.setActionCommand("RibbonStatusDate");
		popup.add(transientStatusDate);
		popup.putClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY, popup);
		overflow.putClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY, popup);
		JPanel root = new JPanel();
		root.add(overflow);
		MenuManager menuManager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		String label = menuManager.getStringOrNull("RibbonStatusDate.text");
		String expectedDisplay = "NA " + (label == null ? "Status Date" : label) + ":";

		SwingUtilities.invokeAndWait(() -> {
			StatusDateControlUpdater.refresh(menuManager, root, null);
			assertEquals(expectedDisplay, transientStatusDate.getText());
			assertEquals(expectedDisplay, transientStatusDate.getAccessibleContext().getAccessibleName());
		});
	}

	private static JButton equalButton(String actionCommand) {
		JButton button = new JButton() {
			private static final long serialVersionUID = 1L;
			@Override public boolean equals(Object other) { return other instanceof JButton; }
			@Override public int hashCode() { return 1; }
		};
		button.setActionCommand(actionCommand);
		return button;
	}

	@Test
	void coverageEntriesResolveToLiveActions() throws Exception {
		Harness harness = newHarness();
		for (String buttonId : COVERAGE.keySet()) {
			String actionId = harness.actionId(buttonId);
			assertNotNull(actionId, () -> buttonId + " does not resolve to an action");
			Action action = assertDoesNotThrow(() -> harness.manager.getAction(actionId),
				() -> buttonId + " does not resolve to a live action");
			assertNotNull(action, () -> buttonId + " has no live action");
		}
	}

	@Test
	void notesRoutesRespectSelectionKindAndTaskOrResourceMode() throws Exception {
		Harness harness = newHarness();

		harness.selectSingle(harness.taskNode);
		harness.invoke("RibbonNotes");
		assertCall(harness, "taskInfo", harness.task, Boolean.TRUE, Boolean.FALSE);

		harness.resetCalls();
		harness.selectSingle(harness.resourceNode);
		harness.invoke("RibbonNotes");
		assertCall(harness, "resourceInfo", harness.resource, Boolean.TRUE);

		harness.resetCalls();
		harness.selectSingle(harness.assignmentNode);
		harness.setTaskInformation(true, false);
		harness.invoke("RibbonNotes");
		assertCall(harness, "taskInfo", harness.assignment.getTask(), Boolean.TRUE, Boolean.TRUE);

		harness.resetCalls();
		harness.selectSingle(harness.assignmentNode);
		harness.setTaskInformation(false, true);
		harness.invoke("RibbonNotes");
		assertCall(harness, "resourceInfo", harness.assignment.getResource(), Boolean.TRUE);

		harness.resetCalls();
		harness.selectNone();
		harness.invoke("RibbonNotes");
		assertTrue(harness.calls.isEmpty(), "No selection should not open a notes dialog");

		harness.resetCalls();
		harness.selectMultiple(harness.taskNode, harness.resourceNode);
		harness.invoke("RibbonNotes");
		assertTrue(harness.calls.isEmpty(), "Multiple selection should not open a notes dialog");
	}

	@Test
	void informationAndRouteDialogsFireExactlyOnce() throws Exception {
		Harness harness = newHarness();

		harness.selectSingle(harness.taskNode);
		harness.invoke("RibbonTaskInformation");
		assertCall(harness, "taskInfo", harness.task, Boolean.FALSE, Boolean.FALSE);
		assertTaskInformationOutcome(harness, RibbonCommandResult.Status.REJECTED,
			List.of(harness.task.getUniqueId()), List.of());

		harness.selectNone();
		harness.invoke("RibbonTaskInformation");
		assertTaskInformationOutcome(harness, RibbonCommandResult.Status.REJECTED, List.of(), List.of());

		harness.resetCalls();
		harness.selectSingle(harness.resourceNode);
		harness.invoke("RibbonResourceInformation");
		assertCall(harness, "resourceInfo", harness.resource, Boolean.FALSE);

		harness.resetCalls();
		harness.selectSingle(harness.assignmentNode);
		harness.setTaskInformation(false, true);
		harness.invoke("RibbonResourceInformation");
		assertCall(harness, "resourceInfo", harness.assignment.getResource(), Boolean.FALSE);

		harness.resetCalls();
		harness.selectSingle(harness.projectNode);
		harness.invoke("RibbonProjectInformation");
		assertCall(harness, "projectInfo", harness.project);

		harness.resetCalls();
		harness.selectSingle(harness.taskNode);
		harness.invoke("RibbonFind");
		assertFalse(harness.calls.isEmpty(), "Find should record a route");
		assertEquals("find", harness.calls.get(0).name);
		assertEquals(2, harness.calls.get(0).args.size());

		harness.resetCalls();
		harness.invoke("RibbonCalendarOptions");
		assertCall(harness, "calendarOptions");

		harness.resetCalls();
		harness.invoke("RibbonProjectsDialog");
		assertCall(harness, "projectsDialog", harness.project);

		harness.resetCalls();
		harness.invoke("RibbonAssignResources");
		assertCall(harness, "assignResources", harness.frame);

		harness.resetCalls();
		harness.invoke("RibbonTimesheet");
		assertCall(harness, "timesheet", harness.frame);

		harness.resetCalls();
		harness.invoke("RibbonUpdateTasks");
		assertCall(harness, "updateTasks", harness.frame);
		assertEquals(RibbonCommandResult.Status.REJECTED,
			harness.manager.getLastRibbonCommandResult().status());
		assertEquals("route-rejected", harness.manager.getLastRibbonCommandResult().reason());

		harness.resetCalls();
		harness.invoke("RibbonUpdateProject");
		assertCall(harness, "updateProject", harness.frame);

		harness.resetCalls();
		harness.invoke("RibbonMoveProject");
		assertCall(harness, "moveProject", harness.frame);

		harness.resetCalls();
		harness.invoke("RibbonSaveBaseline");
		assertEquals(1, harness.frame.baselineDialogCallCount(true));
		assertNotNull(harness.task.getSnapshot(Snapshottable.BASELINE),
			"Saving a baseline must create a project snapshot");

		harness.resetCalls();
		harness.invoke("RibbonClearBaseline");
		assertEquals(1, harness.frame.baselineDialogCallCount(false));
		assertNull(harness.task.getSnapshot(Snapshottable.BASELINE),
			"Clearing a baseline must remove the project snapshot");
	}

	@Test
	void taskInformationPresentationFailureRecordsFailedOutcome() throws Exception {
		Harness harness = newHarness();
		harness.selectSingle(harness.taskNode);
		harness.manager.taskInformationRouteAccepted = true;

		harness.invoke("RibbonTaskInformation");

		RibbonCommandResult result = harness.manager.getLastRibbonCommandResult();
		assertNotNull(result);
		assertEquals(RibbonCommandResult.Status.FAILED, result.status());
		assertEquals("dialog-not-visible", result.reason());
		assertEquals(List.of(harness.task.getUniqueId()), result.selectedTaskIds());
		assertTrue(result.affectedTaskIds().isEmpty());
		assertEquals(harness.frame.getTopViewId(), result.activeViewId());
	}

	@Test
	void taskInformationPresentationExceptionRecordsFailureBeforeRethrowing() throws Exception {
		Harness harness = newHarness();
		harness.selectSingle(harness.taskNode);
		harness.manager.taskInformationRouteAccepted = true;
		harness.manager.taskInformationFailure = new IllegalStateException("presentation failed");

		Exception wrappedFailure = assertThrows(Exception.class,
			() -> harness.invoke("RibbonTaskInformation"));

		assertSame(harness.manager.taskInformationFailure, wrappedFailure.getCause());
		RibbonCommandResult result = harness.manager.getLastRibbonCommandResult();
		assertNotNull(result);
		assertEquals(RibbonCommandResult.Status.FAILED, result.status());
		assertEquals("IllegalStateException", result.reason());
		assertEquals(List.of(harness.task.getUniqueId()), result.selectedTaskIds());
		assertTrue(result.affectedTaskIds().isEmpty());
		assertEquals(harness.frame.getTopViewId(), result.activeViewId());
	}

	private static void assertTaskInformationOutcome(Harness harness, RibbonCommandResult.Status status,
			List<Long> selectedTaskIds, List<Long> affectedTaskIds) {
		RibbonCommandResult result = harness.manager.getLastRibbonCommandResult();
		assertNotNull(result);
		assertEquals(status, result.status());
		assertEquals(selectedTaskIds, result.selectedTaskIds());
		assertEquals(affectedTaskIds, result.affectedTaskIds());
		assertEquals(harness.frame.getTopViewId(), result.activeViewId());
	}

	@Test
	void ganttTaskInformationRouteSurvivesTransientDocumentDeactivation() throws Exception {
		Harness harness = newHarness();
		harness.frame.setActive(false);

		harness.manager.doInformationDialog(harness.task, false);

		assertCall(harness, "taskInfo", harness.task, Boolean.FALSE, Boolean.FALSE);
	}

	@Test
	void insertTaskRouteSurvivesTransientDocumentDeactivation() throws Exception {
		Harness harness = newHarness();
		harness.frame.setActive(false);

		SwingUtilities.invokeAndWait(() -> harness.manager.getAction(MenuActionConstants.ACTION_INSERT_TASK)
			.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "insert")));

		assertEquals(1, harness.frame.insertTaskCallCount());
	}

	@Test
	void insertTaskAddsContiguousRowsAndOneUndoRevertsTheWholeBatch() throws Exception {
		Harness harness = newHarness();
		SpreadSheet sheet = harness.frame.getTopSpreadSheet();
		var taskModel = harness.project.getTaskModel();
		Object root = taskModel.getRoot();
		int beforeNodes = taskModel.getChildCount(root);
		List<Object> beforeChildren = new ArrayList<>();
		for (int index = 0; index < beforeNodes; index++) beforeChildren.add(taskModel.getChild(root, index));
		harness.undoController.clear();
		SwingUtilities.invokeAndWait(() -> {
			sheet.setRowSelectionInterval(0, 1);
			assertEquals(2, sheet.getSelectedNodes().size(), "test rows should resolve to two task rows");
			assertTrue(sheet.getSelectedNodes().stream().allMatch(node -> node.getImpl() instanceof Task));
			int selectedLastIndex = beforeChildren.indexOf(sheet.getSelectedNodes().get(1));
			assertTrue(selectedLastIndex >= 0);
			var outcome = harness.frame.routeTaskCommand(CommandId.INSERT);
			assertEquals(RibbonCommandResult.Status.CHANGED, outcome.status(), outcome.reason());
			assertEquals(beforeNodes + 2, taskModel.getChildCount(root));
			List<Object> afterChildren = new ArrayList<>();
			for (int index = 0; index < taskModel.getChildCount(root); index++) afterChildren.add(taskModel.getChild(root, index));
			assertEquals(beforeChildren.subList(0, selectedLastIndex + 1), afterChildren.subList(0, selectedLastIndex + 1));
			assertEquals(beforeChildren.subList(selectedLastIndex + 1, beforeChildren.size()),
				afterChildren.subList(selectedLastIndex + 3, afterChildren.size()),
				"the batch must be inserted immediately below the last selected task");
			harness.undoController.undo();
			assertEquals(beforeNodes, taskModel.getChildCount(root), "one undo must remove both inserted rows");
			harness.undoController.redo();
			assertEquals(beforeNodes + 2, taskModel.getChildCount(root), "one redo must restore both inserted rows");
		});
	}

	@Test
	void assignmentReplaceRedoRunsThroughDocumentFrameShortcutRoute() throws Exception {
		Harness harness = newHarness();
		NormalTask task = (NormalTask) harness.task;
		SwingUtilities.invokeAndWait(() -> {
			harness.project.initialize(false, false);
			task.setDuration(2L * CalendarOption.getInstance().getMillisPerDay());
			assertTrue(task.getDurationMillis() > 0L, "the source task must have a scheduled duration");
			assertTrue(task.getStart() < task.getEnd(), "the source task must have valid scheduled bounds");
			Resource original = harness.resource;
			Resource replacement = harness.project.getResourcePool().createScriptedResource();
			Assignment source = AssignmentService.getInstance().newAssignment(task, original, 1.0D, 0L, this, false);
			long sourceWork = source.getWork(null);
			assertTrue(sourceWork > 0L, "the source assignment must have work before exercising Undo/Redo");
			source.setActualWork(sourceWork / 2L, null);
			long actualWork = source.getActualWork(null);
			long remainingWork = source.getRemainingWork();
			assertTrue(actualWork > 0L, "the source assignment must have actual work");
			assertTrue(remainingWork > 0L, "the source assignment must have remaining work");
			assertEquals(28_800_000L, remainingWork,
				"the headless route must use the Robot fixture's two-day, half-complete work split");
			harness.undoController.clear();
			List<Assignment> created = AssignmentService.getInstance().replaceAssignment(
				source, List.of(replacement), this, true);
			assertEquals(1, created.size());
			Assignment replacementAssignment = created.get(0);
			assertEquals(0L, replacementAssignment.getActualWork(null));
			assertEquals(remainingWork, replacementAssignment.getRemainingWork());
			assertEquals(1, harness.project.getUndoController().getEditNames().size(),
				"the replacement must publish one undo group after assignment events");
			assertSame(harness.project.getUndoController(), harness.frame.getUndoController(),
				"the visible frame must observe the project undo controller");
			assertTrue(harness.project.getUndoController().canUndo(),
				"the replacement must be undoable after its project event publication");
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_UNDO).isEnabled(),
				"the document Undo action must be enabled when the committed replacement route returns; frameController="
					+ (harness.frame.getUndoController() == harness.project.getUndoController())
					+ ", controllerCanUndo=" + harness.project.getUndoController().canUndo()
					+ ", actionEnabled=" + harness.manager.getAction(MenuActionConstants.ACTION_UNDO).isEnabled());
			InputMap shortcuts = new InputMap();
			ActionMap actions = new ActionMap();
			harness.manager.applyMicrosoftShortcuts(shortcuts, actions);
			Action undo = actions.get(shortcuts.get(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)));
			Action redo = actions.get(shortcuts.get(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK)));
			assertNotNull(undo, "Ctrl+Z must resolve through the document shortcut map");
			assertNotNull(redo, "Ctrl+Y must resolve through the document shortcut map");
			undo.actionPerformed(new ActionEvent(harness.manager, ActionEvent.ACTION_PERFORMED, "undo"));
			assertFalse(harness.project.getUndoController().canUndo(), "one undo must reverse the entire replacement");
			assertNull(task.findAssignment(replacement), "one undo must remove the replacement assignment");
			assertSame(source, task.findAssignment(original));
			assertEquals(actualWork, source.getActualWork(null));
			assertEquals(remainingWork, source.getRemainingWork());
			redo.actionPerformed(new ActionEvent(harness.manager, ActionEvent.ACTION_PERFORMED, "redo"));

			assertSame(source, task.findAssignment(original));
			assertEquals(actualWork, source.getActualWork(null));
			assertEquals(0L, source.getRemainingWork());
			assertSame(replacementAssignment, task.findAssignment(replacement));
			assertEquals(remainingWork, replacementAssignment.getRemainingWork(),
				"redo must restore transferred remaining work after reconnecting the replacement");
			assertEquals(0L, replacementAssignment.getActualWork(null));
		});
	}

	@Test
	void hierarchyCommandsFollowTaskSelectionState() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.manager.setButtonState(harness.task, harness.project);
			assertInformationButtons(harness, false, false, false);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_LINK).isEnabled(),
				"Link must be disabled without selected tasks");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_UNLINK).isEnabled(),
				"Unlink must be disabled without a selected task");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_INDENT).isEnabled());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_OUTDENT).isEnabled());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_HIDE_SELECTED_TASKS).isEnabled());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_EXPAND).isEnabled());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_COLLAPSE).isEnabled());
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 0);
			harness.manager.setButtonState(harness.task, harness.project);
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_INDENT).isEnabled());
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_HIDE_SELECTED_TASKS).isEnabled(),
				"Hide Selected enablement must use the same visibility resolver as execution");
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_EXPAND).isEnabled());
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_COLLAPSE).isEnabled());
			// The first task is at the root and therefore cannot be outdented;
			// selection still enables the hierarchy command that is applicable.
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_OUTDENT).isEnabled());

			// A deferred frame selection can still expose the previous pair while
			// the active JTable already owns one selected row. Enablement and command
			// execution must both resolve the active table's typed selection.
			harness.frame.setSelection(List.of(harness.taskNode, harness.secondTaskNode));
			List<Node> resolvedTasks = harness.frame.getSelectedTaskNodes(false, true);
			assertEquals(1, resolvedTasks.size());
			assertSame(harness.task, resolvedTasks.get(0).getImpl());
			harness.manager.setButtonState(harness.task, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_LINK).isEnabled(),
				"one physical row must not enable Link from a stale two-task frame selection");
			harness.frame.setExecuteLinkForSelectionTest(true);
			RibbonCommandResult rejectedLink = harness.frame.routeTaskCommand(CommandId.LINK);
			assertEquals(RibbonCommandResult.Status.REJECTED, rejectedLink.status(), rejectedLink.reason());
			assertTrue(harness.secondTask.getPredecessorList().isEmpty(),
				"execution must use the same one-row selection and leave dependencies unchanged");

			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			resolvedTasks = harness.frame.getSelectedTaskNodes(false, true);
			assertEquals(2, resolvedTasks.size());
			assertSame(harness.task, resolvedTasks.get(0).getImpl());
			assertSame(harness.secondTask, resolvedTasks.get(1).getImpl());
			harness.manager.setButtonState(harness.task, harness.project);
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_LINK).isEnabled(),
				"two physical task rows must enable Link from the same resolver used at execution");

			harness.frame.getTopSpreadSheet().setRowSelectionInterval(1, 1);
			harness.frame.setSelection(List.of(harness.taskNode));
			resolvedTasks = harness.frame.getSelectedTaskNodes(true, true);
			assertEquals(1, resolvedTasks.size());
			assertSame(harness.secondTask, resolvedTasks.get(0).getImpl());
			List<Node> visibilityTasks = harness.frame.getSelectedVisibilityTaskNodes();
			assertEquals(1, visibilityTasks.size());
			assertSame(harness.secondTask, visibilityTasks.get(0).getImpl(),
				"Hide Selected must use the same active table row as other task commands");
			harness.frame.setExecuteIndentForSelectionTest(true);
			RibbonCommandResult indent = harness.frame.routeTaskCommand(CommandId.INDENT);
			assertEquals(RibbonCommandResult.Status.CHANGED, indent.status(), indent.reason());
			assertEquals(List.of(harness.secondTask.getUniqueId()), indent.selectedTaskIds());
			assertEquals(List.of(harness.secondTask.getUniqueId()), indent.affectedTaskIds());
			assertSame(harness.task, harness.secondTask.getWbsParentTask(),
				"Indent execution must use the active table row, not the stale frame selection");

			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 0);
			RibbonCommandResult collapse = harness.frame.routeTaskCommand(CommandId.COLLAPSE);
			assertEquals(RibbonCommandResult.Status.NO_CHANGE, collapse.status(), collapse.reason());
			assertEquals(List.of(harness.task.getUniqueId()), collapse.selectedTaskIds());
			assertEquals(List.of(harness.task.getUniqueId()), collapse.affectedTaskIds());
			RibbonCommandResult expand = harness.frame.routeTaskCommand(CommandId.EXPAND);
			assertEquals(RibbonCommandResult.Status.NO_CHANGE, expand.status(), expand.reason());
			assertEquals(List.of(harness.task.getUniqueId()), expand.selectedTaskIds());
			assertEquals(List.of(harness.task.getUniqueId()), expand.affectedTaskIds());
		});
	}

	@Test
	void hideSelectedUsesActiveBottomSpreadsheetRowInsteadOfStaleFrameSelection() throws Exception {
		Harness harness = newHarness();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.activateView(MenuActionConstants.ACTION_WBS);
			harness.frame.activateBottomView(harness.frame.getGanttView(), MenuActionConstants.ACTION_GANTT);
			SpreadSheet bottomSheet = harness.frame.getActiveSpreadSheet();
			assertSame(harness.frame.getGanttView().getSpreadSheet(), bottomSheet);
			assertNull(harness.frame.getTopSpreadSheet(), "WBS top view has no task spreadsheet");

			bottomSheet.setRowSelectionInterval(1, 1);
			harness.frame.setSelection(List.of(harness.taskNode));
			RibbonCommandResult result = harness.frame.routeTaskCommand(CommandId.HIDE_SELECTED);

			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), result.reason());
			assertEquals(List.of(harness.secondTask.getUniqueId()), result.selectedTaskIds(),
				"selection IDs must come from the active bottom spreadsheet row");
			assertEquals(List.of(harness.secondTask.getUniqueId()), result.affectedTaskIds(),
				"Hide Selected must hide the bottom row, not stale frame selection");
			assertTrue(harness.secondTask.isHiddenTask());
			assertFalse(harness.task.isHiddenTask(), "stale frame selection must remain visible");
		});
	}

	@Test
	void hierarchyShortcutPublishesSemanticCommandResult() throws Exception {
		Harness harness = newHarness();
		harness.frame.setExecuteIndentForSelectionTest(true);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(1, 1);
			InputMap shortcuts = new InputMap();
			ActionMap actions = new ActionMap();
			harness.manager.applyMicrosoftShortcuts(shortcuts, actions);
			KeyStroke indentShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
				InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
			Action indent = actions.get(shortcuts.get(indentShortcut));
			assertNotNull(indent, "Alt+Shift+Right must resolve from the root-pane shortcut maps");

			indent.actionPerformed(new ActionEvent(harness.manager, ActionEvent.ACTION_PERFORMED, "indent-shortcut"));

			RibbonCommandResult result = harness.manager.getLastRibbonCommandResult();
			assertNotNull(result);
			assertEquals(CommandId.INDENT.actionId(), result.commandId());
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), result.reason());
			assertEquals(List.of(harness.secondTask.getUniqueId()), result.selectedTaskIds());
			assertEquals(List.of(harness.secondTask.getUniqueId()), result.affectedTaskIds());
			assertEquals(harness.frame.getTopViewId(), result.activeViewId());
			assertSame(harness.task, harness.secondTask.getWbsParentTask());
		});
	}

	@Test
	void dependencyCommandResultsReportOperationSelectionAndAffectedTasks() throws Exception {
		Harness harness = newHarness();
		harness.frame.setExecuteLinkForSelectionTest(true);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			RibbonCommandResult result = harness.frame.routeTaskCommand(CommandId.LINK);
			List<Long> selectedIds = List.of(harness.task.getUniqueId(), harness.secondTask.getUniqueId());
			assertEquals(RibbonCommandResult.Status.CHANGED, result.status(), result.reason());
			assertEquals("", result.reason());
			assertEquals(selectedIds, result.selectedTaskIds());
			assertEquals(selectedIds, result.affectedTaskIds());
		});
	}

	@Test
	void hierarchyCommandsAreDisabledForReadOnlySubprojectSelection() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		DefaultSubProj readOnlySubproject = new DefaultSubProj(harness.project, 771L);
		readOnlySubproject.setName("Read-only subproject row");
		harness.project.connectTask(readOnlySubproject);
		harness.project.getTaskOutlines().addToAll(readOnlySubproject, null);
		Node readOnlyNode = NodeFactory.getInstance().createNode(readOnlySubproject);

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = harness.frame.getTopSpreadSheet();
			sheet.clearSelection();
			assertEquals(0, sheet.getSelectedRows().length);
			harness.frame.setSelection(List.of(readOnlyNode));
			List<Node> selectedTasks = harness.frame.getSelectedTaskNodes(false, true);
			assertEquals(1, selectedTasks.size());
			assertSame(readOnlySubproject, selectedTasks.get(0).getImpl());
			assertTrue(harness.frame.getSelectedTaskNodes(true, true).isEmpty(),
				"execution must exclude this read-only subproject task");
			harness.manager.setButtonState(readOnlySubproject, harness.project);
			assertTrue(readOnlySubproject.isReadOnly());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_INDENT).isEnabled(),
				"Indent enablement must apply the read-only filter used by execution");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_MARK_ON_TRACK).isEnabled(),
				"Mark on Track must be disabled when the selected task is not editable");
			assertEquals(RibbonCommandResult.Status.REJECTED,
				harness.frame.routeTaskCommand(CommandId.MARK_ON_TRACK).status(),
				"Mark on Track execution must use the same editable selection filter");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_UNLINK).isEnabled(),
				"Unlink must be disabled for a read-only selected task");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_HIDE_SELECTED_TASKS).isEnabled(),
				"Hide Selected must be disabled for a read-only selected task");
			readOnlySubproject.setHiddenTask(true);
			harness.manager.setButtonState(readOnlySubproject, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_SHOW_ALL_TASKS).isEnabled(),
				"Show All must ignore a hidden row that the current document cannot edit");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_MANUAL).isEnabled(),
				"Manual scheduling must be disabled for a read-only selected task");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_AUTOMATIC).isEnabled(),
				"Automatic scheduling must be disabled for a read-only selected task");
			harness.frame.setExecuteUnlinkForSelectionTest(true);
			harness.frame.setExecuteLinkForSelectionTest(true);
			RibbonCommandResult unlinkResult = harness.frame.routeTaskCommand(CommandId.UNLINK);
			assertEquals(RibbonCommandResult.Status.REJECTED, unlinkResult.status(),
				"Unlink execution must use the same editable selection filter");
			assertEquals("task-read-only", unlinkResult.reason());
			assertEquals(List.of(readOnlySubproject.getUniqueId()), unlinkResult.selectedTaskIds());
			assertTrue(unlinkResult.affectedTaskIds().isEmpty());
			RibbonCommandResult linkResult = harness.frame.routeTaskCommand(CommandId.LINK);
			assertEquals(RibbonCommandResult.Status.REJECTED, linkResult.status());
			assertEquals("task-read-only", linkResult.reason());
			assertEquals(List.of(readOnlySubproject.getUniqueId()), linkResult.selectedTaskIds());
			assertTrue(linkResult.affectedTaskIds().isEmpty());
			harness.frame.setExecuteIndentForSelectionTest(true);
			var readOnlySheet = harness.frame.getTopSpreadSheet();
			readOnlySheet.setRowSelectionInterval(1, 1);
			assertEquals(RibbonCommandResult.Status.CHANGED,
				harness.frame.routeTaskCommand(CommandId.INDENT).status());
			assertSame(harness.task, harness.secondTask.getWbsParentTask());
			readOnlySheet.setRowSelectionInterval(0, 0);
			harness.project.setReadOnly(true);
			RibbonCommandResult readOnlyProjectLink = harness.frame.routeTaskCommand(CommandId.LINK);
			assertEquals("document-read-only", readOnlyProjectLink.reason());
			assertEquals(List.of(harness.task.getUniqueId()), readOnlyProjectLink.selectedTaskIds());
			assertTrue(readOnlyProjectLink.affectedTaskIds().isEmpty());
			assertEquals(harness.frame.getTopViewId(), readOnlyProjectLink.activeViewId());
			RibbonCommandResult readOnlyProjectUnlink = harness.frame.routeTaskCommand(CommandId.UNLINK);
			assertEquals(readOnlyProjectLink.reason(), readOnlyProjectUnlink.reason());
			assertEquals(List.of(harness.task.getUniqueId()), readOnlyProjectUnlink.selectedTaskIds());
			assertTrue(readOnlyProjectUnlink.affectedTaskIds().isEmpty());
			assertEquals(harness.frame.getTopViewId(), readOnlyProjectUnlink.activeViewId());
			readOnlySheet.setRowSelectionInterval(0, 0);
			int readOnlyHierarchyRows = readOnlySheet.getRowCount();
			for (CommandId command : List.of(CommandId.EXPAND, CommandId.COLLAPSE)) {
				RibbonCommandResult result = harness.frame.routeTaskCommand(command);
				assertEquals(RibbonCommandResult.Status.REJECTED, result.status(), command.toString());
				assertEquals("document-read-only", result.reason(), command.toString());
				assertEquals(List.of(harness.task.getUniqueId()), result.selectedTaskIds(), command.toString());
				assertTrue(result.affectedTaskIds().isEmpty(), command.toString());
				assertEquals(harness.frame.getTopViewId(), result.activeViewId(), command.toString());
				assertEquals(readOnlyHierarchyRows, readOnlySheet.getRowCount(), command.toString());
			}
			readOnlySheet.setRowSelectionInterval(1, 1);
			int readOnlyRowsBefore = readOnlySheet.getRowCount();
			var parentBeforeReadOnlyHierarchy = harness.secondTask.getWbsParentTask();
			for (CommandId command : List.of(CommandId.INDENT, CommandId.OUTDENT)) {
				RibbonCommandResult readOnlyHierarchy = harness.frame.routeTaskCommand(command);
				assertEquals(RibbonCommandResult.Status.REJECTED, readOnlyHierarchy.status(), command.toString());
				assertEquals("document-read-only", readOnlyHierarchy.reason(), command.toString());
				assertEquals(List.of(harness.secondTask.getUniqueId()), readOnlyHierarchy.selectedTaskIds(), command.toString());
				assertTrue(readOnlyHierarchy.affectedTaskIds().isEmpty(), command.toString());
				assertEquals(harness.frame.getTopViewId(), readOnlyHierarchy.activeViewId(), command.toString());
				assertEquals(readOnlyRowsBefore, readOnlySheet.getRowCount(), command.toString());
				assertSame(parentBeforeReadOnlyHierarchy, harness.secondTask.getWbsParentTask(), command.toString());
			}
			harness.frame.setSelection(List.of(readOnlyNode));
			assertEquals(RibbonCommandResult.Status.REJECTED,
				harness.frame.routeTaskCommand(CommandId.TASK_MODE_MANUAL).status(),
				"Task mode execution must not modify a read-only selected task");
		});
	}

	@Test
	void taskModeButtonsRequireAnEditableTaskSelection() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		DefaultSubProj readOnlySubproject = new DefaultSubProj(harness.project, 775L);
		readOnlySubproject.setName("Read-only task mode row");
		harness.project.connectTask(readOnlySubproject);
		harness.project.getTaskOutlines().addToAll(readOnlySubproject, null);
		Node readOnlyNode = NodeFactory.getInstance().createNode(readOnlySubproject);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(harness.taskNode));
			harness.manager.setButtonState(harness.task, harness.project);
			Action manualAction = harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_MANUAL);
			assertTrue(manualAction.isEnabled());
			manualAction.actionPerformed(new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED,
				MenuActionConstants.ACTION_TASK_MODE_MANUAL));
			assertEquals(RibbonCommandResult.Status.CHANGED,
				manualAction.getValue(RibbonCommandResult.STATUS_ACTION_PROPERTY),
				"Task Mode must publish its semantic command outcome for diagnostics");
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_AUTOMATIC).isEnabled());
			harness.frame.setSelection(List.of(readOnlyNode));
			harness.manager.setButtonState(readOnlySubproject, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_MANUAL).isEnabled());
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_TASK_MODE_AUTOMATIC).isEnabled());
		});
	}

	// ----- Task tab commands added for #769 / #453: one shared fixture, one Action per command. -----

	private static Task newTaskSince(Project project, Set<Task> existing) {
		return project.getTaskList().stream().filter(candidate -> !existing.contains(candidate)).findFirst().orElseThrow();
	}

	private static Set<Task> identitySnapshot(Project project) {
		Set<Task> snapshot = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		snapshot.addAll(project.getTaskList());
		return snapshot;
	}

	private static Project mpoRoundTrip(Project source) throws Exception {
		java.io.ByteArrayOutputStream encoded = new java.io.ByteArrayOutputStream();
		assertTrue(new com.microproject.exchange.MpoFileImporter().saveProject(source, encoded), "MPO save must succeed");
		try (var input = new java.io.ByteArrayInputStream(encoded.toByteArray())) {
			return new com.microproject.exchange.MpoFileImporter().loadProject(input);
		}
	}

	private static Task findByName(Project project, String name) {
		return project.getTaskList().stream().filter(candidate -> name.equals(candidate.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("task not found after reload: " + name));
	}

	private static RibbonCommandResult.Status statusOf(Harness harness, String actionId) {
		return (RibbonCommandResult.Status) harness.manager.getAction(actionId)
			.getValue(RibbonCommandResult.STATUS_ACTION_PROPERTY);
	}

	/** Every disabled precondition shared by the Task-tab commands that mutate selected rows. */
	private static void assertTaskTabCommandsDisabledWithoutEditableSelection(Harness harness, List<String> actionIds)
			throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(null);
			harness.manager.setButtonState(null, harness.project);
			for (String actionId : actionIds) {
				if (MenuActionConstants.ACTION_INSERT_MILESTONE.equals(actionId))
					continue; // Milestone may insert at the current row without a selection.
				assertFalse(harness.manager.getAction(actionId).isEnabled(), actionId + " needs a selection");
			}
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 0);
			harness.project.setReadOnly(true);
			harness.manager.setButtonState(harness.task, harness.project);
			for (String actionId : actionIds)
				assertFalse(harness.manager.getAction(actionId).isEnabled(), actionId + " must be disabled when read-only");
			harness.project.setReadOnly(false);
		});
	}

	@Test
	void percentCompleteButtonsSetSelectedTasksWithOneUndoEditAndSurviveMpoReload() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		double[] percents = { 25, 50, 75, 100, 0 };
		for (double percent : percents) {
			String actionId = "PercentComplete" + (int) percent;
			SwingUtilities.invokeAndWait(() -> {
				harness.undoController.clear();
				harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
				harness.manager.setButtonState(harness.task, harness.project);
				Action action = harness.manager.getAction(actionId);
				assertTrue(action.isEnabled(), actionId);
				double before = harness.task.getPercentComplete();
				action.actionPerformed(new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED, actionId));
				assertEquals(RibbonCommandResult.Status.CHANGED, statusOf(harness, actionId), actionId);
				assertEquals(percent / 100D, harness.task.getPercentComplete(), 1e-6);
				assertEquals(percent / 100D, harness.secondTask.getPercentComplete(), 1e-6);
				harness.undoController.undo();
				assertEquals(before, harness.task.getPercentComplete(), 1e-6, "one Undo reverts both tasks");
				assertEquals(before, harness.secondTask.getPercentComplete(), 1e-6);
				assertFalse(harness.undoController.canUndo(), "the command must be exactly one Undo edit");
				harness.undoController.redo();
				assertEquals(percent / 100D, harness.task.getPercentComplete(), 1e-6);
				assertEquals(percent / 100D, harness.secondTask.getPercentComplete(), 1e-6);
			});
		}
		// Persist an intermediate value and verify it reloads from MPO.
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			harness.manager.setButtonState(harness.task, harness.project);
			harness.manager.getAction("PercentComplete50").actionPerformed(
				new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED, "PercentComplete50"));
		});
		Project reloaded = mpoRoundTrip(harness.project);
		assertEquals(0.5D, findByName(reloaded, "Ribbon Task").getPercentComplete(), 1e-6);
		assertEquals(0.5D, findByName(reloaded, "Ribbon Task 2").getPercentComplete(), 1e-6);

		assertTaskTabCommandsDisabledWithoutEditableSelection(harness, List.of("PercentComplete0",
			"PercentComplete25", "PercentComplete50", "PercentComplete75", "PercentComplete100"));
	}

	/** Commands that resolve their targets through the shared editable-task boundary (#978). */
	private static final List<CommandId> SHARED_PRECONDITION_COMMANDS = List.of(CommandId.PERCENT_COMPLETE_50,
		CommandId.TASK_MODE_MANUAL, CommandId.INACTIVATE_TASK, CommandId.RESPECT_LINKS, CommandId.MARK_ON_TRACK,
		CommandId.LINK, CommandId.UNLINK, CommandId.RESOURCE_LEVEL_SELECTION,
		CommandId.RESOURCE_CLEAR_LEVEL_SELECTION, CommandId.RESOURCE_LEVEL_ALL, CommandId.RESOURCE_CLEAR_LEVEL_ALL);

	@Test
	void taskCommandFamiliesShareOneRejectionReasonForDocumentAndSelectionPreconditions() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setExecuteLinkForSelectionTest(true);
			harness.frame.setExecuteUnlinkForSelectionTest(true);
			List<Long> bothIds = List.of(harness.task.getUniqueId(), harness.secondTask.getUniqueId());
			// Read-only wins over an empty selection, for every family.
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(null);
			harness.project.setReadOnly(true);
			for (CommandId command : SHARED_PRECONDITION_COMMANDS)
				assertRejected(harness, command, "document-read-only", List.of());
			// Read-only reports the selected ids too.
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			for (CommandId command : SHARED_PRECONDITION_COMMANDS)
				assertRejected(harness, command, "document-read-only", bothIds);
			// Writable document with nothing selected: selection-scoped commands agree on one reason.
			harness.project.setReadOnly(false);
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(null);
			for (CommandId command : SHARED_PRECONDITION_COMMANDS) {
				if (command == CommandId.RESOURCE_LEVEL_ALL || command == CommandId.RESOURCE_CLEAR_LEVEL_ALL)
					continue; // document-scoped: no selection required
				assertRejected(harness, command, "no-selection", List.of());
			}
		});
	}

	private static void assertRejected(Harness harness, CommandId command, String reason, List<Long> selectedIds) {
		RibbonCommandResult result = harness.frame.routeTaskCommand(command);
		assertEquals(RibbonCommandResult.Status.REJECTED, result.status(), command.toString());
		assertEquals(reason, result.reason(), command.toString());
		assertEquals(selectedIds, result.selectedTaskIds(), command.toString());
		assertTrue(result.affectedTaskIds().isEmpty(), command.toString());
		assertEquals(harness.frame.getTopViewId(), result.activeViewId(), command.toString());
	}

	@Test
	void persistedStateTaskCommandsAreBlockedByCollaborationLockWithoutMutationOrUndo() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		List<List<Task>> requestedLocks = new ArrayList<>();
		CollaborationSession session = new CollaborationSession(harness.project,
				System.getProperty("java.io.tmpdir") + "/microproject-task-command-lock-test.mpo", "lock-test") {
			@Override
			public Task tryAcquireTasks(Iterable<Task> tasks) {
				List<Task> requested = new ArrayList<>();
				if (tasks != null) tasks.forEach(requested::add);
				requestedLocks.add(requested);
				return requested.isEmpty() ? null : requested.get(0);
			}

			@Override
			public String describeLockOwner(Task task) {
				return "lock test";
			}
		};
		harness.project.setCollaborationSession(session);
		List<Long> bothIds = List.of(harness.task.getUniqueId(), harness.secondTask.getUniqueId());
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			harness.undoController.clear();
			double percentBefore = harness.task.getPercentComplete();
			boolean manualBefore = harness.task.isManuallyScheduled();
			boolean inactiveBefore = harness.task.isInactiveTask();
			for (CommandId command : List.of(CommandId.PERCENT_COMPLETE_50, CommandId.TASK_MODE_MANUAL,
					CommandId.INACTIVATE_TASK, CommandId.RESPECT_LINKS, CommandId.MARK_ON_TRACK)) {
				requestedLocks.clear();
				assertRejected(harness, command, "collaboration-lock-denied", bothIds);
				assertEquals(List.of(List.of(harness.task, harness.secondTask)), requestedLocks,
					command + " must lock exactly its selected target tasks");
				assertFalse(harness.undoController.canUndo(), command + " must not post an Undo edit when denied");
			}
			assertEquals(percentBefore, harness.task.getPercentComplete(), 1e-6);
			assertEquals(manualBefore, harness.task.isManuallyScheduled());
			assertEquals(inactiveBefore, harness.task.isInactiveTask());
		});
	}

	@Test
	void insertMilestoneAddsOneZeroDurationTaskAboveTheSelectionAsOneUndoEdit() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		Set<Task> existing = identitySnapshot(harness.project);
		var taskModel = harness.project.getTaskModel();
		Object root = taskModel.getRoot();
		int beforeNodes = taskModel.getChildCount(root);
		SwingUtilities.invokeAndWait(() -> {
			harness.undoController.clear();
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(1, 1);
			harness.manager.setButtonState(harness.secondTask, harness.project);
			Action action = harness.manager.getAction(MenuActionConstants.ACTION_INSERT_MILESTONE);
			assertTrue(action.isEnabled());
			action.actionPerformed(new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED,
				MenuActionConstants.ACTION_INSERT_MILESTONE));
			assertEquals(RibbonCommandResult.Status.CHANGED, statusOf(harness, MenuActionConstants.ACTION_INSERT_MILESTONE));
			Task milestone = newTaskSince(harness.project, existing);
			assertTrue(milestone.isMilestone());
			assertEquals(beforeNodes + 1, taskModel.getChildCount(root));
			assertSame(milestone, ((Node) taskModel.getChild(root, 1)).getImpl(), "inserted directly above the selected row");
			assertSame(harness.secondTask, ((Node) taskModel.getChild(root, 2)).getImpl());
			harness.undoController.undo();
			assertEquals(beforeNodes, taskModel.getChildCount(root), "one Undo removes the milestone");
			assertFalse(harness.undoController.canUndo(), "the command must be exactly one Undo edit");
			harness.undoController.redo();
			assertEquals(beforeNodes + 1, taskModel.getChildCount(root));
			assertTrue(milestone.isMilestone(), "Redo keeps zero duration");
		});
		Project reloaded = mpoRoundTrip(harness.project);
		assertEquals(harness.project.getTaskList().stream().filter(Task::isMilestone).count(),
			reloaded.getTaskList().stream().filter(Task::isMilestone).count(), "milestone survives MPO reload");
		assertTaskTabCommandsDisabledWithoutEditableSelection(harness, List.of(MenuActionConstants.ACTION_INSERT_MILESTONE));
	}

	@Test
	void insertSummaryAddsATaskAboveAndIndentsTheSelectionAsOneUndoEdit() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		Set<Task> existing = identitySnapshot(harness.project);
		var taskModel = harness.project.getTaskModel();
		Object root = taskModel.getRoot();
		int beforeNodes = taskModel.getChildCount(root);
		SwingUtilities.invokeAndWait(() -> {
			harness.undoController.clear();
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(0, 1);
			harness.manager.setButtonState(harness.task, harness.project);
			Action action = harness.manager.getAction(MenuActionConstants.ACTION_INSERT_SUMMARY);
			assertTrue(action.isEnabled());
			action.actionPerformed(new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED,
				MenuActionConstants.ACTION_INSERT_SUMMARY));
			assertEquals(RibbonCommandResult.Status.CHANGED, statusOf(harness, MenuActionConstants.ACTION_INSERT_SUMMARY),
				String.valueOf(action.getValue(RibbonCommandResult.REASON_ACTION_PROPERTY)));
			Task summary = newTaskSince(harness.project, existing);
			assertSame(summary, harness.task.getWbsParentTask());
			assertSame(summary, harness.secondTask.getWbsParentTask());
			assertTrue(summary.isWbsParent());
			assertEquals(beforeNodes - 1, taskModel.getChildCount(root));
			assertSame(summary, ((Node) taskModel.getChild(root, 0)).getImpl(), "summary inserted above the selection");
			harness.undoController.undo();
			assertEquals(beforeNodes, taskModel.getChildCount(root), "one Undo removes the summary and the indent");
			assertNull(harness.task.getWbsParentTask());
			assertNull(harness.secondTask.getWbsParentTask());
			assertFalse(harness.undoController.canUndo(), "the command must be exactly one Undo edit");
			harness.undoController.redo();
			assertSame(summary, harness.task.getWbsParentTask());
			assertSame(summary, harness.secondTask.getWbsParentTask());
		});
		Project reloaded = mpoRoundTrip(harness.project);
		Task reloadedParent = findByName(reloaded, "Ribbon Task").getWbsParentTask();
		assertNotNull(reloadedParent, "hierarchy survives MPO reload");
		assertSame(reloadedParent, findByName(reloaded, "Ribbon Task 2").getWbsParentTask());
		assertTaskTabCommandsDisabledWithoutEditableSelection(harness, List.of(MenuActionConstants.ACTION_INSERT_SUMMARY));
	}

	@Test
	void clearSelectAllAndTaskDetailsReuseTheExistingActions() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals("ClearContents", harness.actionId("RibbonClear"));
			assertEquals("SelectAll", harness.actionId("RibbonSelectAll"));
			assertSame(harness.manager.getAction(harness.actionId("RibbonDetails")),
				harness.manager.getAction(harness.actionId("RibbonTaskDetails")),
				"Task > Properties > Details and View > Details are one Action");
			ActionMap shortcuts = new ActionMap();
			harness.manager.applyMicrosoftShortcuts(new InputMap(), shortcuts);
			assertSame(harness.manager.getAction("ClearContents"), shortcuts.get(MenuActionConstants.ACTION_CLEAR_CONTENTS),
				"Ctrl+Delete and the ribbon button share one Action instance");
			assertSame(harness.manager.getAction("SelectAll"), shortcuts.get(MenuActionConstants.ACTION_SELECT_ALL),
				"Ctrl+Shift+Space and the ribbon button share one Action instance");

			SpreadSheet sheet = harness.frame.getTopSpreadSheet();
			sheet.clearSelection();
			harness.manager.setButtonState(null, harness.project);
			Action selectAll = harness.manager.getAction("SelectAll");
			assertTrue(selectAll.isEnabled());
			selectAll.actionPerformed(new ActionEvent(harness.frame, ActionEvent.ACTION_PERFORMED, "SelectAll"));
			assertEquals(sheet.getRowCount(), sheet.getSelectedRowCount());
			assertTrue(harness.manager.getAction("ClearContents").isEnabled());
			harness.project.setReadOnly(true);
			harness.manager.setButtonState(null, harness.project);
			assertFalse(harness.manager.getAction("ClearContents").isEnabled(), "Clear is disabled for a read-only project");
			harness.project.setReadOnly(false);
		});
	}

	@Test
	void inactivateTaskRibbonRouteUsesSelectedRowsAndOneUndoEdit() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		harness.undoController.clear();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(harness.taskNode));
			harness.manager.setButtonState(harness.task, harness.project);
			Action action = harness.manager.getAction(MenuActionConstants.ACTION_INACTIVATE_TASK);
			assertTrue(action.isEnabled());
			RibbonCommandResult firstOutcome = harness.frame.routeTaskCommand(CommandId.INACTIVATE_TASK);
			assertEquals(RibbonCommandResult.Status.CHANGED, firstOutcome.status());
			assertEquals(List.of(harness.task.getUniqueId()), firstOutcome.selectedTaskIds());
			assertEquals(List.of(harness.task.getUniqueId()), firstOutcome.affectedTaskIds());
			assertTrue(harness.task.isInactiveTask());
			assertTrue(harness.undoController.canUndo());
			harness.undoController.undo();
			assertFalse(harness.task.isInactiveTask());
			harness.undoController.redo();
			assertTrue(harness.task.isInactiveTask());
			harness.manager.setButtonState(harness.task, harness.project);
			assertTrue(action.isEnabled(), "the command remains enabled so an all-inactive selection can be reactivated");
			RibbonCommandResult secondOutcome = harness.frame.routeTaskCommand(CommandId.INACTIVATE_TASK);
			assertEquals(RibbonCommandResult.Status.CHANGED, secondOutcome.status());
			assertEquals(List.of(harness.task.getUniqueId()), secondOutcome.selectedTaskIds());
			assertEquals(List.of(harness.task.getUniqueId()), secondOutcome.affectedTaskIds());
			assertFalse(harness.task.isInactiveTask(), "an all-inactive selection must reactivate");
			harness.undoController.undo();
			assertTrue(harness.task.isInactiveTask());
			assertFalse(harness.secondTask.isInactiveTask());
			harness.frame.setSelection(List.of(harness.taskNode, harness.secondTaskNode));
			assertEquals(RibbonCommandResult.Status.CHANGED,
				harness.frame.routeTaskCommand(CommandId.INACTIVATE_TASK).status());
			assertTrue(harness.task.isInactiveTask());
			assertTrue(harness.secondTask.isInactiveTask(), "mixed selection should converge to inactive");
		});
	}

	@Test
	void respectLinksOutcomeRetainsSelectedTaskIds() throws Exception {
		Harness harness = newHarness();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(harness.taskNode));
			RibbonCommandResult result = harness.frame.routeTaskCommand(CommandId.RESPECT_LINKS);
			List<Long> selectedIds = List.of(harness.task.getUniqueId());
			assertEquals(selectedIds, result.selectedTaskIds());
			assertEquals(selectedIds, result.affectedTaskIds());
			assertEquals(harness.frame.getTopViewId(), result.activeViewId());
		});
	}

	@Test
	void linkEnablementAndExecutionRequireTwoEditableTasks() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		DefaultSubProj readOnlySubproject = new DefaultSubProj(harness.project, 774L);
		readOnlySubproject.setName("Read-only link row");
		harness.project.connectTask(readOnlySubproject);
		harness.project.getTaskOutlines().addToAll(readOnlySubproject, null);
		Node readOnlyNode = NodeFactory.getInstance().createNode(readOnlySubproject);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(readOnlyNode, harness.secondTaskNode));
			harness.frame.setExecuteLinkForSelectionTest(true);
			harness.manager.setButtonState(harness.secondTask, harness.project);
			assertTrue(harness.frame.hasTaskSelection(false, 2, true),
				"the unfiltered selection demonstrates why the link enablement was wrong");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_LINK).isEnabled(),
				"Link must require two editable tasks, not count a read-only subproject task");
			RibbonCommandResult result = harness.frame.routeTaskCommand(CommandId.LINK);
			assertEquals(RibbonCommandResult.Status.REJECTED, result.status(),
				"Link execution must use the same editable task selection as enablement");
			assertEquals("task-read-only", result.reason());
			assertEquals(List.of(readOnlySubproject.getUniqueId(), harness.secondTask.getUniqueId()), result.selectedTaskIds());
			assertTrue(result.affectedTaskIds().isEmpty());
		});
	}

	@Test
	void taskAssignmentCommandsUseTheEditableSelectionInsteadOfStaleLeadImpl() throws Exception {
		Harness harness = newHarness();
		DefaultSubProj readOnlySubproject = new DefaultSubProj(harness.project, 772L);
		readOnlySubproject.setName("Read-only assignment row");
		harness.project.connectTask(readOnlySubproject);
		harness.project.getTaskOutlines().addToAll(readOnlySubproject, null);
		Node readOnlyNode = NodeFactory.getInstance().createNode(readOnlySubproject);

		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(List.of(readOnlyNode));
			harness.manager.setButtonState(harness.task, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_ASSIGN_RESOURCES).isEnabled(),
				"Assign Resources must not inherit writability from a stale task lead node");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_DELEGATE_TASKS).isEnabled(),
				"Delegate Tasks must be disabled when the selected task is not editable");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_UPDATE_TASKS).isEnabled(),
				"Update Tasks must be disabled when the selected task is not editable");
		});
	}

	@Test
	void linkedProjectCommandsFollowTheCurrentTaskRowInsteadOfStaleLeadImpl() throws Exception {
		Harness harness = newHarness();
		DefaultSubProj staleSubproject = new DefaultSubProj(harness.project, 773L);
		staleSubproject.setName("Stale linked project");
		harness.project.connectTask(staleSubproject);
		harness.project.getTaskOutlines().addToAll(staleSubproject, null);

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = harness.frame.getTopSpreadSheet();
			sheet.setRowSelectionInterval(0, 0);
			harness.frame.setSelection(List.of(harness.taskNode));
			harness.manager.setButtonState(staleSubproject, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_OPEN_SUBPROJECT).isEnabled(),
				"Open Subproject must follow the selected task rather than the stale lead node");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_REMOVE_SUBPROJECT).isEnabled(),
				"Remove Subproject must follow the selected task rather than the stale lead node");
		});
	}

	@Test
	void taskHierarchyCommandsStayDisabledForResourceSelection() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(false, true);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.activateView(MenuActionConstants.ACTION_RESOURCES);
			harness.frame.getResourceView().getSpreadSheet().setRowSelectionInterval(0, 0);
			harness.frame.setSelection(List.of(harness.resourceNode));
			harness.manager.setButtonState(harness.resource, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_INDENT).isEnabled(),
				"Indent must follow the task-only selection used by its execution path");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_OUTDENT).isEnabled(),
				"Outdent must follow the task-only selection used by its execution path");
		});
	}

	@Test
	void scrollToTaskRequiresTaskSelectionInsteadOfStaleTaskLeadImpl() throws Exception {
		Harness harness = newHarness();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(harness.projectNode));
			harness.manager.setButtonState(harness.task, harness.project);
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_SCROLL_TO_TASK).isEnabled(),
				"Scroll to Task must use the task-selection precondition used by its action");
		});
	}

	@Test
	void clipboardRoutesAcceptSelectedResourceRows() throws Exception {
		Harness harness = newHarness();
		AtomicReference<RibbonCommandResult> copyResult = new AtomicReference<>();
		AtomicReference<RibbonCommandResult> cutResult = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.activateView(MenuActionConstants.ACTION_RESOURCES);
			harness.frame.getResourceView().getSpreadSheet().setRowSelectionInterval(0, 0);
			harness.frame.setSelection(List.of(harness.resourceNode));
			harness.manager.setButtonState(null, harness.project);
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_COPY).isEnabled(),
				"Copy is exposed by the resource sheet for a selected resource row");
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_CUT).isEnabled(),
				"Cut is exposed by the writable resource sheet for a selected resource row");
			copyResult.set(harness.frame.routeTaskCommand(CommandId.COPY));
			cutResult.set(harness.frame.routeTaskCommand(CommandId.CUT));
		});
		assertEquals(RibbonCommandResult.Status.DISPATCHED, copyResult.get().status(),
			"Copy must accept the active sheet's non-task row selection");
		assertNotEquals(RibbonCommandResult.Status.REJECTED, cutResult.get().status(),
			"Cut must accept the active sheet's writable non-task row selection");
		assertEquals(1, harness.frame.copyCallCount());
		assertEquals(1, harness.frame.cutCallCount());
	}

	@Test
	void informationRibbonEnablementFollowsTheSelectedResourceWhenLeadImplIsStale() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, true);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.activateView(MenuActionConstants.ACTION_RESOURCES);
			harness.frame.getResourceView().getSpreadSheet().setRowSelectionInterval(0, 0);
			harness.frame.setSelection(List.of(harness.resourceNode));
			harness.manager.setButtonState(harness.task, harness.project);
			assertFalse(harness.manager.getAction("RibbonTaskInformation").isEnabled(),
				"Task Information must not remain enabled from a stale task lead node");
			assertTrue(harness.manager.getAction("RibbonResourceInformation").isEnabled(),
				"Resource Information must follow the selected resource");
		});
	}

	@Test
	void informationEnablementUsesOneTypedSelectionSnapshotForEverySelectionKind() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			harness.selectSingle(harness.taskNode);
			harness.manager.setButtonState(harness.task, harness.project);
			assertInformationButtons(harness, true, true, false);

			harness.selectSingle(harness.assignmentNode);
			harness.manager.setButtonState(harness.assignment, harness.project);
			assertInformationButtons(harness, true, true, false);

			harness.setTaskInformation(false, true);
			harness.selectSingle(harness.resourceNode);
			harness.manager.setButtonState(harness.resource, harness.project);
			assertInformationButtons(harness, true, false, true);

			harness.selectSingle(harness.assignmentNode);
			harness.manager.setButtonState(harness.assignment, harness.project);
			assertInformationButtons(harness, true, false, true);

			harness.setTaskInformation(true, true);
			harness.selectSingle(harness.projectNode);
			harness.manager.setButtonState(harness.project, harness.project);
			assertInformationButtons(harness, true, false, false);

			harness.selectMultiple(harness.taskNode, harness.resourceNode);
			harness.manager.setButtonState(harness.task, harness.project);
			assertInformationButtons(harness, false, false, false);

			harness.selectSingle(harness.taskNode);
			SpreadSheet sheet = harness.frame.getActiveSpreadSheet();
			sheet.setRowSelectionInterval(0, 0);
			setHeaderColumnSelection(sheet, true);
			harness.manager.setButtonState(harness.task, harness.project);
			assertInformationButtons(harness, false, false, false);
		});
	}

	private static void setHeaderColumnSelection(SpreadSheet sheet, boolean active) {
		try {
			var method = sheet.getClass().getSuperclass()
				.getDeclaredMethod("setHeaderColumnSelectionActive", boolean.class);
			method.setAccessible(true);
			method.invoke(sheet, active);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Could not configure the spreadsheet header selection", exception);
		}
	}

	private static void assertInformationButtons(Harness harness, boolean general,
			boolean task, boolean resource) {
		assertEquals(general, harness.manager.getAction(MenuActionConstants.ACTION_INFORMATION).isEnabled(),
			"Information enablement should match the typed selection");
		assertEquals(general, harness.manager.getAction(MenuActionConstants.ACTION_NOTES).isEnabled(),
			"Notes enablement should match the typed selection");
		assertEquals(task, harness.manager.getAction("RibbonTaskInformation").isEnabled(),
			"Task Information enablement should match the typed selection");
		assertEquals(resource, harness.manager.getAction("RibbonResourceInformation").isEnabled(),
			"Resource Information enablement should match the typed selection");
	}

	@Test
	void hideSelectedRemainsEnabledForVirtualGroupRows() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		Node groupNode = NodeFactory.getInstance().createGroup(new NodeGroup(), "Grouped tasks");
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(List.of(groupNode));
			harness.frame.setVisibilitySelectionForTest(List.of(harness.taskNode));
			harness.manager.setButtonState(groupNode.getImpl(), harness.project);
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_HIDE_SELECTED_TASKS).isEnabled(),
				"Hide Selected must use its group-aware visibility resolver for enablement");
		});

		harness.invoke("RibbonHideSelectedTasks");

		assertTrue(harness.task.isHiddenTask(), "execution must hide the group's resolved task member");
		RibbonCommandResult hideResult = harness.manager.getLastRibbonCommandResult();
		assertEquals(RibbonCommandResult.Status.CHANGED, hideResult.status());
		assertNotEquals(hideResult.selectedTaskIds(), hideResult.affectedTaskIds(),
			"the selected group/task context must remain distinct from its changed member task");
		assertEquals(List.of(harness.task.getUniqueId()), hideResult.affectedTaskIds());
		assertEquals(MenuActionConstants.ACTION_GANTT, hideResult.activeViewId());
		harness.undoController.undo();
		assertFalse(harness.task.isHiddenTask(), "the visibility command must retain one undo boundary");
		harness.invoke("RibbonHideSelectedTasks");
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(List.of());
		});
		harness.invoke("RibbonShowAllTasks");
		assertFalse(harness.task.isHiddenTask(), "Show All must restore the hidden task");
		RibbonCommandResult showResult = harness.manager.getLastRibbonCommandResult();
		assertEquals(RibbonCommandResult.Status.CHANGED, showResult.status());
		assertEquals(List.of(), showResult.selectedTaskIds(),
			"Show All must not report changed tasks as selected rows");
		assertEquals(List.of(harness.task.getUniqueId()), showResult.affectedTaskIds());
		assertEquals(MenuActionConstants.ACTION_GANTT, showResult.activeViewId());
	}

	@Test
	void visibilityCommandsAcquireLocksForEveryTaskTheyWillChange() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		List<List<Task>> requestedLocks = new ArrayList<>();
		CollaborationSession session = new CollaborationSession(harness.project,
				System.getProperty("java.io.tmpdir") + "/microproject-visibility-lock-test.mpo", "visibility-test") {
			@Override
			public Task tryAcquireTasks(Iterable<Task> tasks) {
				List<Task> requested = new ArrayList<>();
				if (tasks != null) tasks.forEach(requested::add);
				requestedLocks.add(requested);
				return null;
			}
		};
		harness.project.setCollaborationSession(session);
		SwingUtilities.invokeAndWait(() -> harness.frame.setVisibilitySelectionForTest(List.of(harness.taskNode)));

		harness.invoke("RibbonHideSelectedTasks");
		assertTrue(harness.task.isHiddenTask());
		assertEquals(List.of(harness.task), requestedLocks.getLast(),
				"Hide must lock the exact tasks its visibility mutation will change");

		harness.invoke("RibbonShowAllTasks");
		assertFalse(harness.task.isHiddenTask());
		assertEquals(List.of(harness.task), requestedLocks.getLast(),
				"Show All must lock the exact hidden tasks its visibility mutation will change");
	}

	@Test
	void rejectedHideSelectedOutcomeIncludesSelectionAndActiveView() throws Exception {
		Harness harness = newHarness();
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setSelection(List.of(harness.taskNode));
			harness.project.setReadOnly(true);
			RibbonCommandResult result = harness.frame.routeTaskCommand(CommandId.HIDE_SELECTED);
			assertEquals(RibbonCommandResult.Status.REJECTED, result.status());
			assertEquals("document-read-only", result.reason());
			assertEquals(List.of(harness.task.getUniqueId()), result.selectedTaskIds());
			assertTrue(result.affectedTaskIds().isEmpty());
			assertEquals(harness.frame.getTopViewId(), result.activeViewId());
		});
	}

	@Test
	void insertResourceRouteUsesTheResourceSheetNewCommand() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(false, true);
		harness.undoController.clear();
		int taskCountBefore = count(harness.project.getTaskOutlineIterator());
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.activateView(MenuActionConstants.ACTION_RESOURCES);
			assertNotNull(harness.frame.getResourceView().getSpreadSheet());
			assertTrue(Arrays.asList(harness.frame.getResourceView().getSpreadSheet().getActionList())
				.contains(MenuActionConstants.ACTION_INSERT_RESOURCE));
			assertNotNull(harness.frame.getResourceView().getSpreadSheet()
				.prepareAction(MenuActionConstants.ACTION_INSERT_RESOURCE));
			harness.frame.getResourceView().getSpreadSheet().setRowSelectionInterval(0, 0);
			harness.manager.setButtonState(harness.resource, harness.project);
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_INSERT_RESOURCE).isEnabled(),
				"Insert Resource must be enabled for a writable resource pool");
		});

		harness.invoke("RibbonInsertResource");

		assertTrue(harness.undoController.canUndo(),
			"Insert Resource must create an undoable resource-sheet row");
		assertEquals(taskCountBefore, count(harness.project.getTaskOutlineIterator()),
			"Insert Resource must not insert a task row");
	}

	@Test
	void moveProjectCommandMutatesTheScheduleAndPostsOneUndoUnit() throws Exception {
		Harness harness = newHarness();
		harness.undoController.clear();
		long before = harness.project.getStartDate();
		long requested = harness.project.getEffectiveWorkCalendar().add(before, 5L * 24L * 60L * 60L * 1000L, false);

		SwingUtilities.invokeAndWait(() -> assertTrue(harness.frame.moveProject(requested)));
		long moved = harness.project.getStartDate();
		assertNotEquals(before, moved);
		assertTrue(harness.project.isDirty());
		assertTrue(harness.undoController.canUndo());

		harness.undoController.undo();
		assertEquals(before, harness.project.getStartDate());
		harness.undoController.redo();
		assertEquals(moved, harness.project.getStartDate());
	}

	private static int count(Iterator<?> iterator) {
		int count = 0;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		return count;
	}

	@Test
	void baselineRibbonButtonIsEnabledAndWritesASnapshotWhenClicked() throws Exception {
		Harness harness = newHarness();
		harness.manager.getMenuManager().createRibbonPanel(MenuManager.STANDARD_RIBBON, () -> { });
		harness.manager.setButtonState(null, harness.project);
		AbstractButton button = harness.manager.getMenuManager().getToolButtonsFromId("RibbonSaveBaseline").stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.findFirst()
			.orElseThrow(() -> new AssertionError("RibbonSaveBaseline button was not created"));

		assertTrue(button.isEnabled(), "Baseline save must be enabled for a writable project");
		SwingUtilities.invokeAndWait(button::doClick);

		assertEquals(1, harness.frame.baselineDialogCallCount(true));
		assertNotNull(harness.task.getSnapshot(Snapshottable.BASELINE),
			"Clicking the ribbon button must write the baseline snapshot");
	}

	@Test
	void fileHelpAndChooserRoutesFireExactlyOnce() throws Exception {
		Harness harness = newHarness();

		assertExternal(harness, "RibbonNewProject", "newProject");
		assertExternal(harness, "RibbonNewMasterProject", "newMasterProject");
		assertExternal(harness, "RibbonOpenProject", "openProject");
		assertExternal(harness, "RibbonRecentProjects", "openProject");
		assertExternal(harness, "RibbonImportProject", "openProject");
		assertExternal(harness, "RibbonSaveProject", "saveProject");
		assertExternal(harness, "RibbonTopBarSaveProject", "saveProject");
		assertExternal(harness, "RibbonSaveProjectAs", "saveAsProject");
		assertExternal(harness, "RibbonSaveMpoAs", "saveMpoAs");
		assertExternal(harness, "RibbonExportProject", "saveAsProject");
		assertExternal(harness, "RibbonCloseProject", "closeProject");
		assertExternal(harness, "RibbonPrint", "print");
		assertExternal(harness, "RibbonPrintPreview", "printPreview");
		assertExternal(harness, "RibbonPDF", "pdf");
		assertExternal(harness, "RibbonLocale", "locale");
		assertExternal(harness, "RibbonMicroProjectDocumentation", "help");
		assertExternal(harness, "RibbonAboutMicroProject", "about");
		assertExternal(harness, "RibbonInsertProject", "insertProject");

		assertChooser(harness, "RibbonChooseFilter", MenuActionConstants.ACTION_CHOOSE_FILTER);
		assertChooser(harness, "RibbonChooseSort", MenuActionConstants.ACTION_CHOOSE_SORT);
		assertChooser(harness, "RibbonChooseGroup", MenuActionConstants.ACTION_CHOOSE_GROUP);
		assertChooser(harness, "RibbonTimescale", MenuActionConstants.ACTION_TIMESCALE);
		assertChooser(harness, "RibbonBarStyles", MenuActionConstants.ACTION_BAR_STYLES);
		assertChooser(harness, "RibbonTextStyles", MenuActionConstants.ACTION_TEXT_STYLES);
		assertChooser(harness, "RibbonLayout", MenuActionConstants.ACTION_LAYOUT);
	}

	@Test
	void restoredCommandsReachTheirExistingImplementations() throws Exception {
		Harness harness = newHarness();
		harness.selectSingle(harness.taskNode);

		harness.invoke("RibbonDelegateTasks");
		assertEquals(1, harness.frame.structuralCallCount("RibbonDelegateTasks"));

		boolean previousTeamOnly = harness.manager.getPreferences().isShowProjectResourcesOnly();
		try {
			harness.invoke("RibbonTeamFilter");
			assertEquals(!previousTeamOnly, harness.manager.getPreferences().isShowProjectResourcesOnly());
			assertEquals(!previousTeamOnly,
				harness.manager.getAction(MenuActionConstants.ACTION_TEAM_FILTER).getValue(Action.SELECTED_KEY));
		} finally {
			harness.manager.getPreferences().setShowProjectResourcesOnly(previousTeamOnly);
		}

		harness.invoke("RibbonRecalculate");
		assertCall(harness, "recalculate", harness.project);
	}

	@Test
	void viewAndToggleRoutesAreInvokableAgainstALiveDocumentContext() throws Exception {
		Harness harness = newHarness();

		assertView(harness, "RibbonGantt", MenuActionConstants.ACTION_GANTT);
		assertView(harness, "RibbonTrackingGantt", MenuActionConstants.ACTION_TRACKING_GANTT);
		assertView(harness, "RibbonNetwork", MenuActionConstants.ACTION_NETWORK);
		assertView(harness, "RibbonWBS", MenuActionConstants.ACTION_WBS);
		assertView(harness, "RibbonResources", MenuActionConstants.ACTION_RESOURCES);
		assertView(harness, "RibbonRBS", MenuActionConstants.ACTION_RBS);
		assertView(harness, "RibbonProjects", MenuActionConstants.ACTION_PROJECTS);
		assertView(harness, "RibbonTaskUsageDetail", MenuActionConstants.ACTION_TASK_USAGE_DETAIL);
		assertView(harness, "RibbonResourceUsageDetail", MenuActionConstants.ACTION_RESOURCE_USAGE_DETAIL);
		assertView(harness, "RibbonReport", MenuActionConstants.ACTION_REPORT);
		assertView(harness, "RibbonHistogram", MenuActionConstants.ACTION_HISTOGRAM);
		assertView(harness, "RibbonCharts", MenuActionConstants.ACTION_CHARTS);
		assertView(harness, "RibbonTaskUsage", MenuActionConstants.ACTION_TASK_USAGE);
		assertView(harness, "RibbonDetails", MenuActionConstants.ACTION_DETAILS);
		assertView(harness, "RibbonResourceUsage", MenuActionConstants.ACTION_RESOURCE_USAGE);
		assertView(harness, "RibbonNoTextNoSubWindow", MenuActionConstants.ACTION_NO_SUB_WINDOW);

		assertDoesNotThrow(() -> harness.invoke("RibbonZoomIn"));
		assertDoesNotThrow(() -> harness.invoke("RibbonZoomOut"));
		assertToggle(harness, "RibbonToggleProgressLine", true);
		assertToggle(harness, "RibbonLabelResourceNames", true);
		assertToggle(harness, "RibbonLabelTaskName", true);
		assertChooser(harness, "RibbonGridlines", MenuActionConstants.ACTION_GRIDLINES);
	}

	@Test
	void levelAllIsEnabledWithoutSelectionAndDoesNotLatchAfterNoChange() throws Exception {
		// Microsoft Project Standard 2024's Resource > Level All applies to all
		// resources/tasks in the plan (document-derived momentary command contract):
		// https://support.microsoft.com/en-us/project/distribute-project-work-evenly-level-resource-assignments
		Harness harness = newHarness();
		harness.selectNone();

		AbstractButton button = harness.manager.getMenuManager().getToolButtonsFromId("RibbonLevelAll")
			.stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).findFirst().orElseThrow();
		assertTrue(button.isEnabled(), "Level All requires a writable document but no selected task");
		harness.invoke("RibbonLevelAll");

		var result = harness.frame.getLastTaskCommandResult();
		assertNotNull(result);
		assertEquals(RibbonCommandResult.Status.NO_CHANGE, result.status());
		assertTrue(result.selectedTaskIds().isEmpty(), "Level All must accept an empty task selection");
		assertFalse(button.isSelected(), "Level All is a momentary command and must never latch");
	}

	@Test
	void legacyTransformIdsShareCanonicalActionsAndChooserRoutes() throws Exception {
		Harness harness = newHarness();
		assertSame(harness.manager.getAction(MenuActionConstants.ACTION_CHOOSE_FILTER),
			harness.manager.getAction(MenuActionConstants.ACTION_FILTER));
		assertSame(harness.manager.getAction(MenuActionConstants.ACTION_CHOOSE_SORT),
			harness.manager.getAction(MenuActionConstants.ACTION_SORT));
		assertSame(harness.manager.getAction(MenuActionConstants.ACTION_CHOOSE_GROUP),
			harness.manager.getAction(MenuActionConstants.ACTION_GROUP));

		harness.resetCalls();
		harness.invokeAction(MenuActionConstants.ACTION_FILTER);
		assertCall(harness, "chooser", MenuActionConstants.ACTION_CHOOSE_FILTER);
		harness.resetCalls();
		harness.invokeAction(MenuActionConstants.ACTION_SORT);
		assertCall(harness, "chooser", MenuActionConstants.ACTION_CHOOSE_SORT);
		harness.resetCalls();
		harness.invokeAction(MenuActionConstants.ACTION_GROUP);
		assertCall(harness, "chooser", MenuActionConstants.ACTION_CHOOSE_GROUP);
	}

	@Test
	void primaryExternalRibbonRoutesRecordRejectedOutcomeWhenGuardBlocksThem() throws Exception {
		Harness harness = newHarness();
		for (String buttonId : List.of("RibbonNewProject", "RibbonRecentProjects", "RibbonLocale",
			"RibbonMicroProjectDocumentation", "RibbonAboutMicroProject")) {
			harness.resetCalls();
			harness.invoke(buttonId);
			assertEquals(RibbonCommandResult.Status.REJECTED,
				harness.manager.getLastRibbonCommandResult().status(), buttonId);
		}
	}

	@Test
	void trackingGanttSwitchPreservesTheTaskTableColumns() throws Exception {
		Harness harness = newHarness();
		ArrayList columnsBefore = harness.frame.getGanttView().getSpreadSheet().getFieldArray();

		activateView(harness, MenuActionConstants.ACTION_TRACKING_GANTT);

		assertSame(columnsBefore, harness.frame.getGanttView().getSpreadSheet().getFieldArray(),
			"Tracking Gantt must not replace the task table column layout");
		assertTrue(harness.frame.getGanttView().isTracking());

		activateView(harness, MenuActionConstants.ACTION_GANTT);

		assertSame(columnsBefore, harness.frame.getGanttView().getSpreadSheet().getFieldArray(),
			"Returning to Gantt must keep the task table column layout");
		assertFalse(harness.frame.getGanttView().isTracking());
	}

	@Test
	void bottomViewTransitionsKeepComponentAndRibbonStateInSync() throws Exception {
		Harness harness = newHarness();

		activateView(harness, MenuActionConstants.ACTION_HISTOGRAM);
		assertBottomView(harness, MenuActionConstants.ACTION_HISTOGRAM, harness.frame.getHistogramView());

		// Re-selecting the active view is idempotent: it must not hide or replace it.
		activateView(harness, MenuActionConstants.ACTION_HISTOGRAM);
		assertBottomView(harness, MenuActionConstants.ACTION_HISTOGRAM, harness.frame.getHistogramView());

		activateView(harness, MenuActionConstants.ACTION_CHARTS);
		assertBottomView(harness, MenuActionConstants.ACTION_CHARTS, harness.frame.getChartView());

		// Switching the top view must not leave the selected bottom view detached.
		activateView(harness, MenuActionConstants.ACTION_GANTT);
		assertBottomView(harness, MenuActionConstants.ACTION_CHARTS, harness.frame.getChartView());

		activateView(harness, MenuActionConstants.ACTION_NO_SUB_WINDOW);
		assertBottomView(harness, MenuActionConstants.ACTION_NO_SUB_WINDOW, null);
		activateView(harness, MenuActionConstants.ACTION_NO_SUB_WINDOW);
		assertBottomView(harness, MenuActionConstants.ACTION_NO_SUB_WINDOW, null);

		activateView(harness, MenuActionConstants.ACTION_TASK_USAGE);
		assertBottomView(harness, MenuActionConstants.ACTION_TASK_USAGE, harness.frame.getTaskUsageView());
		activateView(harness, MenuActionConstants.ACTION_DETAILS);
		assertBottomView(harness, MenuActionConstants.ACTION_NO_SUB_WINDOW, null);
		assertFalse((Boolean) harness.manager.getAction(MenuActionConstants.ACTION_DETAILS)
			.getValue(Action.SELECTED_KEY));
		activateView(harness, MenuActionConstants.ACTION_DETAILS);
		assertBottomView(harness, MenuActionConstants.ACTION_TASK_USAGE, harness.frame.getTaskUsageView());
		assertTrue((Boolean) harness.manager.getAction(MenuActionConstants.ACTION_DETAILS)
			.getValue(Action.SELECTED_KEY));
		WorkspaceSetting workspace = harness.frame.createWorkspace(0);
		assertEquals(MenuActionConstants.ACTION_TASK_USAGE,
			((DocumentFrame.Workspace) workspace).getBottomViewName());
		harness.frame.restoreWorkspace(workspace, 0);
		assertBottomView(harness, MenuActionConstants.ACTION_TASK_USAGE, harness.frame.getTaskUsageView());
	}

	@Test
	void detailsSelectionClearsWhenTheDocumentLosesCommandFocus() throws Exception {
		Harness harness = newHarness();
		activateView(harness, MenuActionConstants.ACTION_TASK_USAGE);
		assertTrue((Boolean) harness.manager.getAction(MenuActionConstants.ACTION_DETAILS)
			.getValue(Action.SELECTED_KEY));

		harness.frame.refreshViewButtons(false);

		assertFalse((Boolean) harness.manager.getAction(MenuActionConstants.ACTION_DETAILS)
			.getValue(Action.SELECTED_KEY));
	}

	private static void assertBottomView(Harness harness, String actionId, BaseView view) {
		assertSame(view, harness.frame.getActiveBottomView());
		assertSame(view, harness.frame.getMainView().getBottomComponent());
		assertEquals(actionId, harness.frame.lastBottomButton);
		for (String bottomAction : List.of(
			MenuActionConstants.ACTION_HISTOGRAM,
			MenuActionConstants.ACTION_CHARTS,
			MenuActionConstants.ACTION_TASK_USAGE,
			MenuActionConstants.ACTION_RESOURCE_USAGE,
			MenuActionConstants.ACTION_NO_SUB_WINDOW)) {
			assertEquals(bottomAction.equals(actionId),
				harness.manager.getAction(bottomAction).getValue(Action.SELECTED_KEY),
				() -> bottomAction + " selection state is inconsistent");
		}
		assertEquals(!MenuActionConstants.ACTION_NO_SUB_WINDOW.equals(actionId),
			harness.manager.getAction(MenuActionConstants.ACTION_DETAILS).getValue(Action.SELECTED_KEY));
	}

	private static void activateView(Harness harness, String viewName) throws Exception {
		SwingUtilities.invokeAndWait(() -> harness.frame.activateView(viewName));
	}

	@Test
	void structuralEditingButtonsAreStillExecutableInALiveFrameContext() throws Exception {
		Harness harness = newHarness();
		harness.selectSingle(harness.taskNode);

		for (String buttonId : List.of(
			"RibbonCut",
			"RibbonCopy",
			"RibbonPaste",
			"RibbonDelete",
			"RibbonTaskModeManual",
			"RibbonTaskModeAutomatic",
			"RibbonMarkOnTrack",
			"RibbonInsert",
			"RibbonInsertResource",
			"RibbonInsertRecurring",
			"RibbonLevelSelection",
			"RibbonClearLevelSelection",
			"RibbonClearLevelAll",
			"RibbonLevelResources",
			"RibbonScrollToTask",
			"RibbonHideSelectedTasks",
			"RibbonShowAllTasks",
			"RibbonIndent",
			"RibbonOutdent",
			"RibbonMoveTaskUp",
			"RibbonMoveTaskDown",
			"RibbonExpand",
			"RibbonCollapse",
			"RibbonLink",
			"RibbonUnlink")) {
			assertDoesNotThrow(() -> harness.invoke(buttonId), () -> buttonId + " should be invokable against a live frame");
			harness.resetCalls();
		}
	}

	@Test
	void pasteRunsLocallyWhenTheActionRouteDoesNotInterceptIt() throws Exception {
		Harness harness = newHarness();
		harness.selectSingle(harness.taskNode);
		harness.manager.setInterceptPaste(false);
		harness.frame.resetStructuralCalls();

		harness.invoke("RibbonPaste");

		assertEquals(1, harness.frame.structuralCallCount("RibbonPaste"));
	}

	@Test
	void taskOnlyStructuralButtonsIgnoreResourceAndMixedSelections() throws Exception {
		Harness harness = newHarness();

		harness.selectSingle(harness.resourceNode);
		for (String buttonId : List.of(
			"RibbonScrollToTask",
			"RibbonIndent",
			"RibbonOutdent",
			"RibbonMoveTaskUp",
			"RibbonMoveTaskDown",
			"RibbonExpand",
			"RibbonCollapse")) {
			harness.frame.resetStructuralCalls();
			harness.invoke(buttonId);
			assertEquals(0, harness.frame.structuralCallCount(buttonId), () -> buttonId + " should ignore a resource-only selection");
		}

		harness.selectMultiple(harness.taskNode, harness.resourceNode);
		for (String buttonId : List.of(
			"RibbonScrollToTask",
			"RibbonIndent",
			"RibbonOutdent",
			"RibbonMoveTaskUp",
			"RibbonMoveTaskDown",
			"RibbonExpand",
			"RibbonCollapse")) {
			harness.frame.resetStructuralCalls();
			harness.invoke(buttonId);
			assertEquals(0, harness.frame.structuralCallCount(buttonId), () -> buttonId + " should ignore a mixed task/resource selection");
		}
	}

	@Test
	void linkAndUnlinkIgnoreNonTaskRowsButStillRunWhenEnoughTasksRemain() throws Exception {
		Harness harness = newHarness();

		harness.selectMultiple(harness.taskNode, harness.resourceNode);
		harness.frame.resetStructuralCalls();
		harness.invoke("RibbonLink");
		assertEquals(0, harness.frame.structuralCallCount("RibbonLink"));

		harness.selectMultiple(harness.taskNode, harness.secondTaskNode, harness.resourceNode);
		harness.frame.resetStructuralCalls();
		harness.invoke("RibbonLink");
		assertEquals(1, harness.frame.structuralCallCount("RibbonLink"));

		harness.selectMultiple(harness.taskNode, harness.secondTaskNode, harness.resourceNode);
		harness.frame.resetStructuralCalls();
		harness.invoke("RibbonUnlink");
		assertEquals(1, harness.frame.structuralCallCount("RibbonUnlink"));
	}

	@Test
	void hierarchyEnablementRejectsMixedSelectionWhileLinkAndUnlinkKeepTheirMixedContract() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.setExecuteIndentForSelectionTest(true);
			harness.frame.getTopSpreadSheet().setRowSelectionInterval(1, 1);
			assertEquals(RibbonCommandResult.Status.CHANGED,
				harness.frame.routeTaskCommand(CommandId.INDENT).status());
			assertSame(harness.task, harness.secondTask.getWbsParentTask(),
				"fixture must make Outdent otherwise applicable to the selected task");

			harness.frame.getTopSpreadSheet().clearSelection();
			harness.frame.setSelection(List.of(harness.taskNode, harness.secondTaskNode, harness.resourceNode));
			harness.setTaskInformation(true, true);
			harness.manager.setButtonState(harness.secondTask, harness.project);

			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_INDENT).isEnabled(),
				"Indent execution rejects mixed task/resource selection, so enablement must reject it too");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_OUTDENT).isEnabled(),
				"Outdent execution rejects mixed task/resource selection even when a selected task has a parent");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_EXPAND).isEnabled(),
				"Expand execution rejects mixed task/resource selection");
			assertFalse(harness.manager.getAction(MenuActionConstants.ACTION_COLLAPSE).isEnabled(),
				"Collapse execution rejects mixed task/resource selection");
			int rowsBeforeExpand = harness.frame.getTopSpreadSheet().getRowCount();
			var parentBeforeExpand = harness.secondTask.getWbsParentTask();
			for (CommandId command : List.of(CommandId.EXPAND, CommandId.COLLAPSE)) {
				RibbonCommandResult hierarchyResult = harness.frame.routeTaskCommand(command);
				assertEquals(RibbonCommandResult.Status.REJECTED, hierarchyResult.status(), command.toString());
				assertEquals("mixed-selection", hierarchyResult.reason(), command.toString());
				assertEquals(List.of(harness.task.getUniqueId(), harness.secondTask.getUniqueId()),
					hierarchyResult.selectedTaskIds(), command.toString());
				assertTrue(hierarchyResult.affectedTaskIds().isEmpty(), command.toString());
				assertEquals(harness.frame.getTopViewId(), hierarchyResult.activeViewId(), command.toString());
				assertEquals(rowsBeforeExpand, harness.frame.getTopSpreadSheet().getRowCount(), command.toString());
				assertSame(parentBeforeExpand, harness.secondTask.getWbsParentTask(), command.toString());
			}
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_LINK).isEnabled(),
				"Link intentionally ignores non-task rows when two editable tasks remain");
			assertTrue(harness.manager.getAction(MenuActionConstants.ACTION_UNLINK).isEnabled(),
				"Unlink intentionally ignores non-task rows when an editable task remains");
		});
	}

	private static void assertExternal(Harness harness, String buttonId, String routeId) throws Exception {
		harness.resetCalls();
		harness.invoke(buttonId);
		assertCall(harness, "external", routeId);
	}

	private static void assertChooser(Harness harness, String buttonId, String chooserId) throws Exception {
		harness.resetCalls();
		harness.invoke(buttonId);
		assertCall(harness, "chooser", chooserId);
	}

	private static void assertView(Harness harness, String buttonId, String viewId) throws Exception {
		harness.resetCalls();
		harness.invoke(buttonId);
		assertCall(harness, "view", viewId);
	}

	private static void assertToggle(Harness harness, String buttonId, boolean hookPresent) throws Exception {
		harness.resetCalls();
		harness.invoke(buttonId);
		if (hookPresent) {
			assertTrue(harness.hasCall("toggle"), () -> buttonId + " should route through the toggle hook");
			assertEquals(1, harness.calls.size(), "Expected exactly one hook call");
		}
	}

	private static void assertCall(Harness harness, String name, Object... args) {
		assertFalse(harness.calls.isEmpty(), "Expected at least one hook call");
		Call call = harness.calls.get(0);
		assertEquals(name, call.name, "Unexpected hook kind");
		if (args.length > 0) {
			assertEquals(Arrays.asList(args), call.args, "Unexpected hook arguments");
		}
		assertEquals(1, harness.calls.size(), "Expected exactly one hook call");
	}

	private static String missing(Set<String> inventory, Set<String> covered) {
		return inventory.stream().filter(id -> !covered.contains(id)).toList().toString();
	}

	private static Map<String, Strategy> coverageTable() {
		LinkedHashMap<String, Strategy> map = new LinkedHashMap<>();
		add(map, Strategy.ROUTE_EXTERNAL,
			"RibbonNewProject",
			"RibbonNewMasterProject",
			"RibbonOpenProject",
			"RibbonRecentProjects",
			"RibbonImportProject",
			"RibbonSaveProject",
			"RibbonTopBarSaveProject",
			"RibbonSaveProjectAs",
			"RibbonSaveMpoAs",
			"RibbonExportProject",
			"RibbonCloseProject",
			"RibbonPrint",
			"RibbonPrintPreview",
			"RibbonPDF",
			"RibbonLocale",
			"RibbonMicroProjectDocumentation",
			"RibbonAboutMicroProject",
			"RibbonInsertProject");
		add(map, Strategy.STRUCTURAL_ONLY,
			"RibbonDelegateTasks",
			"RibbonTeamFilter",
			"RibbonRecalculate");
		add(map, Strategy.ROUTE_DIALOG,
			"RibbonCustomFields",
			"RibbonCustomReport",
			"RibbonTaskInformation",
			"RibbonResourceInformation",
			"RibbonProjectInformation",
			"RibbonBackstageProjectInformation",
			"RibbonBackstageOptions",
			"RibbonNotes",
			"RibbonChangeWorkingTime",
			"RibbonAssignResources",
			"RibbonTimesheet",
			"RibbonFind",
			"RibbonProjectsDialog",
			"RibbonCalendarOptions",
			"RibbonUpdateTasks",
			"RibbonUpdateProject",
			"RibbonStatusDate",
			"RibbonMoveProject",
			"RibbonSaveBaseline",
			"RibbonClearBaseline",
			"RibbonCCPMBufferStatus",
			"RibbonCCPMNetwork");
		add(map, Strategy.ROUTE_VIEW,
			"RibbonTaskInspector",
			"RibbonTimeline",
			"RibbonCalendarView",
			"RibbonGantt",
			"RibbonTrackingGantt",
			"RibbonNetwork",
			"RibbonWBS",
			"RibbonResources",
			"RibbonRBS",
			"RibbonProjects",
			"RibbonTaskUsageDetail",
			"RibbonResourceUsageDetail",
			"RibbonReport",
			"RibbonHistogram",
			"RibbonCharts",
			"RibbonTaskUsage",
			"RibbonDetails",
			"RibbonResourceUsage",
			"RibbonNoTextNoSubWindow");
		add(map, Strategy.VIEW_ONLY, "RibbonZoomSelectedTasks");
		add(map, Strategy.ROUTE_CHOOSER,
			"RibbonChooseFilter",
			"RibbonChooseSort",
			"RibbonChooseGroup",
			"RibbonTimescale",
			"RibbonBarStyles",
			"RibbonTextStyles",
				"RibbonLayout", "RibbonGridlines");
		add(map, Strategy.STATE_TOGGLE,
			"RibbonZoomIn",
			"RibbonZoomOut",
			"RibbonInactivateTask",
			"RibbonToggleProgressLine",
			"RibbonLabelResourceNames",
			"RibbonLabelTaskName",
				"RibbonToggleCriticalChain",
			"RibbonPrivacyMask");
		add(map, Strategy.STRUCTURAL_ONLY,
			"RibbonTopBarUndo",
			"RibbonTopBarRedo",
			"RibbonCut",
			"RibbonCopy",
			"RibbonPaste",
			"RibbonDelete",
			"RibbonInsert",
			"RibbonInsertResource",
			"RibbonInsertRecurring",
			"RibbonArrangeAll",
			"RibbonLevelSelection",
			"RibbonLevelAll",
			"RibbonClearLevelSelection",
			"RibbonClearLevelAll",
			"RibbonLevelResources",
			"RibbonNextOverallocation",
			"RibbonUseResourcePool",
			"RibbonCreateResourcePool",
			"RibbonRefreshResourcePool",
			"RibbonRefreshSubprojects",
			"RibbonOpenSubproject",
			"RibbonRemoveSubproject",
			"RibbonCCPMSettings",
			"RibbonCCPMClear",
			"RibbonTaskModeManual",
			"RibbonTaskModeAutomatic",
			"RibbonStatusDate",
			"RibbonMarkOnTrack",
			"RibbonScrollToTask",
			"RibbonHideSelectedTasks",
			"RibbonShowAllTasks",
			"RibbonIndent",
			"RibbonOutdent",
			"RibbonMoveTaskUp",
			"RibbonMoveTaskDown",
			"RibbonExpand",
			"RibbonCollapse",
			"RibbonLink",
			"RibbonUnlink",
			"RibbonRespectLinks");
		add(map, Strategy.STRUCTURAL_ONLY, "RibbonFormatPainter");
		// Task-tab commands added for #769/#453; behavior is asserted in TaskTabRibbonCommandsTest.
		add(map, Strategy.STRUCTURAL_ONLY,
			"RibbonInsertMilestone", "RibbonInsertSummary", "RibbonClear", "RibbonSelectAll", "RibbonTaskDetails",
			"RibbonPercentComplete0", "RibbonPercentComplete25", "RibbonPercentComplete50",
			"RibbonPercentComplete75", "RibbonPercentComplete100");
		return map;
	}

	private static void add(Map<String, Strategy> map, Strategy strategy, String... ids) {
		for (String id : ids) {
			map.put(id, strategy);
		}
	}

	private static Harness newHarness() throws Exception {
		AtomicReference<Harness> ref = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		boolean previousNewLook = Environment.isNewLook();
		Environment.setNewLook(true);
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					ref.set(new Harness());
				} catch (Throwable t) {
					failure.set(t);
				}
			});
		} finally {
			Environment.setNewLook(previousNewLook);
		}
		if (failure.get() != null) {
			Throwable t = failure.get();
			if (t instanceof Exception e) {
				throw e;
			}
			throw new RuntimeException(t);
		}
		SwingUtilities.invokeAndWait(() -> {
			// flush queued activation work from the frame constructor
		});
		return ref.get();
	}

	private static final class Harness {
		final RecordingGraphicManager manager;
		final TestDocumentFrame frame;
		final Project project;
		final Task task;
		final Task secondTask;
		final Resource resource;
		final Assignment assignment;
		final Node taskNode;
		final Node secondTaskNode;
		final Node resourceNode;
		final Node projectNode;
		final Node assignmentNode;
		final List<Call> calls = new ArrayList<>();
		final DataFactoryUndoController undoController;

		Harness() {
			manager = new RecordingGraphicManager(new JPanel(), calls);
			manager.getMenuManager().createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			undoController = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("Ribbon Test Pool", undoController);
			pool.setLocal(true);
			project = Project.createProject(pool, undoController);
			project.setName("Ribbon Test Project");
			task = project.createScriptedTask();
			task.setName("Ribbon Task");
			secondTask = project.createScriptedTask();
			secondTask.setName("Ribbon Task 2");
			resource = project.getResourcePool().createScriptedResource();
			resource.setName("Ribbon Resource");
			assignment = Assignment.getInstance(task, resource, 1.0, 0);
			taskNode = NodeFactory.getInstance().createNode(task);
			secondTaskNode = NodeFactory.getInstance().createNode(secondTask);
			resourceNode = NodeFactory.getInstance().createNode(resource);
			projectNode = NodeFactory.getInstance().createNode(project);
			assignmentNode = NodeFactory.getInstance().createNode(assignment);
			frame = new TestDocumentFrame(manager, project);
			manager.setCurrentFrame(frame);
			frame.setActive(true);
			frame.activateView(MenuActionConstants.ACTION_GANTT);
		}

		void resetCalls() {
			calls.clear();
		}

		boolean hasCall(String name) {
			return calls.stream().anyMatch(call -> Objects.equals(call.name, name));
		}

		void setTaskInformation(boolean taskType, boolean resourceType) {
			manager.setTaskInformation(taskType, resourceType);
		}

		void selectNone() {
			frame.setSelection(null);
		}

		void selectSingle(Node node) {
			frame.setSelection(List.of(node));
		}

		void selectMultiple(Node... nodes) {
			frame.setSelection(Arrays.asList(nodes));
		}

		String actionId(String buttonId) {
			return manager.getMenuManager().getRibbonFactory().getActionStringFromId(buttonId);
		}

		void invoke(String buttonId) throws Exception {
			SwingUtilities.invokeAndWait(() -> {
				String actionId = actionId(buttonId);
				assertNotNull(actionId, () -> buttonId + " does not resolve to an action");
				Action action = manager.getAction(actionId);
				action.actionPerformed(new ActionEvent(new JButton(buttonId), ActionEvent.ACTION_PERFORMED, buttonId));
			});
		}

		void invokeAction(String actionId) throws Exception {
			SwingUtilities.invokeAndWait(() -> manager.getAction(actionId).actionPerformed(
				new ActionEvent(new JButton(actionId), ActionEvent.ACTION_PERFORMED, actionId)));
		}
	}

	private static final class RecordingGraphicManager extends GraphicManager {
		private final List<Call> calls;
		private final FrameManager frameManager = new StubFrameManager();
		private boolean interceptPaste = true;
		private boolean taskInformationRouteAccepted;
		private boolean taskInformationVisible;
		private RuntimeException taskInformationFailure;

		RecordingGraphicManager(JPanel panel, List<Call> calls) {
			super(panel);
			this.calls = calls;
		}

		@Override
		public FrameManager getFrameManager() {
			return frameManager;
		}

		@Override
		protected boolean beforeActionRoute(String actionId) {
			if (interceptPaste && "paste".equals(actionId)) {
				calls.add(new Call("action", List.of(actionId)));
				return true;
			}
			return super.beforeActionRoute(actionId);
		}

		void setInterceptPaste(boolean interceptPaste) {
			this.interceptPaste = interceptPaste;
		}

		@Override
		protected boolean beforeExternalRoute(String routeId) {
			calls.add(new Call("external", List.of(routeId)));
			return false;
		}

		@Override
		protected boolean beforeChooserRoute(String chooserId) {
			calls.add(new Call("chooser", List.of(chooserId)));
			return false;
		}

		@Override
		protected boolean beforeViewSwitchRoute(String viewId) {
			calls.add(new Call("view", List.of(viewId)));
			return false;
		}

		@Override
		protected boolean beforeToggleRoute(String actionId) {
			calls.add(new Call("toggle", List.of(actionId)));
			return false;
		}

		@Override
		protected boolean beforeProjectInformationRoute(Project project) {
			calls.add(new Call("projectInfo", List.of(project)));
			return false;
		}

		@Override
		protected boolean beforeTaskInformationRoute(Task task, boolean notes, boolean resourcesTab) {
			calls.add(new Call("taskInfo", List.of(task, notes, resourcesTab)));
			return taskInformationRouteAccepted;
		}

		@Override
		protected boolean presentTaskInformationDialog(Task task, boolean notes, boolean resourcesTab) {
			if (taskInformationFailure != null)
				throw taskInformationFailure;
			return taskInformationVisible;
		}

		@Override
		protected boolean beforeResourceInformationRoute(Resource resource, boolean notes) {
			calls.add(new Call("resourceInfo", List.of(resource, notes)));
			return false;
		}

		@Override
		protected boolean beforeProjectsDialogRoute(Project project) {
			calls.add(new Call("projectsDialog", List.of(project)));
			return false;
		}

		@Override
		protected boolean beforeFindRoute(Searchable searchable, Field field) {
			List<Object> args = new ArrayList<>(2);
			args.add(searchable);
			args.add(field);
			calls.add(new Call("find", args));
			return false;
		}

		@Override
		protected boolean beforeAssignResourcesRoute(DocumentFrame documentFrame) {
			calls.add(new Call("assignResources", List.of(documentFrame)));
			return false;
		}

		@Override
		protected boolean beforeTimesheetRoute(DocumentFrame documentFrame) {
			calls.add(new Call("timesheet", List.of(documentFrame)));
			return false;
		}

		@Override
		protected boolean beforeChangeWorkingTimeRoute(Project project, boolean restrict) {
			calls.add(new Call("changeWorkingTime", List.of(project, restrict)));
			return false;
		}

		@Override
		protected boolean beforeCalendarOptionsRoute() {
			calls.add(new Call("calendarOptions", List.of()));
			return false;
		}

		@Override
		protected boolean beforeUpdateTasksRoute(DocumentFrame documentFrame) {
			calls.add(new Call("updateTasks", List.of(documentFrame)));
			return false;
		}

		@Override
		protected boolean beforeUpdateProjectRoute(DocumentFrame documentFrame) {
			calls.add(new Call("updateProject", List.of(documentFrame)));
			return false;
		}

		@Override
		protected boolean beforeMoveProjectRoute(DocumentFrame documentFrame) {
			calls.add(new Call("moveProject", List.of(documentFrame)));
			return false;
		}

		@Override
		void recalculateProject(Project project) {
			calls.add(new Call("recalculate", List.of(project)));
		}

	}

	private static final class StubFrameManager implements FrameManager {
		private static final long serialVersionUID = 1L;
		private final Workspace workspace = new Workspace();
		private NamedFrame activeFrame;

		@Override
		public void showFrame(NamedFrame frame) {
		}

		@Override
		public void addFrame(NamedFrame frame) {
		}

		@Override
		public void removeFrame(NamedFrame frame) {
			if (activeFrame == frame) activeFrame = null;
		}

		@Override
		public Workspace getWorkspace() {
			return workspace;
		}

		@Override
		public void activateFrame(NamedFrame frame) {
			activeFrame = frame;
		}

		@Override
		public java.awt.Component getSelectedFrame() {
			return activeFrame;
		}

		@Override
		public java.util.AbstractList getAllFrames() {
			return new java.util.AbstractList<Object>() {
				@Override
				public Object get(int index) {
					return null;
				}

				@Override
				public int size() {
					return 0;
				}
			};
		}

		@Override
		public void setTabTitle(NamedFrame frame, String tabTitle) {
		}

		@Override
		public void update() {
		}

		@Override
		public void cleanUp() {
		}

		@Override
		public void restoreWorkspace(WorkspaceSetting setting, int context) {
		}

		@Override
		public WorkspaceSetting createWorkspace(int context) {
			return null;
		}
	}

	private static final class TestDocumentFrame extends DocumentFrame {
		private static final long serialVersionUID = 1L;
		private List<Node> selectedNodes;
		private final Map<String, Integer> structuralCalls = new LinkedHashMap<>();
		private final Map<Boolean, Integer> baselineDialogCalls = new LinkedHashMap<>();
		private boolean executeLinkForSelectionTest;
		private boolean executeUnlinkForSelectionTest;
		private boolean executeIndentForSelectionTest;
		private List<Node> visibilitySelectionForTest;
		private int copyCalls;
		private int cutCalls;

		TestDocumentFrame(GraphicManager parentFrame, Project project) {
			super(parentFrame, project, "ribbon-test");
		}

		void setSelection(List<Node> nodes) {
			selectedNodes = nodes;
		}

		void setExecuteLinkForSelectionTest(boolean execute) {
			executeLinkForSelectionTest = execute;
		}

		void setExecuteUnlinkForSelectionTest(boolean execute) {
			executeUnlinkForSelectionTest = execute;
		}

		void setExecuteIndentForSelectionTest(boolean execute) {
			executeIndentForSelectionTest = execute;
		}

		void setVisibilitySelectionForTest(List<Node> nodes) {
			visibilitySelectionForTest = nodes;
		}

		int copyCallCount() {
			return copyCalls;
		}

		int cutCallCount() {
			return cutCalls;
		}

		@Override
		protected List<Node> getSelectedVisibilityTaskNodes() {
			return visibilitySelectionForTest == null
				? super.getSelectedVisibilityTaskNodes() : visibilitySelectionForTest;
		}

		@Override
		public List<Node> getSelectedNodes(boolean excludeReadOnly) {
			return selectedNodes;
		}

		@Override
		boolean doBaselineDialog(boolean save) {
			baselineDialogCalls.merge(save, 1, Integer::sum);
			BaselineDialog.Form form = new BaselineDialog.Form();
			return applyBaseline(getProject(), save, form, null);
		}

		int baselineDialogCallCount(boolean save) {
			return baselineDialogCalls.getOrDefault(save, 0);
		}

		void resetStructuralCalls() {
			structuralCalls.clear();
		}

		int structuralCallCount(String buttonId) {
			return structuralCalls.getOrDefault(buttonId, 0);
		}

		int insertTaskCallCount() {
			return structuralCalls.getOrDefault("insertTask", 0);
		}

		private void recordStructuralCall(String buttonId) {
			structuralCalls.merge(buttonId, 1, Integer::sum);
		}

		@Override
		public void doScrollToTask() {
			recordStructuralCall("RibbonScrollToTask");
		}

		@Override
		public void doCopy() {
			copyCalls++;
		}

		@Override
		public void doCut() {
			cutCalls++;
		}

		@Override
		public Node addNodeForImpl(Object impl) {
			if (impl == null) {
				recordStructuralCall("insertTask");
				return null;
			}
			return super.addNodeForImpl(impl);
		}

		@Override
		public void doIndent() {
			if (executeIndentForSelectionTest) {
				super.doIndent();
				return;
			}
			recordStructuralCall("RibbonIndent");
		}

		@Override
		public void doOutdent() {
			if (executeIndentForSelectionTest) {
				super.doOutdent();
				return;
			}
			recordStructuralCall("RibbonOutdent");
		}

		@Override
		public void doMoveSelectedTasks(int direction) {
			recordStructuralCall(direction < 0?"RibbonMoveTaskUp":"RibbonMoveTaskDown");
		}

		@Override
		public void doExpand() {
			recordStructuralCall("RibbonExpand");
		}

		@Override
		public void doCollapse() {
			recordStructuralCall("RibbonCollapse");
		}

		@Override
		public void doLinkTasks() {
			if (executeLinkForSelectionTest) {
				super.doLinkTasks();
				return;
			}
			recordStructuralCall("RibbonLink");
		}

		@Override
		public void doUnlinkTasks() {
			if (executeUnlinkForSelectionTest) {
				super.doUnlinkTasks();
				return;
			}
			recordStructuralCall("RibbonUnlink");
		}

		@Override
		protected boolean canPasteIntoCurrentSelection() {
			return true;
		}

		@Override
		public void doPaste() {
			recordStructuralCall("RibbonPaste");
		}

		@Override
		void doDelegateTasksDialog() {
			recordStructuralCall("RibbonDelegateTasks");
		}
	}

	private static final class Call {
		final String name;
		final List<Object> args;

		Call(String name, List<Object> args) {
			this.name = name;
			this.args = args;
		}
	}
}
