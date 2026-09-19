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
package com.microproject.pm.earnedvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.ImageLink;
import com.microproject.field.FieldContext;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class EarnedValueIndicatorLabelTest {
	@Test
	void taskScheduleStatusUsesSpiLabel() {
		Project project = createProject();
		NormalTask task = new NormalTask(project) {
			public double getCpi(FieldContext fieldContext) {
				return 1.2D;
			}
			public double getSpi(FieldContext fieldContext) {
				return 0.8D;
			}
		};

		assertEquals("SPI=0.8", task.getScheduleStatusIndicator().getLabel());
		assertEquals("CPI=1.2", task.getBudgetStatusIndicator().getLabel());
	}

	@Test
	void resourceScheduleStatusUsesSpiLabel() {
		EnterpriseResource resource = new EnterpriseResource((ResourcePool) null) {
			public double getCpi(FieldContext fieldContext) {
				return 1.2D;
			}
			public double getSpi(FieldContext fieldContext) {
				return 0.8D;
			}
		};

		ImageLink schedule = resource.getScheduleStatusIndicator();
		ImageLink budget = resource.getBudgetStatusIndicator();

		assertEquals("SPI=0.8", schedule.getLabel());
		assertEquals("CPI=1.2", budget.getLabel());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}
}
