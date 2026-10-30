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

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.ResourceLevelingService;
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
	void resourceConstraintSweepUsesDetachedValuesAndIdentityTokens() {
		var first = new CriticalChainService.ResourceInterval(0, 7L, 10L, 30L);
		var second = new CriticalChainService.ResourceInterval(1, 7L, 20L, 40L);
		var sameTask = new CriticalChainService.ResourceInterval(1, 7L, 21L, 25L);
		var milestone = new CriticalChainService.ResourceInterval(2, 9L, 22L, 22L);
		var input = new java.util.ArrayList<>(List.of(first, second, sameTask, milestone));
		var groups = new java.util.ArrayList<List<CriticalChainService.ResourceInterval>>();
		groups.add(input);
		var snapshot = new CriticalChainService.ResourceConstraints(groups);
		input.clear();
		groups.clear();

		assertEquals(List.of(new CriticalChainService.ResourceLink(0, 1, 7L, 7L)),
			CriticalChainService.resourceLinks(snapshot),
			"linked-project ID collisions must not erase distinct task identities; milestones consume no interval");
		assertThrows(UnsupportedOperationException.class, () -> snapshot.resources().get(0).clear());
	}

	@Test
	void duplicateTaskIdsFailBeforePreviewApplyOrRefreshCanPublishAmbiguousResults() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);
		service.apply(fixture.project, List.of(fixture.resource));
		var baseline = service.findBaseline(fixture.project);
		var snapshot = service.findAnalysis(fixture.project);
		long delay = second.getLevelingDelay();
		second.setUniqueId(first.getUniqueId());

		var error = assertThrows(IllegalStateException.class, () -> service.preview(fixture.project, List.of(fixture.resource)));
		assertTrue(error.getMessage().contains("duplicate unique ID"));
		assertThrows(IllegalStateException.class, () -> service.apply(fixture.project, List.of(fixture.resource)));
		assertThrows(IllegalStateException.class, () -> service.prepareAnalysis(fixture.project));
		assertThrows(IllegalStateException.class, () -> service.analysis(fixture.project));
		assertSame(baseline, service.findBaseline(fixture.project));
		assertSame(snapshot, service.findAnalysis(fixture.project));
		assertEquals(delay, second.getLevelingDelay());
	}

	@Test
	void sharedResourceRejectsCollidingTaskFromAnotherProject() {
		Fixture master = fixture();
		Fixture linked = fixture();
		NormalTask first = task(master.project, "Master");
		NormalTask second = task(linked.project, "Linked");
		second.setUniqueId(first.getUniqueId());
		AssignmentService.getInstance().newAssignment(first, master.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, master.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		service.settings(master.project).setEnabled(true);
		assertThrows(IllegalStateException.class, () -> service.preview(master.project, List.of(master.resource)));
		assertThrows(IllegalStateException.class, () -> service.apply(master.project, List.of(master.resource)));
		assertNull(service.findBaseline(master.project));
	}

	@Test
	void ordinaryLevelingPreviewDoesNotRequireCcpmTaskIds() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		second.setUniqueId(first.getUniqueId());
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);

		CriticalChainService.Analysis analysis = new CriticalChainService().preview(fixture.project,
			List.of(fixture.resource), new CriticalChainService.Settings());
		assertNotNull(analysis.levelingPlan(), "ordinary leveling still needs its scheduling preview");
		assertTrue(analysis.criticalTaskIds().isEmpty(), "CCPM-only analysis is omitted while CCPM is disabled");
		assertNull(analysis.projectBuffer(), "ordinary leveling must not publish an ambiguous CCPM buffer");
	}

	@Test
	void stagedStatusAnalysisMatchesSynchronousAnalysisAndRejectsChangedInputs() throws Exception {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);
		service.settings(fixture.project).setLevelAssignments(false);
		service.apply(fixture.project, List.of(fixture.resource));
		var expected = service.analysis(fixture.project);
		var pending = service.prepareAnalysis(fixture.project);
		var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
		try {
			var result = worker.submit(pending.work()::compute).get();
			var actual = service.finishAnalysis(pending, result);
			assertNotNull(actual);
			assertEquals(expected.criticalTaskIds(), actual.criticalTaskIds());
			assertEquals(expected.resourcePredecessors(), actual.resourcePredecessors());
			assertEquals(expected.projectBuffer(), actual.projectBuffer());
			assertEquals(expected.feedingBuffers(), actual.feedingBuffers());
			assertEquals(expected.resourceBuffers(), actual.resourceBuffers());
			assertEquals(java.util.Set.copyOf(expected.graphEdges()), java.util.Set.copyOf(actual.graphEdges()));

			pending = service.prepareAnalysis(fixture.project);
			result = worker.submit(pending.work()::compute).get();
			first.setPriority(first.getPriority() - 1);
			assertNull(service.finishAnalysis(pending, result), "changed model inputs must discard the result");
			assertSame(actual, service.findAnalysis(fixture.project), "stale work must not publish an analysis");
			pending = service.prepareAnalysis(fixture.project);
			result = worker.submit(pending.work()::compute).get();
			service.settings(fixture.project).setBufferFraction(0.25D);
			assertNull(service.finishAnalysis(pending, result), "changed options must discard the result");
			service.settings(fixture.project).setBufferFraction(0.5D);
			pending = service.prepareAnalysis(fixture.project);
			result = worker.submit(pending.work()::compute).get();
			first.setPercentComplete(0.5D);
			assertNull(service.finishAnalysis(pending, result), "changed progress must discard detached buffer results");
			var refreshed = service.prepareAnalysis(fixture.project);
			var refreshedResult = worker.submit(refreshed.work()::compute).get();
			assertEquals(service.analysis(fixture.project).resourceBuffers(),
				service.finishAnalysis(refreshed, refreshedResult).resourceBuffers());
		} finally { worker.shutdownNow(); }
	}

	@Test
	void levelingPreviewRejectsIdAndWeekStartChanges() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		var staleId = service.preview(fixture.project, List.of(fixture.resource)).levelingPlan();
		first.setId(first.getId() + 100L);
		assertThrows(IllegalStateException.class, staleId::assertCurrent);
		var staleWeek = service.preview(fixture.project, List.of(fixture.resource)).levelingPlan();
		CalendarOption calendarOptions = CalendarOption.getInstance();
		int oldWeekStart = calendarOptions.getWeekStartsOn();
		try {
			calendarOptions.setWeekStartsOn(oldWeekStart == java.util.Calendar.MONDAY ? java.util.Calendar.SUNDAY : java.util.Calendar.MONDAY);
			assertThrows(IllegalStateException.class, staleWeek::assertCurrent);
		} finally { calendarOptions.setWeekStartsOn(oldWeekStart); }
	}

	@Test
	void detachedResourceWorkHonorsInterruptAndRejectsAnotherRequestResult() throws Exception {
		Fixture fixture = fixture();
		NormalTask task = task(fixture.project, "Task");
		AssignmentService.getInstance().newAssignment(task, fixture.resource, 1D, 0L, this);
		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);
		service.apply(fixture.project, List.of(fixture.resource));
		var first = service.prepareAnalysis(fixture.project);
		var second = service.prepareAnalysis(fixture.project);
		assertThrows(IllegalArgumentException.class, () -> service.finishAnalysis(first, second.work().compute()));
		var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
		try {
			assertTrue(worker.submit(() -> {
				Thread.currentThread().interrupt();
				try { first.work().compute(); return false; }
				catch (java.util.concurrent.CancellationException expected) { return true; }
			}).get());
		} finally { worker.shutdownNow(); }
	}

	@Test
	void previewFindsResourceConstraintWithoutMutatingSchedule() {
		Fixture fixture = fixture();
		NormalTask first = task(fixture.project, "First");
		NormalTask second = task(fixture.project, "Second");
		AssignmentService.getInstance().newAssignment(first, fixture.resource, 1D, 0L, this);
		AssignmentService.getInstance().newAssignment(second, fixture.resource, 1D, 0L, this);

		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setLevelAssignments(false);
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
	void analysisMeasuresEditedTaskFinishBeforeProjectEnvelopeRecalculation() {
		Fixture fixture = fixture();
		NormalTask task = task(fixture.project, "scheduled finish");
		fixture.project.recalculate();

		CriticalChainService service = new CriticalChainService();
		service.settings(fixture.project).setEnabled(true);
		service.apply(fixture.project, List.of(fixture.resource));
		long baselineFinish = service.findBaseline(fixture.project).projectFinishMillis();
		long originalDuration = task.getDuration();
		task.setDuration(originalDuration + CalendarOption.getInstance().getMillisPerDay());

		assertTrue(task.getEnd() > baselineFinish, "the edited task schedule must move beyond the captured finish");
		assertEquals(baselineFinish, fixture.project.getEnd(), "the project envelope is still awaiting schedule recalculation");
		long delayedConsumption = service.analysis(fixture.project).projectBuffer().consumedMillis();
		assertEquals(task.getEnd() - baselineFinish, delayedConsumption,
			"CCPM status must measure the current task rollup, not the stale project envelope");
		task.setDuration(originalDuration);
		long shortenedConsumption = service.analysis(fixture.project).projectBuffer().consumedMillis();
		assertEquals(Math.max(0L, task.getEnd() - baselineFinish), shortenedConsumption,
			"CCPM must report the shortened leaf's actual end delta, even before the envelope recalculates");
		assertTrue(shortenedConsumption < delayedConsumption,
			"shortening the leaf schedule must reduce its consumed buffer even before envelope recalculation");
	}

	@Test
	void applyRequiresExplicitEnablementAndPersistsSettings() {
		Fixture fixture = fixture();
		CriticalChainService service = new CriticalChainService();
		assertThrows(IllegalStateException.class, () -> service.apply(fixture.project, List.of(fixture.resource)));

		CriticalChainService.Settings settings = service.settings(fixture.project);
		settings.setEnabled(true);
		settings.setBufferFraction(0.4D);
		settings.setSensitivity(ResourceLevelingService.Sensitivity.HOUR);
		assertEquals(settings, service.settings(fixture.project));
		assertEquals(ResourceLevelingService.Sensitivity.HOUR, settings.copy().getSensitivity());
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
		service.settings(fixture.project).setLevelAssignments(false);

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
		service.settings(fixture.project).setLevelAssignments(false);

		CriticalChainService.Analysis applied = service.apply(fixture.project, List.of(fixture.resource));
		assertTrue(service.analysis(fixture.project) != null,
			"status surfaces must rebuild analysis so assignment and dependency edits cannot leave a stale chain");
		assertTrue(second.getLevelingDelay() > 0L);
		CriticalChainBufferHistory history = fixture.project.getOrCreateTransientDocumentState(
			CriticalChainBufferHistory.class, CriticalChainBufferHistory::new);
		history.add(new CriticalChainBufferHistory.Point(Instant.parse("2026-10-10T09:00:00Z"),
			"planner", "Planner", 25D, 30D, "GREEN", "baseline-a"));
		history.recordRetraction(new CriticalChainBufferHistory.Retraction(UUID.randomUUID(),
			Instant.parse("2026-10-10T08:00:00Z"), "planner", "Planner", "duplicate checkpoint"));

		service.clear(fixture.project);
		assertNull(service.findSettings(fixture.project));
		assertNull(service.findBaseline(fixture.project));
		assertNull(fixture.project.findTransientDocumentState(CriticalChainBufferHistory.class),
			"clear must remove active and retracted buffer history");
		assertEquals(0L, second.getLevelingDelay());

		fixture.project.getUndoController().undo();
		assertTrue(service.findSettings(fixture.project).isEnabled());
		assertTrue(service.findBaseline(fixture.project) != null);
		assertTrue(service.findAnalysis(fixture.project) != null);
		assertTrue(second.getLevelingDelay() > 0L);
		CriticalChainBufferHistory restoredHistory = fixture.project.findTransientDocumentState(CriticalChainBufferHistory.class);
		assertNotNull(restoredHistory, "Undo must restore the buffer history container");
		assertEquals(history.points(), restoredHistory.points(), "Undo must restore active buffer observations unchanged");
		assertEquals(history.retractions(), restoredHistory.retractions(), "Undo must restore the retraction audit trail unchanged");

		fixture.project.getUndoController().redo();
		assertNull(service.findSettings(fixture.project));
		assertNull(service.findBaseline(fixture.project));
		assertNull(fixture.project.findTransientDocumentState(CriticalChainBufferHistory.class),
			"Redo must clear active and retracted buffer history again");
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
		long originalEnd = service.findBaseline(fixture.project).projectFinishMillis();
		task.setDuration(task.getDuration() + 86_400_000L);
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
