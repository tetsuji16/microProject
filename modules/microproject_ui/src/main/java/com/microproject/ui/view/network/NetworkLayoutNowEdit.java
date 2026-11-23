/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.view.network;

import com.microproject.pm.graphic.network.PertManualPositions;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/** Undo record for restoring the automatic Network Diagram layout. */
public final class NetworkLayoutNowEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;
	private final Network network;
	private final Map<Long, PertManualPositions.Position> previousPositions;

	public NetworkLayoutNowEdit(Network network, Map<Long, PertManualPositions.Position> previousPositions) {
		this.network = network;
		this.previousPositions = new HashMap<>(previousPositions);
	}

	@Override
	public String getPresentationName() {
		return "Layout Network Diagram Now";
	}

	@Override
	public void undo() throws CannotUndoException {
		super.undo();
		PertManualPositions state = network.getProject().getOrCreateTransientDocumentState(
			PertManualPositions.class, PertManualPositions::new);
		state.setPositions(previousPositions);
		refresh();
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		PertManualPositions state = network.getProject().getOrCreateTransientDocumentState(
			PertManualPositions.class, PertManualPositions::new);
		state.clear();
		refresh();
	}

	private void refresh() {
		network.getProject().setDirty(true);
		network.relayout();
	}
}
