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
package com.microproject.pm.criticalpath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskReferenceKindTest {
	@Test
	void taskReferenceRetainsItsLegacyCrossTypeComparisonContract() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task-reference", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		NormalTask otherTask = project.createScriptedTask();
		PredecessorTaskList.TaskReference reference = new PredecessorTaskList.TaskReference(task);

		assertEquals(0, reference.compareTo(task));
		assertEquals(-1, reference.compareTo(otherTask));
		assertEquals(0, reference.compareTo(reference));
		assertEquals(-1, reference.compareTo(new PredecessorTaskList.TaskReference(task)));
		assertEquals(-1, reference.compareTo(new Object()));
		assertSame(task, reference.getTask());
	}

	@Test
	void codesAndOppositeKindsPreserveParentBoundarySemantics() {
		assertEquals(-1, PredecessorTaskList.TaskReference.Kind.PARENT_BEGIN.code());
		assertEquals(0, PredecessorTaskList.TaskReference.Kind.CHILD.code());
		assertEquals(1, PredecessorTaskList.TaskReference.Kind.PARENT_END.code());
		assertEquals(PredecessorTaskList.TaskReference.Kind.PARENT_END,
				PredecessorTaskList.TaskReference.Kind.PARENT_BEGIN.opposite());
		assertEquals(PredecessorTaskList.TaskReference.Kind.CHILD,
				PredecessorTaskList.TaskReference.Kind.CHILD.opposite());
		assertEquals(PredecessorTaskList.TaskReference.Kind.PARENT_BEGIN,
				PredecessorTaskList.TaskReference.Kind.PARENT_END.opposite());
		assertEquals(PredecessorTaskList.TaskReference.Kind.PARENT_BEGIN,
				PredecessorTaskList.TaskReference.Kind.fromCode(-1));
		assertThrows(IllegalArgumentException.class,
				() -> PredecessorTaskList.TaskReference.Kind.fromCode(9));
	}
}
