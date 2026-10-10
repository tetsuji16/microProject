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

import java.util.Vector;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

/** Core-owned publisher for undo edits, including nested compound batches. */
public final class EditSupport {
	private final Vector<EditListener> listeners = new Vector<>();
	private int updateLevel;
	private CompoundEdit compoundEdit;

	public synchronized void addEditListener(EditListener listener) {
		listeners.addElement(listener);
	}

	public synchronized void removeEditListener(EditListener listener) {
		listeners.removeElement(listener);
	}

	public synchronized void postEdit(UndoableEdit edit) {
		if (updateLevel > 0) {
			compoundEdit.addEdit(edit);
			return;
		}
		fire(edit);
	}

	public synchronized void beginUpdate() {
		if (updateLevel == 0)
			compoundEdit = new CompoundEdit();
		updateLevel++;
	}

	public synchronized void endUpdate() {
		updateLevel--;
		if (updateLevel != 0)
			return;
		CompoundEdit completed = compoundEdit;
		completed.end();
		fire(completed);
		compoundEdit = null;
	}

	private void fire(UndoableEdit edit) {
		EditListener[] snapshot = listeners.toArray(new EditListener[0]);
		for (EditListener listener : snapshot)
			listener.editPosted(edit);
	}
}
