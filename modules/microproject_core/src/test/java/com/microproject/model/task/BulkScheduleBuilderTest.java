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
package com.microproject.model.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.model.dependency.Dependency;
import com.microproject.model.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class BulkScheduleBuilderTest {
	@Test
	void initializesFiveThousandReverseOrderedTasksInOneScheduleTransaction() {
		assertTimeout(Duration.ofSeconds(30), () -> {
			int count = 5_000;
			DataFactoryUndoController undo = new DataFactoryUndoController();
			Project project = Project.createProject(ResourcePool.createRourcePool("bulk", undo), undo);
			List<NormalTask> tasks = new ArrayList<>(count);
			for (int i = count - 1; i >= 0; i--) {
				NormalTask task = new NormalTask(project);
				project.connectTask(task);
				tasks.add(task);
			}
			for (int i = 0; i < count - 1; i++) {
				Dependency dependency = Dependency.getInstance(tasks.get(count - i - 1), tasks.get(count - i - 2));
				dependency.updateDependencyLists();
			}

			int before = project.getCalculationStateCount();
			BulkScheduleBuilder.initialize(project, false, false);

			assertEquals(3, project.getCalculationStateCount() - before,
				"Initialization should publish one forward/reverse schedule calculation");
			assertTrue(tasks.get(0).getStart() >= tasks.get(count - 1).getStart(),
				"The dependent task should not start before the prerequisite");
		});
	}
}
