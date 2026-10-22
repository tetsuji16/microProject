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
package com.microproject.pm.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import com.microproject.undo.EditSupport;

import com.microproject.association.AssociationList;
import com.microproject.configuration.Settings;
import com.microproject.datatype.TimeUnit;
import com.microproject.grouping.core.Node;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.SchedulingType;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.undo.AssignmentCreationEdit;
import com.microproject.undo.AssignmentDeletionEdit;
import com.microproject.undo.AssignmentDetailStateEdit;
import com.microproject.undo.NodeUndoInfo;
import com.microproject.undo.ScheduleBackupEdit;
import com.microproject.undo.ScheduleEdit;

/**
 * Manages the creation and deleting of assignments as well as events
 */
public class AssignmentService {
	
	private static AssignmentService instance = null;
	private static final ThreadLocal<Boolean> suppressNestedUndo = ThreadLocal.withInitial(() -> false);
	public static AssignmentService getInstance() {
		if (instance == null)
			instance = new AssignmentService();
		return instance;
	}
	
	
	public void newAssignments(Collection<?> tasks, Collection<?> resources, double units, long delay, Object eventSource,boolean undo) {
		if (tasks.isEmpty() || resources.isEmpty()) {
			return;
		}
		List<?> taskSnapshot = new ArrayList<>(tasks);
		List<?> resourceSnapshot = new ArrayList<>(resources);
		if (!(taskSnapshot.get(0) instanceof NormalTask firstTask)) {
			newAssignmentsInternal(taskSnapshot, resourceSnapshot, units, delay, eventSource, undo, null);
			return;
		}
		Project commandProject = firstTask.getProject();
		ResourcePool commandPool = commandProject.getResourcePool();
		if (commandProject != null && commandPool != null) {
			commandProject.runAtomicModelCommand(markApplied -> newAssignmentsInternal(taskSnapshot, resourceSnapshot,
				units, delay, eventSource, undo, markApplied), Boolean.TRUE::equals, ignored -> Boolean.TRUE,
				List.of(new Project.AdditionalObjectEventManager(commandPool.getObjectEventManager(), commandPool, commandPool,
					resourceRefreshObjects(resourceSnapshot))));
			return;
		}
		newAssignmentsInternal(taskSnapshot, resourceSnapshot, units, delay, eventSource, undo, null);
	}

	private boolean newAssignmentsInternal(Collection<?> tasks, Collection<?> resources, double units, long delay,
			Object eventSource, boolean undo, Runnable markApplied) {
		Project transactionProject = null;
		ResourcePool transactionResourcePool = null;
		for (Object taskObject : tasks) {
			if (!(taskObject instanceof NormalTask))
				throw new IllegalArgumentException("Assignments require normal tasks");
			Project taskProject = ((NormalTask) taskObject).getProject();
			if (transactionProject == null)
				transactionProject = taskProject;
			else if (transactionProject != taskProject)
				throw new IllegalArgumentException("A batch assignment must belong to one project");
		}
		for (Object resourceObject : resources) {
			if (!(resourceObject instanceof Resource))
				throw new IllegalArgumentException("Assignments require resources");
			ResourcePool resourcePool = resourcePoolOf((Resource) resourceObject);
			if (resourcePool != null) {
				if (transactionResourcePool == null)
					transactionResourcePool = resourcePool;
				else if (transactionResourcePool != resourcePool)
					throw new IllegalArgumentException("A batch assignment must use one resource pool");
			}
		}
		BatchUpdate batchUpdate = new BatchUpdate();
		AssignmentModelState before = AssignmentModelState.capture(tasks);
		boolean changed = false;
		try {
			for (Object taskObject : tasks) {
				NormalTask task = (NormalTask) taskObject;
				Set<Resource> assignedResources = getAssignedResources(task);
				boolean hasUnassignedResource = resources.stream()
					.anyMatch(resourceObject -> !assignedResources.contains((Resource) resourceObject));
				if (!hasUnassignedResource)
					continue;
				batchUpdate.beginIfNeeded(task, tasks, transactionResourcePool, this, undo);
				changed |= batchAssignResources(task, resources, units, delay, eventSource, undo, markApplied);
			}
		} catch (RuntimeException | Error failure) {
			before.restore(failure);
			throw failure;
		} finally {
			batchUpdate.endIfNeeded();
		}
		return changed;
	}

	/**
	 * Replaces one resource assignment as a single model and Undo transaction.
	 *
	 * <p>Microsoft Project keeps the source assignment when it contains actual
	 * work and gives the selected replacement resources the source's remaining
	 * work.  A source without actual work is removed, which preserves the legacy
	 * remove-then-assign behavior without exposing that two-step implementation
	 * to callers.</p>
	 *
	 * @return assignments created for replacement resources; empty when no
	 *         usable replacement was supplied
	 */
	public List<Assignment> replaceAssignment(Assignment source,
			Collection<? extends Resource> replacementResources, Object eventSource, boolean undo) {
		if (source == null || !(source.getTask() instanceof NormalTask))
			throw new IllegalArgumentException("A replacement requires a normal-task assignment");
		NormalTask task = (NormalTask) source.getTask();
		Project commandProject = task.getProject();
		ResourcePool commandPool = commandProject.getResourcePool();
		if (commandProject != null && commandPool != null) {
			List<Object> refreshObjects = resourceRefreshObjects(replacementResources);
			refreshObjects.add(source.getResource());
			AssignmentMutationResult result = commandProject.runAtomicModelCommand(
				markApplied -> replaceAssignmentInternal(source, replacementResources, eventSource, undo,
					markApplied),
				AssignmentMutationResult::changed, AssignmentMutationResult::withFacts,
				List.of(new Project.AdditionalObjectEventManager(commandPool.getObjectEventManager(), commandPool, commandPool,
					refreshObjects)));
			return result.assignments();
		}
		return replaceAssignmentInternal(source, replacementResources, eventSource, undo, null).assignments();
	}


