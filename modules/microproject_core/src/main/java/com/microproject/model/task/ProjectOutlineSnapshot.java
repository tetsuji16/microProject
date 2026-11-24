/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.model.task;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.microproject.node.Node;
import com.microproject.node.model.NodeModel;

/**
 * Captures the project outline shape so a hierarchy command that fails after it
 * started applying can restore the exact parent and sibling order.
 *
 * <p>The capture records one position per task node. Restoring returns every
 * displaced node to its captured parent first, then repairs sibling order for
 * each captured parent. Both passes relocate with {@link NodeModel#SILENT} so a
 * rollback publishes no hierarchy, Undo or dirty-state change of its own.</p>
 */
public final class ProjectOutlineSnapshot {
	private final Project project;
	private final List<Position> positions;
	private final Project.DirtyState projectDirtyState;

	private ProjectOutlineSnapshot(Project project) {
		this.project = project;
		NodeModel model = project.getTaskModel();
		List<Position> captured = new ArrayList<>();
		for (Task task : project.getTaskList()) {
			Node node = model.search(task);
			if (node == null)
				continue;
			Node parent = (Node) node.getParent();
			if (parent == null)
				continue;
			captured.add(new Position(node, parent, parent.getIndex(node)));
		}
		positions = List.copyOf(captured);
		projectDirtyState = project.captureDirtyState();
	}

	public static ProjectOutlineSnapshot capture(Project project) {
		return new ProjectOutlineSnapshot(Objects.requireNonNull(project, "project"));
	}

	/** Whether this snapshot belongs to the specified project. */
	public boolean belongsTo(Project owner) {
		return project == owner;
	}

	/** Restores the captured parents and sibling order without publishing Undo or change events. */
	public void restore(Project owner) {
		if (owner != project)
			throw new IllegalArgumentException("outline snapshot belongs to a different project");
		NodeModel model = project.getTaskModel();
		for (Position position : positions) {
			if (position.node().getParent() == position.parent())
				continue;
			int index = Math.max(0, Math.min(position.index(), position.parent().getChildCount()));
			model.relocate(List.of(position.node()), position.parent(), index, NodeModel.SILENT);
		}
		Map<Node, List<Position>> childrenByParent = new IdentityHashMap<>();
		List<Node> parentOrder = new ArrayList<>();
		for (Position position : positions) {
			List<Position> children = childrenByParent.get(position.parent());
			if (children == null) {
				children = new ArrayList<>();
				childrenByParent.put(position.parent(), children);
				parentOrder.add(position.parent());
			}
			children.add(position);
		}
		for (Node parent : parentOrder) {
			List<Position> children = childrenByParent.get(parent);
			for (Position child : children) {
				int target = child.index();
				if (target < parent.getChildCount() && parent.getChildAt(target) == child.node())
					continue;
				model.relocate(List.of(child.node()), parent, target, NodeModel.SILENT);
			}
		}
		project.restoreDirtyState(projectDirtyState);
	}

	public record Position(Node node, Node parent, int index) { }
}
