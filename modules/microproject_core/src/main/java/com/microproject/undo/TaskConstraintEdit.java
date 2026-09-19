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
package com.microproject.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.task.Task;

/**
 * Undoable edit for Gantt drag constraint changes.
 */
public class TaskConstraintEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;

	private final Task task;
	private final int oldConstraintType;
	private final long oldConstraintDate;
	private final int newConstraintType;
	private final long newConstraintDate;
	private final Object source;

	public TaskConstraintEdit(Task task, int oldConstraintType, long oldConstraintDate, int newConstraintType, long newConstraintDate, Object source) {
		this.task = task;
		this.oldConstraintType = oldConstraintType;
		this.oldConstraintDate = oldConstraintDate;
		this.newConstraintType = newConstraintType;
		this.newConstraintDate = newConstraintDate;
		this.source = source;
	}

	public String getPresentationName() {
		return "TaskConstraint";
	}

	public void redo() throws CannotRedoException {
		super.redo();
		task.setScheduleConstraintAndUpdate(newConstraintType, newConstraintDate);
	}

	public void undo() throws CannotUndoException {
		super.undo();
		task.setScheduleConstraintAndUpdate(oldConstraintType, oldConstraintDate);
	}

	public Object getSource() {
		return source;
	}
}
