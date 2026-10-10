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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
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
import javax.swing.AbstractButton;
import javax.swing.JButton;
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
import com.microproject.menu.MenuManager;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;
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

class RibbonButtonBehaviorTest {
	private enum Strategy {
		ROUTE_DIALOG,
		ROUTE_CHOOSER,
		ROUTE_VIEW,
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
	void hierarchyCommandsFollowTaskSelectionState() throws Exception {
		Harness harness = newHarness();
		harness.setTaskInformation(true, false);
		SwingUtilities.invokeAndWait(() -> {
			harness.frame.getTopSpreadSheet().clearSelection();
			harness.manager.setButtonState(harness.task, harness.project);
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
			assertSame(harness.task, harness.secondTask.getWbsParentTask(),
				"Indent execution must use the active table row, not the stale frame selection");
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
			assertEquals(RibbonCommandResult.Status.REJECTED,
				harness.frame.routeTaskCommand(CommandId.UNLINK).status(),
				"Unlink execution must use the same editable selection filter");
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
			assertEquals(RibbonCommandResult.Status.REJECTED,
				harness.frame.routeTaskCommand(CommandId.LINK).status(),
				"Link execution must use the same editable task selection as enablement");
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
		assertExternal(harness, "RibbonProjectLibreDocumentation", "help");
		assertExternal(harness, "RibbonAboutProjectLibre", "about");
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
			"RibbonProjectLibreDocumentation", "RibbonAboutProjectLibre")) {
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
			"RibbonProjectLibreDocumentation",
			"RibbonAboutProjectLibre",
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
			"RibbonUnlink");
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
			return false;
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
