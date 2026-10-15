/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;
import com.microproject.transaction.ModelTransaction;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.undo.EditSupport;
import com.microproject.undo.UndoController;

/** Copies the task-specific text formatting used by task rows and Gantt labels. */
final class TaskFormatPainterService {
	private TaskFormatPainterService() {
	}

	static TaskTextFormat capture(Task task) {
		return new TaskTextFormat(task.getFontFamily(), task.getFontSize(), task.getFontColor(), task.isFontBold(),
			task.isFontItalic(), task.isFontStrikethrough());
	}

	static List<Long> apply(Project project, List<Task> targets, TaskTextFormat format,
			UndoController undoController) {
		return apply(project, targets, format, undoController, (task, index) -> { });
	}

	static List<Long> apply(Project project, List<Task> targets, TaskTextFormat format,
			UndoController undoController, ApplyHook hook) {
		if (project == null || project.isReadOnly() || targets == null || targets.isEmpty() || format == null)
			return List.of();
		EditSupport edits = undoController == null ? null : undoController.getEditSupport();
		if (edits == null)
			return List.of();
		Map<Task, TaskTextFormat> before = new LinkedHashMap<>();
		Map<Task, TaskTextFormat> after = new LinkedHashMap<>();
		for (Task target : targets) {
			if (target == null || target.getProject() != project || target.isReadOnly())
				return List.of();
			TaskTextFormat current = capture(target);
			if (!current.equals(format)) {
				before.putIfAbsent(target, current);
				after.put(target, format);
			}
		}
		if (after.isEmpty())
			return List.of();
		boolean projectWasDirty = project.isDirty();
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			Project.PendingEventBatch pending;
			try {
				ModelTransaction.execute(new ModelTransaction.Steps<Map<Task, TaskState>, List<Long>>() {
					@Override public void validate() { }
					@Override public void authorize() {
						if (project.isReadOnly()) throw new IllegalStateException("Cannot format a read-only project");
						if (undoController.getEditSupport() != edits)
							throw new IllegalStateException("Format Painter Undo support changed");
					}
					@Override public Map<Task, TaskState> capture() {
						Map<Task, TaskState> snapshot = new LinkedHashMap<>();
						for (Task task : after.keySet()) snapshot.put(task,
							new TaskState(TaskFormatPainterService.capture(task), task.isDirty()));
						return snapshot;
					}
					@Override public ModelTransaction.Mutation<List<Long>> apply() {
						int index = 0;
						for (Map.Entry<Task, TaskTextFormat> entry : after.entrySet()) {
							entry.getValue().applyTo(entry.getKey());
							hook.afterTask(entry.getKey(), index++);
						}
						return ModelTransaction.Mutation.changed(after.keySet().stream().map(Task::getUniqueId).toList());
					}
					@Override public void schedule() { }
					@Override public void checkInvariants() {
						for (Map.Entry<Task, TaskTextFormat> entry : after.entrySet())
							if (!entry.getValue().equals(TaskFormatPainterService.capture(entry.getKey())))
								throw new IllegalStateException("Task text style did not apply");
					}
					@Override public void restore(Map<Task, TaskState> snapshot) {
						for (Map.Entry<Task, TaskState> entry : snapshot.entrySet()) {
							entry.getValue().format().applyTo(entry.getKey());
							entry.getKey().setDirty(entry.getValue().dirty());
						}
						project.setDirty(projectWasDirty);
					}
					@Override public void commit(List<Long> changed) {
						for (Task task : after.keySet()) project.fireUpdateEvent(TaskFormatPainterService.class, task);
						edits.postEdit(new TaskTextFormatEdit(project, before, after));
					}
				});
				pending = project.commitEventBatch(mutation, true);
			} catch (ModelTransaction.ModelTransactionException failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw new IllegalStateException("Format Painter failed during " + failure.failedStage(), failure);
			} catch (RuntimeException | Error failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw failure;
			}
			project.publishEventBatch(pending);
		}
		return after.keySet().stream().map(Task::getUniqueId).toList();
	}

	private static void applyStates(Project project, Map<Task, TaskTextFormat> states) {
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			Project.PendingEventBatch pending;
			try {
				for (Map.Entry<Task, TaskTextFormat> entry : states.entrySet()) {
					entry.getValue().applyTo(entry.getKey());
					project.fireUpdateEvent(TaskFormatPainterService.class, entry.getKey());
				}
				pending = project.commitEventBatch(mutation, true);
			} catch (RuntimeException | Error failure) {
				try { project.endEventBatch(false); }
				catch (Throwable closeFailure) { failure.addSuppressed(closeFailure); }
				throw failure;
			}
			project.publishEventBatch(pending);
		}
	}

	@FunctionalInterface interface ApplyHook { void afterTask(Task task, int index); }
	private record TaskState(TaskTextFormat format, boolean dirty) { }

	record TaskTextFormat(String family, int size, Integer color, boolean bold, boolean italic, boolean strikethrough) {
		private void applyTo(Task task) {
			task.setFontFamily(family);
			task.setFontSize(size);
			task.setFontColor(color);
			task.setFontBold(bold);
			task.setFontItalic(italic);
			task.setFontStrikethrough(strikethrough);
		}
	}

	private static final class TaskTextFormatEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Project project;
		private final Map<Task, TaskTextFormat> before;
		private final Map<Task, TaskTextFormat> after;

		private TaskTextFormatEdit(Project project, Map<Task, TaskTextFormat> before,
				Map<Task, TaskTextFormat> after) {
			this.project = project;
			this.before = new LinkedHashMap<>(before);
			this.after = new LinkedHashMap<>(after);
		}

		@Override
		public void undo() {
			super.undo();
			applyStates(project, before);
		}

		@Override
		public void redo() {
			super.redo();
			applyStates(project, after);
		}

		@Override
		public String getPresentationName() {
			return Messages.getString("FormatPainter.Undo");
		}
	}
}
