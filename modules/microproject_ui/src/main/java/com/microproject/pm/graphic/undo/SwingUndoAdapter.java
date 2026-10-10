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
package com.microproject.pm.graphic.undo;

import javax.swing.undo.AbstractUndoableEdit;
import com.microproject.undo.EditSupport;

import com.microproject.pm.task.ReversibleModelChange;

/** Bridges core model changes to Swing's document undo history. */
public final class SwingUndoAdapter {
	private SwingUndoAdapter() {
	}

	public static void post(EditSupport edits, ReversibleModelChange change) {
		post(edits, change, null);
	}

	public static void post(EditSupport edits, ReversibleModelChange change, String presentationName) {
		if (edits == null || change == null || !change.hasChanged())
			return;
		edits.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;

			@Override
			public void undo() {
				super.undo();
				change.undo();
			}

			@Override
			public void redo() {
				super.redo();
				change.redo();
			}

			@Override
			public String getPresentationName() {
				return presentationName == null ? super.getPresentationName() : presentationName;
			}
		});
	}
}
