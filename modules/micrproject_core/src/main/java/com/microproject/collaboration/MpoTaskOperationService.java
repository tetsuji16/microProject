/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.collaboration;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.resource.Resource;

/** Applies validated mpo task-update operations deterministically to a snapshot. */
public final class MpoTaskOperationService {
	public void apply(Project project, Collection<OperationLog.Operation> operations) throws IOException {
		if (project == null) throw new IOException("Missing project for mpo operations");
		for (OperationLog.Operation operation : new OperationLog().merge(operations).ready()) {
			switch (operation.kind()) {
				case "task.create" -> applyCreate(project, operation.payload());
				case "task.update" -> applyUpdate(project, operation.payload());
				case "task.delete" -> applyDelete(project, operation.payload());
				case "task.move" -> applyMove(project, operation.payload());
				case "dependency.add" -> applyDependencyAdd(project, operation.payload());
				case "dependency.delete" -> applyDependencyDelete(project, operation.payload());
				case "assignment.add" -> applyAssignmentAdd(project, operation.payload());
				case "assignment.delete" -> applyAssignmentDelete(project, operation.payload());
				default -> throw new IOException("Unsupported mpo operation: " + operation.kind());
			}
		}
	}

	private static void applyDependencyAdd(Project project, Map<String, Object> payload) throws IOException {
		Task predecessor = project.findByUniqueId(number(payload.get("predecessorLegacyUniqueId"), "predecessorLegacyUniqueId"));
		Task successor = project.findByUniqueId(number(payload.get("successorLegacyUniqueId"), "successorLegacyUniqueId"));
		if (predecessor == null || successor == null) throw new IOException("dependency.add references an unknown task");
		for (java.util.Iterator<?> it = predecessor.getSuccessorList().iterator(); it.hasNext();) {
			Dependency existing = (Dependency) it.next();
			if (existing.getSuccessor() == successor && existing.getDependencyType() == intNumber(payload.get("dependencyType"), "dependencyType") && existing.getLag() == longNumber(payload.get("lag"), "lag")) return;
		}
		try { DependencyService.getInstance().newDependency(predecessor, successor, intNumber(payload.get("dependencyType"), "dependencyType"), longNumber(payload.get("lag"), "lag"), null); }
		catch (Exception exception) { throw new IOException("dependency.add is invalid", exception); }
	}

	private static void applyDependencyDelete(Project project, Map<String, Object> payload) throws IOException {
		Task predecessor = project.findByUniqueId(number(payload.get("predecessorLegacyUniqueId"), "predecessorLegacyUniqueId"));
		Task successor = project.findByUniqueId(number(payload.get("successorLegacyUniqueId"), "successorLegacyUniqueId"));
		if (predecessor == null || successor == null) return;
		int type = intNumber(payload.get("dependencyType"), "dependencyType"); long lag = longNumber(payload.get("lag"), "lag");
		for (java.util.Iterator<?> it = predecessor.getSuccessorList().iterator(); it.hasNext();) { Dependency dependency = (Dependency) it.next(); if (dependency.getSuccessor() == successor && dependency.getDependencyType() == type && dependency.getLag() == lag) { DependencyService.getInstance().remove(dependency, null, false); return; } }
	}

	private static void applyAssignmentAdd(Project project, Map<String, Object> payload) throws IOException {
		Task rawTask = project.findByUniqueId(number(payload.get("taskLegacyUniqueId"), "taskLegacyUniqueId"));
		if (!(rawTask instanceof NormalTask)) throw new IOException("assignment.add references a non-normal task");
		long wantedId = number(payload.get("resourceUniqueId"), "resourceUniqueId");
		Resource resource = findResource(project, wantedId);
		if (resource == null && project.getResourcePool() != null) {
			resource = project.getResourcePool().newResourceInstance();
			if (wantedId >= 1L) {
				try {
					project.getResourcePool().setResourceUniqueId(resource, wantedId);
				} catch (IllegalArgumentException taken) {
					// The id is already used by another resource in this pool: keep
					// the freshly created resource's own id instead of failing.
				}
			}
			Object name = payload.get("resourceName");
			if (name instanceof String) resource.setName((String) name);
		}
		if (resource == null) throw new IOException("assignment.add references an unknown resource");
		NormalTask task = (NormalTask) rawTask;
		for (java.util.Iterator<?> it = task.getAssignments().iterator(); it.hasNext();) { Assignment existing = (Assignment) it.next(); if (existing.getResource() == resource) return; }
		AssignmentService.getInstance().newAssignment(task, resource, decimal(payload.get("units"), "units"), longNumber(payload.get("delay"), "delay"), null, false);
	}

