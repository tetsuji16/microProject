/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.task.Project;

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

	public static SharedResourcePoolService getInstance() {
		return INSTANCE;
	}

	private SharedResourcePoolService() {
	}

	public void share(Project sharer, Project poolProject, ConflictPolicy policy) {
		if (sharer == null || poolProject == null || sharer == poolProject)
			throw new IllegalArgumentException("A distinct sharer and resource-pool project are required");
		ResourcePool pool = poolProject.getResourcePool();
		ResourcePool sharerPool = sharer.getResourcePool();
		ConflictPolicy effectivePolicy = policy == null ? ConflictPolicy.POOL_TAKES_PRECEDENCE : policy;
		if (pool == null || sharerPool == null)
			throw new IllegalArgumentException("Both projects must have a resource pool");
		if (canonicalFileName(poolProject.getFileName()) == null)
			throw new IllegalStateException("Save the resource-pool project before sharing resources");
		if (pool != sharerPool)
			mergeResources(sharerPool, pool, effectivePolicy);
		if (sharerPool != pool)
			sharerPool.removeProject(sharer);
		sharer.setResourcePool(pool);
		// The owner also contributes assignments.  Keep it in the same registry
		// as every client so pool views can aggregate the complete workload.
		if (!containsIdentity(pool.getProjects(), poolProject))
			pool.addProject(poolProject);
		if (!containsIdentity(pool.getProjects(), sharer))
			pool.addProject(sharer);
		sharer.setSharedResourcePoolFile(canonicalFileName(poolProject.getFileName()));
		sharer.setSharedResourcePoolProjectId(poolProject.getUniqueId());
		sharer.setResourcePoolTakesPrecedence(effectivePolicy != ConflictPolicy.SHARER_TAKES_PRECEDENCE);
		sharer.setSharedResourcePoolUnresolved(false);
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
					|| candidate.getSharedResourcePoolProjectId() > 0L
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
		for (Iterator<?> iterator = new ArrayList<Object>(source.getAssignments()).iterator(); iterator.hasNext();) {
			Assignment assignment = (Assignment) iterator.next();
			AssignmentService.getInstance().remove(assignment, null, false);
			assignment.setTaskAndResource(assignment.getTask(), destination);
			AssignmentService.getInstance().connect(assignment, null, false);
		}
	}

	private String canonicalFileName(String fileName) {
		if (fileName == null || fileName.isBlank())
			return null;
		try {
			return new File(fileName).getCanonicalPath();
		} catch (IOException e) {
			return new File(fileName).getAbsolutePath();
		}
	}

	private static boolean containsIdentity(Iterable<Project> projects, Project expected) {
		for (Project project : projects)
			if (project == expected)
				return true;
		return false;
	}
}
