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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.microproject.pm.costing.ExpenseType;
import com.microproject.pm.costing.EarnedValueMethodType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskExpenseTypeTest {
	@Test
	@SuppressWarnings("deprecation")
	void earnedValueMethodKindUsesStableCodesAndPreservesUnknownLegacyValues() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		task.setEarnedValueMethodKind(EarnedValueMethodType.Kind.PHYSICAL_PERCENT_COMPLETE);
		assertEquals(EarnedValueMethodType.Kind.PHYSICAL_PERCENT_COMPLETE, task.getEarnedValueMethodKind());
		assertEquals(EarnedValueMethodType.Kind.PHYSICAL_PERCENT_COMPLETE.code(), task.getEarnedValueMethod());

		task.setEarnedValueMethod(99);
		assertEquals(99, task.getEarnedValueMethod());
		assertThrows(IllegalArgumentException.class, task::getEarnedValueMethodKind);
	}

	@Test
	@SuppressWarnings("deprecation")
	void effectiveKindUsesTaskThenProjectAndKeepsIntegerCompatibility() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		project.setExpenseKind(ExpenseType.Kind.OVERHEAD);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		assertEquals(ExpenseType.Kind.OVERHEAD, task.getEffectiveExpenseKind());
		assertEquals(ExpenseType.Kind.OVERHEAD.code(), task.getEffectiveExpenseType());

		task.setExpenseKind(ExpenseType.Kind.DIRECT);
		assertEquals(ExpenseType.Kind.DIRECT, task.getEffectiveExpenseKind());
		assertEquals(ExpenseType.Kind.DIRECT.code(), task.getEffectiveExpenseType());
	}

	@Test
	@SuppressWarnings("deprecation")
	void unknownStoredExpenseTypeIsNotCoercedByTypedAccess() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		task.setExpenseType(99);

		assertEquals(99, task.getExpenseType());
		assertEquals(99, task.getEffectiveExpenseType());
		assertThrows(IllegalArgumentException.class, task::getEffectiveExpenseKind);
	}
}
