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
package com.microproject.core.pm.exchange.converters.op;

import java.util.logging.Logger;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.NormalTask;

/**
 * Copies a microproject Task into a microproject NormalTask. Both sides use the
 * same microproject model, so this is a direct typed-field copy (the deleted
 * reflection-based FieldUtil.convertFields path is no longer used; see issue
 * #154).
 * @author Laurent Chretienneau
 */
public class OpTaskConverter {
	protected static Logger log = Logger.getLogger("OpTaskConverter");

	public void to(NormalTask opTask, Task task, OpImportState state) {
		if (task.getName() != null)
			opTask.setName(task.getName());
		if (task.getWbs() != null)
			opTask.setWbs(task.getWbs());
		if (task.getNotes() != null)
			opTask.setNotes(task.getNotes());
		opTask.setId(task.getId());
		opTask.setCreated(task.getCreated());
		opTask.setStart(task.getStart());
		opTask.setEnd(task.getEnd());
		opTask.setPercentComplete(task.getPercentComplete());
		opTask.setPhysicalPercentComplete(task.getPhysicalPercentComplete());
		opTask.setDeadline(task.getDeadline());
		opTask.setLevelingDelay(task.getLevelingDelay());
		opTask.setEarnedValueMethod(task.getEarnedValueMethod());

		// schedule constraint
		int constraintType = task.getConstraintType();
		if (constraintType != 0) {
			opTask.setScheduleConstraint(constraintType, task.getConstraintDate());
		}
	}
}
