/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.testsupport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.microproject.association.Association;
import com.microproject.model.dependency.Dependency;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.model.task.Task;

/**
 * The model side of a GUI state snapshot: the task list in outline order. It is built from a
 * {@link Project} alone and needs no EDT, so it can also compare a project before saving with the
 * same project after reloading.
 */
public final class ProjectStateSnapshot {
	private ProjectStateSnapshot() {
	}

	/** Stored schedule values are GMT-read milliseconds (ADR-0003), so they are written as UTC. */
	public static String isoUtc(long millis) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC).toString();
	}

	/** One entry per task in outline order. */
	public static List<Map<String, Object>> tasks(Project project) {
		List<Map<String, Object>> tasks = new ArrayList<>();
		Iterator<Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext()) {
			Task task = iterator.next();
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("uniqueId", task.getUniqueId());
			entry.put("name", task.getName());
			entry.put("outlineLevel", task.getOutlineLevel());
			entry.put("summary", task.isSummary());
			entry.put("start", isoUtc(task.getStart()));
			entry.put("end", isoUtc(task.getEnd()));
			entry.put("durationMillis", task.getDurationMillis());
			Task parent = task.getWbsParentTask();
			entry.put("parentUniqueId", parent == null ? null : parent.getUniqueId());
			List<Map<String, Object>> predecessors = new ArrayList<>();
			for (Association association : task.getPredecessorList()) {
				if (!(association instanceof Dependency dependency)) continue;
				Map<String, Object> link = new LinkedHashMap<>();
				link.put("predecessorUniqueId", dependency.getPredecessor() instanceof Task predecessor
					? predecessor.getUniqueId() : null);
				link.put("type", dependency.getDependencyType());
				link.put("lagMillis", dependency.getLag());
				predecessors.add(link);
			}
			entry.put("predecessors", predecessors);
			entry.put("assignmentCount", task instanceof NormalTask normal ? normal.getAssignments().size() : 0);
			tasks.add(entry);
		}
		return tasks;
	}
}
