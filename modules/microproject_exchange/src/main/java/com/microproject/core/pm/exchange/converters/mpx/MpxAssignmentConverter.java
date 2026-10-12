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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import net.sf.mpxj.Duration;
import net.sf.mpxj.AssignmentField;
import net.sf.mpxj.TimephasedWork;
import com.microproject.field.FieldContext;
import com.microproject.core.time.TimeUtil;
import com.microproject.exchange.mpxj.MpxjApi;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Task;
import com.microproject.pm.time.ImmutableInterval;

/**
 * Converts an MPXJ ResourceAssignment into a microproject Assignment.
 * Only fields carried by the microproject Assignment model are mapped. Standard
 * contours and custom contours represented by complete timephased work buckets
 * are imported, together with scalar baseline values.
 * @author Laurent Chretienneau
 */
public class MpxAssignmentConverter {

	public void from(net.sf.mpxj.ResourceAssignment mpxAssignment, Assignment assignment, MpxImportState state, Task task, int snapshotId) {
		Resource resource = resolveResource(mpxAssignment, state);
		if (resource == null) {
			throw new IllegalStateException("Unable to resolve resource for assignment " + mpxAssignment.getResourceUniqueID());
		}
		boolean customWorkContour = MpxjApi.hasCustomWorkContour(mpxAssignment);
		int workContourType = customWorkContour ? 0 : MpxjApi.workContourId(mpxAssignment);
		MpxImportState.RawMspdiAssignmentWork rawWork = customWorkContour
			? state.getRawMspdiAssignmentWork(mpxAssignment.getUniqueID() == null ? null
				: Long.valueOf(mpxAssignment.getUniqueID().longValue())) : null;
		boolean hasSourceRemainingWork = rawWork != null ? rawWork.remainingWork() != null
			: mpxAssignment.getCachedValue(AssignmentField.REMAINING_WORK) != null;
		long sourceWork = rawWork != null && rawWork.work() != null ? rawWork.work().longValue()
			: toLong(mpxAssignment.getWork());
		long sourceActualWork = rawWork != null && rawWork.actualWork() != null ? rawWork.actualWork().longValue()
			: toLong(mpxAssignment.getActualWork());
		long sourceRemainingWork = rawWork != null && rawWork.remainingWork() != null
			? rawWork.remainingWork().longValue() : hasSourceRemainingWork ? toLong(mpxAssignment.getRemainingWork())
			: Math.max(0L, sourceWork - sourceActualWork);
		CustomWorkPlan customWorkPlan = customWorkContour
			? customWorkPlan(mpxAssignment, sourceWork, sourceActualWork, sourceRemainingWork, rawWork) : null;
		assignment.setTaskAndResource(task, resource);
		// MSPDI stores assignment units as a percentage (100 = 1.0 in the
		// microProject model). Restore this explicitly before work is applied;
		// otherwise a round trip can leave the assignment at the default 0 units.
		if (mpxAssignment.getUnits() != null)
			assignment.forceUnits(mpxAssignment.getUnits().doubleValue() / 100.0D);
		// Import current schedule values here; scalar baseline slots are imported
		// separately by importBaselines after this current assignment is attached.
		long assignmentStart = rawWork != null && rawWork.start() != null ? rawWork.start().longValue()
			: toLong(mpxAssignment.getStart());
		Long assignmentFinish = rawWork != null && rawWork.finish() != null ? rawWork.finish()
			: mpxAssignment.getFinish() == null ? null : Long.valueOf(toLong(mpxAssignment.getFinish()));
		// AssignmentDetail stores a start as a delay relative to the task. During
		// import the task's schedule can still be uninitialised, so feeding the
		// identical task date through setStart turns it into an epoch-sized delay.
		if (assignmentStart != 0L && assignmentStart != task.getStart())
			assignment.setStart(assignmentStart);
		if (assignmentFinish != null)
			assignment.setEnd(assignmentFinish.longValue());
		// Install the source profile before applying Work. A standard contour's
		// inverse maps the scalar to the assignment span; applying Work as flat
		// first would shrink the span and reinterpret ActualWork later.
		assignment.setWorkContourType(workContourType);
		assignment.setWork(sourceWork, null);
		// MSPDI assignment PercentWorkComplete is work-based, while the model's
		// PercentComplete is elapsed-duration based. ActualWork below determines
		// the completion stop under the imported contour.
		// These setters edit the schedule and completion state; an absent date
		// must not be imported as an actual finish at the Unix epoch.
		if (mpxAssignment.getActualStart() != null)
			assignment.setActualStart(toLong(mpxAssignment.getActualStart()));
		if (mpxAssignment.getActualFinish() != null)
			assignment.setActualFinish(toLong(mpxAssignment.getActualFinish()));
		// Capture before mutating actuals: MPXJ can derive/cache RemainingWork from
		// the assignment's current state when the source field is absent.
		assignment.setActualWork(sourceActualWork, null);
		assignment.setRemainingWork(sourceRemainingWork, null);
		// WorkContour changes how the imported assignment span distributes work.
		// Setting Work may resize that span, so restore explicit XML assignment
		// bounds after all work/progress fields have been applied.
		if (assignmentStart != 0L)
			assignment.setStart(assignmentStart);
		if (assignmentFinish != null)
			assignment.setEnd(assignmentFinish.longValue());
		// Apply the source contour after progress setters, which may intentionally
		// convert a standard profile to a personal one. Do not call setWork again:
		// that setter represents a user scheduling edit and may convert an imported
		// standard contour into a personal profile when actuals are present.
		if (!customWorkContour)
			assignment.setWorkContourType(workContourType);
		else
			importCustomTimephasedWork(customWorkPlan, assignment, sourceWork, sourceActualWork,
				sourceRemainingWork, state);
		restoreLevelingDelay(mpxAssignment, assignment);
		if (mpxAssignment.getUniqueID() != null)
			assignment.setUniqueId(mpxAssignment.getUniqueID().longValue());
	}

