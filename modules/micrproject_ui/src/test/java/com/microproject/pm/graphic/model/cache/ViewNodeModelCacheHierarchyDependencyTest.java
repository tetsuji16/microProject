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

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.application.task.TaskCommandGateway;

import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ViewNodeModelCacheHierarchyDependencyTest {
	@Test
	void createHierarchyDependencyMovesChildUnderParent() throws Exception {
		Project project = createProject();
		NormalTask parentTask = createTask(project, "Parent");
		NormalTask childTask = createTask(project, "Child");

		ReferenceNodeModelCache referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		referenceCache.setTaskCommandGateway(new TaskCommandGateway(project));
		ViewNodeModelCache viewCache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance()
			.createFilteredCache(referenceCache, "hierarchy-test", null);

		Node parentNode = (Node) project.getTaskModel().search(parentTask);
		Node childNode = (Node) project.getTaskModel().search(childTask);

		viewCache.createHierarchyDependency((GraphicNode) viewCache.getGraphicNode(parentNode),
			(GraphicNode) viewCache.getGraphicNode(childNode));

		assertSame(parentNode, project.getTaskModel().getHierarchy().getParent(childNode));
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
}
