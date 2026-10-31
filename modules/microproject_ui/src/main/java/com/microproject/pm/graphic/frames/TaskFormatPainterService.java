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
		applyStatesAtomically(project, after, undoController, hook,
			() -> edits.postEdit(new TaskTextFormatEdit(project, before, after, undoController,
				(task, index) -> { })));
		return after.keySet().stream().map(Task::getUniqueId).toList();
	}

	private static void applyStatesAtomically(Project project, Map<Task, TaskTextFormat> states,
			UndoController undoController, ApplyHook hook, Runnable committed) {
		Project.DirtyState projectWasDirty = project.captureDirtyState();
		try (ProjectMutationState.Scope mutation = project.getMutationState().begin()) {
			project.beginEventBatch();
			Project.PendingEventBatch pending;
			try {
				ModelTransaction.execute(new ModelTransaction.Steps<Map<Task, TaskState>, Void>() {
					@Override public void validate() { }
					@Override public void authorize() {
						if (project.isReadOnly()) throw new IllegalStateException("Cannot format a read-only project");
						if (undoController == null || undoController.getEditSupport() == null)
							throw new IllegalStateException("Format Painter Undo support is unavailable");
					}
					@Override public Map<Task, TaskState> capture() {
						Map<Task, TaskState> snapshot = new LinkedHashMap<>();
						for (Task task : states.keySet()) snapshot.put(task,
							new TaskState(TaskFormatPainterService.capture(task), task.isDirty()));
						return snapshot;
					}
					@Override public ModelTransaction.Mutation<Void> apply() {
						int index = 0;
						for (Map.Entry<Task, TaskTextFormat> entry : states.entrySet()) {
							entry.getValue().applyTo(entry.getKey());
							hook.afterTask(entry.getKey(), index++);
						}
						return ModelTransaction.Mutation.changed(null);
					}
					@Override public void schedule() { }
					@Override public void checkInvariants() {
						for (Map.Entry<Task, TaskTextFormat> entry : states.entrySet())
							if (!entry.getValue().equals(TaskFormatPainterService.capture(entry.getKey())))
								throw new IllegalStateException("Task text style did not apply");
					}
					@Override public void restore(Map<Task, TaskState> snapshot) {
						for (Map.Entry<Task, TaskState> entry : snapshot.entrySet()) {
							entry.getValue().format().applyTo(entry.getKey());
							entry.getKey().setDirty(entry.getValue().dirty());
						}
						project.restoreDirtyState(projectWasDirty);
					}
					@Override public void commit(Void ignored) {
						for (Task task : states.keySet()) project.fireUpdateEvent(TaskFormatPainterService.class, task);
						committed.run();
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
	}

	/*
	 * Apply, Undo, and Redo use the same transactional path. In particular, a failure
	 * while restoring multiple tasks must leave both the model and edit history intact.
	 */
	@FunctionalInterface interface ApplyHook { void afterTask(Task task, int index); }
	private record TaskState(TaskTextFormat format, boolean dirty) { }

	private static void applyStates(Project project, Map<Task, TaskTextFormat> states,
			UndoController undoController, ApplyHook hook) {
		applyStatesAtomically(project, states, undoController, hook, () -> { });
	}

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

	static final class TaskTextFormatEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Project project;
		private final UndoController undoController;
		private final Map<Task, TaskTextFormat> before;
		private final Map<Task, TaskTextFormat> after;
		private final ApplyHook hook;

		TaskTextFormatEdit(Project project, Map<Task, TaskTextFormat> before,
				Map<Task, TaskTextFormat> after, UndoController undoController, ApplyHook hook) {
			this.project = project;
			this.undoController = undoController;
			this.before = new LinkedHashMap<>(before);
			this.after = new LinkedHashMap<>(after);
			this.hook = hook;
		}

		@Override
		public void undo() {
			if (!canUndo()) throw new javax.swing.undo.CannotUndoException();
			applyStates(project, before, undoController, hook);
			super.undo();
		}

		@Override
		public void redo() {
			if (!canRedo()) throw new javax.swing.undo.CannotRedoException();
			applyStates(project, after, undoController, hook);
			super.redo();
		}

		@Override
		public String getPresentationName() {
			return Messages.getString("FormatPainter.Undo");
		}
	}
}
