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
package com.microproject.pm.task;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import com.microproject.association.Association;
import com.microproject.pm.dependency.Dependency;

/**
 * Task path analysis behind Microsoft Project's "Task Path" highlighting
 * (Gantt Chart Format tab). Pure graph walk over the task links; nothing is modified.
 *
 * <p>Predecessors and successors follow every link transitively. The driving variants follow only
 * links that determine the date of the task they lead to: a link is driving when its link slack
 * ({@link Task#calcFreeSlack(Dependency)}) is zero, i.e. the linked task cannot move without moving
 * the other one. Disabled links are ignored. The selected tasks themselves are never part of a result.</p>
 */
public final class TaskPathAnalyzer {
	/** The four highlight kinds of the Task Path menu. */
	public enum Kind {
		PREDECESSORS(true, false),
		DRIVING_PREDECESSORS(true, true),
		SUCCESSORS(false, false),
		DRIVEN_SUCCESSORS(false, true);

		private final boolean backward;
		private final boolean drivingOnly;

		Kind(boolean backward, boolean drivingOnly) {
			this.backward = backward;
			this.drivingOnly = drivingOnly;
		}

		public boolean isBackward() {
			return backward;
		}

		public boolean isDrivingOnly() {
			return drivingOnly;
		}
	}

	private TaskPathAnalyzer() {
	}

	/** True when the link determines the date of its successor (zero link slack). */
	public static boolean isDriving(Dependency dependency) {
		return !dependency.isDisabled() && Task.calcFreeSlack(dependency) == 0L;
	}

	/** All tasks on the given path of the selection, without the selection itself. */
	public static Set<Task> path(Collection<? extends Task> selection, Kind kind) {
		if (selection == null || selection.isEmpty() || kind == null)
			return Collections.emptySet();
		Set<Task> visited = new LinkedHashSet<>(selection);
		Set<Task> result = new LinkedHashSet<>();
		Deque<Task> pending = new ArrayDeque<>(selection);
		while (!pending.isEmpty()) {
			Task task = pending.removeFirst();
			for (Association association : kind.isBackward() ? task.getPredecessorList() : task.getSuccessorList()) {
				Dependency dependency = (Dependency) association;
				if (dependency.isDisabled() || (kind.isDrivingOnly() && !isDriving(dependency)))
					continue;
				Object next = kind.isBackward() ? dependency.getPredecessor() : dependency.getSuccessor();
				if (next instanceof Task nextTask && visited.add(nextTask)) {
					result.add(nextTask);
					pending.addLast(nextTask);
				}
			}
		}
		return result;
	}

	/** The path of a single task. */
	public static Set<Task> path(Task selected, Kind kind) {
		return selected == null ? Collections.emptySet() : path(Collections.singleton(selected), kind);
	}
}