	/**
	 * Imports custom MSPDI contours from the normalized actual/remaining work
	 * intervals MPXJ exposes. A custom contour without a complete, consistent
	 * timephased representation is rejected instead of being flattened.
	 */
	private CustomWorkPlan customWorkPlan(net.sf.mpxj.ResourceAssignment source, long sourceWork,
			long sourceActualWork, long sourceRemainingWork, MpxImportState.RawMspdiAssignmentWork rawWork) {
		List<WorkInterval> actual = rawWork == null ? readIntervals(source.getTimephasedActualWork())
			: readRawIntervals(rawWork, 2);
		List<WorkInterval> remaining = rawWork == null ? readIntervals(source.getTimephasedWork())
			: readRawIntervals(rawWork, 1);
		long actualTotal = sumWork(actual);
		long remainingTotal = sumWork(remaining);
		if (actualTotal != sourceActualWork || remainingTotal != sourceRemainingWork
				|| Math.addExact(actualTotal, remainingTotal) != sourceWork
				|| actual.isEmpty() && remaining.isEmpty()) {
			throw new IllegalArgumentException("Custom MSPDI work contour has incomplete timephased work data");
		}
		long assignmentStart = rawWork != null && rawWork.start() != null ? rawWork.start().longValue()
			: toLong(source.getStart());
		long assignmentFinish = rawWork != null && rawWork.finish() != null ? rawWork.finish().longValue()
			: toLong(source.getFinish());
		for (WorkInterval interval : concat(actual, remaining)) {
			if (interval.start() < assignmentStart || interval.end() > assignmentFinish)
				throw new IllegalArgumentException("Custom MSPDI work interval " + interval.start() + ".."
					+ interval.end() + " work=" + interval.work() + " falls outside assignment bounds "
					+ assignmentStart + ".." + assignmentFinish);
		}
		return new CustomWorkPlan(assignmentStart, assignmentFinish, actual, remaining);
	}

	private List<WorkInterval> readRawIntervals(MpxImportState.RawMspdiAssignmentWork rawWork, int type) {
		List<WorkInterval> result = new ArrayList<>();
		for (MpxImportState.RawMspdiWorkInterval interval : rawWork.intervals())
			if (interval.type() == type)
				result.add(new WorkInterval(interval.start(), interval.finish(), interval.work()));
		return List.copyOf(result);
	}

	private List<WorkInterval> readIntervals(List<TimephasedWork> sourceIntervals) {
		List<WorkInterval> result = new ArrayList<>();
		if (sourceIntervals == null)
			return result;
		for (TimephasedWork interval : sourceIntervals) {
			if (interval.getStart() == null || interval.getFinish() == null)
				throw new IllegalArgumentException("Custom MSPDI work interval is missing a bound");
			long start = toLong(interval.getStart());
			long end = toLong(interval.getFinish());
			long work = toLong(interval.getTotalAmount());
			if (end <= start || work < 0L)
				throw new IllegalArgumentException("Custom MSPDI work interval has invalid bounds or value: "
					+ start + ".." + end + " work=" + work + " raw=" + interval);
			result.add(new WorkInterval(start, end, work));
		}
		return result;
	}

