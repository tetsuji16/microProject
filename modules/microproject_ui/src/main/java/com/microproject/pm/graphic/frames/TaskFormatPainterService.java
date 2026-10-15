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
		if (project == null || project.isReadOnly() || targets == null || targets.isEmpty() || format == null)
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
		applyStates(project, after);
		if (undoController != null)
			undoController.getEditSupport().postEdit(new TaskTextFormatEdit(project, before, after));
		return after.keySet().stream().map(Task::getUniqueId).toList();
	}

	private static void applyStates(Project project, Map<Task, TaskTextFormat> states) {
		for (Map.Entry<Task, TaskTextFormat> entry : states.entrySet()) {
			Task task = entry.getKey();
			entry.getValue().applyTo(task);
			project.fireUpdateEvent(TaskFormatPainterService.class, task);
		}
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