	private AssignmentMutationResult replaceAssignmentInternal(Assignment source,
			Collection<? extends Resource> replacementResources, Object eventSource, boolean undo,
			Runnable markApplied) {
		NormalTask task = (NormalTask) source.getTask();
		if (task.findAssignment(source.getResource()) != source)
			throw new IllegalArgumentException("The source assignment is no longer attached to its task");
		if (!task.isAssignable())
			return new AssignmentMutationResult(List.of(), false);
		List<Resource> replacements = replacementResourcesFor(source, replacementResources);
		if (replacements.isEmpty())
			return new AssignmentMutationResult(List.of(), false);

		ResourcePool resourcePool = resourcePoolOf(replacements.get(0));
		BatchUpdate batchUpdate = new BatchUpdate();
		List<Assignment> created = new ArrayList<Assignment>(replacements.size());
		int detailCapacity = undo ? replacements.size() + 1 : 0;
		Map<Assignment, Object> detailBefore = LinkedHashMap.newLinkedHashMap(detailCapacity);
		if (undo)
			detailBefore.put(source, source.backupDetail());
		AssignmentModelState modelBefore = AssignmentModelState.capture(List.of(task));
		boolean sourceHasActualWork = source.getActualWork(null) > 0L;
		long sourceActualWork = source.getActualWork(null);
		long sourceRemainingWork = source.getRemainingWork();
		boolean changed = false;
		try {
			try {
				// Assignment detail state is authoritative for replacement; the generic
				// task snapshot would overwrite it during undo.
				batchUpdate.beginIfNeeded(task, List.of(task), resourcePool, this, undo, false);
				if (undo)
					batchUpdate.postAssignmentDetailBefore(detailBefore);
				if (markApplied != null) markApplied.run();
				changed = true;
				if (!sourceHasActualWork)
					removeInternal(source, true, eventSource, new NodeUndoInfo(undo));
				for (int index = 0; index < replacements.size(); index++) {
					Assignment replacement = newAssignment(task, replacements.get(index), 1.0D, 0L,
						eventSource, new NodeUndoInfo(undo));
					if (replacement == null)
						continue;
					if (undo)
						detailBefore.put(replacement, replacement.backupDetail());
					if (sourceHasActualWork)
						// setRemainingWork derives actual work from total work.  A replacement
						// starts at zero actuals, so set its total work directly instead.
						replacement.setWork(replacementWorkShare(sourceRemainingWork,
							replacements.size(), index), null);
					created.add(replacement);
				}
				if (sourceHasActualWork)
					// Shrinking total work to the recorded actuals leaves the source's
					// actual work intact and removes only its remaining allocation.
					source.setWork(sourceActualWork, null);
				if (undo)
					batchUpdate.queueAssignmentDetailAfter(detailBefore.keySet());
			} finally {
				batchUpdate.endIfNeeded();
			}
		} catch (RuntimeException | Error failure) {
			try {
				modelBefore.restore(failure);
			} catch (RuntimeException | Error rollbackFailure) {
				if (rollbackFailure != failure) failure.addSuppressed(rollbackFailure);
			}
			throw failure;
		}
		return new AssignmentMutationResult(List.copyOf(created), changed);
	}

	private List<Object> resourceRefreshObjects(Collection<?> resources) {
		List<Object> refreshObjects = new ArrayList<>();
		if (resources != null)
			for (Object resource : resources)
				if (resource instanceof Resource) refreshObjects.add(resource);
		return refreshObjects;
	}

	private record AssignmentMutationResult(List<Assignment> assignments, boolean changed) {
		AssignmentMutationResult withFacts() {
			return new AssignmentMutationResult(assignments, true);
		}
	}

	private List<Resource> replacementResourcesFor(Assignment source,
			Collection<? extends Resource> replacementResources) {
		if (replacementResources == null)
			return List.of();
		Project sourceProject = ((NormalTask) source.getTask()).getProject();
		ResourcePool resourcePool = null;
		Set<Resource> unique = LinkedHashSet.newLinkedHashSet(replacementResources.size());
		for (Resource resource : replacementResources) {
			if (resource == null || resource == source.getResource()
					|| ((NormalTask) source.getTask()).findAssignment(resource) != null)
				continue;
			if (resourcePoolOf(resource) == null || resourcePoolOf(resource) != sourceProject.getResourcePool())
				throw new IllegalArgumentException("Replacement resources must belong to the task project resource pool");
			if (resourcePool == null)
				resourcePool = resourcePoolOf(resource);
			else if (resourcePool != resourcePoolOf(resource))
				throw new IllegalArgumentException("A replacement must use one resource pool");
			unique.add(resource);
		}
		return List.copyOf(unique);
	}

