/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.model.dependency.DependencyService;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.dependency.DependencyType;
import com.microproject.model.resource.ResourcePool;
import com.microproject.node.Node;
import com.microproject.node.NodeFactory;
import com.microproject.node.model.NodeModel;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.schedule.criticalpath.CriticalPath;

class TaskDependencyInvalidationTest {
	@Test
	void dependsOnHandlesLongDependencyChainsWithoutRecursion() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		NormalTask first = task(project);
		NormalTask previous = first;
		for (int i = 1; i < 6000; i++) {
			NormalTask current = task(project);
			Dependency dependency = Dependency.getInstance(previous, current, DependencyType.FS, 0L);
			current.getPredecessorList().add(dependency);
			previous = current;
		}

		assertTrue(previous.dependsOn(first));
	}

	@Test
	void dependencyInvalidationReachesOnlyTheAffectedClosure() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		NormalTask predecessor = task(project);
		NormalTask successor = task(project);
		NormalTask downstream = task(project);
		NormalTask unrelatedPredecessor = task(project);
		NormalTask unrelated = task(project);
		NormalTask unrelatedSuccessor = task(project);
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		DependencyService.getInstance().newDependency(successor, downstream, DependencyType.FS, 0L, this);
		DependencyService.getInstance().newDependency(unrelatedPredecessor, unrelated, DependencyType.FS, 0L, this);
		DependencyService.getInstance().newDependency(unrelated, unrelatedSuccessor, DependencyType.FS, 0L, this);

		predecessor.setCalculationStateCount(0);
		successor.setCalculationStateCount(0);
		downstream.setCalculationStateCount(0);
		unrelatedPredecessor.setCalculationStateCount(0);
		unrelated.setCalculationStateCount(0);
		unrelatedSuccessor.setCalculationStateCount(0);

		predecessor.markAllDependentTasksAsNeedingRecalculation(true);

		assertTrue(predecessor.getCalculationStateCount() > 0);
		assertTrue(successor.getCalculationStateCount() > 0);
		assertTrue(downstream.getCalculationStateCount() > 0);
		assertEquals(0, unrelated.getCalculationStateCount());
	}

	@Test
	void extendingCriticalPathRefreshesLateDatesOutsideTheChangedDependencyClosure() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("late date boundary", undoController), undoController);
		project.initialize(false, false);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		NormalTask independent = (NormalTask) project.createLocalTaskNode(null).getImpl();
		NormalTask predecessor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		NormalTask successor = (NormalTask) project.createLocalTaskNode(null).getImpl();
		independent.setDuration(day);
		predecessor.setDuration(day);
		successor.setDuration(day);
		project.recalculate();

		long projectEndBefore = project.getEnd();
		long independentLateFinishBefore = independent.getLateFinish();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		long incrementalLateFinish = independent.getLateFinish();
		assertTrue(project.getEnd() > independent.getEarlyFinish(),
			"the project boundary should expand beyond the independent one-task path");
		assertTrue(incrementalLateFinish != independentLateFinishBefore,
			"extending the critical path changes late dates even for a task outside the dependency closure: before="
					+ independentLateFinishBefore + ", after=" + incrementalLateFinish + ", projectEnd=" + project.getEnd());

		long expectedProjectEnd = project.getEnd();
		project.recalculate();
		assertEquals(expectedProjectEnd, project.getEnd(), "a full recalculation must agree with the incremental project boundary");
		assertEquals(incrementalLateFinish, independent.getLateFinish(),
			"the incremental update must refresh unrelated tasks' late dates to the full-recalculation result");

		project.getUndoController().undo();
		assertEquals(projectEndBefore, project.getEnd(), "undo must restore the project boundary before the link");
		assertEquals(independentLateFinishBefore, independent.getLateFinish(), "undo must restore the independent task's late date");
	}

	@Test
	void sequentialDependenciesDeferBackwardPassUntilLateDateIsRead() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("deferred late dates", undoController), undoController);
		project.initialize(false, false);
		long day = com.microproject.options.CalendarOption.getInstance().getMillisPerDay();
		List<NormalTask> tasks = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
			task.setDuration(day);
			tasks.add(task);
		}
		project.recalculate();
		assertEquals(0L, project.getTaskList().stream().filter(Task::isReverseScheduled).count(),
			"the counter fixture must exercise a project without reverse-scheduled tasks");

		for (int i = 1; i < tasks.size(); i++) {
			DependencyService.getInstance().newDependency(tasks.get(i - 1), tasks.get(i), DependencyType.FS, 0L, this);
			assertEquals(1, currentCalculationPass(project),
				"each dependency addition without a late-date read must leave the backward pass pending");
		}

		NormalTask first = tasks.get(0);
		long lateStart = first.getLateStart();
		assertEquals(2, currentCalculationPass(project),
			"the first late-date read must perform one deferred backward pass");
		assertEquals(lateStart, first.getLateStart());
		first.getLateFinish();
		first.getTotalSlack();
		first.getFreeSlack();
		assertEquals(2, currentCalculationPass(project),
			"subsequent late-date and slack reads must not repeat the backward pass");
		long totalSlack = first.getTotalSlack();
		long freeSlack = first.getFreeSlack();
		project.recalculate();
		assertEquals(totalSlack, first.getTotalSlack(), "total slack must match the full recalculation result");
		assertEquals(freeSlack, first.getFreeSlack(), "free slack must match the full recalculation result");
	}

	private int currentCalculationPass(Project project) throws ReflectiveOperationException {
		Field contextField = CriticalPath.class.getDeclaredField("context");
		contextField.setAccessible(true);
		Object context = contextField.get(project.getSchedulingAlgorithm());
		Field passField = context.getClass().getDeclaredField("pass");
		passField.setAccessible(true);
		return passField.getInt(context);
	}

	@Test
	void outlineInsertionAndRemovalStillInvalidateProjectBoundaryTasks() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("bounds invalidation", undoController), undoController);
		project.initialize(false, false);
		NormalTask boundaryTask = (NormalTask) project.createLocalTaskNode(null).getImpl();
		boundaryTask.setDuration(2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		project.recalculate();
		Node inserted = project.createLocalTaskNode(null);
		NormalTask insertedTask = (NormalTask) inserted.getImpl();
		insertedTask.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		DependencyService.getInstance().newDependency(boundaryTask, insertedTask, DependencyType.FS, 0L, this);
		project.recalculate();
		long expandedProjectEnd = project.getEnd();
		assertTrue(expandedProjectEnd > boundaryTask.getEnd(),
			"an inserted outline task must be included in the project boundary calculation");

		project.getTaskModel().remove(inserted, NodeModel.NORMAL);
		project.recalculate();
		assertTrue(project.getEnd() < expandedProjectEnd,
			"removing an outline task must remove it from the project boundary calculation");
		assertEquals(boundaryTask.getEnd(), project.getEnd(),
			"after removal, the surviving boundary task must determine the project end");
	}

	@Test
	void appendedLeafBatchesRenumberOnlyTheOutlineSuffixWithoutChangingIds() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("incremental numbering", undoController), undoController);
		project.initialize(false, false);
		List<Node> phases = new ArrayList<>();
		for (int phaseIndex = 0; phaseIndex < 3; phaseIndex++) {
			NormalTask phase = new NormalTask(project);
			project.connectTask(phase);
			phases.add(NodeFactory.getInstance().createNode(phase));
		}
		project.getTaskModel().add(null, phases, NodeModel.NORMAL);
		for (Node phase : phases) {
			List<Node> tasks = new ArrayList<>();
			for (int taskIndex = 0; taskIndex < 3; taskIndex++) {
				NormalTask task = new NormalTask(project);
				project.connectTask(task);
				tasks.add(NodeFactory.getInstance().createNode(task));
			}
			project.getTaskModel().add(phase, tasks, NodeModel.NORMAL);
		}
		assertSequentialTaskIds(project, 12);

		NormalTask insertedTask = new NormalTask(project);
		project.connectTask(insertedTask);
		Node inserted = NodeFactory.getInstance().createNode(insertedTask);
		project.getTaskModel().add(phases.get(0), inserted, 0, NodeModel.NORMAL);
		assertSequentialTaskIds(project, 13);

		project.getTaskModel().remove(inserted, NodeModel.NORMAL);
		assertSequentialTaskIds(project, 12);
	}

	@Test
	void eventAppendAfterSilentNumberedAdditionFallsBackToFullRenumber() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("silent numbering prefix", undoController), undoController);
		project.initialize(false, false);
		NormalTask silentlyAdded = task(project);
		silentlyAdded.setId(40L);
		project.getTaskModel().add(null, NodeFactory.getInstance().createNode(silentlyAdded), NodeModel.SILENT);
		assertEquals(40L, silentlyAdded.getId(), "silent loading preserves the incoming ID until a normal event mutation");

		NormalTask eventAppended = task(project);
		eventAppended.setId(90L);
		project.getTaskModel().add(null, NodeFactory.getInstance().createNode(eventAppended), NodeModel.NORMAL);

		assertSequentialTaskIds(project, 2);
	}

	private void assertSequentialTaskIds(Project project, int count) {
		List<Long> actualIds = new ArrayList<>();
		project.getTaskModel().getHierarchy().visitAll(object -> {
			if (object instanceof Node node && node.hasNumber())
				actualIds.add(((Task) node.getImpl()).getId());
		});
		List<Long> expectedIds = new ArrayList<>(count);
		for (long id = 1; id <= count; id++) expectedIds.add(id);
		assertEquals(expectedIds, actualIds);
	}

	private NormalTask task(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}
}
