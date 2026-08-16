package com.microproject.core.pm.exchange.converters.op;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #159: OpAssignmentConverter used to recreate assignments with a hardcoded
 * zero delay, silently dropping the leveling delay on the .pod conversion path.
 */
public class OpAssignmentConverterDelayTest {

	@Test
	public void assignmentDelayIsPreservedThroughConversion() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		Assignment source = Assignment.getInstance(task, ResourceImpl.getUnassignedInstance(), 1.0, 5000L);
		assertEquals(5000L, source.getDelay());

		OpImportState state = new OpImportState();
		state.mapOpTask(task, task);

		Assignment converted = new OpAssignmentConverter().to(source, state);

		assertEquals(5000L, converted.getDelay());
	}
}
