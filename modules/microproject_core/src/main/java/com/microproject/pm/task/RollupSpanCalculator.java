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

import java.util.Collection;

import com.microproject.grouping.core.Node;

/** Calculates task and project schedule spans from their visible WBS children. */
final class RollupSpanCalculator {
	private RollupSpanCalculator() {
	}

	static RollupSpan forTask(Task task) {
		if (!task.isWbsParent())
			return taskSpan(task);

		Collection<?> children = task.getWbsChildrenNodes();
		if (children == null || children.isEmpty())
			return taskSpan(task);

		long start = Long.MAX_VALUE;
		long finish = Long.MIN_VALUE;
		for (Object childNode : children) {
			if (!(childNode instanceof Node node) || !(node.getImpl() instanceof Task childTask))
				continue;
			RollupSpan childRollup = childTask.calculateRollupSpan();
			if (childRollup.getStart() != 0L)
				start = Math.min(start, childRollup.getStart());
			if (childRollup.getFinish() != 0L)
				finish = Math.max(finish, childRollup.getFinish());
		}

		if (start == Long.MAX_VALUE || finish == Long.MIN_VALUE)
			return taskSpan(task);
		return span(task, start, finish);
	}

	static RollupSpan forProject(Project project) {
		Collection<?> children = project.getTaskModel().getChildren(null);
		if (children == null || children.isEmpty())
			return projectSpan(project);

		long start = Long.MAX_VALUE;
		long finish = Long.MIN_VALUE;
		for (Object childNode : children) {
			if (!(childNode instanceof Node node) || !(node.getImpl() instanceof Task task))
				continue;
			RollupSpan childRollup = task.calculateRollupSpan();
			if (childRollup.getStart() != 0L)
				start = Math.min(start, childRollup.getStart());
			if (childRollup.getFinish() != 0L)
				finish = Math.max(finish, childRollup.getFinish());
		}

		if (start == Long.MAX_VALUE || finish == Long.MIN_VALUE)
			return projectSpan(project);
		long duration = project.getEffectiveWorkCalendar().compare(finish, start, false);
		return new RollupSpan(start, finish, duration);
	}

	private static RollupSpan taskSpan(Task task) {
		long start = task.getCurrentSchedule().getStart();
		long finish = task.getCurrentSchedule().getFinish();
		return span(task, start, finish);
	}

	private static RollupSpan span(Task task, long start, long finish) {
		long duration = task.getEffectiveWorkCalendar().compare(finish, start, false);
		return new RollupSpan(start, finish, duration);
	}

	private static RollupSpan projectSpan(Project project) {
		long duration = project.getEffectiveWorkCalendar().compare(project.getEnd(), project.getStart(), false);
		return new RollupSpan(project.getStart(), project.getEnd(), duration);
	}
}