	private List<WorkInterval> concat(List<WorkInterval> first, List<WorkInterval> second) {
		List<WorkInterval> result = new ArrayList<>(first.size() + second.size());
		result.addAll(first);
		result.addAll(second);
		return result;
	}

	private long sumWork(List<WorkInterval> intervals) {
		long total = 0L;
		for (WorkInterval interval : intervals)
			total = Math.addExact(total, interval.work());
		return total;
	}

	private void importCustomTimephasedWork(CustomWorkPlan plan, Assignment target, long sourceWork,
			long sourceActualWork, long sourceRemainingWork, MpxImportState state) {
		List<Long> boundaries = new ArrayList<>();
		boundaries.add(plan.start());
		boundaries.add(plan.finish());
		for (WorkInterval interval : concat(plan.actual(), plan.remaining())) {
			boundaries.add(interval.start());
			boundaries.add(interval.end());
		}
		boundaries.sort(Long::compare);
		List<WorkSegment> segments = new ArrayList<>();
		var calendar = target.getEffectiveWorkCalendar();
		validateNoWorkingOverlap(plan.actual(), calendar, "actual");
		validateNoWorkingOverlap(plan.remaining(), calendar, "remaining");
		for (int index = 0; index < boundaries.size() - 1; index++) {
			long start = boundaries.get(index);
			long end = boundaries.get(index + 1);
			if (start == end)
				continue;
			long duration = calendar.compare(end, start, false);
			if (duration == 0L)
				continue;
			long actual = segmentWork(plan.actual(), start, end, calendar);
			long remaining = segmentWork(plan.remaining(), start, end, calendar);
			segments.add(new WorkSegment(start, end, actual, remaining));
		}
		long mappedActual = 0L;
		long mappedRemaining = 0L;
		for (WorkSegment segment : segments) {
			mappedActual = Math.addExact(mappedActual, segment.actualWork());
			mappedRemaining = Math.addExact(mappedRemaining, segment.remainingWork());
		}
		if (segments.isEmpty() || mappedActual != sourceActualWork || mappedRemaining != sourceRemainingWork)
			throw new IllegalArgumentException("Custom MSPDI work intervals cannot be mapped to assignment calendar: actual="
				+ mappedActual + "/" + sourceActualWork + ", remaining="
				+ mappedRemaining + "/" + sourceRemainingWork
				+ ", segments=" + segments);

		// Clear the initial scalar/flat profile first so source gaps become zero-work buckets.
		target.setInterval(Assignment.WORK, plan.start(), plan.finish(), 0L);
		for (WorkSegment segment : segments) {
			target.setInterval(Assignment.WORK, segment.start(), segment.end(), segment.totalWork());
		}
		target.setActualWork(sourceActualWork, null);
		// Task schedules can still be provisional while assignments are imported.
		// Keep this immutable source plan in the per-import state and validate only
		// after the project has completed its final initialize/recalculate pass.
		List<WorkSegment> importedSegments = List.copyOf(segments);
		state.addPostScheduleValidation(() -> validateAppliedCustomWorkPlan(
			target, plan, importedSegments, sourceWork, sourceActualWork, sourceRemainingWork));
	}

	private void validateAppliedCustomWorkPlan(Assignment target, CustomWorkPlan plan, List<WorkSegment> segments,
			long sourceWork, long sourceActualWork, long sourceRemainingWork) {
		long work = target.getWork(null);
		long actual = target.getActualWork(null);
		long remaining = target.getRemainingWork();
		if (work != sourceWork || actual != sourceActualWork || remaining != sourceRemainingWork) {
			throw new IllegalArgumentException("Custom MSPDI work intervals do not preserve assignment totals: "
				+ "work=" + work + "/" + sourceWork
				+ ", actual=" + actual + "/" + sourceActualWork
				+ ", remaining=" + remaining + "/" + sourceRemainingWork
				+ ", bounds=" + plan.start() + ".." + plan.finish()
				+ ", current=" + target.getStart() + ".." + target.getEnd() + "/" + target.getDuration()
				+ ", contour=" + target.getWorkContour().getContourBuckets());
		}
		for (WorkSegment segment : segments) {
			FieldContext interval = new FieldContext();
			interval.setInterval(new ImmutableInterval(segment.start(), segment.end()));
			long segmentActual = target.getActualWork(interval);
			long segmentRemaining = target.getRemainingWork(interval);
			if (!withinRoundingTolerance(segmentActual, segment.actualWork())
					|| !withinRoundingTolerance(segmentRemaining, segment.remainingWork())) {
				throw new IllegalArgumentException("Custom MSPDI work interval cannot be represented by the assignment profile "
					+ "after project scheduling: "
					+ "segment=" + segment + ", actual=" + segmentActual
					+ ", remaining=" + segmentRemaining + ", task=" + target.getTask());
			}
		}
	}

