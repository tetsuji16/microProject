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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskCleanupTest {
	@Test
	void cleanupPreservesDependenciesWhenRequested() throws Exception {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask task = createTask(project);
		NormalTask successor = createTask(project);
		Dependency incoming = DependencyService.getInstance().newDependency(predecessor, task, DependencyType.FS, 0L, this);
		Dependency outgoing = DependencyService.getInstance().newDependency(task, successor, DependencyType.FS, 0L, this);

		task.cleanUp(this, false, false, false);

		assertTrue(predecessor.getSuccessorList().contains(incoming));
		assertTrue(task.getPredecessorList().contains(incoming));
		assertTrue(task.getSuccessorList().contains(outgoing));
		assertTrue(successor.getPredecessorList().contains(outgoing));
	}

	@Test
	void cleanupRemovesDependenciesWhenRequested() throws Exception {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask task = createTask(project);
		NormalTask successor = createTask(project);
		Dependency incoming = DependencyService.getInstance().newDependency(predecessor, task, DependencyType.FS, 0L, this);
		Dependency outgoing = DependencyService.getInstance().newDependency(task, successor, DependencyType.FS, 0L, this);

		task.cleanUp(this, false, false, true);

		assertFalse(predecessor.getSuccessorList().contains(incoming));
		assertFalse(task.getPredecessorList().contains(incoming));
		assertFalse(task.getSuccessorList().contains(outgoing));
		assertFalse(successor.getPredecessorList().contains(outgoing));
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}
}
