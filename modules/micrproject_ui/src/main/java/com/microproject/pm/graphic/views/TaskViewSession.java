/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import java.awt.Component;
import java.util.Objects;

import com.microproject.application.task.TaskCommandGateway;
import com.microproject.application.task.TaskAuthorizationPort;
import com.microproject.application.task.TaskCommandType;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;

/** Thin, per-view composition root. It owns subscriptions and is idempotently closeable. */
public final class TaskViewSession implements AutoCloseable {
	private final ViewNodeModelCache cache;
	private final TaskProjectionStore projections;
	private final TaskCommandGateway commands;
	private final TaskViewBindings bindings = new TaskViewBindings();
	private final Project project;
	private volatile Component authorizationParent;
	private boolean closed;

	public TaskViewSession(Project project, ReferenceNodeModelCache reference, String viewName) {
		this.project = Objects.requireNonNull(project, "project");
		cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
				Objects.requireNonNull(reference, "reference"), viewName, null);
		projections = new TaskProjectionStore(cache);
		commands = new TaskCommandGateway(project, this::authorize);
		cache.setTaskCommandGateway(commands);
	}

	public ViewNodeModelCache cache() { return cache; }
	public TaskProjectionStore projections() { return projections; }
	public TaskCommandGateway commands() { return commands; }
	public void bind(Gantt gantt, SpreadSheet spreadSheet) {
		if (closed) throw new IllegalStateException("session is closed");
		authorizationParent = gantt != null ? gantt : spreadSheet;
		gantt.setTaskCommandGateway(commands);
		bindings.bind(gantt, spreadSheet);
	}
	public void unbind() { bindings.close(); authorizationParent = null; }

	private TaskAuthorizationPort.Decision authorize(ProjectTaskKey key, TaskCommandType commandType) {
		Task task = resolve(key);
		if (task == null || project.isReadOnly() || task.isReadOnly())
			return TaskAuthorizationPort.Decision.READ_ONLY;
		String action = commandType == TaskCommandType.CREATE_DEPENDENCY ? "link" : "edit";
		return CollaborationHelper.tryLockObject(project, task, authorizationParent, action)
				? TaskAuthorizationPort.Decision.ALLOWED : TaskAuthorizationPort.Decision.LOCK_DENIED;
	}

	private Task resolve(ProjectTaskKey key) {
		if (key == null) return null;
		for (Task task : project.getTasks())
			if (ProjectTaskKey.from(task).filter(key::equals).isPresent()) return task;
		return null;
	}

	@Override public void close() {
		if (closed) return;
		closed = true;
		authorizationParent = null;
		bindings.close();
		cache.close();
	}
}
