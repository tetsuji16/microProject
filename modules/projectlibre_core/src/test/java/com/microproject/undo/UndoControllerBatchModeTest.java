package com.microproject.undo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.undo.AbstractUndoableEdit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.util.Environment;

class UndoControllerBatchModeTest {
	@AfterEach
	void resetBatchMode() {
		Environment.setBatchMode(false);
	}

	@Test
	void undoFailureRestoresPreviousBatchMode() {
		UndoController controller = new UndoController();
		controller.getEditSupport().postEdit(new AbstractUndoableEdit() {
			public void undo() {
				super.undo();
				throw new IllegalStateException("expected");
			}
		});

		assertThrows(IllegalStateException.class, controller::undo);
		assertFalse(Environment.isBatchMode());
	}

	@Test
	void redoFailurePreservesAnAlreadyActiveBatchMode() {
		UndoController controller = new UndoController();
		controller.getEditSupport().postEdit(new AbstractUndoableEdit() {
			public void redo() {
				super.redo();
				throw new IllegalStateException("expected");
			}
		});
		controller.undo();
		Environment.setBatchMode(true);

		assertThrows(IllegalStateException.class, controller::redo);
		assertTrue(Environment.isBatchMode());
	}
}
