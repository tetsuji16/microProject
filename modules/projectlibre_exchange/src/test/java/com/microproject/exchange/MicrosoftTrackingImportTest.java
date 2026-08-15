package com.microproject.exchange;

import java.util.Date;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;
import com.projectlibre.core.pm.exchange.converters.op.OpImportState;
import com.projectlibre.core.pm.exchange.converters.op.OpTaskConverter;

import junit.framework.TestCase;

public class MicrosoftTrackingImportTest extends TestCase {
	public void testTrackingFieldsAreAppliedAfterAssignments() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask target = new NormalTask(project);
		project.connectTask(target);

		com.projectlibre.pm.tasks.Task source = new com.projectlibre.pm.tasks.Task();
		source.setPropertyValue("percentComplete", 0.40d);
		source.setPropertyValue("physicalPercentComplete", 0.30d);
		source.setPropertyValue("actualStart", new Date(target.getStart()));

		new MicrosoftImporter().applyImportedTrackingFields(source, target);

		assertEquals(0.40d, target.getPercentComplete(), 0.00001d);
		assertEquals(0.40d, target.getPercentWorkComplete(), 0.00001d);
		assertEquals(0.30d, target.getPhysicalPercentComplete(), 0.00001d);
		assertEquals(target.getStart(), target.getActualStart());
	}

	public void testActualFinishIsPreservedForCompletedTasks() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask target = new NormalTask(project);
		project.connectTask(target);
		long importedFinish = target.getEnd();

		com.projectlibre.pm.tasks.Task source = new com.projectlibre.pm.tasks.Task();
		source.setPropertyValue("percentComplete", 1.0d);
		source.setPropertyValue("actualFinish", new Date(importedFinish));

		new MicrosoftImporter().applyImportedTrackingFields(source, target);

		assertEquals(1.0d, target.getPercentComplete(), 0.00001d);
		assertEquals(importedFinish, target.getActualFinish());
	}

	public void testManualAndInactiveTaskModesAreApplied() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask target = new NormalTask(project);
		project.connectTask(target);

		com.projectlibre.pm.tasks.Task source = new com.projectlibre.pm.tasks.Task();
		source.setPropertyValue("manuallyScheduled", Boolean.TRUE);
		source.setPropertyValue("inactiveTask", Boolean.TRUE);

		new OpTaskConverter().to(target, source, new OpImportState());

		assertTrue(target.isManuallyScheduled());
		assertTrue(target.isInactiveTask());
	}
}
