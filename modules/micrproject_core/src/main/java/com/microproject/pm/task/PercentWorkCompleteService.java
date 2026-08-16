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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.microproject.datatype.Duration;
import com.microproject.grouping.core.Node;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.util.DisplayMath;

final class PercentWorkCompleteService {
	private PercentWorkCompleteService() {
	}

	static double aggregate(NormalTask parent) {
		List<NormalTask> leaves = new ArrayList<NormalTask>();
		collectLeafTasks(parent, leaves);

		long actualWork = 0L;
		long totalWork = 0L;
		for (NormalTask leaf : leaves) {
			long work = Duration.millis(leaf.getWork(null));
			if (work <= 0L)
				continue;
			actualWork += Math.min(work, Duration.millis(leaf.getActualWork(null)));
			totalWork += work;
		}
		if (totalWork <= 0L)
			return 0.0d;
		return ((double) actualWork) / totalWork;
	}

	static void distribute(NormalTask parent, double targetProgress) {
		List<NormalTask> leaves = new ArrayList<NormalTask>();
		collectLeafTasks(parent, leaves);

		List<LeafTaskProgress> activeLeaves = new ArrayList<LeafTaskProgress>();
		long totalWork = 0L;
		for (NormalTask leaf : leaves) {
			long duration = leaf.getDurationMillis();
			long work = Duration.millis(leaf.getWork(null));
			if (duration <= 0L || work <= 0L)
				continue;
			totalWork += work;
			activeLeaves.add(new LeafTaskProgress(leaf, duration, work));
		}
		if (totalWork <= 0L)
			return;

		long targetCompletedWork = Math.round(DisplayMath.clampProgressValue(targetProgress) * totalWork);
		targetCompletedWork = Math.max(0L, Math.min(targetCompletedWork, totalWork));
		if (activeLeaves.isEmpty())
			return;

		double currentProgress = aggregate(parent);
		boolean rollback = DisplayMath.clampProgressValue(targetProgress) < currentProgress;
		long cutoff = rollback
				? resolveRollbackCutoff(activeLeaves, targetCompletedWork)
				: resolveForwardCutoff(activeLeaves, targetCompletedWork);
		for (LeafTaskProgress leaf : activeLeaves) {
			long completed = rollback ? completedBeforeRollbackCutoff(leaf, cutoff) : completedBeforeCutoff(leaf, cutoff);
			double progress = leaf.work <= 0L ? 0.0d : ((double) completed) / leaf.work;
			leaf.task.applyPercentWorkCompleteOverride(progress);
		}
	}

	private static long resolveForwardCutoff(List<LeafTaskProgress> leaves, long targetCompletedDuration) {
		if (targetCompletedDuration <= 0L) {
			long earliestStart = Long.MAX_VALUE;
			for (LeafTaskProgress leaf : leaves)
				earliestStart = Math.min(earliestStart, leaf.task.getStart());
			return earliestStart == Long.MAX_VALUE ? 0L : earliestStart;
		}

		long earliestStart = Long.MAX_VALUE;
		long latestEnd = Long.MIN_VALUE;
		for (LeafTaskProgress leaf : leaves) {
			earliestStart = Math.min(earliestStart, leaf.task.getStart());
			latestEnd = Math.max(latestEnd, leaf.task.getEnd());
		}
		if (earliestStart == Long.MAX_VALUE || latestEnd == Long.MIN_VALUE)
			return 0L;

		long low = earliestStart;
		long high = latestEnd;
		while (low < high) {
			long mid = low + ((high - low) / 2L);
			long completed = completedThroughCutoff(leaves, mid);
			if (completed >= targetCompletedDuration)
				high = mid;
			else
				low = mid + 1L;
		}
		return low;
	}

