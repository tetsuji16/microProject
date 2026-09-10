/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CriticalChainBufferHistoryServiceTest {
	@Test
	void retractingAnAccidentalObservationIsAuditableAndUndoable() {
		Project project = project();
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		CriticalChainBufferHistory.Point point = new CriticalChainBufferHistory.Point(Instant.parse("2026-09-11T00:00:00Z"),
			"user-1", "Planner", 25D, 50D, "AMBER", "baseline-1");
		history.add(point);

		CriticalChainBufferHistoryService service = new CriticalChainBufferHistoryService();
		CriticalChainBufferHistoryService.Outcome outcome = service.retract(project, point.observationId(),
			"Accidental preview refresh", "user-1", "Planner");

		assertTrue(outcome.changed());
		assertTrue(history.points().isEmpty(), "retracted data must leave the visible chart");
		assertEquals(1, history.retractions().size());
		assertEquals("Accidental preview refresh", history.retractions().getFirst().reason());

		project.getUndoController().undo();
		assertEquals(1, history.points().size());
		assertTrue(history.retractions().isEmpty());
		project.getUndoController().redo();
		assertTrue(history.points().isEmpty());
		assertEquals(1, history.retractions().size());
	}

	@Test
	void retractionRequiresARationaleAndDoesNotChangeHistoryWhenRejected() {
		Project project = project();
		CriticalChainBufferHistory history = project.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		CriticalChainBufferHistory.Point point = new CriticalChainBufferHistory.Point(Instant.now(), "user", "Planner", 0D, 0D, "GREEN", "");
		history.add(point);

		CriticalChainBufferHistoryService.Outcome outcome = new CriticalChainBufferHistoryService().retract(project,
			point.observationId(), " ", "user", "Planner");

		assertFalse(outcome.changed());
		assertEquals("reason-required", outcome.reason());
		assertEquals(1, history.points().size());
		assertTrue(history.retractions().isEmpty());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-history", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		return project;
	}
}
