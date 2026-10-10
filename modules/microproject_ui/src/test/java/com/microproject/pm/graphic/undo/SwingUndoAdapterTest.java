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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import com.microproject.undo.EditSupport;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.ReversibleModelChange;

class SwingUndoAdapterTest {
	@Test
	void postsModelChangeIntoSwingUndoHistoryAndIgnoresNoopChanges() {
		EditSupport edits = new EditSupport();
		UndoManager undoManager = new UndoManager();
		edits.addEditListener(undoManager::addEdit);
		UndoableEdit[] posted = new UndoableEdit[1];
		edits.addEditListener(edit -> posted[0] = edit);
		AtomicInteger value = new AtomicInteger(1);

		SwingUndoAdapter.post(edits, ReversibleModelChange.changed(() -> value.set(0), () -> value.set(1)),
			"SaveSnapshot: Project test(1)");
		assertEquals(1, value.get());
		assertTrue(undoManager.canUndo(), "a changed model operation must be posted");
		assertEquals("SaveSnapshot: Project test(1)", posted[0].getPresentationName());
		undoManager.undo();
		assertEquals(0, value.get());
		undoManager.redo();
		assertEquals(1, value.get());

		undoManager.discardAllEdits();
		SwingUndoAdapter.post(edits, ReversibleModelChange.unchanged());
		assertFalse(undoManager.canUndo(), "a no-op model operation must not create an undo entry");
	}
}
