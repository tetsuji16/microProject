/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CriticalChainServiceTest {
	@Test
	void analyzeApplyRefreshDiscardStaleGenerations() {
		CriticalChainService.Generation generations = new CriticalChainService.Generation();
		long analyze = generations.begin();
		long apply = generations.begin();
		java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
		assertFalse(generations.accept(analyze, calls::incrementAndGet));
		assertTrue(generations.accept(apply, calls::incrementAndGet));
		long refresh = generations.begin();
		assertFalse(generations.accept(apply, calls::incrementAndGet));
		assertTrue(generations.accept(refresh, calls::incrementAndGet));
		assertEquals(2, calls.get());
	}
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
	void readOnlyAnalysisRequiresAnAppliedBaselineAndNeverMutatesAnOrdinaryProject() {
		Fixture fixture = fixture();
		NormalTask task = task(fixture.project, "ordinary task");
		long start = task.getStart();
		CriticalChainService service = new CriticalChainService();

		assertNull(service.analysis(fixture.project), "a non-CCPM project must not expose a fabricated analysis");
		assertEquals(start, task.getStart(), "read-only analysis must not change the schedule");
		assertNull(service.findBaseline(fixture.project));
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
		assertTrue(!baseline.allResources() && baseline.resourceIds().equals(List.of(Long.valueOf(fixture.resource.getUniqueId()))),
			"an applied CCPM plan must retain its selected-resource scope for later status analysis");
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
		assertTrue(service.analysis(fixture.project) != null,
			"status surfaces must rebuild analysis so assignment and dependency edits cannot leave a stale chain");
		assertTrue(second.getLevelingDelay() > 0L);

		service.clear(fixture.project);
		assertNull(service.findSettings(fixture.project));
		assertNull(service.findBaseline(fixture.project));
		assertEquals(0L, second.getLevelingDelay());

		fixture.project.getUndoController().undo();
		assertTrue(service.findSettings(fixture.project).isEnabled());
		assertTrue(service.findBaseline(fixture.project) != null);
		assertTrue(service.findAnalysis(fixture.project) != null);
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

	@Test
	void emptyAndBoundaryTasksProduceTypedSafeBuffers() {
		Fixture fixture = fixture();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Analysis empty = service.preview(fixture.project, List.of(fixture.resource));
		assertTrue(empty.criticalTaskIds().isEmpty());
		assertTrue(empty.graphEdges().isEmpty());
		assertTrue(empty.projectBufferMillis() >= 0L);

		NormalTask milestone = task(fixture.project, "Zero milestone");
		milestone.setDuration(0L);
		milestone.setPercentComplete(1.0D);
		fixture.project.recalculate();
		CriticalChainService.Settings settings = service.settings(fixture.project);
		settings.setEnabled(true);
		CriticalChainService.Analysis boundary = service.apply(fixture.project, List.of(fixture.resource));
		assertTrue(milestone.isMilestone(), "zero duration must remain a milestone");
		assertEquals(1.0D, milestone.getPercentComplete(), 0.00001D);
		assertTrue(boundary.projectBuffer().remainingMillis() <= boundary.projectBuffer().plannedMillis());
		assertTrue(boundary.feedingBuffers().values().stream().allMatch(buffer ->
			buffer.remainingMillis() <= buffer.plannedMillis()));
		assertTrue(boundary.projectBuffer().status() instanceof CriticalChainService.BufferStatus);
		assertEquals(CriticalChainService.BufferKind.PROJECT, boundary.projectBuffer().kind());
		assertTrue(boundary.feedingBuffers().values().stream().allMatch(buffer ->
			buffer.kind() == CriticalChainService.BufferKind.FEEDING));
		assertThrows(UnsupportedOperationException.class,
			() -> boundary.criticalTaskIds().add(Long.valueOf(99L)),
			"critical task projection must be immutable");
	}

	@Test
	void typedBufferRejectsInvalidValuesAndPreservesEdgeKinds() {
		assertThrows(IllegalArgumentException.class,
			() -> new CriticalChainService.Buffer(-1L, 0L, 0L, 0D, CriticalChainService.BufferStatus.GREEN));
		assertThrows(IllegalArgumentException.class,
			() -> new CriticalChainService.Buffer(1L, 0L, 0L, Double.NaN, CriticalChainService.BufferStatus.GREEN));
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "Typed predecessor");
		NormalTask second = task(fixture.project, "Typed successor");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Analysis analysis = service.preview(fixture.project, List.of(fixture.resource));
		assertTrue(analysis.graphEdges().stream().anyMatch(edge ->
			edge.kind() == CriticalChainService.ChainEdge.Kind.RESOURCE_CONSTRAINT));
		assertTrue(analysis.resourceBuffers().values().stream().allMatch(buffer ->
			buffer.kind() == CriticalChainService.BufferKind.RESOURCE
				&& buffer.consumedMillis() <= buffer.plannedMillis()));
	}

	@Test
	void multipleFeedingBranchesRemainSeparateFromFixedDependencies() throws Exception {
		Fixture fixture = fixture();
		NormalTask feederOne = task(fixture.project, "Feeder one");
		NormalTask feederTwo = task(fixture.project, "Feeder two");
		NormalTask critical = task(fixture.project, "Critical");
		DependencyService.getInstance().newDependency(feederOne, critical, DependencyType.FS, 0L, this);
		DependencyService.getInstance().newDependency(feederTwo, critical, DependencyType.FS, 0L, this);
		AssignmentService.getInstance().newAssignment(feederOne, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(critical, fixture.resource, 1D, 0L, this);
		CriticalChainService.Analysis analysis = new CriticalChainService().preview(fixture.project, List.of(fixture.resource));
		assertNotNull(analysis.feedingBuffers(), "feeding projection must remain separate from constraints");
		assertNotNull(analysis.resourcePredecessors(), "resource constraints must remain separate from feeding buffers");
		assertTrue(java.util.Collections.disjoint(analysis.feedingBuffers().keySet(), analysis.resourceBuffers().keySet()),
			"feeding task keys and resource IDs must not be conflated");
		assertTrue(analysis.graphEdges().stream().allMatch(edge -> edge.kind() != null));
	}

	@Test
	void resourceBufferRemainingWorkUsesGreenAmberRedThresholds() {
		assertEquals(CriticalChainService.BufferStatus.GREEN,
			new CriticalChainService.Buffer(100L, 0L, 100L, 0D, CriticalChainService.BufferStatus.GREEN,
				CriticalChainService.BufferKind.RESOURCE).status());
		assertEquals(CriticalChainService.BufferStatus.AMBER,
			new CriticalChainService.Buffer(100L, 50L, 50L, 0.5D, CriticalChainService.BufferStatus.AMBER,
				CriticalChainService.BufferKind.RESOURCE).status());
		assertEquals(CriticalChainService.BufferStatus.RED,
			new CriticalChainService.Buffer(100L, 100L, 0L, 1D, CriticalChainService.BufferStatus.RED,
				CriticalChainService.BufferKind.RESOURCE).status());
	}

	@Test
	void supportedReportProjectsAllTypedBufferKinds() {
		CriticalChainService.Buffer project = new CriticalChainService.Buffer(100L, 0L, 100L, 0D,
			CriticalChainService.BufferStatus.GREEN, CriticalChainService.BufferKind.PROJECT);
		CriticalChainService.Buffer feeding = new CriticalChainService.Buffer(100L, 50L, 50L, .5D,
			CriticalChainService.BufferStatus.AMBER, CriticalChainService.BufferKind.FEEDING);
		CriticalChainService.Buffer resource = new CriticalChainService.Buffer(100L, 100L, 0L, 1D,
			CriticalChainService.BufferStatus.RED, CriticalChainService.BufferKind.RESOURCE);
		CriticalChainService.Analysis analysis = new CriticalChainService.Analysis(null, List.of(), 100L,
			java.util.Map.of(), project, java.util.Map.of(2L, feeding), java.util.Map.of(), List.of(), java.util.Map.of(7L, resource));
		String report = new CriticalChainReportService().toBufferCsv(analysis);
		assertTrue(report.contains("PROJECT,project") && report.contains("FEEDING,2") && report.contains("RESOURCE,7"));
	}

	@Test
	void clearReportsEditedBaselineAndUndoRestoresIt() {
		Fixture fixture = fixture();
		NormalTask task = task(fixture.project, "edited baseline");
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);
		service.apply(fixture.project, List.of(fixture.resource));
		long originalEnd = fixture.project.getEnd();
		task.setDuration(task.getDuration() + 86_400_000L);
		fixture.project.recalculate();
		CriticalChainService.ClearResult result = service.clearWithReport(fixture.project);
		assertTrue(result.cleared());
		assertTrue(result.baselineEdited());
		assertNotNull(result.discardedBaseline());
		assertNull(service.findBaseline(fixture.project));
		fixture.project.getUndoController().undo();
		assertNotNull(service.findBaseline(fixture.project));
		assertEquals(originalEnd, service.findBaseline(fixture.project).projectFinishMillis());
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
