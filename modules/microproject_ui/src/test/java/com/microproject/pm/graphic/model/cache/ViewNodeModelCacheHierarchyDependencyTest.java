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
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ViewNodeModelCacheHierarchyDependencyTest {
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
