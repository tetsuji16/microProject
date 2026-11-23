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
package com.microproject.pm.graphic.network;

import java.awt.geom.Point2D;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.pm.task.Project;
import com.microproject.pm.graphic.network.PertManualPositions;

/**
 * Undoable edit for moving a Network Diagram box manually.
 */
public class NetworkBoxMoveEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;

	private final Project project;
	private final long taskUniqueId;
	private final Point2D oldPosition;
	private final Point2D newPosition;
	private final Object source;

	public NetworkBoxMoveEdit(Network network, long taskUniqueId, Point2D oldPosition, Point2D newPosition, Object source) {
		this.project = network.getProject();
		this.taskUniqueId = taskUniqueId;
		this.oldPosition = oldPosition != null ? new Point2D.Double(oldPosition.getX(), oldPosition.getY()) : null;
		this.newPosition = newPosition != null ? new Point2D.Double(newPosition.getX(), newPosition.getY()) : null;
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
		if (newPosition != null) {
			pmp.putPosition(taskUniqueId, new Point2D.Double(newPosition.getX(), newPosition.getY()));
		} else {
			pmp.removePosition(taskUniqueId);
		}
		if (project != null) {
			// Trigger layout update and repaint through the network view
			// The network view will pick up the new positions on next updateBounds()
		}
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		PertManualPositions pmp = getPertManualPositions();
		if (oldPosition != null) {
			pmp.putPosition(taskUniqueId, new Point2D.Double(oldPosition.getX(), oldPosition.getY()));
		} else {
			pmp.removePosition(taskUniqueId);
		}
		if (project != null) {
			// Trigger layout update and repaint through the network view
		}
	}

	public Object getSource() {
		return source;
	}
}