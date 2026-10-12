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

import net.sf.mpxj.Duration;
import net.sf.mpxj.AssignmentField;
import com.microproject.core.time.TimeUtil;
import com.microproject.exchange.mpxj.MpxjApi;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Task;

/**
 * Converts an MPXJ ResourceAssignment into a microproject Assignment.
 * Only fields carried by the microproject Assignment model are mapped. Standard
 * work-contour kinds and scalar baseline values are imported; timephased bucket
 * distributions are not yet represented by this exchange path (see issue #154).
 * @author Laurent Chretienneau
 */
public class MpxAssignmentConverter {

	public void from(net.sf.mpxj.ResourceAssignment mpxAssignment, Assignment assignment, MpxImportState state, Task task, int snapshotId) {
		Resource resource = resolveResource(mpxAssignment, state);
		if (resource == null) {
			throw new IllegalStateException("Unable to resolve resource for assignment " + mpxAssignment.getResourceUniqueID());
		}
		int workContourType = MpxjApi.workContourId(mpxAssignment);
		assignment.setTaskAndResource(task, resource);
		// MSPDI stores assignment units as a percentage (100 = 1.0 in the
		// microProject model). Restore this explicitly before work is applied;
		// otherwise a round trip can leave the assignment at the default 0 units.
		if (mpxAssignment.getUnits() != null)
			assignment.forceUnits(mpxAssignment.getUnits().doubleValue() / 100.0D);
		// Import current schedule values here; scalar baseline slots are imported
		// separately by importBaselines after this current assignment is attached.
		long assignmentStart = toLong(mpxAssignment.getStart());
		// AssignmentDetail stores a start as a delay relative to the task. During
		// import the task's schedule can still be uninitialised, so feeding the
		// identical task date through setStart turns it into an epoch-sized delay.
		if (assignmentStart != 0L && assignmentStart != task.getStart())
			assignment.setStart(assignmentStart);
		if (mpxAssignment.getFinish() != null)
			assignment.setEnd(toLong(mpxAssignment.getFinish()));
		// Install the source profile before applying Work. A standard contour's
		// inverse maps the scalar to the assignment span; applying Work as flat
		// first would shrink the span and reinterpret ActualWork later.
		assignment.setWorkContourType(workContourType);
		assignment.setWork(toLong(mpxAssignment.getWork()), null);
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
		long sourceRemainingWork = toLong(mpxAssignment.getRemainingWork());
		assignment.setActualWork(toLong(mpxAssignment.getActualWork()), null);
		assignment.setRemainingWork(sourceRemainingWork, null);
		// WorkContour changes how the imported assignment span distributes work.
		// Setting Work may resize that span, so restore explicit XML assignment
		// bounds after all work/progress fields have been applied.
		if (mpxAssignment.getStart() != null)
			assignment.setStart(toLong(mpxAssignment.getStart()));
		if (mpxAssignment.getFinish() != null)
			assignment.setEnd(toLong(mpxAssignment.getFinish()));
		// Apply the source contour after progress setters, which may intentionally
		// convert a standard profile to a personal one. Do not call setWork again:
		// that setter represents a user scheduling edit and may convert an imported
		// standard contour into a personal profile when actuals are present.
		assignment.setWorkContourType(workContourType);
		restoreLevelingDelay(mpxAssignment, assignment);
		if (mpxAssignment.getUniqueID() != null)
			assignment.setUniqueId(mpxAssignment.getUniqueID().longValue());
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
