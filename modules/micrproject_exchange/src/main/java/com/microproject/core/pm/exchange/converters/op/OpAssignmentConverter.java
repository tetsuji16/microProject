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

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;

/**
 * Converts a microproject Assignment into another microproject Assignment (the .pod
 * (de)serialization path). Both sides use the same microproject model, so this is a
 * direct typed-field copy. Timephased data is intentionally skipped (see issue #154).
 * @author Laurent Chretienneau
 */
public class OpAssignmentConverter {

	public com.microproject.pm.assignment.Assignment to(Assignment assignment, OpImportState state) {
		com.microproject.pm.resource.Resource resource;
		if (assignment.getResource().getUniqueId() == EnterpriseResource.UNASSIGNED_ID)
			resource = ResourceImpl.getUnassignedInstance();
		else
			resource = state.getOpResource(assignment.getResource());
		if (resource == null)
			throw new IllegalStateException("Unable to resolve resource for assignment " + assignment);

		NormalTask task = state.getOpTask(assignment.getTask());
		if (task == null)
			throw new IllegalStateException("Unable to resolve task for assignment " + assignment);

		com.microproject.pm.assignment.Assignment opAssignment = com.microproject.pm.assignment.Assignment
				.getInstance(task, resource, assignment.getUnits(), assignment.getDelay());

		if (assignment.getName() != null)
			opAssignment.setName(assignment.getName());
		opAssignment.setStart(assignment.getStart());
		opAssignment.setEnd(assignment.getEnd());
		opAssignment.setWork(assignment.getWork(null), null);
		opAssignment.setActualStart(assignment.getActualStart());
		opAssignment.setActualFinish(assignment.getActualFinish());
		opAssignment.setActualWork(assignment.getActualWork(null), null);
		opAssignment.setRemainingWork(assignment.getRemainingWork(null), null);
		opAssignment.setPercentComplete(assignment.getPercentComplete());
		opAssignment.setWorkContourType(assignment.getWorkContourType());

		return opAssignment;
	}
}
