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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import org.junit.jupiter.api.Test;

class UndoStateListenerTest {
	@Test
	void failingObserverCannotRejectAnEditAlreadyAddedAtCommit() {
		UndoController controller = new UndoController();
		controller.addUndoStateListener(event -> { throw new AssertionError("observer failed"); });

		controller.commitEdit(new javax.swing.undo.AbstractUndoableEdit() { private static final long serialVersionUID = 1L; });

		assertTrue(controller.canUndo());
	}
	@Test
	void observerReceivesEditUndoRedoAndClearWithoutManualUiRefresh() {
		UndoController controller = new UndoController();
		List<UndoStateEvent.Cause> causes = new ArrayList<>();
		controller.addUndoStateListener(event -> causes.add(event.cause()));

		controller.getEditSupport().postEdit(new AbstractUndoableEdit());
		controller.undo();
		controller.redo();
		controller.clear();

		assertEquals(List.of(UndoStateEvent.Cause.EDIT_ADDED, UndoStateEvent.Cause.UNDO,
				UndoStateEvent.Cause.REDO, UndoStateEvent.Cause.CLEARED), causes);
	}
}
