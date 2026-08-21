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
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.datatype.Duration;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.strings.Messages;

/**
 * Deterministic resource-leveling engine. It delays lower-ranked, unstarted
 * tasks until concurrent assignment units fit the resource's capacity. The
 * result is a previewable plan; no model state changes until {@link Plan#apply()}.
 */
public final class ResourceLevelingService {
	public enum Order {
		ID_ONLY,
		STANDARD,
		PRIORITY_STANDARD
	}

	public record Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
		long rangeStart, long rangeEnd) {
		public Options(Order order, boolean onlyWithinAvailableSlack, long rangeStart, long rangeEnd) {
			this(order, onlyWithinAvailableSlack, true, rangeStart, rangeEnd);
		}
		public Options {
			Objects.requireNonNull(order, "order");
			if (rangeStart > rangeEnd) {
				throw new IllegalArgumentException("rangeStart must not be after rangeEnd");
			}
		}

		public static Options defaults() {
			return new Options(Order.STANDARD, false, true, Long.MIN_VALUE, Long.MAX_VALUE);
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
		private final Map<Task, Object> detailBackups = new IdentityHashMap<>();

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits) {
			this.changes = List.copyOf(changes);
			this.unresolved = List.copyOf(unresolved);
			this.splits = List.copyOf(splits);
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

		public void apply(UndoableEditSupport editSupport) {
			applyChanges(true);
			if (editSupport != null && !changes.isEmpty()) {
				editSupport.postEdit(new AbstractUndoableEdit() {
					private static final long serialVersionUID = 1L;
					@Override public String getPresentationName() { return Messages.getString("ResourceLevelingService.PresentationName"); }
					@Override public void undo() { super.undo(); applyChanges(false); }
					@Override public void redo() { super.redo(); applyChanges(true); }
				});
			}
		}

		private void applyChanges(boolean forward) {
			Set<Project> projects = new LinkedHashSet<>(Math.max(4, splits.size()));
			if (forward) {
				for (Split split : splits) {
					detailBackups.computeIfAbsent(split.task(), Task::backupDetail);
					ScheduleService.getInstance().split(this, split.task(), split.from(), split.to(), null);
					projects.add(split.task().getOwningProject());
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

		public void revert() {
			applyChanges(false);
		}
	}

	private record Scheduled(Assignment assignment, Task task, long start, long end, double units) {
	}

	private static final class LevelingScratch {
		private final ArrayList<Assignment> assignments = new ArrayList<>();
		private final ArrayList<Scheduled> accepted = new ArrayList<>();
		private final ArrayList<Long> boundaries = new ArrayList<>();

		private void reset(int assignmentCount) {
			assignments.ensureCapacity(assignmentCount);
			accepted.ensureCapacity(assignmentCount * 2);
			boundaries.ensureCapacity(assignmentCount + 1);
			assignments.clear();
			accepted.clear();
			boundaries.clear();
		}
	}

	public Plan preview(Project project, Collection<? extends Resource> selectedResources, Options options) {
		Objects.requireNonNull(project, "project");
		Collection<? extends Resource> resources = selectedResources == null
			? project.getResourcePool().getResourceList() : selectedResources;
		return preview(resources, options);
	}

	public Plan preview(Collection<? extends Resource> resources, Options options) {
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
				changed |= levelResource(resource, options, addedDelay, limitingResource, splits, unresolved, scratch);
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

	public void clear(Project project, UndoableEditSupport editSupport) {
		List<Change> changes = new ArrayList<>();
		// The outline iterator omits detached/unparented tasks while a project is
		// being edited.  Leveling applies to every schedulable task, so clear must
		// use the task list as well or leave invisible leveling delays behind.
		for (Object value : project.getTaskList()) {
			Task task = (Task) value;
			if (Duration.millis(task.getLevelingDelay()) != 0L) {
				changes.add(new Change(task, task.getLevelingDelay(), 0L, task.getStart(), task.getStart(), ""));
			}
		}
		new Plan(changes, List.of(), List.of()).apply(editSupport);
	}

	private boolean levelResource(Resource resource, Options options, Map<Task, Long> addedDelay,
		Map<Task, String> limitingResource, Map<Task, Split> splits, List<Conflict> unresolved,
		LevelingScratch scratch) {
		if (resource == null || !resource.isLabor() || resource.getMaximumUnits() <= 0D) {
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
		assignments.sort(assignmentComparator(options.order()));
		List<Scheduled> accepted = scratch.accepted;
		boolean changed = false;
		for (Assignment assignment : assignments) {
			Task task = assignment.getTask();
			long existingAdded = addedDelay.getOrDefault(task, 0L);
			long duration = Math.max(0L, assignment.getEnd() - assignment.getStart());
			long start = shiftedStart(task, assignment.getStart(), existingAdded);
			long end = start + duration;
			Split proposedSplit = null;
			int attempts = 0;
			while (isOverallocated(start, end, assignment.getUnits(), accepted, resource.getMaximumUnits(), scratch.boundaries)
				&& attempts++ < assignments.size() + 2) {
				Scheduled blocker = firstBlockingInterval(start, end, assignment.getUnits(), accepted, resource.getMaximumUnits());
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
				long next = nextAvailableBoundary(start, end, accepted);
				if (next <= start) {
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
			if (proposedSplit == null && isOverallocated(start, end, assignment.getUnits(), accepted, resource.getMaximumUnits(), scratch.boundaries)) {
				unresolved.add(new Conflict(resource, task, "Resource remains overallocated"));
			}
			if (proposedSplit == null) {
				accepted.add(new Scheduled(assignment, task, start, end, Math.max(0D, assignment.getUnits())));
			} else {
				accepted.add(new Scheduled(assignment, task, start, proposedSplit.from(), Math.max(0D, assignment.getUnits())));
				accepted.add(new Scheduled(assignment, task, proposedSplit.to(), proposedSplit.to() + (end - proposedSplit.from()), Math.max(0D, assignment.getUnits())));
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

	private static boolean isOverallocated(long start, long end, double units,
		List<Scheduled> accepted, double capacity, List<Long> boundaries) {
		if (units > capacity + 0.000001D) {
			return true;
		}
		boundaries.clear();
		boundaries.add(start);
		for (Scheduled scheduled : accepted) {
			if (overlaps(start, end, scheduled.start(), scheduled.end())) {
				boundaries.add(Math.max(start, scheduled.start()));
			}
		}
		for (long boundary : boundaries) {
			double total = units;
			for (Scheduled scheduled : accepted) {
				if (scheduled.start() <= boundary && scheduled.end() > boundary) {
					total += scheduled.units();
				}
			}
			if (total > capacity + 0.000001D) {
				return true;
			}
		}
		return false;
	}

	private static long nextAvailableBoundary(long start, long end, List<Scheduled> accepted) {
		long next = Long.MAX_VALUE;
		for (Scheduled scheduled : accepted) {
			if (overlaps(start, end, scheduled.start(), scheduled.end()) && scheduled.end() > start) {
				next = Math.min(next, scheduled.end());
			}
		}
		return next;
	}

	private static Scheduled firstBlockingInterval(long start, long end, double units, List<Scheduled> accepted, double capacity) {
		return accepted.stream().filter(value -> overlaps(start, end, value.start(), value.end()))
			.filter(value -> units + value.units() > capacity + 0.000001D)
			.min(Comparator.comparingLong(Scheduled::start)).orElse(null);
	}

	private static boolean overlaps(long firstStart, long firstEnd, long secondStart, long secondEnd) {
		return firstStart < secondEnd && secondStart < firstEnd;
	}

	private static List<Conflict> deduplicate(List<Conflict> values) {
		Map<String, Conflict> unique = new LinkedHashMap<>(Math.max(4, values.size() * 4 / 3 + 1));
		for (Conflict value : values) {
			String key = value.resource().getUniqueId() + ":" + value.task().getUniqueId() + ":" + value.reason();
			unique.put(key, value);
		}
		return new ArrayList<>(unique.values());
	}
}