	private long replacementWorkShare(long totalRemainingWork, int replacementCount, int replacementIndex) {
		long base = totalRemainingWork / replacementCount;
		long remainder = totalRemainingWork % replacementCount;
		return base + (replacementIndex < remainder ? 1L : 0L);
	}

	private ResourcePool resourcePoolOf(Resource resource) {
		return resource.getDocument() instanceof ResourcePool ? (ResourcePool) resource.getDocument() : null;
	}

	private boolean batchAssignResources(NormalTask task, Collection<?> resources, double units, long delay,
			Object eventSource, boolean undo, Runnable markApplied) {
		boolean taskHadNoRealAssignments = !task.hasRealAssignments() || !task.hasLaborAssignment();
		TaskState taskState = preserveTaskState(task, taskHadNoRealAssignments);
		Set<Resource> assignedResources = getAssignedResources(task);
		boolean changed = false;
		try {
			for (Object resourceObject : resources) {
				Resource resource = (Resource) resourceObject;
				if (assignedResources.contains(resource)) {
					continue;
				}
				if (markApplied != null) markApplied.run();
				Assignment assignment = newAssignment(task, resource, units, delay, eventSource,
					new NodeUndoInfo(undo));
				if (assignment != null) {
					changed = true;
					if (!resource.isLabor()) {
						assignment.setRateUnit(TimeUnit.NON_TEMPORAL);
					}
					assignedResources.add(resource);
				}
			}
		} finally {
			taskState.restore(task);
		}
		return changed;
	}

	private Set<Resource> getAssignedResources(NormalTask task) {
		Set<Resource> assignedResources = new HashSet<Resource>(task.getAssignments().size());
		for (Object existing : task.getAssignments()) {
			assignedResources.add(((Assignment) existing).getResource());
		}
		return assignedResources;
	}

	private TaskState preserveTaskState(NormalTask task, boolean batchWillTouchScheduling) {
		if (!batchWillTouchScheduling) {
			return TaskState.noop();
		}
		TaskState state = new TaskState(task.getSchedulingType(), task.isEffortDriven());
		task.setSchedulingType(SchedulingType.Kind.FIXED_DURATION);
		task.setEffortDriven(false);
		return state;
	}

	private static final class BatchUpdate {
		private Project transactionProject;
		private int transactionId;
		private ResourcePool transactionResourcePool;
		private int resourceTransactionId;
		private Collection<Assignment> queuedDetailAssignments;

		void beginIfNeeded(NormalTask task, Collection<?> tasks, ResourcePool resourcePool, AssignmentService service, boolean undo) {
			beginIfNeeded(task, tasks, resourcePool, service, undo, true);
		}

		void beginIfNeeded(NormalTask task, Collection<?> tasks, ResourcePool resourcePool, AssignmentService service,
				boolean undo, boolean captureScheduleBackup) {
			if (!undo) {
				return;
			}
			if (transactionId != 0) {
				return;
			}
			transactionProject = task.getProject();
			transactionProject.beginUndoUpdate();
			try {
				transactionId = transactionProject.fireMultipleTransaction(0, true);
			} catch (RuntimeException | Error failure) {
				transactionProject.endUndoUpdate();
				transactionProject = null;
				throw failure;
			}
			transactionResourcePool = resourcePool;
			if (transactionResourcePool != null)
				resourceTransactionId = transactionResourcePool.fireMultipleTransaction(0, true);
			if (captureScheduleBackup)
				transactionProject.getUndoController().getEditSupport().postEdit(new ScheduleBackupEdit(tasks, service));
		}

		void endIfNeeded() {
			if (transactionId == 0) {
				return;
			}
			if (transactionResourcePool != null)
				transactionResourcePool.fireMultipleTransaction(resourceTransactionId, false);
			transactionProject.fireMultipleTransaction(transactionId, false);
			if (queuedDetailAssignments != null) {
				Map<Assignment, Object> after = new LinkedHashMap<>();
				for (Assignment assignment : queuedDetailAssignments) {
					after.put(assignment, assignment.backupDetail());
				}
				postAssignmentDetailAfter(after);
			}
			transactionProject.endUndoUpdate();
		}

		void postAssignmentDetailBefore(Map<Assignment, Object> before) {
			if (transactionId != 0 && !before.isEmpty())
				transactionProject.getUndoController().getEditSupport()
					.postEdit(new AssignmentDetailStateEdit(before, Map.of()));
		}

		void postAssignmentDetailAfter(Map<Assignment, Object> after) {
			if (transactionId != 0 && !after.isEmpty())
				transactionProject.getUndoController().getEditSupport()
					.postFinalEdit(new AssignmentDetailStateEdit(Map.of(), after));
		}

		void queueAssignmentDetailAfter(Collection<Assignment> assignments) {
			queuedDetailAssignments = assignments;
		}
	}

	private static final class TaskState {
		private final int schedulingType;
		private final boolean effortDriven;

