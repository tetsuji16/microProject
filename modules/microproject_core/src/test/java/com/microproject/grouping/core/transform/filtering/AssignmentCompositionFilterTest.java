/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class AssignmentCompositionFilterTest {
	@Test
	void composesAnAssignmentThroughItsResource() {
		Assignment assignment = createAssignment();
		ResourceImpl resource = (ResourceImpl) assignment.getResource();
		Node assignmentNode = NodeFactory.getInstance().createNode(assignment);
		NodeFilter delegate = new NodeFilter() {
			@Override
			public boolean evaluate(Object value) {
				assertSame(resource, value);
				return true;
			}
		};

		assertTrue(new AssignmentCompositionFilter().evaluate(delegate, assignmentNode));
	}

	@Test
	void leavesNonAssignmentNodesUnchanged() {
		Node taskNode = NodeFactory.getInstance().createNode(new NormalTask());
		NodeFilter delegate = new NodeFilter() {
			@Override
			public boolean evaluate(Object value) {
				assertSame(taskNode, value);
				return true;
			}
		};

		assertTrue(new AssignmentCompositionFilter().evaluate(delegate, taskNode));
	}

	private Assignment createAssignment() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		var resourcePool = com.microproject.pm.resource.ResourcePool.createRourcePool("filter-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		ResourceImpl resource = resourcePool.newResourceInstance();
		return Assignment.getInstance(task, resource, 1.0D, 0);
	}
}
