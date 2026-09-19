/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.undo;

import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.pm.task.Project;

/** Undo unit for the MSP-compatible Move Project operation. */
public final class ProjectStartDateEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private final long before;
	private final long after;

	public ProjectStartDateEdit(Project project, long before, long after) {
		this.project = project;
		this.before = before;
		this.after = after;
	}

	@Override public String getPresentationName() { return "Move Project"; }
	@Override public void undo() { super.undo(); apply(before); }
	@Override public void redo() { super.redo(); apply(after); }

	private void apply(long value) {
		project.setStartDate(value);
		project.setDirty(true);
	}
}
