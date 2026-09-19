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
