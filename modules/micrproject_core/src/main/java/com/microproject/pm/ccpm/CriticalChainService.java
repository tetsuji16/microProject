/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.ccpm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.datatype.Duration;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceLevelingService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/**
 * CCPM coordinator built on the normal scheduler and deterministic resource
 * leveling. It does not change a project until {@link #apply(Project, Collection)}
 * is explicitly called.
 */
public final class CriticalChainService {
	public enum BufferStatus { GREEN, AMBER, RED }
	public record ChainEdge(long predecessorTaskId, long successorTaskId, Kind kind) {
		public enum Kind { DEPENDENCY, RESOURCE_CONSTRAINT }
	}

	/** Immutable buffer measurement. Consumption is never allowed to exceed the planned amount. */
	public record Buffer(long plannedMillis, long consumedMillis, long remainingMillis, double consumptionRatio,
		BufferStatus status) {
		public Buffer {
			if (plannedMillis < 0L || consumedMillis < 0L || remainingMillis < 0L || !Double.isFinite(consumptionRatio)) {
				throw new IllegalArgumentException("Invalid CCPM buffer");
			}
		}
	}

	/** Immutable reference captured when a CCPM plan is applied, and persisted only in mpo. */
	public record Baseline(long projectFinishMillis, long projectBufferMillis, double bufferFraction,
		List<Long> criticalTaskIds, Map<Long, Long> feedingTaskStartMillis, Map<Long, Long> feedingBufferMillis) {
		public Baseline {
			if (projectFinishMillis < 0L || projectBufferMillis < 0L || Double.isNaN(bufferFraction)
				|| bufferFraction < 0D || bufferFraction > 1D) throw new IllegalArgumentException("Invalid CCPM baseline");
			criticalTaskIds = List.copyOf(criticalTaskIds);
			feedingTaskStartMillis = Map.copyOf(feedingTaskStartMillis);
			feedingBufferMillis = Map.copyOf(feedingBufferMillis);
		}
	}

	/** CCPM options. They are persisted by MPOF's optional settings.xml entry. */
	public static final class Settings implements Serializable {
		private static final long serialVersionUID = 1L;
		private boolean enabled;
		private double bufferFraction = 0.5D;
		private ResourceLevelingService.Order levelingOrder = ResourceLevelingService.Order.PRIORITY_STANDARD;
		private boolean onlyWithinAvailableSlack;
		private boolean allowTaskSplits = true;

		public boolean isEnabled() { return enabled; }
		public void setEnabled(boolean enabled) { this.enabled = enabled; }
		public double getBufferFraction() { return bufferFraction; }
		public void setBufferFraction(double bufferFraction) {
			if (Double.isNaN(bufferFraction) || bufferFraction < 0D || bufferFraction > 1D) {
				throw new IllegalArgumentException("bufferFraction must be between 0 and 1");
			}
			this.bufferFraction = bufferFraction;
		}
		public ResourceLevelingService.Order getLevelingOrder() { return levelingOrder; }
		public void setLevelingOrder(ResourceLevelingService.Order levelingOrder) {
			this.levelingOrder = Objects.requireNonNull(levelingOrder, "levelingOrder");
		}
		public boolean isOnlyWithinAvailableSlack() { return onlyWithinAvailableSlack; }
		public void setOnlyWithinAvailableSlack(boolean onlyWithinAvailableSlack) { this.onlyWithinAvailableSlack = onlyWithinAvailableSlack; }
		public boolean isAllowTaskSplits() { return allowTaskSplits; }
		public void setAllowTaskSplits(boolean allowTaskSplits) { this.allowTaskSplits = allowTaskSplits; }
		public Settings copy() {
			Settings copy = new Settings();
			copy.enabled = enabled;
			copy.bufferFraction = bufferFraction;
			copy.levelingOrder = levelingOrder;
			copy.onlyWithinAvailableSlack = onlyWithinAvailableSlack;
			copy.allowTaskSplits = allowTaskSplits;
			return copy;
		}
	}