	private boolean withinRoundingTolerance(long actual, long expected) {
		return Math.abs(actual - expected) <= 1L;
	}

	private void validateNoWorkingOverlap(List<WorkInterval> intervals,
		com.microproject.pm.calendar.WorkCalendar calendar, String kind) {
		for (int first = 0; first < intervals.size(); first++) {
			WorkInterval left = intervals.get(first);
			if (left.work() == 0L)
				continue;
			for (int second = first + 1; second < intervals.size(); second++) {
				WorkInterval right = intervals.get(second);
				if (right.work() == 0L)
					continue;
				long overlapStart = Math.max(left.start(), right.start());
				long overlapEnd = Math.min(left.end(), right.end());
				if (overlapEnd > overlapStart && calendar.compare(overlapEnd, overlapStart, false) > 0L)
					throw new IllegalArgumentException("Custom MSPDI " + kind + " work intervals overlap in working time");
			}
		}
	}

	private long segmentWork(List<WorkInterval> intervals, long segmentStart, long segmentEnd,
			com.microproject.pm.calendar.WorkCalendar calendar) {
		long result = 0L;
		for (WorkInterval interval : intervals) {
			long overlapStart = Math.max(segmentStart, interval.start());
			long overlapEnd = Math.min(segmentEnd, interval.end());
			if (overlapEnd <= overlapStart)
				continue;
			long intervalDuration = calendar.compare(interval.end(), interval.start(), false);
			long overlapDuration = calendar.compare(overlapEnd, overlapStart, false);
			if (intervalDuration <= 0L || overlapDuration <= 0L)
				throw new IllegalArgumentException("Custom MSPDI work interval has no working-calendar duration");
			long before = calendar.compare(overlapStart, interval.start(), false);
			long through = calendar.compare(overlapEnd, interval.start(), false);
			long workAtStart = Math.round(interval.work() * (before / (double) intervalDuration));
			long workAtEnd = Math.round(interval.work() * (through / (double) intervalDuration));
			result = Math.addExact(result, workAtEnd - workAtStart);
		}
		return result;
	}

	private record WorkInterval(long start, long end, long work) {
	}

	private record CustomWorkPlan(long start, long finish, List<WorkInterval> actual, List<WorkInterval> remaining) {
		private CustomWorkPlan {
			actual = List.copyOf(actual);
			remaining = List.copyOf(remaining);
		}
	}

	private record WorkSegment(long start, long end, long actualWork, long remainingWork) {
		long totalWork() {
			return Math.addExact(actualWork, remainingWork);
		}
	}

	/**
	 * Restores the leveler-imposed offset after an assignment is attached to its
	 * task. {@link com.microproject.pm.task.NormalTask#addAssignment(Assignment)}
	 * may copy the default assignment's properties while attaching it, so callers
	 * must invoke this again after that operation.
	 */
	public void restoreLevelingDelay(net.sf.mpxj.ResourceAssignment mpxAssignment, Assignment assignment) {
		assignment.setLevelingDelay(toLong(mpxAssignment.getLevelingDelay()));
	}

