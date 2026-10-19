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

	static List<Node> preferPhysicalRowSelection(boolean hasPhysicalRowSelection,
			List<Node> physicalRows, List<Node> frameSelection) {
		return hasPhysicalRowSelection ? physicalRows : frameSelection;
	}

	static Selection resolve(List<Node> selectedNodes, boolean headerColumnSelection,
			boolean excludeReadOnly, boolean allowMixedSelection) {
		if (headerColumnSelection)
			return Selection.rejected("task-row-selection-required");
		if (selectedNodes == null || selectedNodes.isEmpty())
			return Selection.rejected("no-selection");

		List<Node> tasks = new ArrayList<>(selectedNodes.size());
		List<Long> selectedTaskIds = new ArrayList<>(selectedNodes.size());
		boolean hasReadOnlyTask = false;
		boolean hasMixedSelection = false;
		for (Node node : selectedNodes) {
			if (node == null)
				continue;
			Object implementation = node.getImpl();
			if (implementation instanceof Task) {
				selectedTaskIds.add(((Task) implementation).getUniqueId());
				if (excludeReadOnly && ClassUtils.isObjectReadOnly(implementation)) {
					hasReadOnlyTask = true;
				} else {
					tasks.add(node);
				}
			} else if (!allowMixedSelection) {
				hasMixedSelection = true;
			}
		}
		if (hasMixedSelection)
			return Selection.rejected("mixed-selection", selectedTaskIds);
		if (hasReadOnlyTask)
			return Selection.rejected("task-read-only", selectedTaskIds);
		if (tasks.isEmpty())
			return Selection.rejected("no-task-selection");
		return new Selection(tasks, "", selectedTaskIds);
	}

	record Selection(List<Node> nodes, String rejectionReason, List<Long> stableTaskIds) {
		Selection {
			nodes = List.copyOf(nodes);
			stableTaskIds = List.copyOf(stableTaskIds);
		}

		static Selection rejected(String reason) {
			return rejected(reason, List.of());
		}

		static Selection rejected(String reason, List<Long> selectedTaskIds) {
			return new Selection(List.of(), reason, selectedTaskIds);
		}

		boolean isEligible(int minimumCount) {
			return rejectionReason.isEmpty() && nodes.size() >= minimumCount;
		}

	}
}
