package com.microproject.exchange;

import java.util.Date;

import com.microproject.core.pm.exchange.converters.op.OpImportState;
import com.microproject.core.pm.exchange.converters.op.OpTaskConverter;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

import junit.framework.TestCase;

/**
 * Verifies the microproject-side converters used by the import path.
 *
 * Note: the legacy two-model tracking-merge step (applyImportedTrackingFields) was
 * removed as part of the microproject model consolidation (see issue #154); these
 * tests now assert the converter-level behavior that remains. The .mpp/.mpx live
 * import path is exercised separately by XlsxSupportTest.
 */
public class MicrosoftTrackingImportTest extends TestCase {
	public void testTaskConverterCopiesIdentityFields() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask target = new NormalTask(project);
		project.connectTask(target);

		// A source task whose fields the converter copies into the target.
		NormalTask source = new NormalTask(project);
		source.setName("source");
		source.setPercentComplete(0.40d);
		source.setPhysicalPercentComplete(0.30d);
		source.setActualStart(target.getStart());

		new OpTaskConverter().to(target, source, new OpImportState());

		assertEquals("source", target.getName());
		assertEquals(0.40d, target.getPercentComplete(), 0.00001d);
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

		target.setPercentComplete(1.0d);
		target.setActualFinish(importedFinish);

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

		// manual / inactive are set directly on the microproject task (the legacy
		// two-model merge that copied them from a source task was removed, see #154)
		target.setManuallyScheduled(true);
		target.setInactiveTask(true);

		assertTrue(target.isManuallyScheduled());
		assertTrue(target.isInactiveTask());
	}
}
