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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ProjectTaskKeyTest {
	@Test
	void combinesOwningProjectAndTaskIdentity() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("task-key", undo), undo);
		project.setUniqueId(4100L);
		NormalTask task = project.createScriptedTask();
		task.setUniqueId(77L);
		task.setProjectId(project.getUniqueId());

		ProjectTaskKey key = ProjectTaskKey.from(task).orElseThrow();

		assertEquals(4100L, key.owningProjectId());
		assertEquals(77L, key.taskUniqueId());
	}

	@Test
	void rejectsIdentityUntilBothDurableIdsExist() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("runtime-key", undo), undo);
		project.setUniqueId(0L);
		NormalTask task = project.createScriptedTask();
		task.setUniqueId(0L);

		assertTrue(ProjectTaskKey.from(task).isEmpty());
	}
}
