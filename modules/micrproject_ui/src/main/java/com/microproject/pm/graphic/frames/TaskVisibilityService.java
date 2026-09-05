/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.grouping.core.Node;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.UndoController;

/** Applies task-view visibility as one undoable user operation. */
final class TaskVisibilityService {
	private TaskVisibilityService() {
	}

	static int hideSelected(Project project, Collection<Node> selectedNodes, UndoController undoController) {
		Map<Task, Boolean> changes = new LinkedHashMap<>();
		if (selectedNodes != null) {
			for (Node node : selectedNodes) {
				if (node != null && node.getImpl() instanceof Task task) {
					collectTaskAndDescendants(task, true, changes, new IdentityHashMap<>());
				}
			}
		}
		return apply(project, changes, undoController, "Hide Tasks");
	}

	static int showAll(Project project, UndoController undoController) {
		Map<Task, Boolean> changes = new LinkedHashMap<>();
		for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			Task task = iterator.next();
			if (task.isHiddenTask()) {
				changes.put(task, Boolean.FALSE);
			}
		}
		return apply(project, changes, undoController, "Show All Tasks");
	}

	private static void collectTaskAndDescendants(Task task, boolean hidden, Map<Task, Boolean> changes,
			IdentityHashMap<Task, Boolean> visited) {
		if (visited.put(task, Boolean.TRUE) != null) {
			return;
		}
		if (task.isHiddenTask() != hidden) {
			changes.put(task, hidden);
		}
		Collection<?> children = task.getWbsChildrenNodes();
		if (children == null) {
			return;
		}
		for (Object childNode : children) {
			if (childNode instanceof Node node && node.getImpl() instanceof Task child) {
				collectTaskAndDescendants(child, hidden, changes, visited);
			}
		}
	}

	private static int apply(Project project, Map<Task, Boolean> after, UndoController undoController, String name) {
		if (project == null || after.isEmpty()) {
			return 0;
		}
		Map<Task, Boolean> before = new LinkedHashMap<>();
		for (Task task : after.keySet()) {
			before.put(task, task.isHiddenTask());
		}
		applyStates(project, after);
		if (undoController != null) {
			undoController.getEditSupport().postEdit(new TaskVisibilityEdit(project, before, after, name));
		}
		return after.size();
	}

	private static void applyStates(Project project, Map<Task, Boolean> states) {
		for (Map.Entry<Task, Boolean> entry : states.entrySet()) {
			Task task = entry.getKey();
			boolean hidden = entry.getValue();
			if (task.isHiddenTask() != hidden) {
				task.setHiddenTask(hidden);
				project.fireUpdateEvent(TaskVisibilityService.class, task);
			}
		}
	}

	private static final class TaskVisibilityEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Project project;
		private final Map<Task, Boolean> before;
		private final Map<Task, Boolean> after;
		private final String name;

		private TaskVisibilityEdit(Project project, Map<Task, Boolean> before, Map<Task, Boolean> after, String name) {
			this.project = project;
			this.before = new LinkedHashMap<>(before);
			this.after = new LinkedHashMap<>(after);
			this.name = name;
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
			return name;
		}
	}
}
