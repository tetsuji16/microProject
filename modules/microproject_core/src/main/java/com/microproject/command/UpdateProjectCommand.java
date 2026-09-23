/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.command;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.UpdateProjectRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;


/**
 * Consumer<Object> that holds parameters from UpdateProject dialog and executes the action by visiting a task collection
 */
public class UpdateProjectCommand extends Command {
	private final Project project;
	private final UpdateProjectRequest request;
	private final List<Long> affectedTaskIds = new ArrayList<>();
	public UpdateProjectCommand(Project project, long date, boolean updateWorkAsCompleteThrough, boolean setFractionalPercentComplete) {
		this(project, new UpdateProjectRequest(DateTime.nextDay(date), updateWorkAsCompleteThrough,
			setFractionalPercentComplete, false));
	}
	public UpdateProjectCommand(Project project, UpdateProjectRequest request) {
		super(Messages.getString("Command.UpdateProject"),project);
		this.project = project;
		this.request = java.util.Objects.requireNonNull(request, "request");
	}
	public void accept(Object arg0) {
		Task task = (Task) arg0;
		if (task.isReadOnly() || task.isSummary()) return;
		applyToTask(task);
	}

	/** Applies one Update Project transaction and posts exactly one undoable edit. */
	public List<Long> execute(List<? extends Task> targets, UndoableEditSupport edits) {
		Map<Task, TaskState> before = backupProjectTasks();
		affectedTaskIds.clear();
		for (Task task : targets) accept(task);
		if (!affectedTaskIds.isEmpty()) {
			Map<Task, TaskState> after = backupProjectTasks();
			edits.postEdit(new UpdateProjectEdit(project, before, after));
		}
		return affectedTaskIds();
	}

	private void applyToTask(Task task) {
		long taskEndBeforeUpdate = task.getEnd();
		if (task.updateProjectTask(request.statusDate(), request.updateWorkAsCompleteThrough(),
			request.setFractionalPercentComplete())) {
			if (task instanceof com.microproject.pm.task.NormalTask normal) {
				double exportedProgress = request.updateWorkAsCompleteThrough()
					&& !request.setFractionalPercentComplete()
					&& request.statusDate() >= taskEndBeforeUpdate ? 1D : task.getPercentComplete();
				normal.setImportedPercentComplete(exportedProgress);
			}
			affectedTaskIds.add(Long.valueOf(task.getUniqueId()));
		}
	}

	private Map<Task, TaskState> backupProjectTasks() {
		Map<Task, TaskState> result = new LinkedHashMap<>();
		for (Task task : project.getTaskList()) {
			Double override = task instanceof com.microproject.pm.task.NormalTask normal
				? normal.getImportedPercentCompleteOverride() : null;
			result.put(task, new TaskState(task.backupDetail(), override));
		}
		return result;
	}

	private record TaskState(Object detail, Double importedPercentCompleteOverride) { }

	private static final class UpdateProjectEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Project project;
		private final Map<Task, TaskState> before;
		private final Map<Task, TaskState> after;
		UpdateProjectEdit(Project project, Map<Task, TaskState> before, Map<Task, TaskState> after) {
			this.project = project;
			this.before = before;
			this.after = after;
		}
		@Override public String getPresentationName() { return Messages.getString("Command.UpdateProject"); }
		@Override public void undo() {
			super.undo();
			restore(before);
		}
		@Override public void redo() {
			super.redo();
			restore(after);
		}
		private void restore(Map<Task, TaskState> state) {
			for (Map.Entry<Task, TaskState> entry : state.entrySet()) {
				entry.getKey().restoreDetail(this, entry.getValue().detail(), false);
				if (entry.getKey() instanceof com.microproject.pm.task.NormalTask normal)
					normal.restoreImportedPercentCompleteOverride(entry.getValue().importedPercentCompleteOverride());
			}
			project.setDirty(true);
		}
	}
	public UpdateProjectRequest request() { return request; }
	public List<Long> affectedTaskIds() { return List.copyOf(affectedTaskIds); }
}
