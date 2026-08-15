package com.microproject.pm.graphic.collaboration;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microproject.collaboration.CollaborationSession;
import com.microproject.grouping.core.Node;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;

public final class CollaborationHelper {
	private CollaborationHelper() {
	}

	public static Task toTask(Object value) {
		if (value instanceof Task) {
			return (Task) value;
		}
		if (value instanceof Assignment) {
			return ((Assignment) value).getTask();
		}
		if (value instanceof Node) {
			return toTask(((Node) value).getImpl());
		}
		return null;
	}

	public static boolean tryLockObject(Project project, Object value, Component parent, String actionLabel) {
		if (project == null) {
			project = getProject(value);
		}
		if (project == null || project.getCollaborationSession() == null) {
			return true;
		}
		Task task = toTask(value);
		if (task == null) {
			return true;
		}
		return project.getCollaborationSession().tryLockTask(task, parent, actionLabel);
	}

	public static Project getProject(Object value) {
		Task task = toTask(value);
		return task == null ? null : task.getProject();
	}

	public static boolean tryLockNodes(Project project, List nodes, Component parent, String actionLabel) {
		if (nodes == null) {
			return true;
		}
		List<Task> tasks = new ArrayList<Task>();
		for (Iterator i = nodes.iterator(); i.hasNext();) {
			Object value = i.next();
			if (project == null) {
				project = getProject(value);
			}
			Task task = toTask(value);
			if (task != null) {
				tasks.add(task);
			}
		}
		if (project == null || project.getCollaborationSession() == null) {
			return true;
		}
		return project.getCollaborationSession().tryLockTasks(tasks, parent, actionLabel);
	}
}
