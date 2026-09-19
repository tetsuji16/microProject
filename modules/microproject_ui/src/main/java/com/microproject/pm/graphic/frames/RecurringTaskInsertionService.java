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
package com.microproject.pm.graphic.frames;

import java.util.List;

import javax.swing.undo.UndoableEditSupport;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.RecurringTaskGenerator;
import com.microproject.pm.task.RecurringTaskSpec;

final class RecurringTaskInsertionService {
	void insertRecurringTasks(DocumentFrame frame, RecurringTaskSpec spec) {
		SelectionAnchor anchor = resolveAnchor(frame);
		insertRecurringTasks(
			frame.getProject(),
			frame.getTaskModel(),
			anchor.getNode(),
			frame.getUndoController() == null ? null : frame.getUndoController().getEditSupport(),
			spec);
	}

	Node insertRecurringTasks(Project project, NodeModel nodeModel, Node anchor, UndoableEditSupport editSupport, RecurringTaskSpec spec) {
		SelectionAnchor selectionAnchor = SelectionAnchor.from(anchor);
		if (editSupport != null)
			editSupport.beginUpdate();
		try {
			Node summaryNode = createSummaryNode(project, spec);
			if (!selectionAnchor.hasParent())
				nodeModel.add(summaryNode, NodeModel.NORMAL);
			else
				nodeModel.add(selectionAnchor.getParent(), summaryNode, selectionAnchor.getPosition(), NodeModel.NORMAL);

			List<RecurringTaskGenerator.Occurrence> occurrences =
				RecurringTaskGenerator.generateOccurrences(spec, project.getEffectiveWorkCalendar());
			for (RecurringTaskGenerator.Occurrence occurrence : occurrences) {
				Node childNode = createOccurrenceNode(project, spec, occurrence);
				nodeModel.add(summaryNode, childNode, NodeModel.NORMAL);
			}
			project.recalculate();
			return summaryNode;
		} finally {
			if (editSupport != null)
				editSupport.endUpdate();
		}
	}

	private Node createSummaryNode(Project project, RecurringTaskSpec spec) {
		return createTaskNode(project, spec.getName());
	}

	private Node createOccurrenceNode(Project project, RecurringTaskSpec spec, RecurringTaskGenerator.Occurrence occurrence) {
		Node childNode = createTaskNode(project, spec.getName());
		configureOccurrence((NormalTask) childNode.getImpl(), spec, occurrence);
		return childNode;
	}

	private Node createTaskNode(Project project, String name) {
		NormalTask task = project.newStandaloneNormalTaskInstance();
		task.setName(name);
		return NodeFactory.getInstance().createNode(task);
	}

	private void configureOccurrence(NormalTask childTask, RecurringTaskSpec spec, RecurringTaskGenerator.Occurrence occurrence) {
		childTask.getCurrentSchedule().setStart(occurrence.getStart());
		childTask.setDuration(spec.getDuration());
	}

	private SelectionAnchor resolveAnchor(DocumentFrame frame) {
		CommonSpreadSheet spreadSheet = frame.getTopSpreadSheet();
		if (spreadSheet != null) {
			Node current = spreadSheet.getCurrentRowNode();
			if (current != null)
				return SelectionAnchor.from(current);
		}
		@SuppressWarnings("unchecked")
		List<Node> nodes = (List<Node>) frame.getSelectedNodes(true);
		if (nodes == null || nodes.isEmpty())
			return SelectionAnchor.root();
		return SelectionAnchor.from(nodes.get(0));
	}

	private static final class SelectionAnchor {
		private final Node node;
		private final Node parent;
		private final int position;

		private SelectionAnchor(Node node, Node parent, int position) {
			this.node = node;
			this.parent = parent;
			this.position = position;
		}

		static SelectionAnchor from(Node anchor) {
			if (anchor == null || anchor.getParent() == null)
				return root();
			Node parent = (Node) anchor.getParent();
			return new SelectionAnchor(anchor, parent, parent.getIndex(anchor) + 1);
		}

		static SelectionAnchor root() {
			return new SelectionAnchor(null, null, -1);
		}

		Node getNode() {
			return node;
		}

		Node getParent() {
			return parent;
		}

		int getPosition() {
			return position;
		}

		boolean hasParent() {
			return parent != null;
		}
	}
}
