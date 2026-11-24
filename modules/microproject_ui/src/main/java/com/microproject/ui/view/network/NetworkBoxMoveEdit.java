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
package com.microproject.ui.view.network;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.task.Project;
import com.microproject.view.network.PertManualPositions;

/**
 * Undoable edit for moving a Network Diagram box manually.
 */
public class NetworkBoxMoveEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;

	private final Network network;
	private final Project project;
	private final long taskUniqueId;
	private final PertManualPositions.Position oldPosition;
	private final PertManualPositions.Position newPosition;
	private final Object source;

	public NetworkBoxMoveEdit(Network network, long taskUniqueId, PertManualPositions.Position oldPosition,
			PertManualPositions.Position newPosition, Object source) {
		this.network = network;
		this.project = network.getProject();
		this.taskUniqueId = taskUniqueId;
		this.oldPosition = oldPosition;
		this.newPosition = newPosition;
		this.source = source;
	}

	@Override
	public String getPresentationName() {
		return "Move Network Box";
	}

	private PertManualPositions getPertManualPositions() {
		return project.getOrCreateTransientDocumentState(PertManualPositions.class, PertManualPositions::new);
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		PertManualPositions pmp = getPertManualPositions();
		pmp.putPosition(taskUniqueId, newPosition);
		refresh();
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		PertManualPositions pmp = getPertManualPositions();
		pmp.putPosition(taskUniqueId, oldPosition);
		refresh();
	}

	private void refresh() {
		project.setDirty(true);
		network.relayout();
	}

	public Object getSource() {
		return source;
	}
}
