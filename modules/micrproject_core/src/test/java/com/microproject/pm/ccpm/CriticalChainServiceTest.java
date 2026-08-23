/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CriticalChainServiceTest {
	@Test
	void previewFindsResourceConstraintWithoutMutatingSchedule() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Analysis analysis = service.preview(fixture.project, List.of(fixture.resource));

		assertEquals(1, analysis.levelingPlan().changes().size());
		assertEquals(0L, second.getLevelingDelay());
		assertTrue(analysis.criticalTaskIds().contains(Long.valueOf(second.getUniqueId())));
		assertTrue(analysis.resourcePredecessors().getOrDefault(Long.valueOf(second.getUniqueId()), List.of()).contains(Long.valueOf(first.getUniqueId())));
		assertTrue(analysis.graphEdges().stream().anyMatch(edge -> edge.predecessorTaskId() == first.getUniqueId()
			&& edge.successorTaskId() == second.getUniqueId()
			&& edge.kind() == CriticalChainService.ChainEdge.Kind.RESOURCE_CONSTRAINT));
	}

	@Test
	void applyRequiresExplicitEnablementAndPersistsSettings() {
		Fixture fixture = fixture();
		CriticalChainService service = new CriticalChainService();
		assertThrows(IllegalStateException.class, () -> service.apply(fixture.project, List.of(fixture.resource)));

		CriticalChainService.Settings settings = service.settings(fixture.project);
		settings.setEnabled(true);
		settings.setBufferFraction(0.4D);
		assertEquals(settings, service.settings(fixture.project));
		assertTrue(fixture.project.getExtraFields().isEmpty(), "CCPM must not alter the legacy POD object graph");
		assertTrue(settings.isEnabled());
		assertTrue(service.requiresMpo(fixture.project));
	}

	@Test
	void applyLevelsResourceConflictsOnlyAfterCcpMIsEnabled() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);

		CriticalChainService.Analysis result = service.apply(fixture.project, List.of(fixture.resource));

		assertTrue(second.getLevelingDelay() > 0L);
		assertTrue(result.projectBufferMillis() >= 0L);
		assertTrue(result.projectBuffer().remainingMillis() <= result.projectBuffer().plannedMillis());
		assertTrue(service.findBaseline(fixture.project) != null);
		assertEquals(CriticalChainService.BufferStatus.GREEN, result.projectBuffer().status());

		CriticalChainService.Baseline baseline = service.findBaseline(fixture.project);
		service.restoreBaseline(fixture.project, new CriticalChainService.Baseline(
			Math.max(0L, fixture.project.getEnd() - CalendarOption.getInstance().getMillisPerDay()), baseline.projectBufferMillis(),
			baseline.bufferFraction(), baseline.criticalTaskIds(), baseline.feedingTaskStartMillis(), baseline.feedingBufferMillis()));
		CriticalChainService.Analysis refreshed = service.preview(fixture.project, List.of(fixture.resource));
		assertTrue(refreshed.projectBuffer().consumedMillis() > 0L, "The current schedule must be measured against the saved CCPM baseline");
	}

	@Test
	void clearUndoRedoRestoresScheduleAndCcpMStateTogether() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);

		CriticalChainService.Analysis applied = service.apply(fixture.project, List.of(fixture.resource));
		assertSame(applied, service.analysis(fixture.project));
		assertTrue(second.getLevelingDelay() > 0L);

		service.clear(fixture.project);
		assertNull(service.findSettings(fixture.project));
		assertNull(service.findBaseline(fixture.project));
		assertEquals(0L, second.getLevelingDelay());

		fixture.project.getUndoController().undo();
		assertTrue(service.findSettings(fixture.project).isEnabled());
		assertTrue(service.findBaseline(fixture.project) != null);
		assertSame(applied, service.findAnalysis(fixture.project));
		assertTrue(second.getLevelingDelay() > 0L);

		fixture.project.getUndoController().redo();
		assertNull(service.findSettings(fixture.project));
		assertNull(service.findBaseline(fixture.project));
		assertEquals(0L, second.getLevelingDelay());
	}

	@Test
	void settingsAreDocumentScopedAndNeverChangeLegacySerialization() throws Exception {
		Fixture first = fixture();
		Fixture second = fixture();
		byte[] before = serialize(first.project);
		CriticalChainService service = new CriticalChainService();
		service.settings(first.project).setEnabled(true);

		assertTrue(service.findSettings(second.project) == null);
		org.junit.jupiter.api.Assertions.assertArrayEquals(before, serialize(first.project));
		service.forget(first.project);
		assertTrue(service.findSettings(first.project) == null);
	}

	@Test
	void previewScalesToLargeSharedResourceWithoutQuadraticExpiryScan() {
		Fixture fixture = fixture();
		for (int index = 0; index < 1000; index++) {
			NormalTask task = task(fixture.project, "Task-" + index);
			AssignmentService.getInstance().newAssignment(task, fixture.resource, 1D, 0L, this);
		}
		long started = System.nanoTime();
		CriticalChainService.Analysis analysis = new CriticalChainService().preview(fixture.project, List.of(fixture.resource));
		long elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
		assertEquals(1000, analysis.criticalTaskIds().size());
		assertTrue(analysis.resourcePredecessors().values().stream().mapToInt(List::size).sum() <= 1000,
			"resource constraint edges must remain linear for fully overlapping assignments");
		assertTrue(elapsedMillis < 10_000L, "large shared-resource preview took " + elapsedMillis + " ms");
	}

	private Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Engineer");
		return new Fixture(project, resource);
	}

	private NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	private static byte[] serialize(Project project) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
			out.writeObject(project);
		}
		return bytes.toByteArray();
	}

	private record Fixture(Project project, ResourceImpl resource) { }
}
