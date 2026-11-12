/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.exchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.microproject.collaboration.OperationLog;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.TaskSnapshot;

final class MpoOperationState {
	byte[] json; String documentId; String actorId = java.util.UUID.randomUUID().toString();
	final java.util.List<OperationLog.Operation> operations = new java.util.ArrayList<OperationLog.Operation>();
	final java.util.Set<String> appliedOperationIds = new java.util.LinkedHashSet<String>();
	// History present when the document was opened: this session's edits follow it, so it cannot conflict with them.
	final java.util.Set<String> loadedOperationIds = new java.util.LinkedHashSet<String>();
	final java.util.Map<Long, TaskSnapshot> snapshots = new java.util.LinkedHashMap<Long, TaskSnapshot>();
	final java.util.Map<Long, Long> parentSnapshots = new java.util.LinkedHashMap<Long, Long>();
	final java.util.Set<String> dependencySnapshots = new java.util.LinkedHashSet<String>();
	final java.util.Map<String, String> assignmentSnapshots = new java.util.LinkedHashMap<String, String>();
	void capture(Project project) {
		snapshots.clear();
		parentSnapshots.clear();
		dependencySnapshots.clear();
		assignmentSnapshots.clear();
		for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) {
			Task task = (Task) it.next();
			Long id = Long.valueOf(task.getUniqueId());
			snapshots.put(id, signature(task));
			parentSnapshots.put(id, parentId(task));
			for (java.util.Iterator<?> links = task.getSuccessorList().iterator(); links.hasNext();) {
				Dependency dependency = (Dependency) links.next();
				dependencySnapshots.add(dependencyKey(dependency));
			}
			if (task instanceof NormalTask) {
				for (java.util.Iterator<?> assignments = ((NormalTask) task).getAssignments().iterator(); assignments.hasNext();) {
					Assignment assignment = (Assignment) assignments.next();
					if (!assignment.isDefault())
						assignmentSnapshots.put(assignmentKey(assignment), assignmentValue(assignment));
				}
			}
		}
	}
	void remapTaskIds(java.util.Map<Long, Long> identities) throws IOException {
		java.util.List<OperationLog.Operation> normalized = MpoFileImporter.remapTaskOperations(operations, identities);
		MpoFileImporter.ensureOperationCount(normalized);
		operations.clear(); operations.addAll(normalized);
		json = new OperationLog().writeJsonl(documentId, operations, appliedOperationIds);
	}
	void appendChanges(Project project) throws IOException {
		java.util.Set<String> existingOperationIds = operations.stream().map(OperationLog.Operation::id)
				.collect(java.util.stream.Collectors.toSet());
		boolean changed = false; long sequence = operations.stream().filter(value -> actorId.equals(value.actorId())).mapToLong(OperationLog.Operation::sequence).max().orElse(0L);
		java.util.Set<Long> seen = new java.util.LinkedHashSet<Long>();
		for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) {
			Task task = (Task) it.next();
			Long id = Long.valueOf(task.getUniqueId());
			seen.add(id);
			TaskSnapshot current = signature(task);
			TaskSnapshot previous = snapshots.get(id);
			Long currentParentId = parentId(task);
			if (previous == null) {
				java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
				payload.put("legacyUniqueId", id);
				payload.put("name", task.getName());
				payload.put("notes", task.getNotes());
				payload.put("percentComplete", Double.valueOf(task.getPercentComplete()));
				if (currentParentId != null)
					payload.put("parentLegacyUniqueId", currentParentId);
				addTaskOperation("task.create", id.longValue(), payload, ++sequence);
				changed = true;
			} else {
				if (!java.util.Objects.equals(currentParentId, parentSnapshots.get(id))) {
					java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
					payload.put("legacyUniqueId", id);
					if (currentParentId != null)
						payload.put("parentLegacyUniqueId", currentParentId);
					addTaskOperation("task.move", id.longValue(), payload, ++sequence);
					changed = true;
				}
				java.util.Map<String, Object> payload = new java.util.LinkedHashMap<String, Object>();
				payload.put("legacyUniqueId", id);
				if (!java.util.Objects.equals(current.name(), previous.name()))
					payload.put("name", current.name());
				if (!java.util.Objects.equals(current.notes(), previous.notes()))
					payload.put("notes", current.notes());
				if (Double.compare(current.percentComplete(), previous.percentComplete()) != 0)
					payload.put("percentComplete", Double.valueOf(current.percentComplete()));
				if (payload.size() > 1) {
					addTaskOperation("task.update", id.longValue(), payload, ++sequence);
					changed = true;
				}
			}
		}
		for (Long id : snapshots.keySet()) if (!seen.contains(id)) { operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, ++sequence, java.util.Set.of(), "task.delete", java.util.UUID.nameUUIDFromBytes((documentId + ":" + id).getBytes(StandardCharsets.UTF_8)).toString(), java.util.Map.of("legacyUniqueId", id))); changed = true; }
		java.util.Set<String> dependencies = new java.util.LinkedHashSet<String>(); java.util.Map<String, String> assignments = new java.util.LinkedHashMap<String, String>();
		for (java.util.Iterator<?> it = project.getTaskOutlineIterator(); it.hasNext();) { Task task = (Task) it.next(); for (java.util.Iterator<?> links = task.getSuccessorList().iterator(); links.hasNext();) dependencies.add(dependencyKey((Dependency) links.next())); if (task instanceof NormalTask) for (java.util.Iterator<?> values = ((NormalTask) task).getAssignments().iterator(); values.hasNext();) { Assignment assignment = (Assignment) values.next(); if (!assignment.isDefault()) assignments.put(assignmentKey(assignment), assignmentValue(assignment)); } }
		for (String key : dependencies) if (!dependencySnapshots.contains(key)) { String[] parts = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("predecessorLegacyUniqueId", Long.valueOf(parts[0])); payload.put("successorLegacyUniqueId", Long.valueOf(parts[1])); payload.put("dependencyType", Integer.valueOf(parts[2])); payload.put("lag", Long.valueOf(parts[3])); addOperation("dependency.add", dependencyEntityKey(key), payload, ++sequence); changed = true; }
		for (String key : dependencySnapshots) if (!dependencies.contains(key)) { String[] parts = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("predecessorLegacyUniqueId", Long.valueOf(parts[0])); payload.put("successorLegacyUniqueId", Long.valueOf(parts[1])); payload.put("dependencyType", Integer.valueOf(parts[2])); payload.put("lag", Long.valueOf(parts[3])); addOperation("dependency.delete", dependencyEntityKey(key), payload, ++sequence); changed = true; }
		for (java.util.Map.Entry<String,String> entry : assignments.entrySet()) if (!assignmentSnapshots.containsKey(entry.getKey())) { String[] ids = entry.getKey().split(":", -1); String[] values = entry.getValue().split(":", -1); Assignment assignment = findAssignment(project, Long.parseLong(ids[0]), Long.parseLong(ids[1])); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("taskLegacyUniqueId", Long.valueOf(ids[0])); payload.put("resourceUniqueId", Long.valueOf(ids[1])); payload.put("resourceName", assignment.getResource().getName()); payload.put("units", Double.valueOf(values[0])); payload.put("delay", Long.valueOf(values[1])); addOperation("assignment.add", entry.getKey(), payload, ++sequence); changed = true; }
		for (String key : assignmentSnapshots.keySet()) if (!assignments.containsKey(key)) { String[] ids = key.split(":", -1); java.util.Map<String,Object> payload = new java.util.LinkedHashMap<String,Object>(); payload.put("taskLegacyUniqueId", Long.valueOf(ids[0])); payload.put("resourceUniqueId", Long.valueOf(ids[1])); addOperation("assignment.delete", key, payload, ++sequence); changed = true; }
		if (changed) {
			MpoFileImporter.ensureOperationCount(operations);
			for (OperationLog.Operation operation : operations)
				if (!existingOperationIds.contains(operation.id())) appliedOperationIds.add(operation.id());
			json = new OperationLog().writeJsonl(documentId, operations, appliedOperationIds);
			capture(project);
		}
	}
	void addOperation(String kind, String key, java.util.Map<String,Object> payload, long sequence) { operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, sequence, java.util.Set.of(), kind, java.util.UUID.nameUUIDFromBytes((documentId + ":" + kind + ":" + key).getBytes(StandardCharsets.UTF_8)).toString(), payload)); }
	MpoOperationState copy() {
		MpoOperationState copy = new MpoOperationState();
		copy.json = json == null ? null : json.clone();
		copy.documentId = documentId;
		copy.actorId = actorId;
		copy.operations.addAll(operations);
		copy.appliedOperationIds.addAll(appliedOperationIds);
		copy.loadedOperationIds.addAll(loadedOperationIds);
		copy.snapshots.putAll(snapshots);
		copy.parentSnapshots.putAll(parentSnapshots);
		copy.dependencySnapshots.addAll(dependencySnapshots);
		copy.assignmentSnapshots.putAll(assignmentSnapshots);
		return copy;
	}
	void commitFrom(MpoOperationState source) {
		json = source.json == null ? null : source.json.clone();
		documentId = source.documentId;
		actorId = source.actorId;
		operations.clear(); operations.addAll(source.operations);
		appliedOperationIds.clear(); appliedOperationIds.addAll(source.appliedOperationIds);
		loadedOperationIds.clear(); loadedOperationIds.addAll(source.loadedOperationIds);
		snapshots.clear(); snapshots.putAll(source.snapshots);
		parentSnapshots.clear(); parentSnapshots.putAll(source.parentSnapshots);
		dependencySnapshots.clear(); dependencySnapshots.addAll(source.dependencySnapshots);
		assignmentSnapshots.clear(); assignmentSnapshots.putAll(source.assignmentSnapshots);
	}
	static TaskSnapshot signature(Task task) {
		return new TaskSnapshot(task.getName(), task.getNotes(), task.getPercentComplete());
	}
	void addTaskOperation(String kind, long taskId, java.util.Map<String, Object> payload, long sequence) {
		String entityId = java.util.UUID.nameUUIDFromBytes((documentId + ":" + taskId).getBytes(StandardCharsets.UTF_8)).toString();
		operations.add(new OperationLog.Operation(java.util.UUID.randomUUID().toString(), actorId, sequence,
				java.util.Set.of(), kind, entityId, payload));
	}
	static Long parentId(com.microproject.pm.task.Task task) { return task.getWbsParentTask() == null ? null : Long.valueOf(task.getWbsParentTask().getUniqueId()); }
	static String dependencyKey(Dependency dependency) { return dependency.getPredecessorId() + ":" + dependency.getSuccessorId() + ":" + dependency.getDependencyType() + ":" + dependency.getLag(); }
	// Lag is mutable relationship data, not part of the relationship identity.
	// Keeping it out of entityId lets concurrent lag changes conflict instead of
	// merging as two unrelated dependency records.
	static String dependencyEntityKey(String key) {
		String[] parts = key.split(":", -1);
		if (parts.length != 4) throw new IllegalArgumentException("Invalid dependency key");
		return parts[0] + ":" + parts[1] + ":" + parts[2];
	}
	static String assignmentKey(Assignment assignment) { return assignment.getTask().getUniqueId() + ":" + assignment.getResource().getUniqueId(); }
	static String assignmentValue(Assignment assignment) { return assignment.getUnits() + ":" + assignment.getDelay(); }
	static Assignment findAssignment(Project project, long taskId, long resourceId) { Task task = project.findByUniqueId(taskId); if (task instanceof NormalTask) for (java.util.Iterator<?> it = ((NormalTask) task).getAssignments().iterator(); it.hasNext();) { Assignment assignment = (Assignment) it.next(); if (!assignment.isDefault() && assignment.getResource().getUniqueId() == resourceId) return assignment; } throw new IllegalStateException("Assignment snapshot disappeared"); }
	record TaskSnapshot(String name, String notes, double percentComplete) { }
}
