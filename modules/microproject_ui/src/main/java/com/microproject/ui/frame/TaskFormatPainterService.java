/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.model.task.Project;
import com.microproject.model.task.Task;
import com.microproject.i18n.Messages;
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
			task.isFontItalic(), task.isFontStrikethrough(), task.isFontUnderline(), task.getFontBackground());
	}

	static List<Long> apply(Project project, List<Task> targets, TaskTextFormat format,
			UndoController undoController) {
		return apply(project, targets, format, undoController, (task, index) -> { });
	}

	static List<Long> apply(Project project, List<Task> targets, TaskTextFormat format,
			UndoController undoController, ApplyHook hook) {
		if (format == null)
			return List.of();
		return applyDerived(project, targets, ignored -> format, undoController, hook);
	}

	/**
	 * Applies {@code change} to the current text format of every target, as one undoable edit. Used by
	 * the Task &gt; Font group, where each task keeps the attributes the command does not touch.
	 */
	static List<Long> applyChange(Project project, List<Task> targets, UnaryOperator<TaskTextFormat> change,
			UndoController undoController) {
		return applyDerived(project, targets, change, undoController, (task, index) -> { });
	}

	private static List<Long> applyDerived(Project project, List<Task> targets, UnaryOperator<TaskTextFormat> change,
			UndoController undoController, ApplyHook hook) {
		if (project == null || project.isReadOnly() || targets == null || targets.isEmpty() || change == null)
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
			TaskTextFormat changed = change.apply(current);
			if (changed != null && !current.equals(changed)) {
				before.putIfAbsent(target, current);
				after.put(target, changed);
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

	record TaskTextFormat(String family, int size, Integer color, boolean bold, boolean italic, boolean strikethrough,
			boolean underline, Integer background) {
		TaskTextFormat(String family, int size, Integer color, boolean bold, boolean italic, boolean strikethrough) {
			this(family, size, color, bold, italic, strikethrough, false, null);
		}

		TaskTextFormat withFamily(String value) {
			return new TaskTextFormat(value, size, color, bold, italic, strikethrough, underline, background);
		}

		TaskTextFormat withSize(int value) {
			return new TaskTextFormat(family, value, color, bold, italic, strikethrough, underline, background);
		}

		TaskTextFormat withBold(boolean value) {
			return new TaskTextFormat(family, size, color, value, italic, strikethrough, underline, background);
		}

		TaskTextFormat withItalic(boolean value) {
			return new TaskTextFormat(family, size, color, bold, value, strikethrough, underline, background);
		}

		TaskTextFormat withUnderline(boolean value) {
			return new TaskTextFormat(family, size, color, bold, italic, strikethrough, value, background);
		}

		TaskTextFormat withColor(Integer value) {
			return new TaskTextFormat(family, size, value, bold, italic, strikethrough, underline, background);
		}

		TaskTextFormat withBackground(Integer value) {
			return new TaskTextFormat(family, size, color, bold, italic, strikethrough, underline, value);
		}

		private void applyTo(Task task) {
			task.setFontFamily(family);
			task.setFontSize(size);
			task.setFontColor(color);
			task.setFontBold(bold);
			task.setFontItalic(italic);
			task.setFontStrikethrough(strikethrough);
			task.setFontUnderline(underline);
			task.setFontBackground(background);
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
