/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotUndoException;

import org.junit.jupiter.api.Test;

class AtomicCompoundEditTest {
	@Test
	void failedChildUndoCompensatesPreviouslyUndoneChildren() {
		AtomicInteger state = new AtomicInteger(2);
		AtomicCompoundEdit compound = new AtomicCompoundEdit();
		compound.addEdit(new DeltaEdit(state, 1, true));
		compound.addEdit(new DeltaEdit(state, 1, false));
		compound.end();

		assertThrows(CannotUndoException.class, compound::undo);

		assertEquals(2, state.get());
	}

	@Test
	void editCaptureKeepsLegacyEditsOffHistoryUntilTransactionCommit() {
		UndoController controller = new UndoController();
		UndoController.EditCapture capture = controller.captureEdits();
		controller.commitEdit(new AbstractUndoableEdit() { });
		capture.close();

		assertEquals(false, controller.canUndo());
		controller.commitEdit(capture.edit());
		assertEquals(true, controller.canUndo());
	}

	private static final class DeltaEdit extends AbstractUndoableEdit {
		private final AtomicInteger state;
		private final int delta;
		private final boolean failUndo;
		private DeltaEdit(AtomicInteger state, int delta, boolean failUndo) {
			this.state = state;
			this.delta = delta;
			this.failUndo = failUndo;
		}
		@Override public void undo() {
			if (failUndo) throw new CannotUndoException();
			state.addAndGet(-delta);
			super.undo();
		}
		@Override public void redo() {
			state.addAndGet(delta);
			super.redo();
		}
	}
}
