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
import com.microproject.pm.calendar.WorkCalendar;
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
		long rangeStart, long rangeEnd, boolean includeProposedResources, Sensitivity sensitivity,
		boolean levelAssignments, boolean levelManuallyScheduledTasks) {
		public Options(Order order, boolean onlyWithinAvailableSlack, long rangeStart, long rangeEnd) {
			this(order, onlyWithinAvailableSlack, true, rangeStart, rangeEnd, false, Sensitivity.DAY, true, false);
		}
		public Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
			long rangeStart, long rangeEnd) {
			this(order, onlyWithinAvailableSlack, allowTaskSplits, rangeStart, rangeEnd, false, Sensitivity.DAY, true, false);
		}
		public Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
			long rangeStart, long rangeEnd, boolean includeProposedResources) {
			this(order, onlyWithinAvailableSlack, allowTaskSplits, rangeStart, rangeEnd,
				includeProposedResources, Sensitivity.DAY, true, false);
		}
		public Options(Order order, boolean onlyWithinAvailableSlack, boolean allowTaskSplits,
			long rangeStart, long rangeEnd, boolean includeProposedResources, Sensitivity sensitivity) {
			this(order, onlyWithinAvailableSlack, allowTaskSplits, rangeStart, rangeEnd,
				includeProposedResources, sensitivity, true, false);
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
	public record AssignmentSplit(Assignment assignment, long from, long to, String limitingResource) { }
	public record AssignmentChange(Assignment assignment, long oldDelay, long newDelay) { }

	public static final class Plan {
		private final List<Change> changes;
		private final List<Conflict> unresolved;
		private final List<Split> splits;
		private final List<AssignmentSplit> assignmentSplits;
		private final List<AssignmentChange> assignmentChanges;
		private final List<NormalTask> clearGeneratedSplits;
		private final List<Assignment> clearGeneratedAssignmentSplits;
		private final Map<Task, Object> detailBackups;
		private final Map<Task, ManualDates> manualDateChanges = new IdentityHashMap<>();
		private record ManualDates(long oldStart, long oldFinish, long newStart, long newFinish,
			Task.LevelingManualDateState oldState, Task.LevelingManualDateState newState, boolean changeDates) { }
		private final Map<Assignment, Object> assignmentDetailBackups;
		private final PlanSourceState sourceState;

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits) {
			this(changes, unresolved, splits, List.of(), List.of(), List.of(), List.of(), null);
		}

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits,
			List<NormalTask> clearGeneratedSplits) {
			this(changes, unresolved, splits, List.of(), List.of(), clearGeneratedSplits, List.of(), null);
		}

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits,
			List<AssignmentChange> assignmentChanges, List<NormalTask> clearGeneratedSplits) {
			this(changes, unresolved, splits, List.of(), assignmentChanges, clearGeneratedSplits, List.of(), null);
		}

		private Plan(List<Change> changes, List<Conflict> unresolved, List<Split> splits,
			List<AssignmentSplit> assignmentSplits, List<AssignmentChange> assignmentChanges,
			List<NormalTask> clearGeneratedSplits, List<Assignment> clearGeneratedAssignmentSplits,
			PlanSourceState sourceState) {
			this.changes = List.copyOf(changes);
			this.unresolved = List.copyOf(unresolved);
			this.splits = List.copyOf(splits);
			this.assignmentSplits = List.copyOf(assignmentSplits);
			this.assignmentChanges = List.copyOf(assignmentChanges);
			this.clearGeneratedSplits = List.copyOf(clearGeneratedSplits);
			this.clearGeneratedAssignmentSplits = List.copyOf(clearGeneratedAssignmentSplits);
			this.detailBackups = new IdentityHashMap<>(splits.size() + clearGeneratedSplits.size()
				+ assignmentChanges.size() + assignmentSplits.size() + clearGeneratedAssignmentSplits.size());
			this.assignmentDetailBackups = new IdentityHashMap<>(assignmentSplits.size() + clearGeneratedAssignmentSplits.size());
			this.sourceState = sourceState;
			for (Change change : changes) {
				Task task = change.task();
				Task.LevelingManualDateState oldState = task.getLevelingManualDateState();
				if (change.oldDelay() != change.newDelay()
					|| (change.newDelay() == 0L && oldState != null)) {
					long oldStart = task.getManualStart();
					long oldFinish = task.getManualFinish();
					if (change.newDelay() == 0L) {
						boolean restoreDates = task.isManuallyScheduled() && oldState != null && oldStart == oldState.generatedStart()
							&& oldFinish == oldState.generatedFinish();
						manualDateChanges.put(task, new ManualDates(oldStart, oldFinish,
							restoreDates ? oldState.originalStart() : oldStart,
							restoreDates ? oldState.originalFinish() : oldFinish, oldState, null, restoreDates));
					} else if (task.isManuallyScheduled()) {
						long shift = Math.subtractExact(change.projectedStart(), change.oldStart());
						boolean continuing = oldState != null && oldStart == oldState.generatedStart()
							&& oldFinish == oldState.generatedFinish();
						long originalStart = continuing ? oldState.originalStart() : oldStart;
						long originalFinish = continuing ? oldState.originalFinish() : oldFinish;
						long generatedStart = Math.addExact(oldStart, shift);
						long generatedFinish = Math.addExact(oldFinish, shift);
						Task.LevelingManualDateState newState = shift == 0L || oldStart <= 0L || oldFinish < oldStart ? oldState
							: new Task.LevelingManualDateState(originalStart, originalFinish, generatedStart, generatedFinish);
						manualDateChanges.put(task, new ManualDates(oldStart, oldFinish, generatedStart,
							generatedFinish, oldState, newState, newState != oldState && oldStart > 0L && oldFinish >= oldStart));
					} else if (oldState != null) {
						manualDateChanges.put(task, new ManualDates(oldStart, oldFinish, oldStart, oldFinish,
							oldState, null, false));
					}
				}
			}
		}

		public List<Change> changes() {
			return changes;
		}

		public List<Conflict> unresolved() {
			return unresolved;
		}
		public List<Split> splits() { return splits; }
		public List<AssignmentSplit> assignmentSplits() { return assignmentSplits; }
		public List<AssignmentChange> assignmentChanges() { return assignmentChanges; }
		public boolean hasChanges() { return hasMutations(); }

		public boolean isComplete() {
			return unresolved.isEmpty();
		}

		/** Revalidates preview inputs without mutating the model. */
		public void assertCurrent() {
			ProjectMutationState.coordinated(() -> {
				if (sourceState != null) sourceState.assertUnchanged();
			});
		}

		public void apply() {
			applyAtomically(null, List.of(), null, null);
		}

		public void apply(EditSupport editSupport) {
			applyAtomically(editSupport, List.of(), null, null);
		}

		private void applyAtomically(EditSupport editSupport, Collection<? extends Task> requiredTasks,
			Runnable validationCheck, Runnable invariantCheck) {
			ProjectMutationState.coordinated(() -> applyAtomicallyCoordinated(
				editSupport, requiredTasks, validationCheck, invariantCheck));
		}

		private void applyAtomicallyCoordinated(EditSupport editSupport, Collection<? extends Task> requiredTasks,
			Runnable validationCheck, Runnable invariantCheck) {
			List<Project> projects = affectedProjects(requiredTasks);
			if (!hasMutations()) {
				if (sourceState != null) sourceState.assertUnchanged();
				if (validationCheck != null) validationCheck.run();
				return;
			}
			if (projects.isEmpty()) throw new IllegalStateException("Leveling plan has no owning project");
			List<ProjectMutationState.Scope> scopes = new ArrayList<>(projects.size());
			List<Project> openedScopes = new ArrayList<>(projects.size());
			List<Project> openedBatches = new ArrayList<>(projects.size());
			List<Project.PendingEventBatch> pendingBatches = new ArrayList<>(projects.size());
			Map<Task, Object> originalBackups = new IdentityHashMap<>(detailBackups);
			Map<Assignment, Object> originalAssignmentBackups = new IdentityHashMap<>(assignmentDetailBackups);
			try {
				for (Project project : projects) {
					scopes.add(project.getMutationState().begin());
					openedScopes.add(project);
					project.beginEventBatch();
					openedBatches.add(project);
				}
				ModelTransaction.execute(new ModelTransaction.Steps<Map<Project, ProjectTaskSnapshot>, Boolean>() {
					@Override public void validate() {
						if (sourceState != null) sourceState.assertUnchanged();
						if (validationCheck != null) validationCheck.run();
						for (Task task : mutatedTasks(requiredTasks)) {
							if (task.getOwningProject() == null || task.isReadOnly())
								throw new IllegalStateException("A leveling task is no longer editable");
						}
					}
					@Override public void authorize() {
						for (Project project : projects)
							if (project.isReadOnly()) throw new IllegalStateException("Cannot level a read-only project");
					}
					@Override public Map<Project, ProjectTaskSnapshot> capture() {
						Map<Project, ProjectTaskSnapshot> snapshots = new LinkedHashMap<>();
						for (Project project : projects) snapshots.put(project, ProjectTaskSnapshot.capture(project));
						return snapshots;
					}
					@Override public ModelTransaction.Mutation<Boolean> apply() {
						applyChanges(true);
						return ModelTransaction.Mutation.changed(Boolean.TRUE);
					}
					@Override public void schedule() { }
					@Override public void checkInvariants() {
						if (invariantCheck != null) invariantCheck.run();
					}
					@Override public void restore(Map<Project, ProjectTaskSnapshot> snapshots) {
						for (Project project : projects) {
							ProjectTaskSnapshot snapshot = snapshots.get(project);
							if (snapshot != null) snapshot.restore(project, Plan.this);
						}
						for (Map.Entry<Assignment, Object> backup : assignmentDetailBackups.entrySet())
							backup.getKey().restoreDetail(backup.getValue());
						for (Project project : projects) project.recalculate();
						detailBackups.clear();
						detailBackups.putAll(originalBackups);
						assignmentDetailBackups.clear();
						assignmentDetailBackups.putAll(originalAssignmentBackups);
					}
					@Override public void commit(Boolean changed) {
						for (int index = projects.size() - 1; index >= 0; index--) {
							Project project = projects.get(index);
							Project.PendingEventBatch pending = project.commitEventBatch(scopes.get(index), changed);
							openedBatches.remove(project);
							openedScopes.remove(project);
							pendingBatches.add(pending);
						}
					}
				});
				Project.PendingEventBatch undoBatch = pendingBatches.stream()
					.filter(batch -> batch != null && batch.undoGroup() != null).findFirst().orElse(null);
				if (editSupport != null && undoBatch != null) {
					try (EditSupport.Activation ignored = editSupport.activatePublication(undoBatch.undoGroup())) {
						postUndoEdit(editSupport);
					}
				} else {
					postUndoEdit(editSupport);
				}
				for (int index = 0; index < projects.size(); index++)
					projects.get(index).publishEventBatch(pendingBatches.get(projects.size() - index - 1));
			} catch (ModelTransaction.ModelTransactionException failure) {
				rollbackBatches(openedScopes, openedBatches, scopes, failure);
				throw new IllegalStateException("Apply leveling failed during " + failure.failedStage(), failure);
			} catch (RuntimeException | Error failure) {
				rollbackBatches(openedScopes, openedBatches, scopes, failure);
				throw failure;
			}
		}

		private List<Project> affectedProjects(Collection<? extends Task> requiredTasks) {
			Set<Project> result = new LinkedHashSet<>();
			for (Task task : mutatedTasks(requiredTasks)) {
				Project project = task.getOwningProject();
				if (project != null) result.add(project);
			}
			return List.copyOf(result);
		}

		private List<Task> mutatedTasks(Collection<? extends Task> requiredTasks) {
			List<Task> result = new ArrayList<>();
			if (requiredTasks != null) for (Task task : requiredTasks) addIdentity(result, task);
			for (Change change : changes) addIdentity(result, change.task());
			for (Split split : splits) addIdentity(result, split.task());
			for (NormalTask task : clearGeneratedSplits) addIdentity(result, task);
			for (AssignmentChange change : assignmentChanges) addIdentity(result, change.assignment().getTask());
			for (AssignmentSplit split : assignmentSplits) addIdentity(result, split.assignment().getTask());
			for (Assignment assignment : clearGeneratedAssignmentSplits) addIdentity(result, assignment.getTask());
			return List.copyOf(result);
		}

		private static void addIdentity(List<Task> tasks, Task candidate) {
			if (candidate != null && tasks.stream().noneMatch(task -> task == candidate)) tasks.add(candidate);
		}

		private static void rollbackBatches(List<Project> openedScopes, List<Project> openedBatches,
			List<ProjectMutationState.Scope> scopes, Throwable failure) {
			for (int index = openedScopes.size() - 1; index >= 0; index--) {
				Project project = openedScopes.get(index);
				if (openedBatches.contains(project)) {
					try { project.endEventBatch(false); }
					catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				}
				try { scopes.get(index).close(); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
			}
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
				for (AssignmentSplit split : assignmentSplits) {
					assignmentDetailBackups.computeIfAbsent(split.assignment(), Assignment::backupDetail);
					if (!ScheduleService.getInstance().splitForLeveling(this, split.assignment(), split.from(), split.to()))
						throw new IllegalStateException("The planned assignment-level split could not be applied");
					projects.add(split.assignment().getTask().getOwningProject());
				}
				for (Assignment assignment : clearGeneratedAssignmentSplits) {
					assignmentDetailBackups.computeIfAbsent(assignment, Assignment::backupDetail);
					assignment.clearLevelingGeneratedSplits(this);
					projects.add(assignment.getTask().getOwningProject());
				}
			} else {
				for (Map.Entry<Task, Object> backup : detailBackups.entrySet()) {
					backup.getKey().restoreDetail(this, backup.getValue(), false);
					projects.add(backup.getKey().getOwningProject());
				}
				for (Map.Entry<Assignment, Object> backup : assignmentDetailBackups.entrySet()) {
					backup.getKey().restoreDetail(backup.getValue());
					projects.add(backup.getKey().getTask().getOwningProject());
				}
			}
			for (Change change : changes) {
				Task task = change.task();
				task.setLevelingDelay(forward ? change.newDelay() : change.oldDelay());
				ManualDates manualDates = manualDateChanges.get(task);
				if (manualDates != null) {
					if (manualDates.changeDates()) {
						task.setManualDates(forward ? manualDates.newStart() : manualDates.oldStart(),
							forward ? manualDates.newFinish() : manualDates.oldFinish());
					}
					task.setLevelingManualDateState(forward ? manualDates.newState() : manualDates.oldState());
				}
				task.setDirty(true);
				projects.add(task.getOwningProject());
			}
			for (AssignmentChange change : assignmentChanges) {
				change.assignment().setLevelingDelay(forward ? change.newDelay() : change.oldDelay());
				Task task = change.assignment().getTask();
				task.setDirty(true);
				projects.add(task.getOwningProject());
			}
			for (Project project : projects) {
				project.recalculate();
				project.setGroupDirty(true);
			}
		}

		private boolean hasMutations() {
			return !changes.isEmpty() || !assignmentChanges.isEmpty() || !splits.isEmpty()
				|| !assignmentSplits.isEmpty() || !clearGeneratedSplits.isEmpty()
				|| !clearGeneratedAssignmentSplits.isEmpty();
		}

		public void revert() {
			applyChanges(false);
		}
	}

	/** Captures the mutable scheduling inputs used by a preview so Apply cannot commit a stale plan. */
	private static final class PlanSourceState {
		private final List<ProjectRevision> projectRevisions;
		private final List<ResourcePoolRevision> resourcePoolRevisions;
		private final List<ResourceInput> resources;
		private final List<AssignmentInput> assignments;
		private final List<TaskInput> tasks;
		private final Map<WorkCalendar, byte[]> calendars;
		private final int weekStartsOn;

		private PlanSourceState(List<ProjectRevision> projectRevisions, List<ResourcePoolRevision> resourcePoolRevisions,
			List<ResourceInput> resources,
			List<AssignmentInput> assignments, List<TaskInput> tasks, Map<WorkCalendar, byte[]> calendars) {
			this.projectRevisions = List.copyOf(projectRevisions);
			this.resourcePoolRevisions = List.copyOf(resourcePoolRevisions);
			this.resources = List.copyOf(resources);
			this.assignments = List.copyOf(assignments);
			this.tasks = List.copyOf(tasks);
			this.calendars = calendars;
			this.weekStartsOn = CalendarOption.getInstance().getWeekStartsOn();
		}

		private static PlanSourceState capture(Collection<? extends Resource> sourceResources) {
			Map<Project, Long> revisions = new IdentityHashMap<>();
			Map<ResourcePool, Long> resourcePoolRevisions = new IdentityHashMap<>();
			List<ResourceInput> resourceInputs = new ArrayList<>();
			List<AssignmentInput> assignmentInputs = new ArrayList<>();
			List<TaskInput> taskInputs = new ArrayList<>();
			Map<WorkCalendar, byte[]> calendars = new IdentityHashMap<>();
			Set<Resource> seenResources = Collections.newSetFromMap(new IdentityHashMap<>());
			Set<Assignment> seenAssignments = Collections.newSetFromMap(new IdentityHashMap<>());
			Set<Task> seenTasks = Collections.newSetFromMap(new IdentityHashMap<>());
			for (Resource resource : sourceResources) {
				if (resource == null || !seenResources.add(resource)) continue;
				List<Assignment> resourceAssignments = new ArrayList<>();
				for (Object value : resource.getAssignments()) {
					if (!(value instanceof Assignment assignment)) continue;
					resourceAssignments.add(assignment);
					if (seenAssignments.add(assignment)) {
						assignmentInputs.add(AssignmentInput.capture(assignment));
						captureCalendar(calendars, assignment.getEffectiveWorkCalendar());
					}
					Task task = assignment.getTask();
					if (task != null && seenTasks.add(task)) {
						taskInputs.add(TaskInput.capture(task));
						captureCalendar(calendars, task.getEffectiveWorkCalendar());
					}
					Project project = task == null ? null : task.getOwningProject();
					if (project != null) {
						revisions.putIfAbsent(project, project.getMutationState().revision());
						ResourcePool pool = project.getResourcePool();
						resourcePoolRevisions.putIfAbsent(pool, pool.getCoordinationRevision());
					}
				}
			List<AvailabilityInput> availability = new ArrayList<>();
			for (ValueObjectForInterval interval : resource.getAvailabilityTable().getList())
				availability.add(new AvailabilityInput(interval.getStart(), interval.getEnd(),
					((Availability) interval).getMaximumUnits()));
				captureCalendar(calendars, resource.getEffectiveWorkCalendar());
				resourceInputs.add(new ResourceInput(resource, resource.getUniqueId(), resource.getName(), resource.getMaximumUnits(), resource.getBookingTypeKind(),
				resource.isLabor(), resource.getEffectiveWorkCalendar(), List.copyOf(resourceAssignments), List.copyOf(availability)));
			}
			return new PlanSourceState(projectRevisions(revisions), poolRevisions(resourcePoolRevisions),
				resourceInputs, assignmentInputs, taskInputs, calendars);
		}

		private static void captureCalendar(Map<WorkCalendar, byte[]> calendars, WorkCalendar calendar) {
			if (calendar != null && !calendars.containsKey(calendar)) calendars.put(calendar, calendarRules(calendar));
		}

		/* Serialize only the effective rules, excluding mutable scheduling caches. */
		private static byte[] calendarRules(WorkCalendar calendar) {
			var definition = calendar.getConcreteInstance();
			try (var bytes = new java.io.ByteArrayOutputStream(); var output = new java.io.ObjectOutputStream(bytes)) {
				for (int day = 0; day < 7; day++) output.writeObject(definition.getWeekDay(day));
				output.writeObject(definition.getExceptionDays());
				output.writeObject(new ArrayList<>(definition.getWorkWeekPeriods()));
				output.writeObject(new ArrayList<>(definition.getRecurringExceptions()));
				output.flush();
				return bytes.toByteArray();
			} catch (java.io.IOException exception) {
				throw new IllegalStateException("Unable to capture leveling calendar rules", exception);
			}
		}

		private void assertUnchanged() {
			if (weekStartsOn != CalendarOption.getInstance().getWeekStartsOn())
				throw new IllegalStateException("Leveling preview is stale because the first weekday changed");
			for (var entry : calendars.entrySet())
				if (!java.util.Arrays.equals(entry.getValue(), calendarRules(entry.getKey())))
					throw new IllegalStateException("Leveling preview is stale because calendar rules changed");
			for (ProjectRevision revision : projectRevisions)
				if (revision.project().getMutationState().revision() != revision.revision())
					throw new IllegalStateException("Leveling preview is stale because the project changed");
			for (ResourcePoolRevision revision : resourcePoolRevisions)
				if (revision.pool().getCoordinationRevision() != revision.revision())
					throw new IllegalStateException("Leveling preview is stale because the shared resource pool changed");
			for (ResourceInput input : resources)
				if (!input.matches()) throw new IllegalStateException("Leveling preview is stale because resource capacity changed");
			for (AssignmentInput input : assignments)
				if (!input.matches()) throw new IllegalStateException("Leveling preview is stale because an assignment changed");
			for (TaskInput input : tasks)
				if (!input.matches()) throw new IllegalStateException("Leveling preview is stale because a task schedule changed");
		}

		private static List<ProjectRevision> projectRevisions(Map<Project, Long> revisions) {
			List<ProjectRevision> result = new ArrayList<>(revisions.size());
			revisions.forEach((project, revision) -> result.add(new ProjectRevision(project, revision)));
			return result;
		}
		private static List<ResourcePoolRevision> poolRevisions(Map<ResourcePool, Long> revisions) {
			List<ResourcePoolRevision> result = new ArrayList<>(revisions.size());
			revisions.forEach((pool, revision) -> result.add(new ResourcePoolRevision(pool, revision)));
			return result;
		}
		private record ProjectRevision(Project project, long revision) { }
		private record ResourcePoolRevision(ResourcePool pool, long revision) { }
		private record AvailabilityInput(long start, long end, double units) { }
		private record ResourceInput(Resource resource, long uniqueId, String name, double maximumUnits, BookingType.Kind bookingType,
			boolean labor, WorkCalendar calendar, List<Assignment> assignments, List<AvailabilityInput> availability) {
			private boolean matches() {
				if (resource.getUniqueId() != uniqueId || !Objects.equals(resource.getName(), name)
					|| resource.getMaximumUnits() != maximumUnits || resource.getBookingTypeKind() != bookingType
					|| resource.isLabor() != labor || resource.getEffectiveWorkCalendar() != calendar) return false;
				List<Assignment> currentAssignments = new ArrayList<>();
				for (Object value : resource.getAssignments())
					if (value instanceof Assignment assignment) currentAssignments.add(assignment);
			if (!sameIdentitySequence(assignments, currentAssignments)) return false;
			List<AvailabilityInput> currentAvailability = new ArrayList<>();
			for (ValueObjectForInterval interval : resource.getAvailabilityTable().getList())
				currentAvailability.add(new AvailabilityInput(interval.getStart(), interval.getEnd(),
					((Availability) interval).getMaximumUnits()));
			return availability.equals(currentAvailability);
			}
		}
		private record AssignmentInput(Assignment assignment, Task task, long start, long end, long delay,
			double units, long duration, WorkCalendar calendar, String contour) {
			private static AssignmentInput capture(Assignment assignment) {
				return new AssignmentInput(assignment, assignment.getTask(), assignment.getStart(), assignment.getEnd(),
					assignment.getLevelingDelay(), assignment.getUnits(), assignment.getDurationMillis(),
					assignment.getEffectiveWorkCalendar(),
					assignment.getWorkContour().toString(assignment.getDurationMillis()));
			}
			private boolean matches() {
				return assignment.getTask() == task && assignment.getStart() == start && assignment.getEnd() == end
					&& assignment.getLevelingDelay() == delay && Double.compare(assignment.getUnits(), units) == 0
					&& assignment.getDurationMillis() == duration && assignment.getEffectiveWorkCalendar() == calendar
					&& contour.equals(assignment.getWorkContour().toString(assignment.getDurationMillis()));
			}
		}
		private record TaskInput(Task task, Project project, long id, long uniqueId, boolean summary, boolean milestone,
			List<TaskSplitInterval> splits, long start, long end, long delay, long totalSlack,
			long actualStart, double percentComplete, int priority, boolean manuallyScheduled, boolean inactive,
			boolean complete, boolean readOnly, boolean levelingCanSplit, boolean levelAssignments,
			WorkCalendar calendar) {
			private static TaskInput capture(Task task) {
				WorkCalendar calendar = task.getEffectiveWorkCalendar();
				return new TaskInput(task, task.getOwningProject(), task.getId(), task.getUniqueId(), task.isSummary(), task.isMilestone(),
					task instanceof NormalTask normal ? List.copyOf(normal.getTaskSplitIntervals()) : List.of(),
					task.getStart(), task.getEnd(), task.getLevelingDelay(),
					task.getTotalSlack(), task.getActualStart(), task.getPercentComplete(), task.getPriority(),
					task.isManuallyScheduled(), task.isInactiveTask(), task.isComplete(), task.isReadOnly(),
					task.isLevelingCanSplit(), task.isLevelAssignments(), calendar);
			}
			private boolean matches() {
				return task.getOwningProject() == project && task.getId() == id && task.getUniqueId() == uniqueId
					&& task.isSummary() == summary && task.isMilestone() == milestone
					&& (task instanceof NormalTask normal ? normal.getTaskSplitIntervals() : List.of()).equals(splits)
					&& task.getStart() == start && task.getEnd() == end
					&& task.getLevelingDelay() == delay && task.getTotalSlack() == totalSlack
					&& task.getActualStart() == actualStart && Double.compare(task.getPercentComplete(), percentComplete) == 0
					&& task.getPriority() == priority && task.isManuallyScheduled() == manuallyScheduled
					&& task.isInactiveTask() == inactive && task.isComplete() == complete && task.isReadOnly() == readOnly
					&& task.isLevelingCanSplit() == levelingCanSplit && task.isLevelAssignments() == levelAssignments
					&& task.getEffectiveWorkCalendar() == calendar;
			}
		}
	}

	private static boolean sameIdentitySequence(List<?> expected, List<?> actual) {
		if (expected.size() != actual.size()) return false;
		for (int index = 0; index < expected.size(); index++)
			if (expected.get(index) != actual.get(index)) return false;
		return true;
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
		private final Map<Assignment, Assignment> virtualAssignments = new IdentityHashMap<>();

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
		PlanSourceState sourceState = PlanSourceState.capture(resources);
		Map<Task, Long> addedDelay = new IdentityHashMap<>();
		Map<Assignment, Long> assignmentAddedDelay = new IdentityHashMap<>();
		Map<Task, String> limitingResource = new IdentityHashMap<>();
		Map<Task, Split> splits = new IdentityHashMap<>();
		Map<Assignment, List<AssignmentSplit>> assignmentSplits = new IdentityHashMap<>();
		List<Conflict> unresolved = new ArrayList<>(Math.max(4, resources.size()));
		LevelingScratch scratch = new LevelingScratch();

		boolean changed;
		int pass = 0;
		do {
			changed = false;
			for (Resource resource : resources) {
				changed |= levelResource(resource, options, selectedTasks, addedDelay, assignmentAddedDelay,
					limitingResource, splits, assignmentSplits, unresolved, scratch);
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
		List<AssignmentChange> assignmentChanges = new ArrayList<>(assignmentAddedDelay.size());
		for (Map.Entry<Assignment, Long> entry : assignmentAddedDelay.entrySet()) {
			Assignment assignment = entry.getKey();
			assignmentChanges.add(new AssignmentChange(assignment, assignment.getLevelingDelay(),
				Duration.millis(assignment.getLevelingDelay()) + entry.getValue()));
		}
		sourceState.assertUnchanged();
		List<AssignmentSplit> assignmentSplitChanges = new ArrayList<>();
		for (List<AssignmentSplit> values : assignmentSplits.values()) assignmentSplitChanges.addAll(values);
		return new Plan(new ArrayList<>(result.values()), deduplicate(unresolved), new ArrayList<>(splits.values()),
			assignmentSplitChanges, assignmentChanges, List.of(), List.of(), sourceState);
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
		if (Duration.millis(task.getLevelingDelay()) != 0L
			|| task.getLevelingManualDateState() != null
			|| task instanceof NormalTask normalTask && !normalTask.getLevelingTaskSplitIntervals().isEmpty()) return true;
		if (task instanceof NormalTask normalTask) for (Object value : normalTask.getAssignments())
			if (Duration.millis(((Assignment) value).getLevelingDelay()) != 0L
				|| ((Assignment) value).getLevelingSplitState() != null) return true;
		return false;
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
		List<AssignmentChange> assignmentChanges = new ArrayList<>();
		List<NormalTask> generatedSplits = new ArrayList<>();
		List<Assignment> generatedAssignmentSplits = new ArrayList<>();
		for (Task task : affected) {
			if (Duration.millis(task.getLevelingDelay()) != 0L || task.getLevelingManualDateState() != null) {
				changes.add(new Change(task, task.getLevelingDelay(), 0L, task.getStart(), task.getStart(), ""));
			}
			if (task instanceof NormalTask normalTask && !normalTask.getLevelingTaskSplitIntervals().isEmpty()) {
				generatedSplits.add(normalTask);
			}
			if (task instanceof NormalTask normalTask) for (Object value : normalTask.getAssignments()) {
				Assignment assignment = (Assignment) value;
				if (Duration.millis(assignment.getLevelingDelay()) != 0L)
					assignmentChanges.add(new AssignmentChange(assignment, assignment.getLevelingDelay(), 0L));
				if (assignment.getLevelingSplitState() != null) generatedAssignmentSplits.add(assignment);
			}
		}
		Plan clearPlan = new Plan(changes, List.of(), List.of(), List.of(), assignmentChanges,
			generatedSplits, generatedAssignmentSplits, null);
		clearPlan.applyAtomically(editSupport, affected, () -> {
			for (Task task : affected)
				if (task.getOwningProject() != project || task.isReadOnly())
					throw new IllegalStateException("A leveling task is no longer editable in this project");
		}, () -> {
			for (Task task : affected)
				if (hasLevelingState(task)) throw new IllegalStateException("Leveling state remains after clear");
		});
		return List.copyOf(affected);
	}


	private boolean levelResource(Resource resource, Options options, Set<Task> selectedTasks, Map<Task, Long> addedDelay,
		Map<Assignment, Long> assignmentAddedDelay,
		Map<Task, String> limitingResource, Map<Task, Split> splits,
		Map<Assignment, List<AssignmentSplit>> assignmentSplits, List<Conflict> unresolved,
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
		assignments.sort(Comparator
			.comparing((Assignment assignment) -> !isFixedConstraint(assignment.getTask(), selectedTasks, options))
			.thenComparing(assignmentOrder));
		List<Scheduled> accepted = scratch.accepted;
		boolean changed = false;
		for (Assignment assignment : assignments) {
			Task task = assignment.getTask();
			Assignment virtualAssignment = scratch.virtualAssignments.get(assignment);
			if (isFixedConstraint(task, selectedTasks, options)) {
				Assignment schedule = virtualAssignment == null ? assignment : virtualAssignment;
				Scheduled scheduled = new Scheduled(assignment, task, schedule.getStart(), schedule.getEnd(),
					Math.max(0D, assignment.getUnits()));
				accepted.add(scheduled);
				addBucketLoads(scheduled, options.sensitivity(), scratch);
				continue;
			}
			boolean levelAssignment = options.levelAssignments() && task.isLevelAssignments();
			long existingAdded = levelAssignment
				? assignmentAddedDelay.getOrDefault(assignment, 0L)
				: addedDelay.getOrDefault(task, 0L);
			Assignment schedule = virtualAssignment == null ? assignment : virtualAssignment;
			long duration = Math.max(0L, schedule.getEnd() - schedule.getStart());
			long start = shiftedStart(task, assignment.getStart(), existingAdded);
			long end = start + duration;
			Split proposedSplit = null;
			int attempts = 0;
			while (isOverallocated(start, end, assignment, resource, options.sensitivity(), scratch)
				&& attempts++ < assignments.size() + 2) {
				Scheduled blocker = firstBlockingInterval(start, end, assignment, resource, options.sensitivity(), scratch);
				if (!levelAssignment && options.allowTaskSplits() && task.isLevelingCanSplit()
					&& blocker != null && blocker.start() > start && blocker.end() < end
					&& !task.isMilestone() && !task.inProgress()) {
					proposedSplit = new Split(task, blocker.start(), blocker.end(), resource.getName());
					Split previous = splits.put(task, proposedSplit);
					limitingResource.put(task, resource.getName());
					changed |= !proposedSplit.equals(previous);
					break;
				}
				if (levelAssignment && options.allowTaskSplits() && task.isLevelingCanSplit()
					&& blocker != null && blocker.start() > start && blocker.end() < end
					&& !task.isMilestone() && !task.inProgress()) {
					virtualAssignment = scratch.virtualAssignments.computeIfAbsent(assignment,
						ignored -> (Assignment) assignment.clone());
					if (ScheduleService.getInstance().splitForLeveling(this, virtualAssignment, blocker.start(), blocker.end())) {
						scratch.workProfiles.remove(assignment);
						AssignmentSplit split = new AssignmentSplit(assignment, blocker.start(), blocker.end(), resource.getName());
						assignmentSplits.computeIfAbsent(assignment, ignored -> new ArrayList<>()).add(split);
						limitingResource.put(task, resource.getName());
						changed = true;
						start = shiftedStart(task, virtualAssignment.getStart(), existingAdded);
						end = start + Math.max(0L, virtualAssignment.getEnd() - virtualAssignment.getStart());
						continue;
					}
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
					existingAdded = levelAssignment
						? assignmentAddedDelay.getOrDefault(assignment, 0L)
						: addedDelay.getOrDefault(task, 0L);
					start = shiftedStart(task, assignment.getStart(), existingAdded);
					end = start + duration;
					break;
				}
				start = shiftedStart(task, assignment.getStart(), existingAdded);
				end = start + duration;
			}
			long previousAdded = levelAssignment
				? assignmentAddedDelay.getOrDefault(assignment, 0L)
				: addedDelay.getOrDefault(task, 0L);
			if (existingAdded > previousAdded) {
				if (levelAssignment) assignmentAddedDelay.put(assignment, existingAdded);
				else addedDelay.put(task, existingAdded);
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

	private static boolean isFixedConstraint(Task task, Set<Task> selectedTasks, Options options) {
		return (selectedTasks != null && !selectedTasks.contains(task))
			|| (task.isManuallyScheduled() && !options.levelManuallyScheduledTasks());
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
		Assignment profileAssignment = scratch.virtualAssignments.getOrDefault(assignment, assignment);
		List<WorkSegment> profile = scratch.workProfiles.computeIfAbsent(assignment,
			ignored -> workProfile(profileAssignment));
		long shift = start - profileAssignment.getStart();
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
