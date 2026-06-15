package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import org.junit.jupiter.api.Test;

import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.NodeException;
import com.projectlibre1.grouping.core.NodeVisitor;
import com.projectlibre1.pm.assignment.Assignment;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.undo.DataFactoryUndoController;

class NormalTaskPercentCompleteTest {
	@Test
	void settingPercentCompleteSynchronizesAssignmentPercentages() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		Assignment assignment = firstAssignment(task);
		assignment.setPercentComplete(0.2d);
		assertEquals(0.2d, task.getPercentComplete(), 0.00001d);

		task.setPercentComplete(1.0d);

		assertEquals(1.0d, assignment.getPercentComplete(), 0.00001d);
		assertEquals(1.0d, task.getPercentComplete(), 0.00001d);
	}

	@Test
	void settingPercentCompleteDoesNotChangePlannedBarBounds() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long originalDuration = task.getDuration();

		task.setPercentComplete(0.1d);
		task.setPercentComplete(1.0d);

		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertEquals(originalDuration, task.getDuration());
	}

	@Test
	void settingPercentWorkCompleteDoesNotChangePlannedBarBounds() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		long originalStart = task.getStart();
		long originalEnd = task.getEnd();
		long originalDuration = task.getDuration();

		task.setPercentWorkComplete(0.1d);
		task.setPercentWorkComplete(0.2d);
		task.setPercentWorkComplete(0.4d);

		assertEquals(0.0d, task.getPercentComplete(), 0.00001d);
		assertEquals(originalStart, task.getStart());
		assertEquals(originalEnd, task.getEnd());
		assertEquals(originalDuration, task.getDuration());
	}

	@Test
	void settingPercentWorkCompleteKeepsRequestedValue() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		NormalTask task = new NormalTask(project);
		project.connectTask(task);

		task.setPercentWorkComplete(0.2d);
		assertEquals(0.2d, task.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteUsesDurationWeightedChildCompletion() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);

		NormalTask parent = new NormalTask(project);
		project.connectTask(parent);
		NormalTask firstChild = new NormalTask(project);
		project.connectTask(firstChild);
		NormalTask secondChild = new NormalTask(project);
		project.connectTask(secondChild);

		firstChild.setDuration(10L);
		secondChild.setDuration(30L);
		firstChild.setPercentWorkComplete(1.0d);
		secondChild.setPercentWorkComplete(0.5d);

		firstChild.setWbsParent(parent);
		secondChild.setWbsParent(parent);
		parent.setWbsChildrenNodes(childNodes(firstChild, secondChild));

		assertEquals(0.625d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteIgnoresZeroDurationLeaves() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstChild = createTask(project);
		NormalTask secondChild = createTask(project);
		NormalTask milestoneChild = createTask(project);

		firstChild.setDuration(10L);
		secondChild.setDuration(30L);
		milestoneChild.setDuration(0L);
		firstChild.setPercentWorkComplete(1.0d);
		secondChild.setPercentWorkComplete(0.5d);
		milestoneChild.setPercentWorkComplete(0.0d);

		attachChildren(parent, firstChild, secondChild, milestoneChild);

		assertEquals(0.625d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteInputRedistributesParallelChildrenByElapsedTime() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask shortTask = createTask(project);
		NormalTask longTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(shortTask, start, 10L * day);
		configureTask(longTask, start, 20L * day);
		attachChildren(parent, shortTask, longTask);

		parent.setPercentWorkComplete(0.2d);

		assertEquals(completedDuration(shortTask), completedDuration(longTask), 0.00001d);
		assertEquals(0.2d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteInputCanReduceCompletedChildren() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask doneTask = createTask(project);
		NormalTask remainingTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(doneTask, start, 10L * day);
		configureTask(remainingTask, start, 30L * day);
		doneTask.setPercentWorkComplete(1.0d);
		remainingTask.setPercentWorkComplete(0.5d);
		attachChildren(parent, doneTask, remainingTask);

		parent.setPercentWorkComplete(0.25d);

		assertTrue(doneTask.getPercentWorkComplete() < 1.0d);
		assertTrue(remainingTask.getPercentWorkComplete() < 0.5d);
		assertEquals(0.25d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteInputRedistributesSerialChildrenByTimelineOrder() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstTask = createTask(project);
		NormalTask secondTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(firstTask, start, 10L * day);
		configureTask(secondTask, firstTask.getEnd(), 10L * day);
		attachChildren(parent, firstTask, secondTask);

		parent.setPercentWorkComplete(0.75d);

		assertEquals(1.0d, firstTask.getPercentWorkComplete(), 0.00001d);
		assertTrue(secondTask.getPercentWorkComplete() > 0.0d);
		assertTrue(secondTask.getPercentWorkComplete() < 1.0d);
		assertEquals(0.75d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteDecreaseRedistributesParallelChildrenByElapsedTime() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask shortTask = createTask(project);
		NormalTask longTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(shortTask, start, 10L * day);
		configureTask(longTask, start, 20L * day);
		attachChildren(parent, shortTask, longTask);

		parent.setPercentWorkComplete(0.8d);
		parent.setPercentWorkComplete(0.2d);

		assertEquals(completedDuration(shortTask), completedDuration(longTask), 0.00001d);
		assertEquals(0.2d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteDecreaseRedistributesSerialChildrenFromLatestTask() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstTask = createTask(project);
		NormalTask secondTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(firstTask, start, 10L * day);
		configureTask(secondTask, firstTask.getEnd(), 10L * day);
		attachChildren(parent, firstTask, secondTask);

		parent.setPercentWorkComplete(0.8d);
		parent.setPercentWorkComplete(0.2d);

		assertTrue(firstTask.getPercentWorkComplete() > 0.0d);
		assertTrue(firstTask.getPercentWorkComplete() < 1.0d);
		assertTrue(secondTask.getPercentWorkComplete() < 0.6d);
		assertTrue(firstTask.getPercentWorkComplete() >= secondTask.getPercentWorkComplete());
		assertEquals(0.2d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteDecreaseToZeroClearsAllChildren() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstTask = createTask(project);
		NormalTask secondTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(firstTask, start, 10L * day);
		configureTask(secondTask, start, 20L * day);
		attachChildren(parent, firstTask, secondTask);

		parent.setPercentWorkComplete(0.7d);
		parent.setPercentWorkComplete(0.0d);

		assertEquals(0.0d, firstTask.getPercentWorkComplete(), 0.00001d);
		assertEquals(0.0d, secondTask.getPercentWorkComplete(), 0.00001d);
		assertEquals(0.0d, parent.getPercentWorkComplete(), 0.00001d);
	}

	@Test
	void parentPercentWorkCompleteInputDoesNotChangePlannedBarBounds() {
		Project project = createProject();
		NormalTask parent = createTask(project);
		NormalTask firstTask = createTask(project);
		NormalTask secondTask = createTask(project);

		long day = CalendarOption.getInstance().getMillisPerDay();
		long start = project.getStart();
		configureTask(firstTask, start, 10L * day);
		configureTask(secondTask, start, 20L * day);
		attachChildren(parent, firstTask, secondTask);

		long firstStart = firstTask.getStart();
		long firstEnd = firstTask.getEnd();
		long firstDuration = firstTask.getDuration();
		long secondStart = secondTask.getStart();
		long secondEnd = secondTask.getEnd();
		long secondDuration = secondTask.getDuration();

		parent.setPercentWorkComplete(0.4d);

		assertEquals(firstStart, firstTask.getStart());
		assertEquals(firstEnd, firstTask.getEnd());
		assertEquals(firstDuration, firstTask.getDuration());
		assertEquals(secondStart, secondTask.getStart());
		assertEquals(secondEnd, secondTask.getEnd());
		assertEquals(secondDuration, secondTask.getDuration());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		return Project.createProject(resourcePool, undoController);
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private Assignment firstAssignment(NormalTask task) {
		Iterator iterator = task.getAssignments().iterator();
		return (Assignment) iterator.next();
	}

	private void configureTask(NormalTask task, long start, long duration) {
		task.setDuration(duration);
		task.setStart(start);
		task.setEnd(task.getEffectiveWorkCalendar().add(start, duration, false));
	}

	private void attachChildren(NormalTask parent, NormalTask... children) {
		for (NormalTask child : children)
			child.setWbsParent(parent);
		parent.setWbsChildrenNodes(childNodes(children));
	}

	private double completedDuration(NormalTask task) {
		return task.getPercentWorkComplete() * task.getDurationMillis();
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
			visitor.execute(this);
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
