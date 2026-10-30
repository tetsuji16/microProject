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
package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.document.ObjectEvent;
import com.microproject.configuration.Dictionary;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.DefaultNodeModel;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.BaseFilter;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class ViewNodeModelCacheHierarchyDependencyTest {
	@Test
	void rootChildrenFollowEachPublishedProjection() {
		Dictionary.getInstance();
		Project project = createProject();
		NormalTask first = createTask(project, "First");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
			.createFilteredCache(reference, "root-children-test", null);
		reference.update();
		try {
			Object root = view.getRoot();
			GraphicNode firstNode = reference.getGraphicNode(project.getTaskModel().search(first));
			assertSame(firstNode, view.getChild(root, 0));
			assertEquals(0, view.getIndexOfChild(root, firstNode));
			int count = view.getChildCount(root);
			NormalTask second = createTask(project, "Second");
			reference.update();
			GraphicNode secondNode = reference.getGraphicNode(project.getTaskModel().search(second));
			assertEquals(count + 1, view.getChildCount(root));
			int secondRow = view.getRowAt(secondNode);
			assertSame(secondNode, view.getChild(root, secondRow));
			assertEquals(secondRow, view.getIndexOfChild(root, secondNode));
		} finally {
			view.close();
			reference.close();
		}
	}
	@Test
	void dependencyElementsRemainUntransformed() {
		VisibleDependencies visibleDependencies = new VisibleDependencies("dependency-test");
		GraphicDependency first = new GraphicDependency(null, null, null);
		GraphicDependency second = new GraphicDependency(null, null, null);
		visibleDependencies.getElements().addAll(List.of(first, second));

		visibleDependencies.applyTransformer();

		assertEquals(List.of(first, second), visibleDependencies.getElements());
	}

	@Test
	void createHierarchyDependencyMovesChildUnderParent() throws Exception {
		Project project = createProject();
		NormalTask parentTask = createTask(project, "Parent");
		NormalTask childTask = createTask(project, "Child");

		ReferenceNodeModelCache referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		ViewNodeModelCache viewCache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
			.createFilteredCache(referenceCache, "hierarchy-test", null);

		Node parentNode = (Node) project.getTaskModel().search(parentTask);
		Node childNode = (Node) project.getTaskModel().search(childTask);

		viewCache.createHierarchyDependency((GraphicNode) viewCache.getGraphicNode(parentNode),
			(GraphicNode) viewCache.getGraphicNode(childNode));

		assertSame(parentNode, project.getTaskModel().getHierarchy().getParent(childNode));
	}

	@Test
	void keepOutlineOffFlattensCollapsedSummaryAndChildRowsWithoutMutatingTheModel() throws Exception {
		Dictionary.getInstance();
		Project project = createProject();
		NormalTask parent = createTask(project, "Zulu Parent");
		NormalTask child = createTask(project, "Alpha Child");
		NormalTask sibling = createTask(project, "Beta Sibling");
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.indent(List.of(model.search(child)), 1, NodeModel.SILENT);
		Node parentNode = (Node) model.search(parent);
		Node childNode = (Node) model.search(child);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		GraphicNode parentGraphic = reference.getGraphicNode(parentNode);
		parentGraphic.setCollapsed(true);
		ViewNodeModelCache flatView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "flat-sort-test", transformer -> {
				ViewTransformer viewTransformer = (ViewTransformer) transformer;
				viewTransformer.setUserSorter("Sorter.Name", true);
				viewTransformer.setTaskSortSpec(new TaskSortSpec(
						List.of(new TaskSortSpec.Key("Sorter.Name", true)), false));
			});
		reference.update();
		try {
			assertEquals(List.of("Alpha Child", "Beta Sibling", "Zulu Parent"), visibleNames(flatView));
			assertEquals(3, flatView.getChildCount(flatView.getRoot()), "flat TreeModel exposes every row under the root");
			for (int row = 0; row < flatView.getVisibleNodes().getSize(); row++)
				assertEquals(1, flatView.getLevel(flatView.getVisibleNodes().getElementAt(row)));
			assertSame(parentNode, model.getHierarchy().getParent(childNode), "Keep Outline off is projection-only");
			assertTrue(reference.getGraphicNode(parentNode).isCollapsed(), "the view projection must not change collapse state");
		} finally {
			flatView.close();
			reference.close();
		}
	}

	@Test
	void hiddenSummaryBranchesArePrunedWhileVisibleHierarchyPlaceholdersRemain() throws Exception {
		Dictionary.getInstance();
		Project project = createProject();
		NormalTask hiddenSummary = createTask(project, "Hidden summary");
		NormalTask hiddenDescendant = createTask(project, "Hidden descendant");
		NormalTask visibleSummary = createTask(project, "Visible summary");
		NormalTask hiddenChild = createTask(project, "Hidden child");
		NormalTask placeholderSummary = createTask(project, "Filtered summary with visible child");
		NormalTask visibleDescendant = createTask(project, "Visible descendant");
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		model.indent(List.of((Node) model.search(hiddenDescendant)), 1, NodeModel.SILENT);
		model.indent(List.of((Node) model.search(hiddenChild)), 1, NodeModel.SILENT);
		model.indent(List.of((Node) model.search(visibleDescendant)), 1, NodeModel.SILENT);
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		ViewNodeModelCache view = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "hidden-summary-branch-test", transformer -> {
				ViewTransformer viewTransformer = (ViewTransformer) transformer;
				viewTransformer.setHiddenFilter(new BaseFilter(""));
			});
		reference.update();
		try {
			assertTrue(visibleNames(view).containsAll(List.of("Hidden summary", "Hidden descendant", "Visible summary",
					"Hidden child", "Filtered summary with visible child", "Visible descendant")),
				"the initial projection includes every row before hidden filtering");

			hiddenSummary.setHiddenTask(true);
			hiddenDescendant.setHiddenTask(true);
			hiddenChild.setHiddenTask(true);
			placeholderSummary.setHiddenTask(true);
			reference.update();
			List<String> names = visibleNames(view);
			assertFalse(names.contains("Hidden summary"), "a hidden summary with only hidden descendants is not a useful placeholder");
			assertFalse(names.contains("Hidden descendant"), "hidden descendants stay out of the view");
			assertTrue(names.contains("Visible summary"), "an unhidden empty summary remains visible by default");
			assertFalse(names.contains("Hidden child"), "hidden children stay out of the view");
			assertTrue(names.contains("Filtered summary with visible child"),
				"a filtered summary remains as a hierarchy placeholder for a visible descendant");
			assertTrue(names.contains("Visible descendant"));

			hiddenSummary.setHiddenTask(false);
			hiddenDescendant.setHiddenTask(false);
			hiddenChild.setHiddenTask(false);
			placeholderSummary.setHiddenTask(false);
			reference.update();
			assertTrue(visibleNames(view).containsAll(List.of("Hidden summary", "Hidden descendant", "Visible summary",
					"Hidden child", "Filtered summary with visible child", "Visible descendant")),
				"rebuilding the projection after unhide must restore every summary and descendant row");
		} finally {
			view.close();
			reference.close();
		}
	}

	@Test
	void multiKeyTaskSortAppliesDescendingNameAfterEqualStartDates() throws Exception {
		Dictionary.getInstance();
		Project project = createProject();
		NormalTask earlierName = createTask(project, "Date sort earlier");
		NormalTask laterName = createTask(project, "Date sort tied earlier");
		assertEquals(earlierName.getStart(), laterName.getStart(), "fixture must tie on Start Date");
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		ViewNodeModelCache sortedView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "multi-key-sort-test", transformer -> ((ViewTransformer) transformer).setTaskSortSpec(
				new TaskSortSpec(List.of(new TaskSortSpec.Key("Sorter.StartDate", true),
						new TaskSortSpec.Key("Sorter.Name", false)), true)));
		reference.update();
		try {
			assertEquals(List.of("Date sort tied earlier", "Date sort earlier"), visibleNames(sortedView),
				"the view pipeline must resolve and apply a secondary descending Name comparator");
		} finally {
			sortedView.close();
			reference.close();
		}
	}

	@Test
	void taskSortAppliesToFilteredRowsWithinConfiguredGroupsWithoutChangingModelOrder() throws Exception {
		// Document-derived projection rule: Project describes sorting, filtering, and
		// grouping as view operations. Filtering first limits visible tasks; sorting
		// orders those tasks within the configured group buckets; neither operation
		// changes task outline order. The exact composition is our deterministic
		// Office-consistent rule, not a measured MSP result.
		// Sources: https://support.microsoft.com/en-us/project/sort-tasks-resources-or-dates-in-project-desktop
		// https://support.microsoft.com/en-us/project/filter-tasks-or-resources
		// https://support.microsoft.com/en-us/project/group-tasks-or-resources
		Dictionary.getInstance();
		Project project = createProject();
		NormalTask zuluShort = createTask(project, "Zulu short");
		NormalTask alphaShort = createTask(project, "Alpha short");
		NormalTask bravoLong = createTask(project, "Bravo long");
		NormalTask hiddenComplete = createTask(project, "Hidden complete");
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		zuluShort.setDuration(day);
		alphaShort.setDuration(day);
		bravoLong.setDuration(2L * day);
		hiddenComplete.setDuration(day);
		hiddenComplete.setPercentComplete(1.0d);
		DefaultNodeModel model = (DefaultNodeModel) project.getTaskModel();
		List<Task> originalOrder = List.copyOf(project.getTaskList());
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project, model);
		ViewNodeModelCache sortedView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
			reference, "filtered-grouped-task-sort-test", transformer -> {
				ViewTransformer viewTransformer = (ViewTransformer) transformer;
				viewTransformer.setUserFilterId("Filter.IncompleteTasks");
				viewTransformer.setUserGrouperId("Grouper.Duration");
				viewTransformer.setTaskSortSpec(new TaskSortSpec(
						List.of(new TaskSortSpec.Key("Sorter.Name", true)), true));
			});
		reference.update();
		try {
			List<GraphicNode> projected = sortedView.getVisibleNodes().getElements().stream()
				.filter(GraphicNode.class::isInstance).map(GraphicNode.class::cast).toList();
			List<GraphicNode> visibleTasks = projected.stream()
				.filter(node -> !node.isGroup() && node.getNode().getImpl() instanceof NormalTask).toList();
			assertEquals(List.of("Bravo long", "Alpha short", "Zulu short"), visibleTasks.stream()
				.map(node -> ((NormalTask) node.getNode().getImpl()).getName()).toList(),
				"sorting must order filtered task rows while grouping retains its category buckets");
			assertEquals(2L, projected.stream().filter(GraphicNode::isGroup).count(),
				"the duration grouper must retain one visible header for each nonempty bucket");
			assertFalse(projected.stream().anyMatch(node -> node.getNode().getImpl() == hiddenComplete),
				"the active incomplete-task filter must remove completed tasks from the sorted projection");
			GraphicNode alphaShortNode = visibleTasks.get(1);
			assertTrue(sortedView.getVisibleNodes().getProjectionIndex().rowForNode(alphaShortNode) >= 0,
				"visible task selection must resolve to its post-sort projection row");
			GraphicNode hiddenNode = reference.getGraphicNode((Node) model.search(hiddenComplete));
			assertEquals(-1, sortedView.getVisibleNodes().getProjectionIndex().rowForNode(hiddenNode),
				"filtered tasks must not resolve to a visible selection row");
			assertEquals(originalOrder, project.getTaskList(), "view sort/filter/group must not mutate project task order");
		} finally {
			sortedView.close();
			reference.close();
		}
	}

	@Test
	void firstListenerSeesAllDependencyFactsFromTheSameRevision() throws Exception {
		Project project = createProject();
		NormalTask predecessor = createTask(project, "Predecessor");
		NormalTask successor = createTask(project, "Successor");
		ReferenceNodeModelCache reference = NodeModelCacheFactory.createTaskNodeModelCache(project,
				project.getTaskModel());
		ViewNodeModelCache firstView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
			.createFilteredCache(reference, "same-revision-first", null);
		ViewNodeModelCache secondView = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
			.createFilteredCache(reference, "same-revision-second", null);
		Dependency[] dependencyHolder = new Dependency[1];
		AtomicBoolean firstListenerSawCompleteState = new AtomicBoolean();
		firstView.addNodeModelListener(event -> firstListenerSawCompleteState.set(
			reference.getGraphicDependency(dependencyHolder[0]) != null
				&& firstView.getVisibleDependencies().getElements().stream()
					.anyMatch(edge -> edge.getDependency() == dependencyHolder[0])
				&& secondView.getVisibleDependencies().getElements().stream()
					.anyMatch(edge -> edge.getDependency() == dependencyHolder[0])
				&& firstView.getVisibleNodes().getProjectionIndex().modelRevision()
					== secondView.getVisibleNodes().getProjectionIndex().modelRevision()));

		reference.setReceiveEvents(false);
		try (var mutation = project.getMutationState().begin()) {
			dependencyHolder[0] = DependencyService.getInstance().newDependency(predecessor, successor,
				DependencyType.FS, 0L, this);
			mutation.commit(true);
		}
		long revision = project.getMutationState().revision();
		reference.setReceiveEvents(true);
		SwingUtilities.invokeAndWait(() -> {
			ObjectEvent taskFact = ObjectEvent.getInstance(this, predecessor, ObjectEvent.UPDATE, null);
			taskFact.setPublicationRevision(revision);
			try {
				reference.objectChanged(taskFact);
			} finally {
				taskFact.recycle();
			}
			ObjectEvent dependencyFact = ObjectEvent.getInstance(this, dependencyHolder[0], ObjectEvent.CREATE, null);
			dependencyFact.setPublicationRevision(revision);
			try {
				reference.objectChanged(dependencyFact);
			} finally {
				dependencyFact.recycle();
			}
		});

		assertTrue(firstListenerSawCompleteState.get(),
			"the first view listener must observe the dependency and both view projections from this revision");
		assertTrue(firstView.getVisibleDependencies().getElements().stream()
			.anyMatch(edge -> edge.getDependency() == dependencyHolder[0]));
		assertTrue(secondView.getVisibleDependencies().getElements().stream()
			.anyMatch(edge -> edge.getDependency() == dependencyHolder[0]));
		firstView.close();
		secondView.close();
		reference.close();
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("hierarchy-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private List<String> visibleNames(ViewNodeModelCache cache) {
		List<String> names = new java.util.ArrayList<>();
		for (GraphicNode node : cache.getVisibleNodes().getElements())
			if (node.getNode().getImpl() instanceof NormalTask task) names.add(task.getName());
		return names;
	}
}
