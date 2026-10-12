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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class CriticalPathTraceTest {
	@Test
	void reportsUnavailableTraceBeforeSchedulingIsInitialized() throws ReflectiveOperationException {
		Field lastInstanceField = CriticalPath.class.getDeclaredField("lastInstance");
		Field traceTaskField = CriticalPath.class.getDeclaredField("traceTask");
		lastInstanceField.setAccessible(true);
		traceTaskField.setAccessible(true);
		SchedulingAlgorithm previousInstance = (SchedulingAlgorithm) lastInstanceField.get(null);
		Task previousTask = (Task) traceTaskField.get(null);
		try {
			lastInstanceField.set(null, null);
			traceTaskField.set(null, null);
			Constructor<Project> constructor = Project.class.getDeclaredConstructor(boolean.class);
			constructor.setAccessible(true);
			Project uninitializedProject = constructor.newInstance(true);
			assertNull(uninitializedProject.getSchedulingAlgorithm());

			assertEquals("CriticalPath trace unavailable: no scheduling calculation has run.", CriticalPath.getTrace());
		} finally {
			lastInstanceField.set(null, previousInstance);
			traceTaskField.set(null, previousTask);
		}
	}

	@Test
	void reportsNoTaskWhenAnInitializedEngineHasNoTaskToTrace() throws ReflectiveOperationException {
		Field lastInstanceField = CriticalPath.class.getDeclaredField("lastInstance");
		Field traceTaskField = CriticalPath.class.getDeclaredField("traceTask");
		lastInstanceField.setAccessible(true);
		traceTaskField.setAccessible(true);
		CriticalPath previousInstance = (CriticalPath) lastInstanceField.get(null);
		Task previousTask = (Task) traceTaskField.get(null);
		Project project = null;
		try {
			DataFactoryUndoController undoController = new DataFactoryUndoController();
			ResourcePool resourcePool = ResourcePool.createRourcePool("critical-path-trace-test", undoController);
			project = Project.createProject(resourcePool, undoController);
			project.initialize(false, false);
			traceTaskField.set(null, null);

			assertTrue(CriticalPath.getTrace().contains("(no task is being traced)"));
		} finally {
			if (project != null)
				project.disconnect();
			lastInstanceField.set(null, previousInstance);
			traceTaskField.set(null, previousTask);
		}
	}
}