		private TaskState(int schedulingType, boolean effortDriven) {
			this.schedulingType = schedulingType;
			this.effortDriven = effortDriven;
		}

		static TaskState noop() {
			return new TaskState(-1, false);
		}

		void restore(NormalTask task) {
			if (schedulingType == -1) {
				return;
			}
			task.setSchedulingType(schedulingType);
			task.setEffortDriven(effortDriven);
		}
	}

	/** Captures the assignment graph and task state needed to compensate a failed batch. */
	private static final class AssignmentModelState {
		private final Map<NormalTask, AssignmentTaskState> tasks = new java.util.IdentityHashMap<>();
		private final Map<Assignment, Object> assignmentDetails = new java.util.IdentityHashMap<>();
		private final List<Assignment> originalAssignments = new ArrayList<>();
		private final Set<Assignment> originalAssignmentIdentities = java.util.Collections.newSetFromMap(
			new java.util.IdentityHashMap<>());

		static AssignmentModelState capture(Collection<?> taskObjects) {
			AssignmentModelState state = new AssignmentModelState();
			for (Object value : taskObjects) {
				NormalTask task = (NormalTask) value;
				state.tasks.computeIfAbsent(task, current -> new AssignmentTaskState(current.backupDetail(),
					(TaskSnapshot) current.getCurrentSnapshot()));
				for (Object assignmentObject : task.getAssignments()) {
					Assignment assignment = (Assignment) assignmentObject;
					if (state.originalAssignmentIdentities.add(assignment)) state.originalAssignments.add(assignment);
					state.assignmentDetails.putIfAbsent(assignment, assignment.backupDetail());
				}
			}
			return state;
		}

		void restore(Throwable failure) {
			Throwable rollbackFailure = null;
			for (NormalTask task : tasks.keySet()) {
				List<Assignment> current = new ArrayList<>();
				for (Object value : task.getAssignments()) current.add((Assignment) value);
				for (Assignment assignment : current) {
					if (originalAssignmentIdentities.contains(assignment)) continue;
					Resource resource = assignment.getResource();
					try { if (task.findAssignment(resource) == assignment) task.removeAssignment(assignment); }
					catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
					try { if (resource.findAssignment(task) == assignment) resource.removeAssignment(assignment); }
					catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
				}
			}
			for (Assignment assignment : originalAssignments) {
				NormalTask task = (NormalTask) assignment.getTask();
				Resource resource = assignment.getResource();
				try {
					if (task.findAssignment(resource) != assignment || resource.findAssignment(task) != assignment)
						assignment.setTaskAndResource(task, resource);
					if (task.findAssignment(resource) != assignment) task.addAssignment(assignment);
				} catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
				try { if (resource.findAssignment(task) != assignment) resource.addAssignment(assignment); }
				catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
			}
			for (Map.Entry<Assignment, Object> entry : assignmentDetails.entrySet()) {
				try { entry.getKey().restoreDetail(entry.getValue()); }
				catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
			}
			for (Map.Entry<NormalTask, AssignmentTaskState> entry : tasks.entrySet()) {
				try { entry.getKey().restoreDetail(null, entry.getValue().detail(), true, entry.getValue().snapshot()); }
				catch (RuntimeException | Error problem) { rollbackFailure = appendFailure(rollbackFailure, problem); }
			}
			if (rollbackFailure != null) appendFailure(failure, rollbackFailure);
		}
	}

	private record AssignmentTaskState(Object detail, TaskSnapshot snapshot) { }


/**
 * When importing, we don't update or recalculate duration
 * @param task
 * @param resource
 * @param units
 * @param delay
 * @param eventSource
 * @return
 */	
	public Assignment newAssignment(NormalTask task, Resource resource, double units, long delay, Object eventSource,boolean undo) {
		if (task == null || resource == null) throw new IllegalArgumentException("A task and resource are required");
		Project project = task.getProject();
		ResourcePool pool = resourcePoolOf(resource);
		return runAssignmentCommand(project, pool, List.of(task), List.of(resource), project != null && project.isInitialized() || eventSource != null, markApplied -> {
			if (!task.isAssignable()) return null;
			markApplied.run();
			Assignment assignment = newAssignment(task, resource, units, delay, eventSource, new NodeUndoInfo(undo));
			if (assignment != null && eventSource == null && project != null && project.isInitialized())
				publishSilentAssignmentRefresh(project, task, pool, resource);
			return assignment;
		}, java.util.Objects::nonNull, result -> result);
	}
	private Assignment newAssignment(NormalTask task, Resource resource, double units, long delay, Object eventSource, NodeUndoInfo undoInfo) {
		Assignment assignment = Assignment.getInstance(task, resource, units, delay);
		if (!connectInternal(assignment,eventSource,undoInfo))
			return null;
		// NormalTask.addAssignment may copy the default assignment's fields. Apply
		// the caller-provided delay after that normalization so it is not lost.
		if (delay != 0L)
			assignment.setDelay(delay);
		return assignment;
	}
	public Assignment newAssignment(NormalTask task, Resource resource, double units, long delay, Object eventSource) {
		return newAssignment(task,resource,units,delay,eventSource,true);
	}
		
