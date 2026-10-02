/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.model.cache;

import com.microproject.grouping.core.Node;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;

/** Stable, tagged identity for one row in a view projection. */
public sealed interface ProjectionRowKey permits ProjectionRowKey.TaskRow, ProjectionRowKey.SyntheticRow {
	int occurrence();

	static ProjectionRowKey forNode(GraphicNode graphicNode, int occurrence) {
		if (graphicNode == null)
			throw new IllegalArgumentException("graphicNode must not be null");
		if (occurrence < 0)
			throw new IllegalArgumentException("occurrence must not be negative");
		Node node = graphicNode.getNode();
		Object implementation = node == null ? null : node.getImpl();
		if (implementation instanceof Task task) {
			var taskKey = ProjectTaskKey.from(task);
			if (taskKey.isPresent())
				return new TaskRow(taskKey.orElseThrow(), occurrence);
		}
		if (node == null)
			throw new IllegalArgumentException("graphicNode must have a node");
		return new SyntheticRow(node, occurrence);
	}

	/** Task identity is independent of its current row and view ordering. */
	record TaskRow(ProjectTaskKey taskKey, int occurrence) implements ProjectionRowKey {
		public TaskRow {
			if (taskKey == null)
				throw new IllegalArgumentException("taskKey must not be null");
			if (occurrence < 0)
				throw new IllegalArgumentException("occurrence must not be negative");
		}
	}

	/** Non-task/group rows use their node identity until they gain a domain key. */
	record SyntheticRow(Node node, int occurrence) implements ProjectionRowKey {
		public SyntheticRow {
			if (node == null)
				throw new IllegalArgumentException("node must not be null");
			if (occurrence < 0)
				throw new IllegalArgumentException("occurrence must not be negative");
		}
	}
}
