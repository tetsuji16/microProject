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
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.grouping.core.Node;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectHierarchyQueries;
import com.microproject.pm.task.Task;
import com.microproject.undo.UndoController;

/** Applies task-view visibility as one undoable user operation. */
final class TaskVisibilityService {
	private TaskVisibilityService() {
	}

	static int hideSelected(Project project, Collection<Node> selectedNodes, UndoController undoController) {
		if (project == null || project.isReadOnly()) return 0;
		Map<Task, Boolean> changes = new LinkedHashMap<>();
		for (Task task : tasksToHide(selectedNodes)) changes.put(task, Boolean.TRUE);
		return apply(project, changes, undoController, "Hide Tasks");
	}

	/** Tasks whose persistent visibility state will change for the selected rows. */
	static List<Task> tasksToHide(Collection<Node> selectedNodes) {
		Map<Task, Boolean> changes = new LinkedHashMap<>();
		if (selectedNodes != null) {
			for (Node node : selectedNodes) {
				if (node != null && node.getImpl() instanceof Task task)
					collectTaskAndDescendants(task, true, changes, new IdentityHashMap<>());
			}
		}
		return List.copyOf(changes.keySet());
	}

	/** Returns stable IDs for tasks that will transition to hidden state. */
	static List<Long> affectedHiddenTaskIds(Collection<Node> selectedNodes) {
		return tasksToHide(selectedNodes).stream().map(Task::getUniqueId).filter(id -> id != null).toList();
	}

	/** Returns stable IDs for tasks that will transition to visible state. */
	static List<Long> affectedShownTaskIds(Project project) {
		return tasksToShow(project).stream().map(Task::getUniqueId).filter(id -> id != null).toList();
	}

	/** Hidden editable tasks that Show All would mutate. */
	static List<Task> tasksToShow(Project project) {
		if (project == null || project.isReadOnly()) return List.of();
		return ProjectHierarchyQueries.outline(project).stream()
				.filter(task -> task.isHiddenTask() && !task.isReadOnly()).toList();
	}

	static int showAll(Project project, UndoController undoController) {
		Map<Task, Boolean> changes = new LinkedHashMap<>();
		for (Task task : tasksToShow(project)) changes.put(task, Boolean.FALSE);
		return apply(project, changes, undoController, "Show All Tasks");
	}

	static boolean hasHiddenTasks(Project project) {
		return !tasksToShow(project).isEmpty();
	}

	private static void collectTaskAndDescendants(Task task, boolean hidden, Map<Task, Boolean> changes,
			IdentityHashMap<Task, Boolean> visited) {
		if (visited.put(task, Boolean.TRUE) != null) {
			return;
		}
		if (!task.isReadOnly() && task.isHiddenTask() != hidden) {
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
