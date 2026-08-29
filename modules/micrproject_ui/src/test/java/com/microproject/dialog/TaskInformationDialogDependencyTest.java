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
package com.microproject.dialog;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class TaskInformationDialogDependencyTest {
	@Test
	void createsDefaultFinishToStartPredecessorAndSuccessorLinks() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("test", undoController), undoController);
		NormalTask predecessor = addTask(project);
		NormalTask current = addTask(project);
		NormalTask successor = addTask(project);

		Dependency predecessorLink = TaskInformationDialog.createDependency(current, predecessor, true, this);
		Dependency successorLink = TaskInformationDialog.createDependency(current, successor, false, this);

		assertSame(predecessor, predecessorLink.getPredecessor());
		assertSame(current, predecessorLink.getSuccessor());
		assertSame(current, successorLink.getPredecessor());
		assertSame(successor, successorLink.getSuccessor());
		assertEquals(DependencyType.Kind.FS.code(), predecessorLink.getDependencyType());
		assertEquals(DependencyType.Kind.FS.code(), successorLink.getDependencyType());
		assertEquals(1, current.getPredecessorList().size());
		assertEquals(1, current.getSuccessorList().size());
	}

	@Test
	void offersTasksFromOtherOpenProjectsForCrossProjectLinks() throws Exception {
		Project firstProject = newProject("First");
		Project secondProject = newProject("Second");
		NormalTask current = addTask(firstProject);
		NormalTask localCandidate = addTask(firstProject);
		NormalTask crossProjectCandidate = addTask(secondProject);

		List<com.microproject.pm.task.Task> candidates = TaskInformationDialog.getLinkableTasks(
				current, true, List.of(firstProject, secondProject));

		assertEquals(2, candidates.size());
		assertTrue(candidates.contains(localCandidate));
		assertTrue(candidates.contains(crossProjectCandidate));
		for (DependencyType.Kind kind : DependencyType.Kind.values()) {
			Dependency dependency = TaskInformationDialog.createDependency(current, crossProjectCandidate, true, kind.code(), this);
			assertSame(crossProjectCandidate, dependency.getPredecessor());
			assertSame(current, dependency.getSuccessor());
			assertEquals(kind.code(), dependency.getDependencyType());
			DependencyService.getInstance().remove(dependency, this, false);
		}
	}

	@Test
	void dependencyTypeChoicesExposeAllMicrosoftProjectLinkTypes() {
		TaskInformationDialog.DependencyTypeChoice[] choices = TaskInformationDialog.dependencyTypeChoices();
		assertEquals(4, choices.length);
		assertTrue(choices[0].toString().contains("FS"));
		assertTrue(choices[1].toString().contains("SS"));
		assertTrue(choices[2].toString().contains("FF"));
		assertTrue(choices[3].toString().contains("SF"));
	}

	private Project newProject(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		return Project.createProject(ResourcePool.createRourcePool(name, undoController), undoController);
	}

	private NormalTask addTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}
}
