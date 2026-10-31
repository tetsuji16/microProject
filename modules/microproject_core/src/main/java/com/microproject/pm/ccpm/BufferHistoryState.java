/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.ccpm;

import java.util.List;

import com.microproject.pm.task.Project;

/** Immutable snapshot of a project's CCPM buffer history, used for Undo/Redo and rollback. */
record BufferHistoryState(List<CriticalChainBufferHistory.Point> points,
	List<CriticalChainBufferHistory.Retraction> retractions) {
	/** Returns {@code null} when the project has no history yet. */
	static BufferHistoryState capture(CriticalChainBufferHistory history) {
		return history == null ? null : new BufferHistoryState(history.points(), history.retractions());
	}

	void restore(Project project) {
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		history.replace(points, retractions);
	}
}