	public boolean connect(Assignment assignment, Object eventSource) {
		return connect(assignment, eventSource,true);
	}
	public boolean connect(Assignment assignment, Object eventSource,boolean undo) {
		if (!connect(assignment,eventSource,new NodeUndoInfo(undo)))
			return false;
//		EditSupport undoableEditSupport=getEditSupport(assignment);
//		if (undoableEditSupport!=null&&undo){
//			undoableEditSupport.postEdit(new AssignmentCreationEdit(assignment,eventSource));
//		}
		return true;
	}
	public boolean connect(Node node, Object eventSource, boolean undo) {
		return connect((Assignment)node.getImpl(),eventSource,new NodeUndoInfo(node,undo));
	}
	public boolean connect(Assignment assignment, Object eventSource, NodeUndoInfo undo) {
		if (assignment == null) return false;
		NormalTask task = (NormalTask)assignment.getTask();
		Project project = task.getProject();
		Resource resource = assignment.getResource();
		ResourcePool pool = resourcePoolOf(resource);
		return runAssignmentCommand(project, pool, List.of(task), List.of(resource), project != null && project.isInitialized() || eventSource != null, markApplied -> {
			if (!task.isAssignable()) return false;
			markApplied.run();
			boolean connected = connectInternal(assignment, eventSource, undo);
			if (eventSource == null && project != null && project.isInitialized()) publishSilentAssignmentRefresh(project, task, pool, resource);
			return connected;
		}, Boolean.TRUE::equals, result -> result);
	}

	private boolean connectInternal(Assignment assignment, Object eventSource, NodeUndoInfo undo) {
		if (!assignment.getTask().isAssignable())
			return false;
		withNestedUndoSuppressed(undo == null || !undo.isUndo(),
			() -> ((NormalTask)assignment.getTask()).addAssignment(assignment));
		assignment.getResource().addAssignment(assignment);
		if (undo != null && undo.isUndo() && !suppressNestedUndo.get()) {
			EditSupport editSupport = getEditSupport(assignment);
			if (editSupport != null) {
				if (undo.getNode() == null) editSupport.postEdit(new AssignmentCreationEdit(assignment));
				else editSupport.postEdit(new AssignmentCreationEdit(undo.getNode()));
			}
		}
		if (eventSource != null){
			NodeUndoInfo observerInfo = new NodeUndoInfo(undo == null ? null : undo.getNode(), false);
			assignment.getDocument().getObjectEventManager().fireCreateEvent(eventSource,assignment,observerInfo);
			ResourcePool pool = resourcePoolOf(assignment.getResource());
			if (pool != null) pool.getObjectEventManager().fireCreateEvent(eventSource,assignment,observerInfo);
		}
		return true;
	}
	public void remove(Node node, Object eventSource,boolean undo) {
		remove((Assignment)node.getImpl(),true,eventSource,new NodeUndoInfo(node,undo));
	}
//	public void remove(Assignment assignment, Object eventSource) {
//		remove(assignment, eventSource,true);
//	}
	public void remove(Assignment assignment, Object eventSource, boolean undo) {
		remove(assignment,true,eventSource,new NodeUndoInfo(undo));
	}
	
	public void remove(Assignment assignment, boolean cleanTaskLink, Object eventSource, boolean undo) {
		remove(assignment,cleanTaskLink,eventSource,new NodeUndoInfo(undo));
	}

	/** Reconnects an assignment across resource pools as one task-owned publication. */
	public void rewireSharedPoolAssignment(Assignment assignment, Resource replacement, Object eventSource) {
		if (assignment == null || replacement == null)
			throw new IllegalArgumentException("An assignment and replacement resource are required");
		com.microproject.transaction.ProjectMutationState.coordinated(
			() -> rewireSharedPoolAssignmentWithinCoordinator(assignment, replacement, eventSource));
	}