	public record Analysis(ResourceLevelingService.Plan levelingPlan, List<Long> criticalTaskIds,
		long projectBufferMillis, Map<Long, Long> feedingBufferMillis, Buffer projectBuffer,
		Map<Long, Buffer> feedingBuffers, Map<Long, List<Long>> resourcePredecessors,
		List<ChainEdge> graphEdges) {
		public Analysis {
			criticalTaskIds = List.copyOf(criticalTaskIds);
			feedingBufferMillis = Map.copyOf(feedingBufferMillis);
			feedingBuffers = Map.copyOf(feedingBuffers);
			resourcePredecessors = Map.copyOf(resourcePredecessors);
			graphEdges = List.copyOf(graphEdges);
		}
	}

	/**
	 * The one analysis version displayed by Gantt, network and buffer surfaces.
	 * It is document-scoped but intentionally not serialized: a reopened project
	 * creates a fresh, internally consistent analysis from its persisted baseline.
	 */
	private static final class AnalysisSnapshot {
		private final Analysis analysis;
		private AnalysisSnapshot(Analysis analysis) { this.analysis = analysis; }
	}

	private record State(Settings settings, Baseline baseline, Analysis analysis) { }

	public Settings settings(Project project) {
		Objects.requireNonNull(project, "project");
		return project.getOrCreateTransientDocumentState(Settings.class, Settings::new);
	}

	/** Returns configured settings without creating CCPM state for an ordinary project. */
	public Settings findSettings(Project project) {
		return project == null ? null : project.findTransientDocumentState(Settings.class);
	}

	/** CCPM state needs mpo; legacy POD deliberately has no CCPM extension. */
	public boolean requiresMpo(Project project) {
		Settings settings = findSettings(project);
		return settings != null && settings.isEnabled();
	}

	public void forget(Project project) {
		if (project != null) {
			project.removeTransientDocumentState(Settings.class);
			project.removeTransientDocumentState(Baseline.class);
			project.removeTransientDocumentState(AnalysisSnapshot.class);
		}
	}

	/** Removes CCPM state while participating in a caller-owned undo transaction. */
	public void forget(Project project, UndoableEditSupport editSupport) {
		if (project == null) return;
		State before = captureState(project);
		forget(project);
		postStateEdit(project, editSupport, before, captureState(project));
	}

	public Baseline findBaseline(Project project) {
		return project == null ? null : project.findTransientDocumentState(Baseline.class);
	}

	/** Returns the display snapshot when a CCPM plan has already been analyzed. */
	public Analysis findAnalysis(Project project) {
		AnalysisSnapshot snapshot = project == null ? null : project.findTransientDocumentState(AnalysisSnapshot.class);
		return snapshot == null ? null : snapshot.analysis;
	}

	/**
	 * Gets the analysis used by every read-only CCPM surface.  The first caller
	 * after reload creates it; later callers reuse precisely the same snapshot.
	 */
	public Analysis analysis(Project project) {
		Analysis existing = findAnalysis(project);
		if (existing != null) return existing;
		Settings settings = findSettings(project);
		if (settings == null || !settings.isEnabled() || findBaseline(project) == null) return null;
		Analysis analysis = preview(project, null, settings);
		rememberAnalysis(project, analysis);
		return analysis;
	}

	/** Restores the mpo-only reference used to measure later schedule slippage. */
	public void restoreBaseline(Project project, Baseline baseline) {
		Objects.requireNonNull(project, "project");
		project.removeTransientDocumentState(Baseline.class);
		project.removeTransientDocumentState(AnalysisSnapshot.class);
		if (baseline != null) project.getOrCreateTransientDocumentState(Baseline.class, () -> baseline);
	}

	public Analysis preview(Project project, Collection<? extends Resource> resources) {
		return preview(project, resources, settingsOrDefault(project));
	}

