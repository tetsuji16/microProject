/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.ArrayList;
import java.util.List;

import com.microproject.grouping.core.Node;
import com.microproject.pm.task.Task;
import com.microproject.util.ClassUtils;

/** Resolves the active task-row selection and its command eligibility once. */
final class ActiveTaskSelectionResolver {
	private ActiveTaskSelectionResolver() {
	}

	static Selection resolve(List<Node> selectedNodes, boolean headerColumnSelection,
			boolean excludeReadOnly, boolean allowMixedSelection) {
		if (headerColumnSelection)
			return Selection.rejected("task-row-selection-required");
		if (selectedNodes == null || selectedNodes.isEmpty())
			return Selection.rejected("no-selection");

		List<Node> tasks = new ArrayList<>(selectedNodes.size());
		for (Node node : selectedNodes) {
			if (node == null)
				continue;
			Object implementation = node.getImpl();
			if (implementation instanceof Task) {
				if (!excludeReadOnly || !ClassUtils.isObjectReadOnly(implementation))
					tasks.add(node);
			} else if (!allowMixedSelection) {
				return Selection.rejected("mixed-selection");
			}
		}
		if (tasks.isEmpty())
			return Selection.rejected("no-task-selection");
		return new Selection(tasks, "");
	}

	record Selection(List<Node> nodes, String rejectionReason) {
		Selection {
			nodes = List.copyOf(nodes);
		}

		static Selection rejected(String reason) {
			return new Selection(List.of(), reason);
		}

		boolean isEligible(int minimumCount) {
			return rejectionReason.isEmpty() && nodes.size() >= minimumCount;
		}

		List<Long> stableTaskIds() {
		return nodes.stream().map(Node::getImpl).filter(Task.class::isInstance)
			.map(Task.class::cast).map(Task::getUniqueId).toList();
		}
	}
}
