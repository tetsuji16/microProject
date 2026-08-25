/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.undo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.CompoundEdit;

/** Compound edit that compensates already-applied children when a later child fails. */
public final class AtomicCompoundEdit extends CompoundEdit {
	private boolean ended;
	private boolean done = true;
	private boolean alive = true;

	@Override public boolean addEdit(UndoableEdit edit) {
		if (ended) return false;
		return super.addEdit(java.util.Objects.requireNonNull(edit, "edit"));
	}

	@Override public void end() { ended = true; super.end(); }

	@Override public void undo() throws CannotUndoException {
		if (!canUndo()) throw new CannotUndoException();
		List<UndoableEdit> completed = new ArrayList<>();
		try {
			for (int index = edits.size() - 1; index >= 0; index--) {
				UndoableEdit edit = edits.get(index);
				edit.undo();
				completed.add(edit);
			}
			done = false;
		} catch (RuntimeException failure) {
			for (int index = completed.size() - 1; index >= 0; index--)
				try { completed.get(index).redo(); } catch (RuntimeException compensation) { failure.addSuppressed(compensation); }
			CannotUndoException wrapped = failure instanceof CannotUndoException value ? value : new CannotUndoException();
			if (wrapped != failure) wrapped.initCause(failure);
			throw wrapped;
		}
	}

	@Override public void redo() throws CannotRedoException {
		if (!canRedo()) throw new CannotRedoException();
		List<UndoableEdit> completed = new ArrayList<>();
		try {
			for (UndoableEdit edit : edits) {
				edit.redo();
				completed.add(edit);
			}
			done = true;
		} catch (RuntimeException failure) {
			for (int index = completed.size() - 1; index >= 0; index--)
				try { completed.get(index).undo(); } catch (RuntimeException compensation) { failure.addSuppressed(compensation); }
			CannotRedoException wrapped = failure instanceof CannotRedoException value ? value : new CannotRedoException();
			if (wrapped != failure) wrapped.initCause(failure);
			throw wrapped;
		}
	}

	@Override public String getPresentationName() {
		return edits.isEmpty() ? "Atomic edit" : edits.get(0).getPresentationName();
	}

	@Override public boolean canUndo() { return ended && alive && done && edits.stream().allMatch(UndoableEdit::canUndo); }
	@Override public boolean canRedo() { return ended && alive && !done && edits.stream().allMatch(UndoableEdit::canRedo); }
	@Override public void die() { super.die(); alive = false; }
}
