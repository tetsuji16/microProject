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
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.junit.jupiter.api.Test;

import com.microproject.association.InvalidAssociationException;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeException;
import com.microproject.grouping.core.NodeVisitor;
import com.microproject.field.FieldContext;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class NormalTaskDurationTest {
	@Test
	void durationInputKeepsStartAndMovesFinishForRegularTask() {
		Project project = createProject();
		NormalTask task = createTask(project);

		long start = task.getStart();
		long duration = 3L * day();
		long expectedEnd = task.getEffectiveWorkCalendar().add(start, duration, false);

		task.setDuration(duration);

		assertEquals(start, task.getStart());
		assertEquals(expectedEnd, task.getEnd());
		assertEquals(duration, task.getDuration());
	}

	@Test
	void finishAnchoredDurationInputKeepsFinishAndMovesStart() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(2L * day());
		task.setScheduleConstraint(ConstraintType.FNLT, task.getEnd());

		long finish = task.getEnd();
		long duration = day();
		long expectedStart = task.getEffectiveWorkCalendar().add(finish, -duration, true);

		task.setDuration(duration);

		assertEquals(finish, task.getEnd());
		assertEquals(expectedStart, task.getStart());
		assertEquals(duration, task.getDuration());
	}

	@Test
	void durationShorterThanActualMarksTaskComplete() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(3L * day());
		task.setActualDuration(2L * day());

		task.setDuration(day());

		assertEquals(1.0D, task.getPercentComplete());
	}

	@Test
	void taskSheetStartEditKeepsDurationAndMovesFinish() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(2L * day());
		FieldContext context = taskSheetContext();

		long newStart = task.getEffectiveWorkCalendar().add(task.getStart(), day(), false);
		long expectedFinish = task.getEffectiveWorkCalendar().add(newStart, 2L * day(), false);

		task.setStart(newStart, context);

		assertEquals(newStart, task.getStart());
		assertEquals(expectedFinish, task.getEnd());
		assertEquals(2L * day(), task.getDuration());
	}

	@Test
	void taskSheetFinishEditKeepsDurationAndMovesStart() {
		Project project = createProject();
		NormalTask task = createTask(project);
		task.setDuration(2L * day());
		FieldContext context = taskSheetContext();

		long newFinish = task.getEffectiveWorkCalendar().add(task.getEnd(), day(), false);
		long expectedStart = task.getEffectiveWorkCalendar().add(newFinish, -(2L * day()), true);

		task.setEnd(newFinish, context);

		assertEquals(expectedStart, task.getStart());
		assertEquals(newFinish, task.getEnd());
		assertEquals(2L * day(), task.getDuration());
	}

	@Test
	void predecessorDurationInputReschedulesFinishToStartSuccessor() throws InvalidAssociationException {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		predecessor.setDuration(day());
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		recalculate(project, predecessor);
		long successorStartBefore = successor.getStart();

		predecessor.setDuration(2L * day());
		recalculate(project, predecessor);

		assertTrue(successor.getStart() > successorStartBefore,
				"successorStartBefore=" + successorStartBefore + " successorStart=" + successor.getStart()
						+ " predecessorEnd=" + predecessor.getEnd());
		assertTrue(successor.getStart() >= predecessor.getEnd(),
				"successorStart=" + successor.getStart() + " predecessorEnd=" + predecessor.getEnd());
	}

	@Test
	void successorDurationInputKeepsDependencyAlignedStartAndMovesFinish() throws InvalidAssociationException {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		predecessor.setDuration(day());
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		recalculate(project, predecessor);

		long dependencyAlignedStart = successor.getStart();

		successor.setDuration(2L * day());
		recalculate(project, successor);

		assertEquals(dependencyAlignedStart, successor.getStart());
		assertTrue(successor.getEnd() > dependencyAlignedStart);
		assertEquals(2L * day(), successor.getDuration());
	}

	@Test
	void durationInputHonorsStartToStartDependency() throws InvalidAssociationException {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		predecessor.setDuration(day());
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.SS, 0L, this);
		recalculate(project, predecessor);

		predecessor.setDuration(2L * day());
		recalculate(project, predecessor);

		assertEquals(predecessor.getStart(), successor.getStart());
	}

	@Test
	void durationInputHonorsFinishToFinishDependency() throws InvalidAssociationException {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		predecessor.setDuration(day());
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FF, 0L, this);
		recalculate(project, predecessor);

		predecessor.setDuration(2L * day());
		recalculate(project, predecessor);

		assertEquals(predecessor.getEnd(), successor.getEnd(),
				"successorEnd=" + successor.getEnd() + " predecessorEnd=" + predecessor.getEnd());
	}

	@Test
	void durationInputHonorsStartToFinishDependency() throws InvalidAssociationException {
		Project project = createProject();
		NormalTask predecessor = createTask(project);
		NormalTask successor = createTask(project);
		predecessor.setDuration(day());
		successor.setDuration(day());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.SF, 0L, this);
		recalculate(project, predecessor);

		predecessor.setDuration(2L * day());
		recalculate(project, predecessor);

		assertTrue(successor.getEnd() <= predecessor.getStart(),
				"successorEnd=" + successor.getEnd() + " predecessorStart=" + predecessor.getStart());
	}

	@Test
	void summaryDurationInputDoesNotResizeChildren() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstChild = createTask(project);
		NormalTask secondChild = createTask(project);

		long start = project.getStart();
		configureTask(firstChild, start, day());
		configureTask(secondChild, firstChild.getEnd(), day());
		attachChildren(parent, firstChild, secondChild);
		parent.setParentDuration();

		long originalFirstStart = firstChild.getStart();
		long originalFirstEnd = firstChild.getEnd();
		long originalSecondStart = secondChild.getStart();
		long originalSecondEnd = secondChild.getEnd();

		parent.setDuration(10L * day());

		assertEquals(10L * day(), parent.getDuration());
		assertEquals(originalFirstStart, firstChild.getStart());
		assertEquals(originalFirstEnd, firstChild.getEnd());
		assertEquals(originalSecondStart, secondChild.getStart());
		assertEquals(originalSecondEnd, secondChild.getEnd());
	}

	@Test
	void taskSheetSummaryDurationEditUpdatesEnvelopeOnly() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask child = createTask(project);
		configureTask(child, project.getStart(), day());
		attachChildren(parent, child);
		FieldContext context = taskSheetContext();

		parent.setDuration(10L * day(), context);

		assertTrue(parent.hasSummaryEnvelope());
		assertEquals(10L * day(), parent.getSummaryEnvelope().getManualDuration().longValue());
		assertEquals(child.getStart(), parent.calculateRollupSpan().getStart());
		assertEquals(child.getEnd(), parent.calculateRollupSpan().getFinish());
	}

	@Test
	void taskSheetSummaryDurationFieldUpdateUsesEnvelope() throws Exception {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask child = createTask(project);
		configureTask(child, project.getStart(), day());
		attachChildren(parent, child);
		FieldContext context = taskSheetContext();

		Configuration.getFieldFromId("Field.duration").setValue(parent, this, new Duration(10L * day()), context);

		assertTrue(parent.hasSummaryEnvelope());
		assertEquals(10L * day(), parent.getSummaryEnvelope().getManualDuration().longValue());
	}

	@Test
	void taskSheetSummaryStartEditKeepsRollupSeparate() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask child = createTask(project);
		configureTask(child, project.getStart(), day());
		attachChildren(parent, child);
		FieldContext context = taskSheetContext();
		long manualStart = child.getEffectiveWorkCalendar().add(child.getStart(), -day(), false);

		parent.setStart(manualStart, context);

		assertTrue(parent.hasSummaryEnvelope());
		assertEquals(manualStart, parent.getSummaryEnvelope().getManualStart().longValue());
		assertEquals(child.getStart(), parent.calculateRollupSpan().getStart());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		return task;
	}

	private void configureTask(NormalTask task, long start, long duration) {
		task.getCurrentSchedule().setStart(start);
		task.setDuration(duration);
	}

	private void recalculate(Project project, NormalTask task) {
		project.recalculate();
	}

	private long day() {
		return CalendarOption.getInstance().getMillisPerDay();
	}

	private FieldContext taskSheetContext() {
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);
		return context;
	}

	private void attachChildren(NormalTask parent, NormalTask... children) {
		for (NormalTask child : children)
			child.setWbsParent(parent);
		parent.setWbsChildrenNodes(childNodes(children));
	}

	private List<Node> childNodes(NormalTask... tasks) {
		List<Node> nodes = new ArrayList<Node>(tasks.length);
		for (NormalTask task : tasks)
			nodes.add(new TestNode(task));
		return nodes;
	}

	private static final class TestNode implements Node {
		private Object impl;
		private final List<MutableTreeNode> children = new ArrayList<MutableTreeNode>();
		private MutableTreeNode parent;
		private boolean dirty;
		private int subprojectLevel;

		private TestNode(Object impl) {
			this.impl = impl;
		}

		public void accept(NodeVisitor visitor) {
			visitor.accept(this);
		}

		public Class getType() throws NodeException {
			return impl == null ? Object.class : impl.getClass();
		}

		public boolean isVirtual() {
			return false;
		}

		public void setVirtual(boolean virtual) {
		}

		public boolean isVoid() {
			return false;
		}

		public void setVoid(boolean voidNode) {
		}

		public boolean isRoot() {
			return false;
		}

		public void setRoot(boolean root) {
		}

		public boolean hasNumber() {
			return false;
		}

		public Object getImpl() {
			return impl;
		}

		public void setImpl(Object imp) {
			impl = imp;
		}

		public ListIterator childrenIterator() {
			return children.listIterator();
		}

		public ListIterator childrenIterator(int i) {
			return children.listIterator(i);
		}

		public List getChildren() {
			return children;
		}

		public void add(MutableTreeNode node) {
			children.add(node);
			node.setParent(this);
		}

		public DefaultMutableTreeNode getPreviousSibling() {
			return null;
		}

		public DefaultMutableTreeNode getNextSibling() {
			return null;
		}

		public boolean isIndentable(int value) {
			return true;
		}

		public boolean isLazyParent() {
			return false;
		}

		public boolean canBeChildOf(Node parent) {
			return true;
		}

		public boolean isDirty() {
			return dirty;
		}

		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		public int getSubprojectLevel() {
			return subprojectLevel;
		}

		public void setSubprojectLevel(int subprojectLevel) {
			this.subprojectLevel = subprojectLevel;
		}

		public boolean isInSubproject() {
			return false;
		}

		public void insert(MutableTreeNode child, int index) {
			children.add(index, child);
			child.setParent(this);
		}

		public void remove(int index) {
			children.remove(index);
		}

		public void remove(MutableTreeNode node) {
			children.remove(node);
		}

		public void setUserObject(Object object) {
			impl = object;
		}

		public void removeFromParent() {
			if (parent != null)
				parent.remove(this);
		}

		public void setParent(MutableTreeNode newParent) {
			parent = newParent;
		}

		public TreeNode getChildAt(int childIndex) {
			return (TreeNode) children.get(childIndex);
		}

		public int getChildCount() {
			return children.size();
		}

		public TreeNode getParent() {
			return (TreeNode) parent;
		}

		public int getIndex(TreeNode node) {
			return children.indexOf(node);
		}

		public boolean getAllowsChildren() {
			return true;
		}

		public boolean isLeaf() {
			return children.isEmpty();
		}

		public java.util.Enumeration children() {
			return java.util.Collections.enumeration(children);
		}
	}
}