	public Analysis preview(Project project, Collection<? extends Resource> resources, Settings settings) {
		Objects.requireNonNull(settings, "settings");
		ResourceLevelingService.Options options = new ResourceLevelingService.Options(settings.getLevelingOrder(),
			settings.isOnlyWithinAvailableSlack(), settings.isAllowTaskSplits(), Long.MIN_VALUE, Long.MAX_VALUE);
		ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(project, resources, options);
		return analyze(project, plan, settings, resources);
	}

	private Settings settingsOrDefault(Project project) {
		Objects.requireNonNull(project, "project");
		Settings settings = findSettings(project);
		return settings == null ? new Settings() : settings;
	}

	/** Applies the resource-constrained schedule and returns the resulting CCPM buffers. */
	public Analysis apply(Project project, Collection<? extends Resource> resources) {
		return apply(project, resources, settings(project));
	}

	public Analysis apply(Project project, Collection<? extends Resource> resources, Settings requestedSettings) {
		UndoableEditSupport editSupport = project.getUndoController().getEditSupport();
		State before = captureState(project);
		if (editSupport != null) editSupport.beginUpdate();
		try {
			Settings settings = settings(project);
			settings.setEnabled(requestedSettings.isEnabled());
			settings.setBufferFraction(requestedSettings.getBufferFraction());
			settings.setLevelingOrder(requestedSettings.getLevelingOrder());
			settings.setOnlyWithinAvailableSlack(requestedSettings.isOnlyWithinAvailableSlack());
			settings.setAllowTaskSplits(requestedSettings.isAllowTaskSplits());
			if (!settings.isEnabled()) {
				throw new IllegalStateException("Enable CCPM before applying a critical chain");
			}
			ResourceLevelingService.Options options = new ResourceLevelingService.Options(settings.getLevelingOrder(),
				settings.isOnlyWithinAvailableSlack(), settings.isAllowTaskSplits(), Long.MIN_VALUE, Long.MAX_VALUE);
			ResourceLevelingService.Plan plan = new ResourceLevelingService().preview(project, resources, options);
			plan.apply(editSupport);
			Analysis analysis = analyze(project, plan, settings, resources);
			Baseline baseline = new Baseline(project.getEnd(), analysis.projectBufferMillis(), settings.getBufferFraction(),
				analysis.criticalTaskIds(), taskStarts(project, analysis.feedingBufferMillis().keySet()), analysis.feedingBufferMillis());
			project.removeTransientDocumentState(Baseline.class);
			project.getOrCreateTransientDocumentState(Baseline.class, () -> baseline);
			Analysis result = analyze(project, plan, settings, resources);
			rememberAnalysis(project, result);
			postStateEdit(project, editSupport, before, captureState(project));
			return result;
		} finally {
			if (editSupport != null) editSupport.endUpdate();
		}
	}

	/** Clears leveling and CCPM state as one undoable operation. */
	public void clear(Project project) {
		if (project == null) return;
		UndoableEditSupport editSupport = project.getUndoController().getEditSupport();
		State before = captureState(project);
		if (editSupport != null) editSupport.beginUpdate();
		try {
			new ResourceLevelingService().clear(project, editSupport);
			forget(project);
			postStateEdit(project, editSupport, before, captureState(project));
		} finally {
			if (editSupport != null) editSupport.endUpdate();
		}
	}

	private static State captureState(Project project) {
		Settings settings = project.findTransientDocumentState(Settings.class);
		AnalysisSnapshot snapshot = project.findTransientDocumentState(AnalysisSnapshot.class);
		Analysis analysis = snapshot == null ? null : snapshot.analysis;
		return new State(settings == null ? null : settings.copy(),
			project.findTransientDocumentState(Baseline.class), analysis);
	}

