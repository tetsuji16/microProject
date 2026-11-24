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
package com.microproject.model.dependency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class DependencyGraphTest {
	@Test
	void rejectsSelfLinkAndThreeTaskCycle() throws InvalidAssociationException {
		Project project = project();
		NormalTask first = task(project);
		NormalTask second = task(project);
		NormalTask third = task(project);
		assertThrows(InvalidAssociationException.class, () -> link(first, first));
		link(first, second);
		link(second, third);
		assertThrows(InvalidAssociationException.class, () -> link(third, first));
	}

	@Test
	void diamondHasPrerequisiteFirstTopologicalOrder() throws InvalidAssociationException, Exception {
		Project project = project();
		NormalTask root = task(project), left = task(project), right = task(project), end = task(project);
		link(root, left); link(root, right); link(left, end); link(right, end);

		List<com.microproject.model.task.Task> order = DependencyGraph.topologicalOrder(List.of(root, left, right, end));
		assertEquals(List.of(root, left, right, end), order);
	}

	@Test
	void topologicalOrderRejectsExistingCycle() throws Exception {
		Project project = project();
		NormalTask first = task(project), second = task(project), third = task(project);
		wire(first, second);
		wire(second, third);
		wire(third, first);

		assertThrows(CircularDependencyException.class,
			() -> DependencyGraph.topologicalOrder(List.of(first, second, third)));
	}

	@Test
	void dependsOnTraversesLongChainsIteratively() {
		assertLongChain(5_000);
		assertLongChain(50_000);
	}

	private static void assertLongChain(int count) {
		Project project = project();
		List<NormalTask> tasks = new ArrayList<>(count);
		for (int i = 0; i < count; i++) tasks.add(new NormalTask(project));
		for (int i = 1; i < count; i++) {
			Dependency dependency = Dependency.getInstance(tasks.get(i - 1), tasks.get(i));
			dependency.updateDependencyLists();
		}
		assertTrue(tasks.get(count - 1).dependsOn(tasks.get(0)));
		assertFalse(tasks.get(0).dependsOn(tasks.get(count - 1)));
		assertTrue(DependencyGraph.hasSuccessorPath(tasks.get(0), tasks.get(count - 1)));
		assertFalse(DependencyGraph.hasSuccessorPath(tasks.get(count - 1), tasks.get(0)));
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		return Project.createProject(ResourcePool.createRourcePool("test", undo), undo);
	}

	private static NormalTask task(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private static void link(NormalTask predecessor, NormalTask successor) throws InvalidAssociationException {
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, "test");
	}

	private static void wire(NormalTask predecessor, NormalTask successor) {
		Dependency dependency = Dependency.getInstance(predecessor, successor);
		dependency.updateDependencyLists();
	}
}
