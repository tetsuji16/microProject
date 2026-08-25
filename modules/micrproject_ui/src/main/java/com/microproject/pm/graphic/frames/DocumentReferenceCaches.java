/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.task.Project;
import com.microproject.application.task.TaskCommandGateway;

/** Reference-cache owner whose lifetime is exactly one document frame. */
final class DocumentReferenceCaches implements AutoCloseable {
	private final Project project;
	private final TaskCommandGateway taskCommands;
	private ReferenceNodeModelCache task;
	private ReferenceNodeModelCache resource;
	private boolean closed;

	DocumentReferenceCaches(Project project, TaskCommandGateway taskCommands) {
		this.project = java.util.Objects.requireNonNull(project, "project");
		this.taskCommands = java.util.Objects.requireNonNull(taskCommands, "taskCommands");
	}

	synchronized ReferenceNodeModelCache task() {
		ensureOpen();
		if (task == null) {
			task = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
			task.setTaskCommandGateway(taskCommands);
		}
		return task;
	}

	synchronized ReferenceNodeModelCache resource() {
		ensureOpen();
		if (resource == null)
			resource = NodeModelCacheFactory.createResourceNodeModelCache(project.getResourcePool(), project.getResourceModel());
		return resource;
	}

	private void ensureOpen() {
		if (closed) throw new IllegalStateException("document reference caches are closed");
	}

	@Override
	public synchronized void close() {
		if (closed) return;
		closed = true;
		if (task != null) task.close();
		if (resource != null && resource != task) resource.close();
		task = null;
		resource = null;
	}
}
