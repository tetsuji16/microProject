/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.privacy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/**
 * Non-destructive, document-scoped presentation state for screenshot sharing.
 * This deliberately does not alter MSP-compatible project data or view filters.
 */
public final class PrivacyDisplayMode {
	private static final Map<Project, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

	private PrivacyDisplayMode() {
	}

	public static boolean isMasked(Project project) {
		return project != null && state(project) != null;
	}

	public static boolean toggle(Project project) {
		if (project == null) return false;
		synchronized (STATES) {
			if (STATES.remove(project) != null) return false;
			STATES.put(project, new State(project));
			return true;
		}
	}

	public static String taskName(Task task) {
		if (task == null) return null;
		State state = state(task.getProject());
		return state == null ? task.getName() : state.taskNames.getOrDefault(task.getUniqueId(), "Task " + task.getUniqueId());
	}

	public static String projectName(Project project) {
		State state = state(project);
		return state == null ? project == null ? null : project.getName() : "Project 01";
	}

	public static String resourceName(Project project, Resource resource) {
		if (resource == null) return null;
		State state = state(project);
		return state == null ? resource.getName() : state.resourceNames.getOrDefault(resource.getUniqueId(), "Resource " + resource.getUniqueId());
	}

	/** Resolves a resource in any currently open document without changing its name. */
	public static String resourceName(Resource resource) {
		if (resource == null) return null;
		synchronized (STATES) {
			for (State state : STATES.values()) {
				String masked = state.resourceObjects.get(resource);
				if (masked != null) return masked;
			}
		}
		return resource.getName();
	}

	/** Masks a comma-separated resource-name field without changing its value in the model. */
	public static String resourceNames(Project project, String value) {
		State state = state(project);
		if (state == null || value == null || value.isBlank()) return value;
		String result = value;
		for (Map.Entry<String, String> entry : state.resourceNamesByOriginal.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static State state(Project project) {
		return project == null ? null : STATES.get(project);
	}

	private static final class State {
		private final Map<Long, String> taskNames = new LinkedHashMap<>();
		private final Map<Long, String> resourceNames = new LinkedHashMap<>();
		private final Map<Resource, String> resourceObjects = new java.util.IdentityHashMap<>();
		private final Map<String, String> resourceNamesByOriginal = new LinkedHashMap<>();

		private State(Project project) {
			int taskNumber = 1;
			for (Task task : project.getTaskList()) {
				if (task != null) taskNames.put(task.getUniqueId(), String.format("Task %02d", taskNumber++));
			}
			int resourceNumber = 1;
			if (project.getResourcePool() != null) {
				for (Resource resource : project.getResourcePool().getResourceList()) {
					if (resource == null) continue;
					String masked = String.format("Resource %02d", resourceNumber++);
					resourceNames.put(resource.getUniqueId(), masked);
					resourceObjects.put(resource, masked);
					if (resource.getName() != null && !resource.getName().isBlank())
						resourceNamesByOriginal.put(resource.getName(), masked);
				}
			}
		}
	}
}
