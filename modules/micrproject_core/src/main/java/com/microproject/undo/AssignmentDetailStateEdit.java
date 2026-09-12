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
package com.microproject.undo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;

/** Restores assignment detail state after connection edits have been replayed. */
public final class AssignmentDetailStateEdit extends AbstractUndoableEdit {
	private final Map<Assignment, Object> before;
	private final Map<Assignment, Object> after;

	public AssignmentDetailStateEdit(Map<Assignment, Object> before,
			Map<Assignment, Object> after) {
		this.before = new LinkedHashMap<>(before);
		this.after = new LinkedHashMap<>(after);
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		restore(before);
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		restore(after);
	}

	private void restore(Map<Assignment, Object> states) {
		List<Map.Entry<Assignment, Object>> entries = new ArrayList<>(states.entrySet());
		// Restoring a replacement can recalculate the task and adjust other
		// assignments; restore the source last so its actual/remaining split
		// is the final observable state.
		for (int index = entries.size() - 1; index >= 0; index--) {
			Map.Entry<Assignment, Object> entry = entries.get(index);
			Assignment assignment = entry.getKey();
			if (assignment.getTask() instanceof NormalTask
					&& ((NormalTask) assignment.getTask()).findAssignment(assignment.getResource()) == assignment)
				// The surrounding creation/deletion edit has already recalculated the
				// task. Recalculating while restoring this final replacement detail
				// would redistribute its remaining work again.
				assignment.restoreDetail(entry.getValue());
		}
	}

	@Override
	public String getPresentationName() {
		return "AssignmentDetail";
	}
}