	private static void applyAssignmentDelete(Project project, Map<String, Object> payload) throws IOException {
		Task rawTask = project.findByUniqueId(number(payload.get("taskLegacyUniqueId"), "taskLegacyUniqueId"));
		if (!(rawTask instanceof NormalTask)) return;
		Resource resource = findResource(project, number(payload.get("resourceUniqueId"), "resourceUniqueId"));
		if (resource == null) return;
		for (java.util.Iterator<?> it = ((NormalTask) rawTask).getAssignments().iterator(); it.hasNext();) { Assignment assignment = (Assignment) it.next(); if (assignment.getResource() == resource) { AssignmentService.getInstance().remove(assignment, null, false); return; } }
	}

	private static Resource findResource(Project project, long uniqueId) {
		if (project.getResourcePool() == null) return null;
		for (Resource resource : project.getResourcePool().getResourceList()) if (resource.getUniqueId() == uniqueId) return resource;
		return null;
	}

	private static void applyMove(Project project, Map<String, Object> payload) throws IOException {
		Task task = project.findByUniqueId(legacyUniqueId(payload));
		if (task == null) throw new IOException("task.move references an unknown task");
		Object parentId = payload.get("parentLegacyUniqueId");
		Task parent = parentId == null ? null : project.findByUniqueId(number(parentId, "parentLegacyUniqueId"));
		if (parentId != null && parent == null) throw new IOException("task.move references an unknown parent");
		com.microproject.grouping.core.Node childNode = project.getTaskModel().search(task);
		com.microproject.grouping.core.Node parentNode = parent == null ? null : project.getTaskModel().search(parent);
		if (childNode == null || (parent != null && parentNode == null)) throw new IOException("task.move cannot resolve task hierarchy");
		project.setLocalParent(childNode, parentNode);
	}

	private static void applyDelete(Project project, Map<String, Object> payload) throws IOException {
		Task task = project.findByUniqueId(legacyUniqueId(payload));
		if (task != null) project.removeExternal(task);
	}

	private static void applyCreate(Project project, Map<String, Object> payload) throws IOException {
		long uniqueId = legacyUniqueId(payload);
		Task existing = project.findByUniqueId(uniqueId);
		if (existing != null) { applyUpdate(existing, payload); return; }
		Task created = (Task) project.createLocalTaskNode(null).getImpl();
		created.setUniqueId(uniqueId);
		applyUpdate(created, payload);
	}

	private static void applyUpdate(Project project, Map<String, Object> payload) throws IOException {
		Task task = project.findByUniqueId(legacyUniqueId(payload));
		if (task == null) throw new IOException("task.update references an unknown task");
		applyUpdate(task, payload);
	}

	private static long legacyUniqueId(Map<String, Object> payload) throws IOException {
		Object rawId = payload.get("legacyUniqueId");
		return number(rawId, "legacyUniqueId");
	}

	private static long number(Object rawId, String name) throws IOException {
		if (!(rawId instanceof Number)) throw new IOException("task operation requires " + name);
		long id = ((Number) rawId).longValue();
		// Native POD projects may use zero or negative generated IDs.  The only
		// invalid representation here is a missing/non-numeric value, checked above.
		return id;
	}
	private static long longNumber(Object raw, String name) throws IOException { if (!(raw instanceof Number)) throw new IOException("operation requires " + name); return ((Number) raw).longValue(); }
	private static int intNumber(Object raw, String name) throws IOException { long value = longNumber(raw, name); if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw new IOException("Invalid " + name); return (int) value; }
	private static double decimal(Object raw, String name) throws IOException { if (!(raw instanceof Number) || !Double.isFinite(((Number) raw).doubleValue()) || ((Number) raw).doubleValue() < 0D) throw new IOException("Invalid " + name); return ((Number) raw).doubleValue(); }

	private static void applyUpdate(Task task, Map<String, Object> payload) throws IOException {
		Object name = payload.get("name");
		if (name != null) { if (!(name instanceof String)) throw new IOException("Invalid task name"); task.setName((String) name); }
		Object notes = payload.get("notes");
		if (notes != null) { if (!(notes instanceof String)) throw new IOException("Invalid task notes"); task.setNotes((String) notes); }
		Object complete = payload.get("percentComplete");
		if (complete != null) { if (!(complete instanceof Number)) throw new IOException("Invalid task percentComplete"); double value = ((Number) complete).doubleValue(); if (!Double.isFinite(value) || value < 0D || value > 1D) throw new IOException("Invalid task percentComplete"); task.setPercentComplete(value); }
	}
}
