/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;
import com.microproject.undo.EditSupport;

import com.microproject.datatype.Duration;
import com.microproject.options.CalendarOption;
import com.microproject.pm.availability.Availability;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.TaskSplitInterval;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.ProjectTaskSnapshot;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.strings.Messages;
import com.microproject.transaction.ModelTransaction;
import com.microproject.transaction.ProjectMutationState;

/**
 * Deterministic resource-leveling engine. It delays lower-ranked, unstarted
 * tasks until concurrent assignment units fit the resource's capacity. The
 * result is a previewable plan; no model state changes until {@link Plan#apply()}.
 */
public final class ResourceLevelingService {
	private static final int MAX_PREVIEW_PERIODS = 100_000;
	public enum Order {
		ID_ONLY,
		STANDARD,
		PRIORITY_STANDARD
	}

	/**
	 * Period used to compare task assignment work with calendar-backed resource
	 * availability. Microsoft lists all five bases; its support pages conflict on
	 * the default, so the product currently defaults to Day.
	 */
	public enum Sensitivity {
		MINUTE,
		HOUR,
		DAY,
		WEEK,
		MONTH
	}

	public record Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
		long rangeStart, long rangeEnd, boolean includeProposedResources, Sensitivity sensitivity) {
		public Options(Order order, boolean onlyWithinAvailableSlack, long rangeStart, long rangeEnd) {
			this(order, onlyWithinAvailableSlack, true, rangeStart, rangeEnd, false, Sensitivity.DAY);
		}
		public Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
			long rangeStart, long rangeEnd) {
			this(order, onlyWithinAvailableSlack, allowTaskSplits, rangeStart, rangeEnd, false, Sensitivity.DAY);
		}
		public Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
			long rangeStart, long rangeEnd, boolean includeProposedResources) {
			this(order, onlyWithinAvailableSlack, allowTaskSplits, rangeStart, rangeEnd,
				includeProposedResources, Sensitivity.DAY);
		}
		public Options {
			Objects.requireNonNull(order, "order");
			Objects.requireNonNull(sensitivity, "sensitivity");
			if (rangeStart > rangeEnd) {
				throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
			}
		}

		public static Options defaults() {
			return new Options(Order.STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE, false);
		}
	}

	public record Change(Task task, long oldDelay, long newDelay, long oldStart, long projectedStart,
		String limitingResource) {
		public long addedDelayMillis() {
			return Duration.millis(newDelay) - Duration.millis(oldDelay);
		}
	}

	public record Conflict(Resource resource, Task task, String reason) {
	}
	public record Split(Task task, long from, long to, String limitingResource) { }

	public static final class Plan {
		private final List<Change> changes;
		private final List<Conflict> unresolved;
		private final List<Split> splits;
		private final List<NormalTask> clearGeneratedSplits;
		private final Map<Task, Object> detailBackups;

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits) {
			this(changes, unresolved, splits, List.of());
		}

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits,
			List<NormalTask> clearGeneratedSplits) {
			this.changes = List.copyOf(changes);
			this.unresolved = List.copyOf(unresolved);
			this.splits = List.copyOf(splits);
			this.clearGeneratedSplits = List.copyOf(clearGeneratedSplits);
			this.detailBackups = new IdentityHashMap<>(splits.size() + clearGeneratedSplits.size());
		}

		public List<Change> changes() {
			return changes;
		}

		public List<Conflict> unresolved() {
			return unresolved;
		}
		public List<Split> splits() { return splits; }

		public boolean isComplete() {
			return unresolved.isEmpty();
		}

		public void apply() {
			applyChanges(true);
		}

		public void apply(EditSupport editSupport) {
			applyChanges(true);
			postUndoEdit(editSupport);
		}

		private void postUndoEdit(EditSupport editSupport) {
			if (editSupport != null && hasMutations()) editSupport.postEdit(new AbstractUndoableEdit() {
				private static final long serialVersionUID = 1L;
				@Override public String getPresentationName() { return Messages.getString("ResourceLevelingService.PresentationName"); }
				@Override public void undo() { super.undo(); applyChanges(false); }
				@Override public void redo() { super.redo(); applyChanges(true); }
			});
		}

		private void applyChanges(boolean forward) {
			Set<Project> projects = new LinkedHashSet<>(Math.max(4, splits.size()));
			if (forward) {
				for (Split split : splits) {
					detailBackups.computeIfAbsent(split.task(), Task::backupDetail);
					ScheduleService.getInstance().splitForLeveling(this, split.task(), split.from(), split.to());
					projects.add(split.task().getOwningProject());
				}
				for (NormalTask task : clearGeneratedSplits) {
					detailBackups.computeIfAbsent(task, Task::backupDetail);
					task.clearLevelingGeneratedSplits(this);
					projects.add(task.getOwningProject());
				}
			} else {
				for (Map.Entry<Task, Object> backup : detailBackups.entrySet()) {
					backup.getKey().restoreDetail(this, backup.getValue(), false);
					projects.add(backup.getKey().getOwningProject());
				}
			}
			for (Change change : changes) {
				change.task().setLevelingDelay(forward ? change.newDelay() : change.oldDelay());
				change.task().setDirty(true);
				projects.add(change.task().getOwningProject());
			}
			for (Project project : projects) {
				project.recalculate();
				project.setGroupDirty(true);
			}
		}

		private boolean hasMutations() {
			return !changes.isEmpty() || !splits.isEmpty() || !clearGeneratedSplits.isEmpty();
		}

		public void revert() {
			applyChanges(false);
		}
	}

	private record Scheduled(Assignment assignment, Task task, long start, long end, double units) {
	}

	private static final class LevelingScratch {
		private final ArrayList<Assignment> assignments = new ArrayList<>();
		private final ArrayList<Scheduled> accepted = new ArrayList<>();
		private final Map<Long, Double> bucketLoads = new HashMap<>();
		private final Map<Long, Double> bucketCapacities = new HashMap<>();
		private final Map<Long, Scheduled> firstBucketAssignments = new HashMap<>();
		private final Map<Assignment, List<WorkSegment>> workProfiles = new IdentityHashMap<>();

		private void reset(int assignmentCount) {
			assignments.ensureCapacity(assignmentCount);
			accepted.ensureCapacity(assignmentCount * 2);
			assignments.clear();
			accepted.clear();
			bucketLoads.clear();
			bucketCapacities.clear();
			firstBucketAssignments.clear();
			workProfiles.clear();
		}
	}

	private record WorkSegment(long start, long end, double units) { }

	public Plan preview(Project project, Collection<? extends Resource> selectedResources, Options options) {
		Objects.requireNonNull(project, "project");
		Collection<? extends Resource> resources = selectedResources == null
			? project.getResourcePool().getResourceList() : selectedResources;
		return preview(resources, options);
	}

	/**
	 * Previews MSP-style Level Selection. Selected tasks are the only tasks that
	 * receive leveling delays or splits; assignments on other tasks are fixed
	 * capacity constraints while the leveling plan is calculated. Recalculation
	 * may still move dependent tasks through ordinary schedule logic.
	 */
	public Plan previewSelectedTasks(Project project, Collection<? extends Resource> selectedResources,
		Options options, Collection<? extends Task> selectedTasks) {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(options, "options");
		Objects.requireNonNull(selectedTasks, "selectedTasks");
		if (selectedTasks.isEmpty()) {
			return new Plan(List.of(), List.of(), List.of());
		}
		Set<Task> selected = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Task task : selectedTasks) {
			if (task == null || task.getOwningProject() != project) {
				throw new IllegalArgumentException("selectedTasks must belong to the supplied project");
			}
			selected.add(task);
		}
		Collection<? extends Resource> resources = selectedResources == null
			? project.getResourcePool().getResourceList() : selectedResources;
		return preview(resources, options, selected);
	}

	public Plan preview(Collection<? extends Resource> resources, Options options) {
		return preview(resources, options, null);
	}

	private Plan preview(Collection<? extends Resource> resources, Options options, Set<Task> selectedTasks) {
		Objects.requireNonNull(resources, "resources");
		Objects.requireNonNull(options, "options");
		Map<Task, Long> addedDelay = new IdentityHashMap<>();
		Map<Task, String> limitingResource = new IdentityHashMap<>();
		Map<Task, Split> splits = new IdentityHashMap<>();
		List<Conflict> unresolved = new ArrayList<>(Math.max(4, resources.size()));
		LevelingScratch scratch = new LevelingScratch();

		boolean changed;
		int pass = 0;
		do {
			changed = false;
			for (Resource resource : resources) {
				changed |= levelResource(resource, options, selectedTasks, addedDelay, limitingResource, splits, unresolved, scratch);
			}
		} while (changed && ++pass < Math.max(8, resources.size() * 4));

		Map<Task, Change> result = new LinkedHashMap<>(Math.max(4, resources.size() * 2));
		for (Map.Entry<Task, Long> entry : addedDelay.entrySet()) {
			Task task = entry.getKey();
			long extra = entry.getValue();
			if (extra <= 0L) {
				continue;
			}
			long oldDelay = task.getLevelingDelay();
			long newDelay = Duration.millis(oldDelay) + extra;
			long projectedStart = task.getEffectiveWorkCalendar().add(task.getStart(), extra, false);
			result.put(task, new Change(task, oldDelay, newDelay, task.getStart(), projectedStart,
				limitingResource.get(task)));
		}
		for (Split split : splits.values()) result.putIfAbsent(split.task(), new Change(split.task(), split.task().getLevelingDelay(), split.task().getLevelingDelay(), split.task().getStart(), split.task().getStart(), split.limitingResource()));
		return new Plan(new ArrayList<>(result.values()), deduplicate(unresolved), new ArrayList<>(splits.values()));
	}

	public void clear(Project project) {
		clear(project, null);
	}

	public void clear(Project project, EditSupport editSupport) {
		clear(project, null, editSupport);
	}

	/** Returns the schedulable tasks that own leveling delays or generated gaps in the requested scope. */
	public List<Task> tasksWithLevelingState(Project project, Collection<? extends Task> selectedTasks) {
		Objects.requireNonNull(project, "project");
		Set<Task> selected = null;
		if (selectedTasks != null) {
			selected = Collections.newSetFromMap(new IdentityHashMap<>());
			for (Task task : selectedTasks) {
				if (task == null || task.getOwningProject() != project)
					throw new IllegalArgumentException("selectedTasks must belong to the supplied project");
				selected.add(task);
			}
		}
		List<Task> affected = new ArrayList<>();
		for (Object value : project.getTaskList()) {
			Task task = (Task) value;
			if (selected != null && !selected.contains(task)) continue;
			if (hasLevelingState(task)) affected.add(task);
		}
		return List.copyOf(affected);
	}

	private static boolean hasLevelingState(Task task) {
		return Duration.millis(task.getLevelingDelay()) != 0L
			|| task instanceof NormalTask normalTask && !normalTask.getLevelingTaskSplitIntervals().isEmpty();
	}

	/** Clears leveling state for all tasks, or only the supplied project tasks. */
	public List<Task> clear(Project project, Collection<? extends Task> selectedTasks, EditSupport editSupport) {
		List<Task> affected = tasksWithLevelingState(project, selectedTasks);
		if (project.isReadOnly()) throw new IllegalStateException("Cannot clear leveling in a read-only project");
		for (Task task : affected) {
			if (task.isReadOnly()) throw new IllegalStateException("Cannot clear leveling on a read-only task: " + task.getName());
		}
		if (affected.isEmpty()) return List.of();
		List<Change> changes = new ArrayList<>(affected.size());
		List<NormalTask> generatedSplits = new ArrayList<>();
		for (Task task : affected) {
			if (Duration.millis(task.getLevelingDelay()) != 0L) {
				changes.add(new Change(task, task.getLevelingDelay(), 0L, task.getStart(), task.getStart(), ""));
			}
			if (task instanceof NormalTask normalTask && !normalTask.getLevelingTaskSplitIntervals().isEmpty()) {
				generatedSplits.add(normalTask);
			}
		}
		clearAtomically(project, affected, changes, generatedSplits, editSupport);
		return List.copyOf(affected);
	}

	private void clearAtomically(Project project, List<Task> affected, List<Change> changes,
		List<NormalTask> generatedSplits, EditSupport editSupport) {
		ProjectTaskSnapshot[] before = new ProjectTaskSnapshot[1];
		Plan clearPlan = new Plan(changes, List.of(), List.of(), generatedSplits);
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			try {
				ModelTransaction.execute(new ModelTransaction.Steps<ProjectTaskSnapshot, Boolean>() {
					@Override public void validate() {
						for (Task task : affected) {
							if (task.getOwningProject() != project || task.isReadOnly())
								throw new IllegalStateException("A leveling task is no longer editable in this project");
						}
					}
					@Override public void authorize() {
						if (project.isReadOnly()) throw new IllegalStateException("Cannot clear leveling in a read-only project");
					}
					@Override public ProjectTaskSnapshot capture() {
						before[0] = ProjectTaskSnapshot.capture(project);
						return before[0];
					}
					@Override public ModelTransaction.Mutation<Boolean> apply() {
						clearPlan.applyChanges(true);
						return ModelTransaction.Mutation.changed(Boolean.TRUE);
					}
					@Override public void schedule() { }
					@Override public void checkInvariants() {
						for (Task task : affected)
							if (hasLevelingState(task)) throw new IllegalStateException("Leveling state remains after clear");
					}
					@Override public void restore(ProjectTaskSnapshot snapshot) { snapshot.restore(project, ResourceLevelingService.this); }
					@Override public void commit(Boolean changed) { }
				});
				Project.PendingEventBatch pending = project.commitEventBatch(mutation, true);
				project.publishEventBatch(pending);
				clearPlan.postUndoEdit(editSupport);
			} catch (ModelTransaction.ModelTransactionException failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw new IllegalStateException("Clear leveling failed during " + failure.failedStage(), failure);
			} catch (RuntimeException | Error failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw failure;
			}
		}
	}


	private boolean levelResource(Resource resource, Options options, Set<Task> selectedTasks, Map<Task, Long> addedDelay,
		Map<Task, String> limitingResource, Map<Task, Split> splits, List<Conflict> unresolved,
		LevelingScratch scratch) {
		if (resource == null || !resource.isLabor() || resource.getMaximumUnits() <= 0D
			|| (!options.includeProposedResources()
				&& resource.getBookingTypeKind() == BookingType.Kind.PROPOSED)) {
			return false;
		}
		scratch.reset(resource.getAssignments().size());
		List<Assignment> assignments = scratch.assignments;
		for (Object value : resource.getAssignments()) {
			Assignment assignment = (Assignment) value;
			Task task = assignment.getTask();
			if (eligible(task, assignment, options)) {
				assignments.add(assignment);
			}
		}
		for (Assignment assignment : assignments) {
			if (exceedsBucketLimit(assignment.getStart(), assignment.getEnd(), options.sensitivity())) {
				unresolved.add(new Conflict(resource, assignment.getTask(),
					"Selected sensitivity exceeds the supported " + MAX_PREVIEW_PERIODS + "-period preview bound"));
				return false;
			}
		}
		Comparator<Assignment> assignmentOrder = assignmentComparator(options.order());
		if (selectedTasks != null) {
			assignments.sort(Comparator
				.comparing((Assignment assignment) -> selectedTasks.contains(assignment.getTask()))
				.thenComparing(assignmentOrder));
		} else {
			assignments.sort(assignmentOrder);
		}
		List<Scheduled> accepted = scratch.accepted;
		boolean changed = false;
		for (Assignment assignment : assignments) {
			Task task = assignment.getTask();
			if (selectedTasks != null && !selectedTasks.contains(task)) {
				Scheduled scheduled = new Scheduled(assignment, task, assignment.getStart(), assignment.getEnd(),
					Math.max(0D, assignment.getUnits()));
				accepted.add(scheduled);
				addBucketLoads(scheduled, options.sensitivity(), scratch);
				continue;
			}
			long existingAdded = addedDelay.getOrDefault(task, 0L);
			long duration = Math.max(0L, assignment.getEnd() - assignment.getStart());
			long start = shiftedStart(task, assignment.getStart(), existingAdded);
			long end = start + duration;
			Split proposedSplit = null;
			int attempts = 0;
			while (isOverallocated(start, end, assignment, resource, options.sensitivity(), scratch)
				&& attempts++ < assignments.size() + 2) {
				Scheduled blocker = firstBlockingInterval(start, end, assignment, resource, options.sensitivity(), scratch);
				if (options.allowTaskSplits() && blocker != null && blocker.start() > start && blocker.end() < end
					&& !task.isMilestone() && !task.inProgress()) {
					proposedSplit = new Split(task, blocker.start(), blocker.end(), resource.getName());
					Split previous = splits.put(task, proposedSplit);
					limitingResource.put(task, resource.getName());
					changed |= !proposedSplit.equals(previous);
					break;
				}
				if (!canMove(task, options, existingAdded, unresolved, resource)) {
					break;
				}
				long next = nextAvailableBoundary(start, end, assignment, resource, options.sensitivity(), scratch);
				if (next == Long.MAX_VALUE || next <= start) {
					unresolved.add(new Conflict(resource, task, "No later capacity boundary was found"));
					break;
				}
				long incremental = Math.max(0L,
					Duration.millis(task.getEffectiveWorkCalendar().compare(next, start, false)));
				if (incremental == 0L) {
					incremental = next - start;
				}
				existingAdded += incremental;
				if (options.onlyWithinAvailableSlack()
					&& existingAdded > Math.max(0L, Duration.millis(task.getTotalSlack()))) {
					unresolved.add(new Conflict(resource, task, "Available slack is insufficient"));
					existingAdded = addedDelay.getOrDefault(task, 0L);
					start = shiftedStart(task, assignment.getStart(), existingAdded);
					end = start + duration;
					break;
				}
				start = shiftedStart(task, assignment.getStart(), existingAdded);
				end = start + duration;
			}
			if (existingAdded > addedDelay.getOrDefault(task, 0L)) {
				addedDelay.put(task, existingAdded);
				limitingResource.put(task, resource.getName());
				changed = true;
			}
			if (proposedSplit == null && isOverallocated(start, end, assignment, resource, options.sensitivity(), scratch)) {
				unresolved.add(new Conflict(resource, task, "Resource remains overallocated"));
			}
			if (proposedSplit == null) {
				Scheduled scheduled = new Scheduled(assignment, task, start, end, Math.max(0D, assignment.getUnits()));
				accepted.add(scheduled);
				addBucketLoads(scheduled, options.sensitivity(), scratch);
			} else {
				Scheduled first = new Scheduled(assignment, task, start, proposedSplit.from(), Math.max(0D, assignment.getUnits()));
				Scheduled second = new Scheduled(assignment, task, proposedSplit.to(), proposedSplit.to() + (end - proposedSplit.from()), Math.max(0D, assignment.getUnits()));
				accepted.add(first);
				accepted.add(second);
				addBucketLoads(first, options.sensitivity(), scratch);
				addBucketLoads(second, options.sensitivity(), scratch);
			}
		}
		return changed;
	}

	private static boolean eligible(Task task, Assignment assignment, Options options) {
		return task != null && !task.isSummary() && !task.isComplete() && !task.isInactiveTask() && !task.isReadOnly()
			&& assignment.getEnd() > options.rangeStart() && assignment.getStart() < options.rangeEnd();
	}

	private static boolean canMove(Task task, Options options, long existingAdded,
		List<Conflict> unresolved, Resource resource) {
		if (task instanceof NormalTask normalTask && normalTask.getPriority() >= 1000) {
			unresolved.add(new Conflict(resource, task, "Task priority 1000 prevents leveling"));
			return false;
		}
		if (task.inProgress()) {
			unresolved.add(new Conflict(resource, task, "In-progress tasks are not delayed"));
			return false;
		}
		return !options.onlyWithinAvailableSlack() || existingAdded <= Math.max(0L, Duration.millis(task.getTotalSlack()));
	}

	private static Comparator<Assignment> assignmentComparator(Order order) {
		Comparator<Assignment> byId = Comparator.comparingLong(value -> value.getTask().getId());
		Comparator<Assignment> byStart = Comparator.comparingLong(Assignment::getStart).thenComparing(byId);
		if (order == Order.ID_ONLY) {
			return byId;
		}
		if (order == Order.PRIORITY_STANDARD) {
			return Comparator.<Assignment>comparingInt(value -> priority(value.getTask())).reversed()
				.thenComparing(byStart);
		}
		return Comparator.<Assignment>comparingInt(value -> priority(value.getTask())).reversed()
			.thenComparingLong(value -> Math.max(0L, Duration.millis(value.getTask().getTotalSlack())))
			.thenComparing(byStart);
	}

	private static int priority(Task task) {
		return task instanceof NormalTask normalTask ? normalTask.getPriority() : 500;
	}

	private static long shiftedStart(Task task, long originalStart, long addedDelay) {
		return addedDelay == 0L ? originalStart : task.getEffectiveWorkCalendar().add(originalStart, addedDelay, false);
	}

	private static boolean isOverallocated(long start, long end, Assignment candidate,
		Resource resource, Sensitivity sensitivity, LevelingScratch scratch) {
		for (long bucketStart = periodStart(start, sensitivity); bucketStart < end;
			bucketStart = nextPeriodStart(bucketStart, sensitivity)) {
			long bucketEnd = nextPeriodStart(bucketStart, sensitivity);
			double load = assignmentLoad(candidate, start, end, bucketStart, bucketEnd, scratch)
				+ scratch.bucketLoads.getOrDefault(bucketStart, 0D);
			double available = bucketCapacity(resource, bucketStart, bucketEnd, scratch.bucketCapacities);
			if (load > available + 1D) return true;
		}
		return false;
	}

	static boolean exceedsBucketLimit(long start, long end, Sensitivity sensitivity) {
		long bucket = periodStart(start, sensitivity);
		for (int count = 0; bucket < end; count++) {
			if (count >= MAX_PREVIEW_PERIODS) return true;
			long next = nextPeriodStart(bucket, sensitivity);
			if (next <= bucket) return true;
			bucket = next;
		}
		return false;
	}

	private static double assignmentLoad(Assignment assignment, long start, long end, long bucketStart, long bucketEnd,
		LevelingScratch scratch) {
		long overlapStart = Math.max(start, bucketStart);
		long overlapEnd = Math.min(end, bucketEnd);
		if (overlapStart >= overlapEnd) return 0D;
		List<WorkSegment> profile = scratch.workProfiles.computeIfAbsent(assignment, ResourceLevelingService::workProfile);
		long shift = start - assignment.getStart();
		long taskShift = start - assignment.getStart();
		double work = 0D;
		for (WorkSegment segment : profile) {
			long segmentStart = segment.start() + shift;
			long segmentEnd = segment.end() + shift;
			long from = Math.max(overlapStart, segmentStart);
			long to = Math.min(overlapEnd, segmentEnd);
			if (from >= to) continue;
			for (TaskSplitInterval split : assignment.getTask() instanceof NormalTask task
				? task.getTaskSplitIntervals() : List.<TaskSplitInterval>of()) {
				long gapStart = assignment.getTask().getStart() + split.startOffset() + taskShift;
				long gapEnd = assignment.getTask().getStart() + split.endOffset() + taskShift;
				if (gapEnd <= from || gapStart >= to) continue;
				if (gapStart > from) work += segment.units()
					* Math.max(0L, assignment.getEffectiveWorkCalendar().compare(gapStart, from, false));
				from = Math.max(from, gapEnd);
				if (from >= to) break;
			}
			if (from < to) work += segment.units()
				* Math.max(0L, assignment.getEffectiveWorkCalendar().compare(to, from, false));
		}
		return work;
	}

	private static List<WorkSegment> workProfile(Assignment assignment) {
		ArrayList<WorkSegment> profile = new ArrayList<>();
		ContourBucketIntervalGenerator generator = assignment.contourGeneratorInstance(HasTimeDistributedData.WORK);
		while (generator.hasNext() && generator.test(null)) {
			AbstractContourBucket bucket = (AbstractContourBucket) generator.current();
			if (bucket == null) continue;
			double units = Math.max(0D, bucket.getEffectiveUnits(assignment.getUnits()));
			if (units > 0D && generator.currentStart() < generator.currentEnd()) {
				profile.add(new WorkSegment(generator.currentStart(), generator.currentEnd(), units));
			}
		}
		return profile;
	}

	private static long nextAvailableBoundary(long start, long end, Assignment candidate,
		Resource resource, Sensitivity sensitivity, LevelingScratch scratch) {
		for (long bucketStart = periodStart(start, sensitivity); bucketStart < end;
			bucketStart = nextPeriodStart(bucketStart, sensitivity)) {
			long bucketEnd = nextPeriodStart(bucketStart, sensitivity);
			double candidateLoad = assignmentLoad(candidate, start, end, bucketStart, bucketEnd, scratch);
			double load = candidateLoad + scratch.bucketLoads.getOrDefault(bucketStart, 0D);
			double available = bucketCapacity(resource, bucketStart, bucketEnd, scratch.bucketCapacities);
			if (load > available + 1D) return candidateLoad > available + 1D ? Long.MAX_VALUE : bucketEnd;
		}
		return Long.MAX_VALUE;
	}

	private static Scheduled firstBlockingInterval(long start, long end, Assignment candidate,
		Resource resource, Sensitivity sensitivity, LevelingScratch scratch) {
		for (long bucketStart = periodStart(start, sensitivity); bucketStart < end;
			bucketStart = nextPeriodStart(bucketStart, sensitivity)) {
			long bucketEnd = nextPeriodStart(bucketStart, sensitivity);
			double load = assignmentLoad(candidate, start, end, bucketStart, bucketEnd, scratch)
				+ scratch.bucketLoads.getOrDefault(bucketStart, 0D);
			double available = bucketCapacity(resource, bucketStart, bucketEnd, scratch.bucketCapacities);
			if (load <= available + 1D) continue;
			return scratch.firstBucketAssignments.get(bucketStart);
		}
		return null;
	}

	private static double bucketCapacity(Resource resource, long bucketStart, long bucketEnd, Map<Long, Double> cache) {
		return cache.computeIfAbsent(bucketStart, ignored -> {
			double capacity = 0D;
			for (ValueObjectForInterval interval : resource.getAvailabilityTable().getList()) {
				long start = Math.max(bucketStart, interval.getStart());
				long end = Math.min(bucketEnd, interval.getEnd());
				if (start < end) {
					double units = ((Availability) interval).getMaximumUnits();
					capacity += Math.max(0L, resource.getEffectiveWorkCalendar().compare(end, start, false)) * Math.max(0D, units);
				}
			}
			return capacity;
		});
	}

	private static void addBucketLoads(Scheduled scheduled, Sensitivity sensitivity, LevelingScratch scratch) {
		for (long bucketStart = periodStart(scheduled.start(), sensitivity); bucketStart < scheduled.end();
			bucketStart = nextPeriodStart(bucketStart, sensitivity)) {
			long bucketEnd = nextPeriodStart(bucketStart, sensitivity);
			double load = assignmentLoad(scheduled.assignment(), scheduled.start(), scheduled.end(), bucketStart, bucketEnd, scratch);
			if (load > 0D) {
				scratch.bucketLoads.merge(bucketStart, load, Double::sum);
				scratch.firstBucketAssignments.merge(bucketStart, scheduled,
					(first, next) -> first.start() <= next.start() ? first : next);
			}
		}
	}

	static long periodStart(long time, Sensitivity sensitivity) {
		// Project bucket boundaries use the shared UTC calendar basis; week starts
		// additionally honor CalendarOption's configured first day of week.
		GregorianCalendar calendar = com.microproject.util.DateTime.calendarInstance();
		calendar.setTimeInMillis(time);
		if (sensitivity == Sensitivity.MINUTE) {
			calendar.set(GregorianCalendar.SECOND, 0);
			calendar.set(GregorianCalendar.MILLISECOND, 0);
			return calendar.getTimeInMillis();
		}
		if (sensitivity == Sensitivity.HOUR) {
			calendar.set(GregorianCalendar.MINUTE, 0);
			calendar.set(GregorianCalendar.SECOND, 0);
			calendar.set(GregorianCalendar.MILLISECOND, 0);
			return calendar.getTimeInMillis();
		}
		calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
		calendar.set(GregorianCalendar.MINUTE, 0);
		calendar.set(GregorianCalendar.SECOND, 0);
		calendar.set(GregorianCalendar.MILLISECOND, 0);
		if (sensitivity == Sensitivity.WEEK) {
			int firstDay = CalendarOption.getInstance().getWeekStartsOn();
			int distance = Math.floorMod(calendar.get(GregorianCalendar.DAY_OF_WEEK) - firstDay, 7);
			calendar.add(GregorianCalendar.DAY_OF_MONTH, -distance);
		} else if (sensitivity == Sensitivity.MONTH) {
			calendar.set(GregorianCalendar.DAY_OF_MONTH, 1);
		}
		return calendar.getTimeInMillis();
	}

	static long nextPeriodStart(long time, Sensitivity sensitivity) {
		GregorianCalendar calendar = com.microproject.util.DateTime.calendarInstance();
		calendar.setTimeInMillis(time);
		if (sensitivity == Sensitivity.MINUTE) {
			calendar.add(GregorianCalendar.MINUTE, 1);
			return calendar.getTimeInMillis();
		}
		if (sensitivity == Sensitivity.HOUR) {
			calendar.add(GregorianCalendar.HOUR_OF_DAY, 1);
			return calendar.getTimeInMillis();
		}
		calendar.add(GregorianCalendar.DAY_OF_MONTH, sensitivity == Sensitivity.DAY ? 1
			: sensitivity == Sensitivity.WEEK ? 7 : 0);
		if (sensitivity == Sensitivity.MONTH) calendar.add(GregorianCalendar.MONTH, 1);
		return calendar.getTimeInMillis();
	}

	private static boolean overlaps(long firstStart, long firstEnd, long secondStart, long secondEnd) {
		return firstStart < secondEnd && secondStart < firstEnd;
	}

	private static List<Conflict> deduplicate(List<Conflict> values) {
		Map<String, Conflict> unique = LinkedHashMap.newLinkedHashMap(values.size());
		for (Conflict value : values) {
			String key = value.resource().getUniqueId() + ":" + value.task().getUniqueId() + ":" + value.reason();
			unique.put(key, value);
		}
		return new ArrayList<>(unique.values());
	}
}