	private static void restoreState(Project project, State state) {
		project.removeTransientDocumentState(Settings.class);
		project.removeTransientDocumentState(Baseline.class);
		project.removeTransientDocumentState(AnalysisSnapshot.class);
		if (state.settings != null) project.getOrCreateTransientDocumentState(Settings.class, state.settings::copy);
		if (state.baseline != null) project.getOrCreateTransientDocumentState(Baseline.class, () -> state.baseline);
		if (state.analysis != null) project.getOrCreateTransientDocumentState(AnalysisSnapshot.class,
			() -> new AnalysisSnapshot(state.analysis));
	}

	private static void rememberAnalysis(Project project, Analysis analysis) {
		project.removeTransientDocumentState(AnalysisSnapshot.class);
		project.getOrCreateTransientDocumentState(AnalysisSnapshot.class, () -> new AnalysisSnapshot(analysis));
	}

	private static void postStateEdit(Project project, UndoableEditSupport editSupport, State before, State after) {
		if (editSupport == null) return;
		editSupport.postEdit(new AbstractUndoableEdit() {
			private static final long serialVersionUID = 1L;
			@Override public String getPresentationName() { return "Critical Chain"; }
			@Override public void undo() { super.undo(); restoreState(project, before); }
			@Override public void redo() { super.redo(); restoreState(project, after); }
		});
	}

