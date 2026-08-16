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
package com.microproject.pm.criticalpath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeException;
import com.microproject.grouping.core.NodeVisitor;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class TaskScheduleTest {
	@Test
	void setForwardKeepsLogicalBoundsAndFlipsStoredDates() {
		TaskSchedule schedule = new TaskSchedule();
		schedule.setStart(10L);
		schedule.setFinish(20L);
		schedule.setDependencyDate(30L);
		schedule.setRemainingDependencyDate(40L);

		schedule.setForward(false);

		assertEquals(10L, schedule.getBegin());
		assertEquals(20L, schedule.getEnd());
		assertEquals(-20L, schedule.getStart());
		assertEquals(-10L, schedule.getFinish());
		assertEquals(-30L, schedule.getDependencyDate());
		assertEquals(-40L, schedule.getRemainingDependencyDate());
	}

	@Test
	void assignDatesFromChildrenUsesMinStartAndMaxFinish() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstChild = createTask(project);
		NormalTask secondChild = createTask(project);
		long day = CalendarOption.getInstance().getMillisPerDay();

		configureTask(firstChild, project.getStart(), day);
		configureTask(secondChild, firstChild.getEnd() + day, 2L * day);
		attachChildren(parent, firstChild, secondChild);

		TaskSchedule schedule = parent.getCurrentSchedule();
		schedule.assignDatesFromChildren(null);

		assertEquals(firstChild.getStart(), schedule.getStart());
		assertEquals(secondChild.getEnd(), schedule.getFinish());
		assertEquals(parent.getEffectiveWorkCalendar().compare(secondChild.getEnd(), firstChild.getStart(), false),
				schedule.getRawDuration());
	}

	@Test
	void serializeRoundTripPreservesPersistentScheduleStateOnly() throws Exception {
		TaskSchedule schedule = new TaskSchedule();
		schedule.setPercentComplete(0.25D);
		schedule.setRawDuration(123L);
		schedule.setStart(10L);
		schedule.setFinish(20L);

		TaskSchedule restored = deserialize(serialize(schedule));

		assertEquals(0.25D, restored.getPercentComplete(), 0.00001D);
		assertEquals(123L, restored.getRawDuration());
		assertEquals(10L, restored.getStart());
		assertEquals(20L, restored.getFinish());
		assertEquals(Dependency.NEEDS_CALCULATION, restored.getDependencyDate());
		assertEquals(0L, restored.getRemainingDependencyDate());
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
		return task;
	}

	private void configureTask(NormalTask task, long start, long duration) {
		task.getCurrentSchedule().setStart(start);
		task.setDuration(duration);
	}

	private void attachChildren(NormalTask parent, NormalTask... children) {
		for (NormalTask child : children)
			child.setWbsParent(parent);
		parent.setWbsChildrenNodes(childNodes(children));
	}

	private byte[] serialize(TaskSchedule schedule) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream objectOut = new ObjectOutputStream(out);
		schedule.serialize(objectOut);
		objectOut.flush();
		return out.toByteArray();
	}

	private TaskSchedule deserialize(byte[] bytes) throws Exception {
		ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return TaskSchedule.deserialize(objectIn);
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

		public int getSubprojectLevel() {
			return subprojectLevel;
		}

		public void setSubprojectLevel(int subprojectLevel) {
			this.subprojectLevel = subprojectLevel;
		}

		public void setImpl(Object impl) {
			this.impl = impl;
		}

		public Object getValue() {
			return impl;
		}

		public Object getImpl() {
			return impl;
		}

		public java.util.ListIterator childrenIterator() {
			return children.listIterator();
		}

		public java.util.ListIterator childrenIterator(int i) {
			return children.listIterator(i);
		}

		public java.util.List getChildren() {
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

		public void setDirty(boolean dirty) {
			this.dirty = dirty;
		}

		public boolean isDirty() {
			return dirty;
		}

		public boolean isInSubproject() {
			return false;
		}

		public TreeNode getChildAt(int childIndex) {
			return children.get(childIndex);
		}

		public int getChildCount() {
			return children.size();
		}

		public TreeNode getParent() {
			return parent;
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
			this.impl = object;
		}

		public void removeFromParent() {
			if (parent != null)
				parent.remove(this);
		}

		public void setParent(MutableTreeNode newParent) {
			parent = newParent;
		}
	}
}