	private void rewireSharedPoolAssignmentWithinCoordinator(Assignment assignment, Resource replacement,
			Object eventSource) {
		if (!(assignment.getTask() instanceof NormalTask task))
			throw new IllegalArgumentException("A shared-pool assignment requires a normal task");
		Project project = task.getProject();
		Resource source = assignment.getResource();
		if (source == replacement) return;
		ResourcePool sourcePool = resourcePoolOf(source);
		ResourcePool replacementPool = resourcePoolOf(replacement);
		if (!task.isAssignable() || task.findAssignment(source) != assignment || source.findAssignment(task) != assignment)
			throw new IllegalStateException("Shared-pool assignment is not attached to its source task and resource");
		if (replacementPool == null || replacementPool.getResourceList().stream().noneMatch(resource -> resource == replacement))
			throw new IllegalArgumentException("Replacement resource is not a member of a resource pool");
		Assignment existingDestination = replacement.findAssignment(task);
		if (existingDestination != null && existingDestination != assignment)
			throw new IllegalStateException("Replacement resource already has an assignment for this task");
		List<Project.AdditionalObjectEventManager> additionalManagers = new ArrayList<>();
		addPoolRefreshParticipant(additionalManagers, sourcePool, source);
		addPoolRefreshParticipant(additionalManagers, replacementPool, replacement);
		project.runAtomicModelCommand(markApplied -> {
			Object assignmentDetail = assignment.backupDetail();
			Object taskDetail = task.backupDetail();
			markApplied.run();
			try {
				removeInternal(assignment, true, null, new NodeUndoInfo(false));
				if (source.findAssignment(task) == assignment || task.findAssignment(source) == assignment)
					throw new IllegalStateException("Could not detach assignment from its previous resource");
				assignment.setTaskAndResource(task, replacement);
				if (!connectForSharedPoolRewire(assignment))
					throw new IllegalStateException("Could not reconnect assignment to the shared resource pool");
				if (replacement.findAssignment(task) != assignment || task.findAssignment(replacement) != assignment)
					throw new IllegalStateException("Shared-pool assignment reverse links were not updated");
			} catch (RuntimeException | Error failure) {
				try {
					if (task.findAssignment(replacement) == assignment || replacement.findAssignment(task) == assignment)
						removeInternal(assignment, true, null, new NodeUndoInfo(false));
					assignment.setTaskAndResource(task, source);
					if ((task.findAssignment(source) != assignment || source.findAssignment(task) != assignment)
							&& !connectInternal(assignment, null, new NodeUndoInfo(false)))
						throw new IllegalStateException("Could not restore assignment to its source resource");
					if (task.findAssignment(source) != assignment || source.findAssignment(task) != assignment)
						throw new IllegalStateException("Assignment source reverse links were not restored");
					task.restoreDetail(eventSource, taskDetail, true, (TaskSnapshot)task.getCurrentSnapshot());
					assignment.restoreDetail(assignmentDetail);
				} catch (RuntimeException | Error rollbackFailure) {
					failure.addSuppressed(rollbackFailure);
				}
				throw failure;
			}
			publishSharedPoolRefreshes(project, eventSource, task, additionalManagers);
			return Boolean.TRUE;
		}, Boolean.TRUE::equals, result -> result, additionalManagers);
	}

	protected boolean connectForSharedPoolRewire(Assignment assignment) {
		return connectInternal(assignment, null, new NodeUndoInfo(false));
	}

	/** Restores one assignment and its task baseline/detail after a shared-pool transaction aborts. */
	public void restoreSharedPoolAssignment(Assignment assignment, Resource originalResource,
			Object assignmentDetail, Object taskDetail) {
		restoreSharedPoolAssignments(java.util.List.of(new SharedPoolAssignmentState(assignment, originalResource,
			assignmentDetail, (NormalTask) assignment.getTask(), taskDetail)));
	}

	/** Restores a set of rewired assignments, restoring each task snapshot only once. */
	public void restoreSharedPoolAssignments(java.util.List<SharedPoolAssignmentState> states) {
		java.util.IdentityHashMap<NormalTask, Object> taskDetails = new java.util.IdentityHashMap<>();
		for (SharedPoolAssignmentState state : states) {
			Assignment assignment = state.assignment();
			NormalTask task = state.task();
			Resource resource = assignment.getResource();
			if (task.findAssignment(resource) == assignment || resource.findAssignment(task) == assignment)
				removeInternal(assignment, true, null, new NodeUndoInfo(false));
			taskDetails.putIfAbsent(task, state.taskDetail());
		}
		for (SharedPoolAssignmentState state : states) {
			state.assignment().setTaskAndResource(state.task(), state.resource());
			if (!connectInternal(state.assignment(), null, new NodeUndoInfo(false)))
				throw new IllegalStateException("Could not restore assignment after shared-pool rollback");
		}
		taskDetails.forEach((task, detail) -> task.restoreDetail(this, detail, true,
			(TaskSnapshot) task.getCurrentSnapshot()));
		for (SharedPoolAssignmentState state : states)
			state.assignment().restoreDetail(state.assignmentDetail());
	}

	public record SharedPoolAssignmentState(Assignment assignment, Resource resource, Object assignmentDetail,
		NormalTask task, Object taskDetail) { }

	private void publishSharedPoolRefreshes(Project project, Object eventSource, NormalTask task,
			List<Project.AdditionalObjectEventManager> additionalManagers) {
		project.fireUpdateEvent(eventSource, task);
		for (Project.AdditionalObjectEventManager participant : additionalManagers)
			for (Object refreshObject : participant.refreshObjects())
				participant.manager().fireUpdateEvent(eventSource, refreshObject);
	}

	private void addPoolRefreshParticipant(List<Project.AdditionalObjectEventManager> participants,
			ResourcePool pool, Resource resource) {
		if (pool == null) return;
		for (int index = 0; index < participants.size(); index++) {
			Project.AdditionalObjectEventManager participant = participants.get(index);
			if (participant.manager() != pool.getObjectEventManager()) continue;
			List<Object> refreshObjects = new ArrayList<>(participant.refreshObjects());
			if (refreshObjects.stream().noneMatch(existing -> existing == resource)) refreshObjects.add(resource);
			participants.set(index, new Project.AdditionalObjectEventManager(participant.manager(), pool,
				participant.source(), refreshObjects));
			return;
		}
		participants.add(new Project.AdditionalObjectEventManager(pool.getObjectEventManager(), pool, pool, List.of(resource)));
	}
	public void remove(Collection<?> assignments, Object eventSource,boolean undo) {
		removeCollection(assignments, eventSource, undo, true);
	}

