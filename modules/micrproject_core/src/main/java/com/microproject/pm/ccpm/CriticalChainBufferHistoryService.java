/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.pm.task.Project;

/** Canonical, undoable command service for excluding accidental CCPM observations. */
public final class CriticalChainBufferHistoryService {
	public enum Status { CHANGED, REJECTED }
	public record Outcome(Status status, String reason, CriticalChainBufferHistory.Point observation) {
		public boolean changed() { return status == Status.CHANGED; }
	}

	public Outcome retract(Project project, UUID observationId, String reason, String actorId, String actorName) {
		if (project == null) return new Outcome(Status.REJECTED, "no-project", null);
		if (project.isReadOnly()) return new Outcome(Status.REJECTED, "read-only", null);
		if (observationId == null) return new Outcome(Status.REJECTED, "no-observation", null);
		if (reason == null || reason.isBlank()) return new Outcome(Status.REJECTED, "reason-required", null);
		CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (history == null) return new Outcome(Status.REJECTED, "history-empty", null);
		HistoryState before = HistoryState.capture(history);
		CriticalChainBufferHistory.Point point = history.retract(observationId, reason.trim(), actorId, actorName, Instant.now());
		if (point == null) return new Outcome(Status.REJECTED, "observation-not-found", null);
		HistoryState after = HistoryState.capture(history);
		postUndo(project, before, after);
		return new Outcome(Status.CHANGED, "retracted", point);
	}

	private static void postUndo(Project project, HistoryState before, HistoryState after) {
		UndoableEditSupport edits = project.getUndoController().getEditSupport();
		if (edits == null) return;
		edits.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public String getPresentationName() { return "Discard CCPM observation"; }
			@Override public void undo() { super.undo(); before.restore(project); }
			@Override public void redo() { super.redo(); after.restore(project); }
		});
	}

	private record HistoryState(List<CriticalChainBufferHistory.Point> points,
		List<CriticalChainBufferHistory.Retraction> retractions) {
		static HistoryState capture(CriticalChainBufferHistory history) {
			return new HistoryState(List.copyOf(history.points()), history.retractions());
		}
		void restore(Project project) {
			CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(
				CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
			history.replace(points, retractions);
		}
	}
}
