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
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.model.gantt.GanttDrawing;
import com.microproject.model.gantt.GanttDrawings;

/** One Gantt drawing operation (add, move, format or delete) as a single undo step. */
public final class GanttDrawingEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;

	private final GanttDrawings target;
	private final List<GanttDrawing> before;
	private final List<GanttDrawing> after;

	public GanttDrawingEdit(GanttDrawings target, List<GanttDrawing> before, List<GanttDrawing> after) {
		this.target = target;
		this.before = new ArrayList<>(before);
		this.after = new ArrayList<>(after);
	}

	@Override public void undo() throws CannotUndoException {
		super.undo();
		target.restore(before);
	}

	@Override public void redo() throws CannotRedoException {
		super.redo();
		target.restore(after);
	}
}
