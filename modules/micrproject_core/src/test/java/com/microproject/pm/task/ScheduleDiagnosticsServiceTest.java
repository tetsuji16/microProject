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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleDiagnosticsServiceTest {
	@Test
	void reportsManualInactiveAndConstraintCauses() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("diagnostic-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName("Inspect me");
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.setManualDates(project.getStart(), project.getStart() + 1L);
		task.setInactiveTask(true);
		task.setConstraintType(ConstraintType.MSO);

		var types = new ScheduleDiagnosticsService().diagnose(task).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();

		assertTrue(types.contains(ScheduleDiagnosticsService.Type.MANUAL));
		assertTrue(types.contains(ScheduleDiagnosticsService.Type.INACTIVE));
	}
}
