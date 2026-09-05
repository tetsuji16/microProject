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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.util.Environment;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.pm.graphic.frames.workspace.Workspace;
import com.microproject.workspace.WorkspaceSetting;

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

		harness.manager.getAction(MenuActionConstants.ACTION_INSERT_TASK)
			.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "insert"));

		assertEquals(1, harness.frame.insertTaskCallCount());
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
		assertChooser(harness, "RibbonBar", MenuActionConstants.ACTION_BAR_STYLES);
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
		assertView(harness, "RibbonResourceUsage", MenuActionConstants.ACTION_RESOURCE_USAGE);
		assertView(harness, "RibbonNoTextNoSubWindow", MenuActionConstants.ACTION_NO_SUB_WINDOW);

		assertDoesNotThrow(() -> harness.invoke("RibbonZoomIn"));
		assertDoesNotThrow(() -> harness.invoke("RibbonZoomOut"));
		assertToggle(harness, "RibbonToggleProgressLine", true);
		assertToggle(harness, "RibbonLabelResourceNames", true);
		assertToggle(harness, "RibbonLabelTaskName", true);
		assertToggle(harness, "RibbonGridlines", true);
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
			"RibbonInsert",
			"RibbonInsertResource",
			"RibbonInsertRecurring",
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
			"RibbonNotes",
			"RibbonChangeWorkingTime",
			"RibbonAssignResources",
			"RibbonTimesheet",
			"RibbonFind",
			"RibbonProjectsDialog",
			"RibbonCalendarOptions",
			"RibbonUpdateTasks",
			"RibbonUpdateProject",
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
			"RibbonResourceUsage",
			"RibbonNoTextNoSubWindow");
		add(map, Strategy.ROUTE_CHOOSER,
			"RibbonChooseFilter",
			"RibbonChooseSort",
			"RibbonChooseGroup",
			"RibbonTimescale",
			"RibbonBar",
			"RibbonBarStyles",
			"RibbonTextStyles",
			"RibbonLayout");
		add(map, Strategy.STATE_TOGGLE,
			"RibbonZoomIn",
			"RibbonZoomOut",
			"RibbonToggleProgressLine",
			"RibbonLabelResourceNames",
			"RibbonLabelTaskName",
			"RibbonGridlines",
			"RibbonToggleCriticalChain");
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
			"RibbonLevelResources",
			"RibbonUseResourcePool",
			"RibbonCreateResourcePool",
			"RibbonRefreshResourcePool",
			"RibbonRefreshSubprojects",
			"RibbonOpenSubproject",
			"RibbonRemoveSubproject",
			"RibbonCCPMSettings",
			"RibbonCCPMClear",
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

		Harness() {
			manager = new RecordingGraphicManager(new JPanel(), calls);
			manager.getMenuManager().createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
			DataFactoryUndoController undoController = new DataFactoryUndoController();
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
		void recalculateProject(Project project) {
			calls.add(new Call("recalculate", List.of(project)));
		}

	}

	private static final class StubFrameManager implements FrameManager {
		private static final long serialVersionUID = 1L;
		private final Workspace workspace = new Workspace();

		@Override
		public void showFrame(NamedFrame frame) {
		}

		@Override
		public void addFrame(NamedFrame frame) {
		}

		@Override
		public void removeFrame(NamedFrame frame) {
		}

		@Override
		public Workspace getWorkspace() {
			return workspace;
		}

		@Override
		public void activateFrame(NamedFrame frame) {
		}

		@Override
		public java.awt.Component getSelectedFrame() {
			return null;
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

		TestDocumentFrame(GraphicManager parentFrame, Project project) {
			super(parentFrame, project, "ribbon-test");
		}

		void setSelection(List<Node> nodes) {
			selectedNodes = nodes;
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
		public Node addNodeForImpl(Object impl) {
			if (impl == null) {
				recordStructuralCall("insertTask");
				return null;
			}
			return super.addNodeForImpl(impl);
		}

		@Override
		public void doIndent() {
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
			recordStructuralCall("RibbonLink");
		}

		@Override
		public void doUnlinkTasks() {
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
