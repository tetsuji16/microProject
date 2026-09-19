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
package com.microproject.grouping.core.model;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.hierarchy.AbstractMutableNodeHierarchy;
import com.microproject.grouping.core.summaries.DeepChildWalker;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.key.HasId;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;

public class NodeModelUtil {
	private static final Logger logger = Logger.getLogger(NodeModelUtil.class.getName());
	static class NonAssignmentEnumerator implements Consumer<Object> {
		int count = 0;

		public void accept(Object node) {
			if (node == null)
				return;
			Object impl = ((Node) node).getImpl();
			if (impl != null && !(impl instanceof Assignment)) {
				((HasId) impl).setId(++count);
			}
		}

	}
    public static boolean nodeIsSubproject(Node node) {
   		Object impl =node.getImpl();
		return impl instanceof SubProj || (impl instanceof Task && ((Task)impl).isSubproject());
    }

	public static void enumerateNonAssignments(NodeModel model) {
		DeepChildWalker.recursivelyTreatBranch(model, null, new NonAssignmentEnumerator());
	}

	public static void dump(NodeModel model) {
		((AbstractMutableNodeHierarchy) model.getHierarchy()).dump();
	}

	public static void dumpTask(NodeModel nodeModel) {
		dumpTask(nodeModel, null, "");
	}

	private static void dumpTask(NodeModel nodeModel, Node parent, String indent) {
		if (parent != null)
			logger.log(Level.FINE, "{0}>{1}", new Object[] { indent, parent.toString() });
		Collection<?> children = nodeModel.getChildren(parent);
		if (children != null) {
			for (Object value : children) {
				Node n = (Node) value;
				Object impl = n.getImpl();
				if (impl instanceof Task) {
					if (((Task) impl).getWbsParentTask() != (parent == null ? null : parent.getImpl()))
						logger.log(Level.WARNING, "cached hierarchy error - child {0} cached parent {1} parent {2}",
							new Object[] { impl, ((Task) impl).getWbsParentTask(), parent == null ? null : parent.getImpl() });
				}
				dumpTask(nodeModel, n, indent + "--");
			}
		}
	}

	public static List extractNodeList(NodeModel nodeModel, Node root) {
		ArrayList<Node> l = new ArrayList<>();
		extractNodeList(nodeModel, root, l);
		return l;
	}

	private static void extractNodeList(NodeModel nodeModel, Node parent, Collection<Node> result) {
		if (parent != null)
			result.add(parent);
		Collection<?> children = nodeModel.getChildren(parent);
		if (children != null) {
			for (Object value : children) {
				Node n = (Node) value;
				extractNodeList(nodeModel, n, result);
			}
		}
	}

	public static void cacheWbs(NodeModel nodeModel, Node parentNode) {
		Object parentImpl = parentNode.getImpl();
		List<Node> children = nodeModel.getChildren(parentNode);
		if (parentImpl instanceof Task && children != null && children.size() > 0) {
			Task parent = (Task) parentImpl;
			parent.setWbsChildrenNodes(children); // cached values
			for (Node child : children) {
				Object impl = child.getImpl();
				if (impl instanceof Task) {
					((Task) impl).setWbsParent(parent); // set cached wbs parent
														// too
					cacheWbs(nodeModel, child);
				}
			}
		}
	}

	public static boolean canBeChildOf(Node parent, Node child) {
		Object parentImpl = parent.getImpl();
		Object childImpl = child.getImpl();
		if (nodeIsSubproject(parent))
			return false;
		if (parentImpl instanceof Task && childImpl instanceof Task) {
			return ((Task)parentImpl).getOwningProject() == ((Task)childImpl).getOwningProject();
		}
		return true;
	}

}
