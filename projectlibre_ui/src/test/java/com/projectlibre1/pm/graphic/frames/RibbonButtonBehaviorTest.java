package com.projectlibre1.pm.graphic.frames;

import static com.projectlibre1.menu.testsupport.MenuDefinitionSupport.ribbonButtonIds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.configuration.FieldDictionary;
import com.projectlibre1.field.Field;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.NodeFactory;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.pm.graphic.views.Searchable;
import com.projectlibre1.util.Environment;
import com.projectlibre1.pm.graphic.frames.workspace.FrameManager;
import com.projectlibre1.pm.graphic.frames.workspace.NamedFrame;
import com.projectlibre1.pm.graphic.frames.workspace.Workspace;
import com.projectlibre1.workspace.WorkspaceSetting;

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
		Set<String> inventory = new LinkedHashSet<>(ribbonButtonIds());
		assertTrue(COVERAGE.keySet().containsAll(inventory), () -> "Missing coverage for: " + missing(inventory, COVERAGE.keySet()));
	}

	@Test
	void coverageEntriesResolveToLiveActions() throws Exception {
		Harness harness = newHarness();
		for (String buttonId : COVERAGE.keySet()) {
			String actionId = harness.actionId(buttonId);
			assertNotNull(actionId, () -> buttonId + " does not resolve to an action");
			assertDoesNotThrow(() -> harness.manager.getAction(actionId), () -> buttonId + " does not resolve to a live action");
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
		assertCall(harness, "saveBaseline", harness.frame);

		harness.resetCalls();
		harness.invoke("RibbonClearBaseline");
		assertCall(harness, "clearBaseline", harness.frame);
	}

	@Test
	void fileHelpAndChooserRoutesFireExactlyOnce() throws Exception {
		Harness harness = newHarness();

		assertExternal(harness, "RibbonNewProject", "newProject");
		assertExternal(harness, "RibbonOpenProject", "openProject");
		assertExternal(harness, "RibbonSaveProject", "saveProject");
		assertExternal(harness, "RibbonSaveProjectAs", "saveAsProject");
		assertExternal(harness, "RibbonCloseProject", "closeProject");
		assertExternal(harness, "RibbonPrint", "print");
		assertExternal(harness, "RibbonPrintPreview", "printPreview");
		assertExternal(harness, "RibbonPDF", "pdf");
		assertExternal(harness, "RibbonLocale", "locale");
		assertExternal(harness, "RibbonProjectLibreDocumentation", "help");
		assertExternal(harness, "RibbonAboutProjectLibre", "about");
		assertExternal(harness, "RibbonTipOfTheDay", "tipOfTheDay");

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
			"RibbonIndent",
			"RibbonOutdent",
			"RibbonExpand",
			"RibbonCollapse",
			"RibbonLink",
			"RibbonUnlink")) {
			assertDoesNotThrow(() -> harness.invoke(buttonId), () -> buttonId + " should be invokable against a live frame");
			harness.resetCalls();
		}
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
			"RibbonOpenProject",
			"RibbonSaveProject",
			"RibbonSaveProjectAs",
			"RibbonCloseProject",
			"RibbonPrint",
			"RibbonPrintPreview",
			"RibbonPDF",
			"RibbonLocale",
			"RibbonProjectLibreDocumentation",
			"RibbonAboutProjectLibre",
			"RibbonTipOfTheDay");
		add(map, Strategy.ROUTE_DIALOG,
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
			"RibbonClearBaseline");
		add(map, Strategy.ROUTE_VIEW,
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
			"RibbonGridlines");
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
			"RibbonLevelResources",
			"RibbonScrollToTask",
			"RibbonIndent",
			"RibbonOutdent",
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
		final Resource resource;
		final Assignment assignment;
		final Node taskNode;
		final Node resourceNode;
		final Node projectNode;
		final Node assignmentNode;
		final List<Call> calls = new ArrayList<>();

		Harness() {
			manager = new RecordingGraphicManager(new JPanel(), calls);
			manager.getMenuManager().getRibbon(MenuManager.STANDARD_RIBBON, null);
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			ResourcePool pool = ResourcePool.createRourcePool("Ribbon Test Pool", undoController);
			pool.setLocal(true);
			project = Project.createProject(pool, undoController);
			project.setName("Ribbon Test Project");
			task = project.createScriptedTask();
			task.setName("Ribbon Task");
			resource = project.getResourcePool().createScriptedResource();
			resource.setName("Ribbon Resource");
			assignment = Assignment.getInstance(task, resource, 1.0, 0);
			taskNode = NodeFactory.getInstance().createNode(task);
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
			if ("paste".equals(actionId)) {
				calls.add(new Call("action", List.of(actionId)));
				return true;
			}
			return false;
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
		protected boolean beforeSaveBaselineRoute(DocumentFrame documentFrame) {
			calls.add(new Call("saveBaseline", List.of(documentFrame)));
			return false;
		}

		@Override
		protected boolean beforeClearBaselineRoute(DocumentFrame documentFrame) {
			calls.add(new Call("clearBaseline", List.of(documentFrame)));
			return false;
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