	private Analysis analyze(Project project, ResourceLevelingService.Plan plan, Settings settings, Collection<? extends Resource> selectedResources) {
		java.util.Set<Task> criticalSet = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Task, Boolean>());
		int taskCapacity = Math.max(4, project.getTaskList().size() * 4 / 3 + 1);
		Map<Long, Task> tasksById = new LinkedHashMap<>(taskCapacity);
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			tasksById.put(Long.valueOf(task.getUniqueId()), task);
			if (!task.isSummary() && task.isCritical()) {
				criticalSet.add(task);
			}
		}
		// The resource-leveling plan adds precedence constraints that the normal
		// critical-path flag cannot see until it is applied. Include its delayed
		// tasks in the preview chain without mutating the schedule.
		for (ResourceLevelingService.Change change : plan.changes()) {
			if (!change.task().isSummary()) criticalSet.add(change.task());
		}
		Map<Long, List<Long>> resourcePredecessors = resourcePredecessors(project, selectedResources, plan, criticalSet);
		List<ChainEdge> graphEdges = new ArrayList<>(Math.max(4, criticalSet.size() * 2));
		for (Task successor : criticalSet) {
			for (Object value : successor.getPredecessorList()) {
				Dependency dependency = (Dependency) value;
				Task predecessor = (Task) dependency.getPredecessor();
				if (criticalSet.contains(predecessor)) graphEdges.add(new ChainEdge(predecessor.getUniqueId(), successor.getUniqueId(), ChainEdge.Kind.DEPENDENCY));
			}
			for (Long predecessorId : resourcePredecessors.getOrDefault(Long.valueOf(successor.getUniqueId()), List.of()))
				graphEdges.add(new ChainEdge(predecessorId.longValue(), successor.getUniqueId(), ChainEdge.Kind.RESOURCE_CONSTRAINT));
		}
		List<Task> criticalTasks = new ArrayList<>(criticalSet);
		criticalTasks.sort(java.util.Comparator.comparingLong(Task::getStart).thenComparingLong(Task::getUniqueId));
		List<Long> ids = new ArrayList<>(criticalTasks.size());
		long chainDuration = 0L;
		for (Task task : criticalTasks) {
			ids.add(Long.valueOf(task.getUniqueId()));
			chainDuration += Math.max(0L, Duration.millis(task.getDuration()));
		}
		Map<Long, Long> feeding = new LinkedHashMap<>(Math.max(4, criticalTasks.size() * 4 / 3 + 1));
		for (Task critical : criticalTasks) {
			long feederDuration = 0L;
			for (Object value : critical.getPredecessorList()) {
				Dependency dependency = (Dependency) value;
				Task predecessor = (Task) dependency.getPredecessor();
				if (!criticalSet.contains(predecessor)) {
					feederDuration += Math.max(0L, Duration.millis(predecessor.getDuration()));
				}
			}
			if (feederDuration > 0L) {
				feeding.put(Long.valueOf(critical.getUniqueId()), scaled(feederDuration, settings.getBufferFraction()));
			}
		}
		long recommendedProjectBuffer = scaled(chainDuration, settings.getBufferFraction());
		Baseline baseline = findBaseline(project);
		// A newly applied leveling plan is empty on the next preview, so its
		// resource-constrained candidate set can legitimately differ from the
		// captured chain. The schedule baseline remains valid as long as the
		// buffer policy did not change.
		boolean baselineMatches = baseline != null && Double.compare(baseline.bufferFraction(), settings.getBufferFraction()) == 0;
		Buffer projectBuffer = buffer(baselineMatches ? baseline.projectBufferMillis() : recommendedProjectBuffer,
			baselineMatches ? Math.max(0L, project.getEnd() - baseline.projectFinishMillis()) : 0L);
		Map<Long, Buffer> feedingBuffers = new LinkedHashMap<>(Math.max(4, feeding.size() * 4 / 3 + 1));
		for (Map.Entry<Long, Long> entry : feeding.entrySet()) {
			long planned = baselineMatches ? baseline.feedingBufferMillis().getOrDefault(entry.getKey(), entry.getValue()) : entry.getValue();
			Task target = tasksById.get(entry.getKey());
			long referenceStart = baselineMatches ? baseline.feedingTaskStartMillis().getOrDefault(entry.getKey(), target == null ? 0L : target.getStart()) : (target == null ? 0L : target.getStart());
			feedingBuffers.put(entry.getKey(), buffer(planned, target == null ? 0L : Math.max(0L, target.getStart() - referenceStart)));
		}
		return new Analysis(plan, ids, recommendedProjectBuffer, feeding, projectBuffer, feedingBuffers, resourcePredecessors, graphEdges);
	}

	private static Map<Long, List<Long>> resourcePredecessors(Project project, Collection<? extends Resource> selectedResources,
		ResourceLevelingService.Plan plan, java.util.Set<Task> criticalSet) {
		Collection<? extends Resource> resources = selectedResources == null ? project.getResourcePool().getResourceList() : selectedResources;
		Map<Task, ResourceLevelingService.Change> changes = new java.util.IdentityHashMap<>();
		for (ResourceLevelingService.Change change : plan.changes()) changes.put(change.task(), change);
		Map<Long, List<Long>> result = new LinkedHashMap<>(Math.max(4, resources.size() * 2));
		for (Resource resource : resources) {
			List<Assignment> assignments = new ArrayList<>(resource.getAssignments().size());
			for (Object value : resource.getAssignments()) { Assignment assignment = (Assignment) value; if (assignment.getTask() != null && !assignment.getTask().isSummary() && !assignment.isDefault()) assignments.add(assignment); }
			assignments.sort(java.util.Comparator.<Assignment>comparingLong(value -> originalStart(value.getTask(), changes)).thenComparingLong(value -> value.getTask().getUniqueId()));
			// Sweep the intervals by start time.  The latest active assignment is
			// the direct resource predecessor for the current task; retaining every
			// overlapping interval would produce a quadratic edge set for a shared
			// resource whose assignments all overlap.
			java.util.Set<Assignment> active = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Assignment, Boolean>());
			PriorityQueue<Assignment> byEnd = new PriorityQueue<>((left, right) -> {
				long leftEnd = originalStart(left.getTask(), changes) + Math.max(0L, left.getEnd() - left.getStart());
				long rightEnd = originalStart(right.getTask(), changes) + Math.max(0L, right.getEnd() - right.getStart());
				int compared = Long.compare(leftEnd, rightEnd);
				return compared != 0 ? compared : Long.compare(left.getTask().getUniqueId(), right.getTask().getUniqueId());
			});
			PriorityQueue<Assignment> byStartDescending = new PriorityQueue<>((left, right) -> {
				int compared = Long.compare(originalStart(right.getTask(), changes), originalStart(left.getTask(), changes));
				return compared != 0 ? compared : Long.compare(right.getTask().getUniqueId(), left.getTask().getUniqueId());
			});
			for (Assignment current : assignments) {
				Task currentTask = current.getTask(); long currentStart = originalStart(currentTask, changes);
				long currentDuration = Math.max(0L, current.getEnd() - current.getStart());
				long currentEnd = currentStart + currentDuration;
				// Milestones do not consume a resource interval and therefore cannot
				// introduce a resource-constraint edge.
				if (currentEnd <= currentStart) continue;
				while (!byEnd.isEmpty()) {
					Assignment previous = byEnd.peek();
					Task previousTask = previous.getTask();
					long previousEnd = originalStart(previousTask, changes) + Math.max(0L, previous.getEnd() - previous.getStart());
					if (previousEnd > currentStart) break;
					byEnd.poll();
					active.remove(previous);
				}
				while (!byStartDescending.isEmpty() && !active.contains(byStartDescending.peek())) byStartDescending.poll();
				Assignment previous = byStartDescending.peek();
				if (previous != null) {
					Task previousTask = previous.getTask();
					long previousStart = originalStart(previousTask, changes);
					long previousEnd = previousStart + Math.max(0L, previous.getEnd() - previous.getStart());
					if (previousTask != currentTask && previousStart < currentEnd && currentStart < previousEnd) {
						result.computeIfAbsent(Long.valueOf(currentTask.getUniqueId()), ignored -> new ArrayList<>(1)).add(Long.valueOf(previousTask.getUniqueId()));
						criticalSet.add(currentTask); criticalSet.add(previousTask);
					}
				}
				active.add(current);
				byEnd.add(current);
				byStartDescending.add(current);
			}
		}
		for (Map.Entry<Long,List<Long>> entry : result.entrySet()) entry.setValue(List.copyOf(entry.getValue()));
		return result;
	}

	private static long originalStart(Task task, Map<Task, ResourceLevelingService.Change> changes) { ResourceLevelingService.Change change = changes.get(task); return change == null ? task.getStart() : change.oldStart(); }

	private static Buffer buffer(long planned, long consumed) {
		long safePlanned = Math.max(0L, planned);
		long safeConsumed = Math.max(0L, consumed);
		long remaining = Math.max(0L, safePlanned - safeConsumed);
		double ratio = safePlanned == 0L ? (safeConsumed == 0L ? 0D : 1D) : Math.min(1D, (double) safeConsumed / safePlanned);
		BufferStatus status = ratio < 1D / 3D ? BufferStatus.GREEN : ratio < 2D / 3D ? BufferStatus.AMBER : BufferStatus.RED;
		return new Buffer(safePlanned, safeConsumed, remaining, ratio, status);
	}

	private static Map<Long, Long> taskStarts(Project project, Collection<Long> ids) {
		Map<Long, Long> result = new LinkedHashMap<>(Math.max(4, ids.size() * 4 / 3 + 1));
		int taskCapacity = Math.max(4, project.getTaskList().size() * 4 / 3 + 1);
		Map<Long, Task> tasksById = new LinkedHashMap<>(taskCapacity);
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = (Task) iterator.next();
			tasksById.put(Long.valueOf(task.getUniqueId()), task);
		}
		for (Long id : ids) { Task task = tasksById.get(id); if (task != null) result.put(id, Long.valueOf(task.getStart())); }
		return result;
	}

	private static long scaled(long duration, double fraction) {
		return Math.round(duration * fraction);
	}
}
