/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.pm.task.Project;

/** Canonical, undoable command service for excluding accidental CCPM observations. */
public final class CriticalChainBufferHistoryService {
	private static final Logger LOGGER = Logger.getLogger(CriticalChainBufferHistoryService.class.getName());
	public enum Status { CHANGED, REJECTED, FAILED }
	public record Outcome(Status status, String reason, CriticalChainBufferHistory.Point observation) {
		public boolean changed() { return status == Status.CHANGED; }
	}

	public Outcome retract(Project project, UUID observationId, String reason, String actorId, String actorName) {
		if (project == null) return rejected("no-project", observationId);
		if (project.isReadOnly()) return rejected("read-only", observationId);
		if (observationId == null) return rejected("no-observation", null);
		if (reason == null || reason.isBlank()) return rejected("reason-required", observationId);
		CriticalChainBufferHistory history = project.findTransientDocumentState(CriticalChainBufferHistory.class);
		if (history == null) return rejected("history-empty", observationId);
		HistoryState before = HistoryState.capture(history);
		CriticalChainBufferHistory.Point point;
		try {
			point = history.retract(observationId, reason.trim(), actorId, actorName, Instant.now());
		} catch (RuntimeException exception) {
			LOGGER.severe(() -> "UI_COMMAND id=CCPM_BUFFER_OBSERVATION_RETRACT status=failed observationId="
				+ observationId + " error=" + exception.getClass().getSimpleName());
			return new Outcome(Status.FAILED, "mutation-failed", null);
		}
		if (point == null) return rejected("observation-not-found", observationId);
		HistoryState after = HistoryState.capture(history);
		postUndo(project, before, after);
		LOGGER.info(() -> "UI_COMMAND id=CCPM_BUFFER_OBSERVATION_RETRACT status=changed observationId=" + observationId
			+ " modelBefore=active observation modelAfter=retracted undo=posted");
		return new Outcome(Status.CHANGED, "retracted", point);
	}

	private static Outcome rejected(String reason, UUID observationId) {
		LOGGER.warning(() -> "UI_COMMAND id=CCPM_BUFFER_OBSERVATION_RETRACT status=rejected reason=" + reason
			+ " observationId=" + observationId);
		return new Outcome(Status.REJECTED, reason, null);
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
