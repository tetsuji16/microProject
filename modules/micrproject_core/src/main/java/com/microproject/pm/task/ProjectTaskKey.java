/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.task;

import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * Durable identity of a task within its owning project.
 *
 * <p>A task unique id is not globally unique: a master project and a loaded
 * subproject may contain the same value.  UI projections and application
 * commands must therefore carry both ids instead of using a transient row
 * number or a task id by itself.</p>
 */
public record ProjectTaskKey(long owningProjectId, long taskUniqueId) implements Serializable {
	private static final long serialVersionUID = 1L;

	public ProjectTaskKey {
		if (owningProjectId <= 0L)
			throw new IllegalArgumentException("owningProjectId must be positive");
		if (taskUniqueId <= 0L)
			throw new IllegalArgumentException("taskUniqueId must be positive");
	}

	/** Returns an empty value while either side of the durable identity is not assigned. */
	public static Optional<ProjectTaskKey> from(Task task) {
		if (task == null || task.getUniqueId() <= 0L)
			return Optional.empty();
		long projectId = task.getProjectId();
		if (projectId <= 0L && task.getOwningProject() != null)
			projectId = task.getOwningProject().getUniqueId();
		return projectId <= 0L
				? Optional.empty()
				: Optional.of(new ProjectTaskKey(projectId, task.getUniqueId()));
	}

	/** Resolves this durable identity through a master project and its loaded subprojects. */
	public static Optional<Task> resolve(Project project, ProjectTaskKey key) {
		return Optional.ofNullable(resolve(project, key,
				Collections.newSetFromMap(new IdentityHashMap<>())));
	}

	private static Task resolve(Project project, ProjectTaskKey key, Set<Project> visited) {
		if (project == null || key == null || !visited.add(project)) return null;
		for (Task task : project.getTasks()) {
			if (from(task).filter(key::equals).isPresent()) return task;
			if (task instanceof SubProj subproject) {
				Task nested = resolve(subproject.getSubproject(), key, visited);
				if (nested != null) return nested;
			}
		}
		return null;
	}
}
