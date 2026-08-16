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
