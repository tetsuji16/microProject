/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microproject.grouping.core.Node;

/** Side-effect-free hierarchy queries shared by UI and exchange callers. */
public final class ProjectHierarchyQueries {
	private ProjectHierarchyQueries() {
	}

	/** Returns the project outline in the same stable order as the legacy iterator. */
	public static List<Task> outline(Project project) {
		List<Task> result = new ArrayList<>();
		if (project == null) return result;
		for (Iterator<Task> iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
			result.add(iterator.next());
		}
		return List.copyOf(result);
	}

	/** Returns descendants depth-first, excluding the supplied root task. */
	public static List<Task> descendants(Task root) {
		List<Task> result = new ArrayList<>();
		if (root != null) collect(root, result);
		return List.copyOf(result);
	}

	private static void collect(Task parent, List<Task> result) {
		if (parent.getWbsChildrenNodes() == null) return;
		for (Object child : parent.getWbsChildrenNodes()) {
			if (child instanceof Node node && node.getImpl() instanceof Task task) {
				result.add(task);
				collect(task, result);
			}
		}
	}
}
