/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.task;

import java.util.Collection;

import com.microproject.grouping.core.Node;

/** Calculates the actual progress span contributed by WBS children. */
record SummaryActualDateSpan(long start, long finish) {
	static SummaryActualDateSpan from(Collection<Node> children) {
		long start = Long.MAX_VALUE;
		long finish = 0L;
		for (Node childNode : children) {
			if (!(childNode.getImpl() instanceof NormalTask child) || child.getPercentComplete() <= 0.0D)
				continue;

			// Completed children contribute their planned start when no actual
			// start was stored. Partial children must have an explicit actual start.
			long childStart = child.getActualStart();
			if (childStart == 0L && child.getPercentComplete() >= 1.0D)
				childStart = child.getStart();
			if (childStart != 0L)
				start = Math.min(start, childStart);

			// Actual dates are an independent track. A scheduled finish is not
			// interchangeable with actual progress.
			long actualFinish = child.getActualFinish();
			if (actualFinish != 0L)
				finish = Math.max(finish, actualFinish);
			long stop = child.getStop();
			if (stop != 0L)
				finish = Math.max(finish, stop);
		}
		return new SummaryActualDateSpan(start == Long.MAX_VALUE ? 0L : start, finish);
	}
}
