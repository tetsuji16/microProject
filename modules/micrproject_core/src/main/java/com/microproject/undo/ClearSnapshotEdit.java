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

import java.util.Collection;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.task.Project;

/**
 *
 */
public class ClearSnapshotEdit extends AbstractUndoableEdit{
	protected Project project;
	protected Object snapshotId;
	protected boolean entireProject;
	protected List selection;
	protected Collection snapshotDetail;

	
	public ClearSnapshotEdit(Project project, Object snapshotId, boolean entireProject, List selection, Collection snapshotDetail) {
		super();
		this.project = project;
		this.snapshotId = snapshotId;
		this.entireProject = entireProject;
		this.selection = selection;
		this.snapshotDetail=snapshotDetail;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "ClearSnapshot";
	}
	public void redo() throws CannotRedoException {
		super.redo();
		project.clearSnapshot(snapshotId, entireProject, selection, false);
	}
	public void undo() throws CannotUndoException {
		super.undo();
		project.restoreSnapshot(snapshotId, entireProject, selection, snapshotDetail);
	}
}
