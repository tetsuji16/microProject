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
import com.microproject.undo.EditSupport;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;
import com.microproject.transaction.ModelTransaction;


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
	public List<Long> execute(List<? extends Task> targets, EditSupport edits) {
		affectedTaskIds.clear();
		try {
			ModelTransaction.execute(new ModelTransaction.Steps<ProjectState, List<Long>>() {
				@Override public void validate() {
					java.util.Objects.requireNonNull(targets, "targets");
					java.util.Objects.requireNonNull(edits, "edits");
					java.util.Set<Task> projectTasks = java.util.Collections.newSetFromMap(
						new java.util.IdentityHashMap<>());
					projectTasks.addAll(project.getTaskList());
					java.util.Set<Task> uniqueTargets = java.util.Collections.newSetFromMap(
						new java.util.IdentityHashMap<>());
					for (Task task : targets) {
						if (task == null || !projectTasks.contains(task))
							throw new IllegalArgumentException("Every Update Project task must belong to its project");
						if (!uniqueTargets.add(task))
							throw new IllegalArgumentException("Update Project task targets must be unique");
					}
				}
				@Override public void authorize() {
					if (project.isReadOnly())
						throw new IllegalStateException("Cannot update a read-only project");
				}
				@Override public ProjectState capture() {
					beforeSnapshot = backupProjectTasks();
					return beforeSnapshot;
				}
				@Override public ModelTransaction.Mutation<List<Long>> apply() {
					for (Task task : targets) accept(task);
					List<Long> changed = affectedTaskIds();
					return changed.isEmpty()
						? ModelTransaction.Mutation.unchanged(changed)
						: ModelTransaction.Mutation.changed(changed);
				}
				@Override public void schedule() {
					// Each task update recalculates incrementally. Finish the batch from one
					// project-wide pass so dependent schedules and the captured Undo state agree.
					project.recalculate();
				}
				@Override public void checkInvariants() {
					for (Long taskId : affectedTaskIds) {
						Task task = project.getTaskList().stream()
							.filter(candidate -> candidate.getUniqueId() == taskId.longValue())
							.findFirst().orElse(null);
						if (task == null)
							throw new IllegalStateException("Updated task was detached from the project");
					}
				}
				@Override public void restore(ProjectState before) {
					restoreProjectTasks(before);
					affectedTaskIds.clear();
				}
				@Override public void commit(List<Long> changed) {
					ProjectState after = backupProjectTasks();
					edits.postEdit(new UpdateProjectEdit(project, beforeSnapshot.tasks(), after.tasks()));
				}
				private ProjectState beforeSnapshot;
			});
		} catch (ModelTransaction.ModelTransactionException failure) {
			if (failure.stage() != ModelTransaction.Stage.COMMIT)
				affectedTaskIds.clear();
			throw new IllegalStateException("Update Project failed during " + failure.failedStage(), failure);
		}
		return affectedTaskIds();
	}

	protected void applyToTask(Task task) {
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

	private ProjectState backupProjectTasks() {
		Map<Task, TaskState> result = new LinkedHashMap<>();
		for (Task task : project.getTaskList()) {
			Double override = task instanceof com.microproject.pm.task.NormalTask normal
				? normal.getImportedPercentCompleteOverride() : null;
			result.put(task, new TaskState(task.backupDetail(), override, task.isDirty()));
		}
		return new ProjectState(result, project.isDirty(), project.isGroupDirty());
	}

	private void restoreProjectTasks(ProjectState state) {
		restoreTaskStates(project, state.tasks(), this);
		project.setDirty(state.projectDirty());
		project.setGroupDirty(state.projectGroupDirty());
	}

	private static void restoreTaskStates(Project project, Map<Task, TaskState> state, Object eventSource) {
		for (Map.Entry<Task, TaskState> entry : state.entrySet()) {
			Task task = entry.getKey();
			TaskState taskState = entry.getValue();
			task.restoreDetail(eventSource, taskState.detail(), true);
			if (task instanceof com.microproject.pm.task.NormalTask normal)
				normal.restoreImportedPercentCompleteOverride(taskState.importedPercentCompleteOverride());
		}
		// Restore all task and assignment inputs before recalculating linked schedules once.
		project.recalculate();
		for (Map.Entry<Task, TaskState> entry : state.entrySet())
			entry.getKey().setDirty(entry.getValue().dirty());
	}

	private record ProjectState(Map<Task, TaskState> tasks, boolean projectDirty, boolean projectGroupDirty) { }
	private record TaskState(Object detail, Double importedPercentCompleteOverride, boolean dirty) { }

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
			restoreTaskStates(project, state, this);
			project.setDirty(true);
		}
	}
	public UpdateProjectRequest request() { return request; }
	public List<Long> affectedTaskIds() { return List.copyOf(affectedTaskIds); }
}
