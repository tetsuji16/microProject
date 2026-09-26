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
package com.microproject.grouping.core.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class NodeModelUtilTest {
	@Test
	void identifiesSubprojectAndRegularTaskNodes() {
		assertTrue(NodeModelUtil.nodeIsSubproject(
			NodeFactory.getInstance().createNode(new DefaultSubProj(null, 42L))));
		assertFalse(NodeModelUtil.nodeIsSubproject(NodeFactory.getInstance().createNode(new NormalTask())));
	}

	@Test
	void taskParentsCanOnlyContainTasksFromTheSameProject() {
		Project firstProject = createProject("first");
		Project secondProject = createProject("second");
		NodeFactory nodeFactory = NodeFactory.getInstance();

		assertTrue(NodeModelUtil.canBeChildOf(
			nodeFactory.createNode(createTask(firstProject)),
			nodeFactory.createNode(createTask(firstProject))));
		assertFalse(NodeModelUtil.canBeChildOf(
			nodeFactory.createNode(createTask(firstProject)),
			nodeFactory.createNode(createTask(secondProject))));
		assertFalse(NodeModelUtil.canBeChildOf(
			nodeFactory.createNode(new DefaultSubProj(firstProject, 42L)),
			nodeFactory.createNode(createTask(firstProject))));
	}

	private NormalTask createTask(Project project) {
		NormalTask task = project.newNormalTaskInstance();
		task.setOwningProject(project);
		return task;
	}

	private Project createProject(String name) {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		return Project.createProject(ResourcePool.createRourcePool(name, undoController), undoController);
	}
}