	public void remove(Assignment assignment, boolean cleanTaskLink, Object eventSource, NodeUndoInfo undo) {
		if (assignment == null) return;
		NormalTask task = (NormalTask)assignment.getTask();
		Resource resource = assignment.getResource();
		Project project = task.getProject();
		ResourcePool pool = resourcePoolOf(resource);
		runAssignmentCommand(project, pool, List.of(task), List.of(resource), project != null && project.isInitialized() || eventSource != null, markApplied -> {
			if (task.findAssignment(resource) == null) return false;
			markApplied.run();
			removeInternal(assignment, cleanTaskLink, eventSource, undo);
			if (eventSource == null && project != null && project.isInitialized()) publishSilentAssignmentRefresh(project, task, pool, resource);
			return true;
		}, Boolean.TRUE::equals, result -> result);
	}

	private void removeInternal(Assignment assignment, boolean cleanTaskLink, Object eventSource, NodeUndoInfo undo) {
		NormalTask task = (NormalTask)assignment.getTask();
		Resource resource = assignment.getResource();
		if (task.findAssignment(resource) == null) return;
		if (cleanTaskLink)
			withNestedUndoSuppressed(undo == null || !undo.isUndo(), () -> task.removeAssignment(assignment));
		resource.removeAssignment(assignment);
		if (cleanTaskLink && undo != null && undo.isUndo() && !suppressNestedUndo.get()) {
			EditSupport editSupport = getEditSupport(assignment);
			if (editSupport != null) {
				if (undo.getNode() == null) editSupport.postEdit(new AssignmentDeletionEdit(assignment));
				else editSupport.postEdit(new AssignmentDeletionEdit(undo.getNode()));
			}
		}
		if (eventSource != null) {
			NodeUndoInfo observerInfo = new NodeUndoInfo(undo == null ? null : undo.getNode(), false);
			if (cleanTaskLink) assignment.getDocument().getObjectEventManager().fireDeleteEvent(eventSource, assignment, observerInfo);
			if (resource.getDocument() != null)
				((ResourcePool)resource.getDocument()).getObjectEventManager().fireDeleteEvent(eventSource, assignment, observerInfo);
		}
	}

	private void withNestedUndoSuppressed(boolean suppress, Runnable mutation) {
		boolean previousSuppression = suppressNestedUndo.get();
		if (suppress) suppressNestedUndo.set(true);
		try {
			mutation.run();
		} finally {
			if (previousSuppression) suppressNestedUndo.set(true);
			else suppressNestedUndo.remove();
		}
	}

	public void remove(Collection assignmentList, Object eventSource) {
		removeCollection(assignmentList, eventSource, false, false);
	}

	private void removeCollection(Collection<?> assignments, Object eventSource, boolean undo, boolean batchUndo) {
		List<Assignment> snapshot = new ArrayList<>();
		Set<Assignment> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (Object value : assignments) {
			if (!(value instanceof Assignment assignment))
				throw new IllegalArgumentException("Assignment removal requires assignments");
			if (seen.add(assignment)) snapshot.add(assignment);
		}
		if (snapshot.isEmpty()) return;
		List<AssignmentRemovalGroup> groups = new ArrayList<>();
		for (Assignment assignment : snapshot) {
			NormalTask task = requireNormalTask(assignment);
			Project project = task.getProject();
			AssignmentRemovalGroup group = groups.stream().filter(existing -> existing.project == project).findFirst().orElse(null);
			if (group == null) {
				group = new AssignmentRemovalGroup(project);
				groups.add(group);
			}
			group.assignments.add(assignment);
		}
		com.microproject.transaction.ProjectMutationState.coordinated(() -> {
			for (AssignmentRemovalGroup group : groups)
				removeCollectionGroup(group, eventSource, undo, batchUndo);
		});
	}

	private static final class AssignmentRemovalGroup {
		final Project project;
		final List<Assignment> assignments = new ArrayList<>();
		AssignmentRemovalGroup(Project project) { this.project = project; }
	}

