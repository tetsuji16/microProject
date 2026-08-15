package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskDependencyInvalidationTest {
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

	private NormalTask task(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}
}
