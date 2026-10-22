/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.resource;

import java.util.ArrayList;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.Project;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.util.FilePathUtils;

/**
 * Connects a sharer project to a separately saved resource-pool project.
 * Assignments are rewired to one resource identity so usage and over-allocation
 * are calculated across every connected sharer.
 */
public final class SharedResourcePoolService {
	public enum ConflictPolicy {
		POOL_TAKES_PRECEDENCE,
		SHARER_TAKES_PRECEDENCE
	}

	private static final SharedResourcePoolService INSTANCE = new SharedResourcePoolService();
	private static volatile Runnable afterFirstRewireForTest;
	private static final ThreadLocal<ResolutionTransaction> RESOLUTION_TRANSACTION = new ThreadLocal<>();

	public static SharedResourcePoolService getInstance() {
		return INSTANCE;
	}

	private SharedResourcePoolService() {
	}

	public void share(Project sharer, Project poolProject, ConflictPolicy policy) {
		ProjectMutationState.coordinated(() -> shareCoordinated(sharer, poolProject, policy));
	}

	private void shareCoordinated(Project sharer, Project poolProject, ConflictPolicy policy) {
		if (sharer == null || poolProject == null || sharer == poolProject)
			throw new IllegalArgumentException("A distinct sharer and resource-pool project are required");
		ResourcePool pool = poolProject.getResourcePool();
		ResourcePool sharerPool = sharer.getResourcePool();
		ConflictPolicy effectivePolicy = policy == null ? ConflictPolicy.POOL_TAKES_PRECEDENCE : policy;
		if (pool == null || sharerPool == null)
			throw new IllegalArgumentException("Both projects must have a resource pool");
		if (canonicalFileName(poolProject.getFileName()) == null)
			throw new IllegalStateException("Save the resource-pool project before sharing resources");
		ArrayList<Project.AdditionalObjectEventManager> participants = new ArrayList<>();
		addEventParticipant(participants, sharerPool);
		addEventParticipant(participants, pool);
		ProjectMutationState.Checkpoint coordinatorCheckpoint = ProjectMutationState.checkpoint();
		SharedPoolSnapshot snapshot = SharedPoolSnapshot.capture(sharer, poolProject, sharerPool, pool);
		ResolutionTransaction transaction = RESOLUTION_TRANSACTION.get();
		poolProject.runAtomicModelCommand(poolMarkApplied -> {
			poolMarkApplied.run();
			return sharer.runAtomicModelCommand(markApplied -> {
				markApplied.run();
				try {
					if (pool != sharerPool) mergeResources(sharerPool, pool, effectivePolicy);
					if (sharerPool != pool) sharerPool.removeProject(sharer);
					sharer.setResourcePool(pool);
					if (!containsIdentity(pool.getProjects(), poolProject)) pool.addProject(poolProject);
					if (!containsIdentity(pool.getProjects(), sharer)) pool.addProject(sharer);
					sharer.setSharedResourcePoolFile(canonicalFileName(poolProject.getFileName()));
					sharer.setSharedResourcePoolProjectId(poolProject.getUniqueId());
					sharer.setResourcePoolTakesPrecedence(effectivePolicy != ConflictPolicy.SHARER_TAKES_PRECEDENCE);
					sharer.setSharedResourcePoolUnresolved(false);
					return Boolean.TRUE;
				} catch (RuntimeException | Error failure) {
					try {
						snapshot.restore();
						ProjectMutationState.rollback(coordinatorCheckpoint);
					} catch (RuntimeException | Error rollbackFailure) { failure.addSuppressed(rollbackFailure); }
					throw failure;
				}
			}, Boolean.TRUE::equals, result -> result, participants);
		}, Boolean.TRUE::equals, result -> result, participants);
		if (transaction != null) transaction.snapshots.add(snapshot);
	}