	private void removeCollectionGroup(AssignmentRemovalGroup group, Object eventSource, boolean undo, boolean batchUndo) {
		List<Project.AdditionalObjectEventManager> participants = new ArrayList<>();
		for (Assignment assignment : group.assignments)
			addPoolRefreshParticipant(participants, resourcePoolOf(assignment.getResource()), assignment.getResource());
		boolean published = group.project != null && group.project.isInitialized() || eventSource != null;
		java.util.function.Function<Runnable, Boolean> operation = markApplied -> {
			List<Assignment> attached = group.assignments.stream().filter(assignment -> {
				NormalTask task = (NormalTask) assignment.getTask();
				return task.findAssignment(assignment.getResource()) == assignment
					&& assignment.getResource().findAssignment(task) == assignment;
			}).toList();
			if (attached.isEmpty()) return false;
			Set<ResourcePool> pools = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
			for (Assignment assignment : attached) {
				ResourcePool pool = resourcePoolOf(assignment.getResource());
				if (pool != null) pools.add(pool);
			}
			boolean undoGroup = batchUndo && undo && group.project != null;
			AssignmentModelState modelBefore = AssignmentModelState.capture(
				attached.stream().map(Assignment::getTask).toList());
			Map<Assignment, Object> assignmentDetailsBefore = new LinkedHashMap<>();
			for (Assignment assignment : attached)
				assignmentDetailsBefore.put(assignment, modelBefore.assignmentDetails.get(assignment));
			int projectTransaction = 0;
			List<ResourcePool> transactionPools = new ArrayList<>();
			List<Integer> poolTransactions = new ArrayList<>();
			boolean undoUpdateStarted = false;
			Throwable commandFailure = null;
			try {
				if (undoGroup) {
					group.project.beginUndoUpdate();
					undoUpdateStarted = true;
					projectTransaction = group.project.fireMultipleTransaction(0, true);
					for (ResourcePool pool : pools) {
						int transaction = pool.fireMultipleTransaction(0, true);
						transactionPools.add(pool);
						poolTransactions.add(transaction);
					}
					group.project.getUndoController().getEditSupport().postEdit(
						new AssignmentDetailStateEdit(assignmentDetailsBefore, Map.of()));
				}
				markApplied.run();
				for (Assignment assignment : attached)
					removeInternal(assignment, true, eventSource, new NodeUndoInfo(undo));
				if (eventSource == null && group.project != null && group.project.isInitialized()) {
					for (Assignment assignment : attached)
						publishSilentAssignmentRefresh(group.project, (NormalTask) assignment.getTask(),
							resourcePoolOf(assignment.getResource()), assignment.getResource());
				}
				return true;
			} catch (RuntimeException | Error failure) {
				commandFailure = failure;
				modelBefore.restore(failure);
				throw failure;
			} finally {
				Throwable cleanupFailure = null;
				for (int index = transactionPools.size() - 1; index >= 0; index--) {
					try { transactionPools.get(index).fireMultipleTransaction(poolTransactions.get(index), false); }
					catch (RuntimeException | Error failure) { cleanupFailure = appendFailure(cleanupFailure, failure); }
				}
				if (projectTransaction != 0) {
					try { group.project.fireMultipleTransaction(projectTransaction, false); }
					catch (RuntimeException | Error failure) { cleanupFailure = appendFailure(cleanupFailure, failure); }
				}
				if (undoUpdateStarted) {
					try { group.project.endUndoUpdate(); }
					catch (RuntimeException | Error failure) { cleanupFailure = appendFailure(cleanupFailure, failure); }
				}
				if (cleanupFailure != null) {
					if (commandFailure != null) appendFailure(commandFailure, cleanupFailure);
					else modelBefore.restore(cleanupFailure);
					if (commandFailure == null && cleanupFailure instanceof Error error) throw error;
					else if (commandFailure == null) throw (RuntimeException) cleanupFailure;
				}
			}
		};
		if (group.project == null || !published) {
			operation.apply(() -> {});
		} else {
			group.project.runAtomicModelCommand(operation::apply, Boolean.TRUE::equals, result -> result, participants);
		}
	}

	private NormalTask requireNormalTask(Assignment assignment) {
		if (!(assignment.getTask() instanceof NormalTask task))
			throw new IllegalArgumentException("Assignments require a normal task");
		return task;
	}

	private static Throwable appendFailure(Throwable current, Throwable next) {
		if (current == null) return next;
		if (current != next) current.addSuppressed(next);
		return current;
	}

	private <T> T runAssignmentCommand(Project project, ResourcePool pool, Collection<? extends NormalTask> affectedTasks,
			Collection<?> refreshObjects, boolean published,
			Function<Runnable, T> operation, Predicate<? super T> changed, UnaryOperator<T> resultWhenFactsExist) {
		List<Project.AdditionalObjectEventManager> participants = pool == null ? List.of()
			: List.of(new Project.AdditionalObjectEventManager(pool.getObjectEventManager(), pool, pool,
				List.copyOf(refreshObjects)));
		java.util.function.Function<Runnable, T> compensatingOperation = markApplied -> {
			AssignmentModelState before = AssignmentModelState.capture(affectedTasks);
			try {
				return operation.apply(markApplied);
			} catch (RuntimeException | Error failure) {
				before.restore(failure);
				throw failure;
			}
		};
		if (project == null || !published)
			return compensatingOperation.apply(() -> {});
		return project.runAtomicModelCommand(compensatingOperation::apply, changed, resultWhenFactsExist, participants);
	}

	private void publishSilentAssignmentRefresh(Project project, NormalTask task, ResourcePool pool, Resource resource) {
		if (project != null) project.fireUpdateEvent(null, task);
		if (pool != null) pool.getObjectEventManager().fireUpdateEvent(null, resource);
	}
	
	//fix
	public void remove(Collection assignmentList, Collection toRemove) {
		for (Object assignment : assignmentList)
			toRemove.add(assignment);
	}
	
	
	//undo
	public EditSupport getEditSupport(Assignment assignment) {
		if (assignment.getTask()==null) return null;
		else return assignment.getTask().getProject().getUndoController().getEditSupport();
	}
	
}	
	
	
