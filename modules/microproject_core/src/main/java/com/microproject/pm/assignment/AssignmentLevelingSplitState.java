/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.assignment;

import java.util.List;
import java.util.Objects;

import com.microproject.pm.assignment.contour.AbstractContour;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.task.TaskSplitInterval;

/** Immutable ownership snapshot for resource-leveling gaps on one assignment. */
public final class AssignmentLevelingSplitState {
	private final AbstractContour beforeContour;
	private final long beforeDuration;
	private final AbstractContour generatedContour;
	private final long generatedDuration;
	private final List<TaskSplitInterval> generatedGaps;

	public AssignmentLevelingSplitState(AbstractContour beforeContour, long beforeDuration,
		AbstractContour generatedContour, long generatedDuration, List<TaskSplitInterval> generatedGaps) {
		this.beforeContour = copy(Objects.requireNonNull(beforeContour, "beforeContour"));
		this.beforeDuration = beforeDuration;
		this.generatedContour = copy(Objects.requireNonNull(generatedContour, "generatedContour"));
		this.generatedDuration = generatedDuration;
		this.generatedGaps = List.copyOf(Objects.requireNonNull(generatedGaps, "generatedGaps"));
		if (this.generatedGaps.isEmpty()) throw new IllegalArgumentException("A leveling split state requires a generated gap");
	}

	public AbstractContour beforeContourCopy() { return copy(beforeContour); }
	public long beforeDuration() { return beforeDuration; }
	public AbstractContour generatedContourCopy() { return copy(generatedContour); }
	public long generatedDuration() { return generatedDuration; }
	public List<TaskSplitInterval> generatedGaps() { return generatedGaps; }
	public BaselineAssignmentProfile.Contour beforeContourSnapshot() {
		return BaselineAssignmentProfile.contourSnapshot(beforeContour, beforeDuration);
	}
	public BaselineAssignmentProfile.Contour generatedContourSnapshot() {
		return BaselineAssignmentProfile.contourSnapshot(generatedContour, generatedDuration);
	}

	public boolean matchesGenerated(AbstractContour contour, long duration) {
		if (duration != generatedDuration || contour == null
			|| contour.getTypeKind() != generatedContour.getTypeKind()
			|| contour.numBuckets() != generatedContour.numBuckets()) return false;
		AbstractContourBucket[] actual = contour.getContourBuckets();
		AbstractContourBucket[] expected = generatedContour.getContourBuckets();
		for (int index = 0; index < actual.length; index++) {
			if (actual[index] == null || expected[index] == null
				|| actual[index].getBucketDuration(duration) != expected[index].getBucketDuration(generatedDuration)
				|| Double.doubleToLongBits(actual[index].getUnits()) != Double.doubleToLongBits(expected[index].getUnits())) return false;
		}
		return true;
	}

	private static AbstractContour copy(AbstractContour contour) {
		return (AbstractContour) contour.clone();
	}
}