	/** Groups successful pool resolutions so an enclosing replacement can undo them as a unit. */
	public ResolutionScope beginResolutionTransaction() {
		if (RESOLUTION_TRANSACTION.get() != null) throw new IllegalStateException("A shared-pool resolution transaction is already active");
		ResolutionTransaction transaction = new ResolutionTransaction();
		RESOLUTION_TRANSACTION.set(transaction);
		return new ResolutionScope(transaction);
	}
	private static final class ResolutionTransaction { final java.util.ArrayList<SharedPoolSnapshot> snapshots = new java.util.ArrayList<>(); }
	public static final class ResolutionScope implements AutoCloseable {
		private final ResolutionTransaction transaction;
		private boolean complete;
		private ResolutionScope(ResolutionTransaction transaction) { this.transaction = transaction; }
		public void commit() { complete = true; }
		public void rollback() {
			for (int i = transaction.snapshots.size() - 1; i >= 0; i--) transaction.snapshots.get(i).restore();
			complete = true;
		}
		@Override public void close() {
			if (RESOLUTION_TRANSACTION.get() == transaction) RESOLUTION_TRANSACTION.remove();
			if (!complete) rollback();
		}
	}

	static void failAfterFirstRewireForTest(Runnable failure) { afterFirstRewireForTest = failure; }
	private static void addEventParticipant(ArrayList<Project.AdditionalObjectEventManager> participants, ResourcePool pool) {
		if (pool != null) participants.add(new Project.AdditionalObjectEventManager(pool.getObjectEventManager(), pool,
			pool, pool.getResourceList()));
	}

	/** Returns whether the project is the persisted resource-pool target of a sharer. */
	public boolean isPoolReference(Project sharer, Project poolProject) {
		return sharer != null && poolProject != null
			&& java.util.Objects.equals(canonicalFileName(sharer.getSharedResourcePoolFile()), canonicalFileName(poolProject.getFileName()));
	}

	public boolean resolve(Project candidate, Iterable<Project> openProjects) {
		if (candidate == null || candidate.getSharedResourcePoolFile() == null || openProjects == null)
			return false;
		String expected = canonicalFileName(candidate.getSharedResourcePoolFile());
		if (expected == null) {
			candidate.setSharedResourcePoolUnresolved(true);
			return false;
		}
		for (Project project : openProjects) {
			if (project != null && project != candidate && (expected.equals(canonicalFileName(project.getFileName()))
					|| candidate.getSharedResourcePoolProjectId() != 0L
							&& candidate.getSharedResourcePoolProjectId() == project.getUniqueId())) {
				share(candidate, project, candidate.isResourcePoolTakesPrecedence()
						? ConflictPolicy.POOL_TAKES_PRECEDENCE : ConflictPolicy.SHARER_TAKES_PRECEDENCE);
				return true;
			}
		}
		candidate.setSharedResourcePoolUnresolved(true);
		return false;
	}

	private void mergeResources(ResourcePool sharerPool, ResourcePool pool, ConflictPolicy policy) {
		for (Resource sharerResource : new ArrayList<Resource>(sharerPool.getResourceList())) {
			Resource poolResource = findById(pool, sharerResource.getUniqueId());
			// A display name is only a legacy recovery key.  Two resources with
			// distinct persisted identities must remain distinct even when users
			// gave them the same name; otherwise their assignments are silently
			// merged and pool over-allocation becomes incorrect.
			if (poolResource == null && sharerResource.getUniqueId() <= 0L)
				poolResource = findByName(pool, sharerResource.getName());
			if (poolResource == null) {
				moveResource(sharerPool, pool, sharerResource);
				continue;
			}
			if (policy == ConflictPolicy.SHARER_TAKES_PRECEDENCE) {
				rewireAssignments(poolResource, sharerResource);
				pool.remove(poolResource);
				moveResource(sharerPool, pool, sharerResource);
			} else {
				rewireAssignments(sharerResource, poolResource);
				sharerPool.remove(sharerResource);
			}
		}
	}

