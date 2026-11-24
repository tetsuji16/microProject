/*******************************************************************************
 * MIT License
 *
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
package com.microproject.pm.dependency;

import com.microproject.association.Association;
import com.microproject.configuration.CircularDependencyException;
import com.microproject.node.Node;
import com.microproject.pm.task.Task;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Iterative traversal helpers for task dependency graphs. */
public final class DependencyGraph {
	private DependencyGraph() { }

	public static boolean dependsOn(Task task, Task other) {
		if (task.isAncestorOrDescendent(other)) return true;
		Set<Task> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		ArrayDeque<Task> pending = new ArrayDeque<>();
		pending.push(task);
		while (!pending.isEmpty()) {
			Task current = pending.pop();
			if (!visited.add(current)) continue;
			if (current == other) return true;
			List<Task> candidates = candidates(current, other);
			for (int i = candidates.size() - 1; i >= 0; i--) pending.push(candidates.get(i));
		}
		return false;
	}

	/** Returns whether an enabled successor-link path reaches {@code other}. */
	public static boolean hasSuccessorPath(Task task, Task other) {
		Set<Task> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		ArrayDeque<Task> pending = new ArrayDeque<>();
		pending.push(task);
		while (!pending.isEmpty()) {
			Task current = pending.pop();
			if (!visited.add(current)) continue;
			if (current == other) return true;
			List<Task> successors = new ArrayList<>();
			for (Association association : current.getSuccessorList()) {
				Dependency dependency = (Dependency) association;
				if (!dependency.isDisabled()) successors.add((Task) dependency.getSuccessor());
			}
			for (int i = successors.size() - 1; i >= 0; i--) pending.push(successors.get(i));
		}
		return false;
	}

	public static void wouldCreateCycle(Task predecessor, Task successor) throws CircularDependencyException {
		if (predecessor == successor || dependsOn(predecessor, successor))
			throw new CircularDependencyException("Circular dependency");
	}

	/** Returns prerequisites before dependents, preserving encounter order among peers. */
	public static List<Task> topologicalOrder(Collection<? extends Task> tasks) throws CircularDependencyException {
		Set<Task> included = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Task> nodes = new ArrayList<>(tasks.size());
		for (Task task : tasks) if (included.add(task)) nodes.add(task);
		Map<Task, Integer> indegree = new IdentityHashMap<>();
		Map<Task, List<Task>> dependents = new IdentityHashMap<>();
		Map<Task, Set<Task>> edges = new IdentityHashMap<>();
		for (Task task : nodes) { indegree.put(task, 0); dependents.put(task, new ArrayList<>()); edges.put(task, Collections.newSetFromMap(new IdentityHashMap<>())); }
		for (Task task : nodes) {
			for (Task prerequisite : candidates(task, task)) addEdge(prerequisite, task, included, edges, dependents, indegree);
			Task parent = task.getWbsParentTask();
			if (parent != null) addEdge(parent, task, included, edges, dependents, indegree);
		}
		ArrayDeque<Task> ready = new ArrayDeque<>();
		for (Task task : nodes) if (indegree.get(task) == 0) ready.add(task);
		List<Task> result = new ArrayList<>(nodes.size());
		while (!ready.isEmpty()) {
			Task task = ready.remove(); result.add(task);
			for (Task dependent : dependents.get(task)) {
				int next = indegree.get(dependent) - 1; indegree.put(dependent, next);
				if (next == 0) ready.add(dependent);
			}
		}
		if (result.size() != included.size()) throw new CircularDependencyException("Circular dependency");
		return result;
	}

	private static void addEdge(Task prerequisite, Task dependent, Set<Task> included,
		Map<Task, Set<Task>> edges, Map<Task, List<Task>> dependents, Map<Task, Integer> indegree) {
		if (prerequisite == dependent || !included.contains(prerequisite)) return;
		if (edges.get(prerequisite).add(dependent)) {
			dependents.get(prerequisite).add(dependent);
			indegree.put(dependent, indegree.get(dependent) + 1);
		}
	}

	private static List<Task> candidates(Task task, Task other) {
		List<Task> result = new ArrayList<>();
		for (Association association : task.getPredecessorList()) {
			Dependency dependency = (Dependency) association;
			if (!dependency.isDisabled()) result.add((Task) dependency.getPredecessor());
		}
		Task parent = task.getWbsParentTask();
		if (other.getWbsParentTask() != parent && parent != null) result.add(parent);
		Collection<Node> children = task.getWbsChildrenNodes();
		if (children != null) for (Node childNode : children) {
			if (!(childNode.getImpl() instanceof Task child)) continue;
			for (Association association : child.getPredecessorList()) {
				Dependency dependency = (Dependency) association;
				if (dependency.isDisabled()) continue;
				Task predecessor = (Task) dependency.getPredecessor();
				if (!predecessor.wbsDescendentOf(task)) result.add(predecessor);
			}
		}
		return result;
	}
}