	private static long resolveRollbackCutoff(List<LeafTaskProgress> leaves, long targetCompletedDuration) {
		if (targetCompletedDuration <= 0L) {
			long earliestStart = Long.MAX_VALUE;
			for (LeafTaskProgress leaf : leaves)
				earliestStart = Math.min(earliestStart, leaf.task.getStart());
			return earliestStart == Long.MAX_VALUE ? 0L : earliestStart;
		}

		long earliestStart = Long.MAX_VALUE;
		long latestCompletedEnd = Long.MIN_VALUE;
		for (LeafTaskProgress leaf : leaves) {
			earliestStart = Math.min(earliestStart, leaf.task.getStart());
			latestCompletedEnd = Math.max(latestCompletedEnd, completedEnd(leaf));
		}
		if (earliestStart == Long.MAX_VALUE || latestCompletedEnd == Long.MIN_VALUE)
			return 0L;

		long low = earliestStart;
		long high = latestCompletedEnd;
		while (low < high) {
			long mid = low + ((high - low) / 2L);
			long completed = completedThroughRollbackCutoff(leaves, mid);
			if (completed >= targetCompletedDuration)
				high = mid;
			else
				low = mid + 1L;
		}
		return low;
	}

	private static long completedThroughRollbackCutoff(List<LeafTaskProgress> leaves, long cutoff) {
		long completed = 0L;
		for (LeafTaskProgress leaf : leaves)
			completed += completedBeforeRollbackCutoff(leaf, cutoff);
		return completed;
	}

	private static long completedBeforeRollbackCutoff(LeafTaskProgress leaf, long cutoff) {
		long completedDuration = currentCompletedDuration(leaf);
		if (completedDuration <= 0L)
			return 0L;
		long duration = completedDuration(
				leaf.task.getEffectiveWorkCalendar(),
				leaf.task.getStart(),
				completedEnd(leaf),
				cutoff,
				completedDuration);
		return Math.round(((double) duration / leaf.duration) * leaf.work);
	}

	private static long currentCompletedDuration(LeafTaskProgress leaf) {
		return Math.round(DisplayMath.clampProgressValue(leaf.task.getPercentWorkComplete()) * leaf.duration);
	}

	private static long completedEnd(LeafTaskProgress leaf) {
		long completedDuration = currentCompletedDuration(leaf);
		if (completedDuration <= 0L)
			return leaf.task.getStart();
		if (completedDuration >= leaf.duration)
			return leaf.task.getEnd();
		return leaf.task.getEffectiveWorkCalendar().add(leaf.task.getStart(), completedDuration, false);
	}

	private static long completedThroughCutoff(List<LeafTaskProgress> leaves, long cutoff) {
		long completed = 0L;
		for (LeafTaskProgress leaf : leaves)
			completed += completedBeforeCutoff(leaf, cutoff);
		return completed;
	}

	private static long completedBeforeCutoff(LeafTaskProgress leaf, long cutoff) {
		long duration = completedDuration(leaf.task.getEffectiveWorkCalendar(), leaf.task.getStart(), leaf.task.getEnd(), cutoff, leaf.duration);
		return Math.round(((double) duration / leaf.duration) * leaf.work);
	}

	private static long completedDuration(WorkCalendar calendar, long start, long end, long cutoff, long plannedDuration) {
		if (plannedDuration <= 0L || cutoff <= start)
			return 0L;
		if (cutoff >= end)
			return plannedDuration;
		long completed = calendar.compare(cutoff, start, false);
		if (completed <= 0L)
			return 0L;
		return Math.min(plannedDuration, completed);
	}

	private static void collectLeafTasks(NormalTask task, List<NormalTask> leaves) {
		Collection children = task.getWbsChildrenNodes();
		if (children == null || children.isEmpty()) {
			if (!task.isWbsParent())
				leaves.add(task);
			return;
		}

		List<NormalTask> childTasks = new ArrayList<NormalTask>();
		for (Object childNode : children) {
			if (!(childNode instanceof Node))
				continue;
			Object impl = ((Node) childNode).getImpl();
			if (impl instanceof NormalTask)
				childTasks.add((NormalTask) impl);
		}
		childTasks.sort(Comparator.comparingLong(NormalTask::getStart).thenComparingLong(NormalTask::getEnd));
		for (NormalTask child : childTasks) {
			if (child.isWbsParent())
				collectLeafTasks(child, leaves);
			else
				leaves.add(child);
		}
	}

	private static final class LeafTaskProgress {
		private final NormalTask task;
		private final long duration;
		private final long work;

		private LeafTaskProgress(NormalTask task, long duration, long work) {
			this.task = task;
			this.duration = duration;
			this.work = work;
		}
	}
}