	/** Imports scalar MSPDI assignment baseline values, preserving absent fields. */
	public void importBaselines(net.sf.mpxj.ResourceAssignment mpxAssignment, Assignment assignment) {
		for (int snapshotId = 0; snapshotId <= 10; snapshotId++) {
			boolean hasBaselineStart = mpxAssignment.getCachedValue(baselineStartField(snapshotId)) != null;
			boolean hasBaselineFinish = mpxAssignment.getCachedValue(baselineFinishField(snapshotId)) != null;
			boolean hasBaselineWork = mpxAssignment.getCachedValue(baselineWorkField(snapshotId)) != null;
			if (!hasBaselineStart && !hasBaselineFinish && !hasBaselineWork)
				continue;
			Date start = snapshotId == 0 ? mpxAssignment.getBaselineStart()
				: mpxAssignment.getBaselineStart(snapshotId);
			Date finish = snapshotId == 0 ? mpxAssignment.getBaselineFinish()
				: mpxAssignment.getBaselineFinish(snapshotId);
			Duration work = snapshotId == 0 ? mpxAssignment.getBaselineWork()
				: mpxAssignment.getBaselineWork(snapshotId);
			assignment.importBaselineValues(snapshotId, start == null ? null : Long.valueOf(toLong(start)),
				finish == null ? null : Long.valueOf(toLong(finish)),
				work == null ? null : Long.valueOf(toLong(work)));
		}
	}

	private static AssignmentField baselineStartField(int slot) {
		return switch (slot) {
			case 0 -> AssignmentField.BASELINE_START;
			case 1 -> AssignmentField.BASELINE1_START;
			case 2 -> AssignmentField.BASELINE2_START;
			case 3 -> AssignmentField.BASELINE3_START;
			case 4 -> AssignmentField.BASELINE4_START;
			case 5 -> AssignmentField.BASELINE5_START;
			case 6 -> AssignmentField.BASELINE6_START;
			case 7 -> AssignmentField.BASELINE7_START;
			case 8 -> AssignmentField.BASELINE8_START;
			case 9 -> AssignmentField.BASELINE9_START;
			case 10 -> AssignmentField.BASELINE10_START;
			default -> throw new IllegalArgumentException("Invalid baseline slot: " + slot);
		};
	}

	private static AssignmentField baselineFinishField(int slot) {
		return switch (slot) {
			case 0 -> AssignmentField.BASELINE_FINISH;
			case 1 -> AssignmentField.BASELINE1_FINISH;
			case 2 -> AssignmentField.BASELINE2_FINISH;
			case 3 -> AssignmentField.BASELINE3_FINISH;
			case 4 -> AssignmentField.BASELINE4_FINISH;
			case 5 -> AssignmentField.BASELINE5_FINISH;
			case 6 -> AssignmentField.BASELINE6_FINISH;
			case 7 -> AssignmentField.BASELINE7_FINISH;
			case 8 -> AssignmentField.BASELINE8_FINISH;
			case 9 -> AssignmentField.BASELINE9_FINISH;
			case 10 -> AssignmentField.BASELINE10_FINISH;
			default -> throw new IllegalArgumentException("Invalid baseline slot: " + slot);
		};
	}

	private static AssignmentField baselineWorkField(int slot) {
		return switch (slot) {
			case 0 -> AssignmentField.BASELINE_WORK;
			case 1 -> AssignmentField.BASELINE1_WORK;
			case 2 -> AssignmentField.BASELINE2_WORK;
			case 3 -> AssignmentField.BASELINE3_WORK;
			case 4 -> AssignmentField.BASELINE4_WORK;
			case 5 -> AssignmentField.BASELINE5_WORK;
			case 6 -> AssignmentField.BASELINE6_WORK;
			case 7 -> AssignmentField.BASELINE7_WORK;
			case 8 -> AssignmentField.BASELINE8_WORK;
			case 9 -> AssignmentField.BASELINE9_WORK;
			case 10 -> AssignmentField.BASELINE10_WORK;
			default -> throw new IllegalArgumentException("Invalid baseline slot: " + slot);
		};
	}

	private static Resource resolveResource(net.sf.mpxj.ResourceAssignment mpxAssignment, MpxImportState state) {
		Integer resourceUniqueID = mpxAssignment.getResourceUniqueID();
		if (resourceUniqueID == null || resourceUniqueID.intValue() == EnterpriseResource.UNASSIGNED_ID)
			return ResourceImpl.getUnassignedInstance();
		net.sf.mpxj.Resource mpxResource = mpxAssignment.getResource();
		return mpxResource != null ? state.getResource(mpxResource) : null;
	}

	private static long toLong(Date d) {
		if (d == null)
			return 0L;
		return TimeUtil.addTimeZoneOffset(d.getTime());
	}

	private static long toLong(net.sf.mpxj.Duration d) {
		return MpxUtils.toMillis(d);
	}
}
