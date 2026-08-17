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
package com.microproject.exchange;

import java.util.Date;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

import junit.framework.TestCase;

/**
 * Verifies microproject-model behavior relevant to the import path.
 *
 * Note: the legacy two-model tracking-merge step (applyImportedTrackingFields) and
 * the obsolete OpenProj-era converters (converters.op, see issues #154/#189) were
 * removed; these tests assert the model behavior that remains. The .mpp/.mpx live
 * import path is exercised separately by XlsxSupportTest.
 */
public class MicrosoftTrackingImportTest extends TestCase {
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
