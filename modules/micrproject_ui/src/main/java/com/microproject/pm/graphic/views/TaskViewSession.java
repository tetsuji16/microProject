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

import java.util.Objects;

import com.microproject.application.task.TaskCommandGateway;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.task.Project;

/** Thin, per-view composition root. It owns subscriptions and is idempotently closeable. */
public final class TaskViewSession implements AutoCloseable {
	private final ViewNodeModelCache cache;
	private final TaskCommandGateway commands;
	private TaskSelectionController selection;
	private boolean closed;

	public TaskViewSession(Project project, ReferenceNodeModelCache reference, String viewName,
			TaskCommandGateway commands) {
		Objects.requireNonNull(project, "project");
		this.commands = Objects.requireNonNull(commands, "commands");
		Objects.requireNonNull(reference, "reference").setTaskCommandGateway(commands);
		cache = (ViewNodeModelCache) NodeModelCacheFactory.getInstance().createFilteredCache(
				reference, viewName, null);
	}

	public ViewNodeModelCache cache() { return cache; }
	public void bind(Gantt gantt, SpreadSheet spreadSheet) {
		if (closed) throw new IllegalStateException("session is closed");
		gantt.setTaskCommandGateway(commands);
		gantt.refreshProjectionCapture();
		if (selection != null) selection.close();
		selection = new TaskSelectionController(gantt, spreadSheet);
	}
	public void unbind() {
		if (selection != null) selection.close();
		selection = null;
	}

	@Override public void close() {
		if (closed) return;
		closed = true;
		if (selection != null) selection.close();
		selection = null;
		cache.close();
	}
}
