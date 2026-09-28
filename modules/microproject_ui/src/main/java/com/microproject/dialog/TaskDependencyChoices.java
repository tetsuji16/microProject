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
package com.microproject.dialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

/** Shared candidate and label rules for predecessor and successor choosers. */
final class TaskDependencyChoices {
	private TaskDependencyChoices() {
	}

	static List<Task> linkableTasks(Task task, boolean predecessors, Iterable<Project> projects) {
		Set<Task> candidates = new LinkedHashSet<>();
		for (Project project : projects) {
			if (project == null)
				continue;
			for (Task candidate : project.getTaskList()) {
				if (candidate != task && !candidate.isExternal() && !isAlreadyLinked(task, candidate, predecessors))
					candidates.add(candidate);
			}
		}
		return new ArrayList<>(candidates);
	}

	static String dependencyDisplayName(Task current, Task endpoint) {
		if (endpoint == null)
			return "";
		Project endpointProject = endpoint.getOwningProject() == null ? endpoint.getProject() : endpoint.getOwningProject();
		Project currentProject = current == null ? null
				: current.getOwningProject() == null ? current.getProject() : current.getOwningProject();
		String taskName = endpoint.getName() == null ? "" : endpoint.getName();
		if (endpointProject == null || endpointProject == currentProject)
			return taskName;
		String projectName = endpointProject.getName();
		return projectName == null || projectName.isBlank() ? taskName : projectName + ": " + taskName;
	}

	static String chooserDisplayName(Task task) {
		if (task == null)
			return "";
		Project project = task.getOwningProject() == null ? task.getProject() : task.getOwningProject();
		String projectName = project == null || project.getName() == null ? "" : project.getName();
		String taskName = task.getName() == null ? "" : task.getName();
		return projectName.isBlank() ? taskName : projectName + ": " + taskName;
	}

	private static boolean isAlreadyLinked(Task task, Task candidate, boolean predecessors) {
		return predecessors
				? task.getPredecessorList().findLeft(candidate) != null
				: task.getSuccessorList().findRight(candidate) != null;
	}

	static final class Choice {
		private final Task task;

		Choice(Task task) {
			this.task = task;
		}

		Task task() {
			return task;
		}

		@Override
		public String toString() {
			return chooserDisplayName(task);
		}
	}
}
