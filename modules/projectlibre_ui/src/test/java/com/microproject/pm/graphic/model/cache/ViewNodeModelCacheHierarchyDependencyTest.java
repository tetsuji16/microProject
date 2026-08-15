package com.microproject.pm.graphic.model.cache;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

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