	private Resource findByName(ResourcePool pool, String name) {
		if (name == null)
			return null;
		Resource match = null;
		for (Resource resource : pool.getResourceList()) {
			if (!name.equals(resource.getName()))
				continue;
			// A legacy display-name fallback is safe only when it identifies one
			// resource.  Picking the first of several homonyms would silently
			// transfer assignments to the wrong person.
			if (match != null)
				return null;
			match = resource;
		}
		return match;
	}

	private Resource findById(ResourcePool pool, long uniqueId) {
		if (uniqueId <= 0L)
			return null;
		for (Resource resource : pool.getResourceList())
			if (resource.getUniqueId() == uniqueId)
				return resource;
		return null;
	}

	private void moveResource(ResourcePool source, ResourcePool destination, Resource resource) {
		source.remove(resource);
		if (resource instanceof ResourceImpl implementation)
			implementation.getGlobalResource().setResourcePool(destination);
		destination.add(resource);
	}

	private void rewireAssignments(Resource source, Resource destination) {
		for (Object candidate : new ArrayList<Object>(source.getAssignments())) {
			Assignment assignment = (Assignment) candidate;
			AssignmentService.getInstance().rewireSharedPoolAssignment(assignment, destination, this);
			Runnable failure = afterFirstRewireForTest;
			if (failure != null) { afterFirstRewireForTest = null; failure.run(); }
		}
	}

	private record SharedPoolSnapshot(Project sharer, Project poolProject, Project.SharedPoolState sharerState,
		Project.SharedPoolState poolProjectState, ResourcePool sharerPool, ResourcePool.PoolState sharerPoolState,
		ResourcePool pool, ResourcePool.PoolState poolState,
		java.util.List<AssignmentService.SharedPoolAssignmentState> assignments) {
		static SharedPoolSnapshot capture(Project sharer, Project poolProject, ResourcePool sharerPool, ResourcePool pool) {
			java.util.IdentityHashMap<com.microproject.pm.task.NormalTask, Object> tasks = new java.util.IdentityHashMap<>();
			java.util.ArrayList<AssignmentService.SharedPoolAssignmentState> assignments = new java.util.ArrayList<>();
			java.util.IdentityHashMap<Assignment, Boolean> seen = new java.util.IdentityHashMap<>();
			for (ResourcePool sourcePool : pool == sharerPool ? java.util.List.of(sharerPool) : java.util.List.of(sharerPool, pool))
			for (Resource resource : sourcePool.getResourceList())
				for (Object value : resource.getAssignments()) {
					Assignment assignment = (Assignment)value;
					if (seen.put(assignment, Boolean.TRUE) != null
							|| !(assignment.getTask() instanceof com.microproject.pm.task.NormalTask task)) continue;
					tasks.computeIfAbsent(task, ignored -> task.backupDetail());
					assignments.add(new AssignmentService.SharedPoolAssignmentState(assignment, resource,
						assignment.backupDetail(), task, tasks.get(task)));
				}
			return new SharedPoolSnapshot(sharer, poolProject, sharer.captureSharedPoolState(), poolProject.captureSharedPoolState(),
				sharerPool, sharerPool.captureSharedPoolState(), pool, pool.captureSharedPoolState(), assignments);
		}
		void restore() {
			sharerPool.restoreSharedPoolState(sharerPoolState);
			if (pool != sharerPool) pool.restoreSharedPoolState(poolState);
			sharer.restoreSharedPoolState(sharerState);
			poolProject.restoreSharedPoolState(poolProjectState);
			AssignmentService.getInstance().restoreSharedPoolAssignments(assignments);
		}
	}

	private String canonicalFileName(String fileName) {
		String canonical = FilePathUtils.canonicalPath(fileName);
		return canonical == null || canonical.isBlank() ? null : canonical;
	}

	private static boolean containsIdentity(Iterable<Project> projects, Project expected) {
		for (Project project : projects)
			if (project == expected)
				return true;
		return false;
	}
}
