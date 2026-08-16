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
import com.microproject.core.time.TimeUtil;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.EnterpriseResource;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Task;

/**
 * Converts an MPXJ ResourceAssignment into a microproject Assignment.
 * Only fields carried by the microproject Assignment model are mapped; timephased
 * data, baseline snapshots and work contour details are intentionally skipped
 * (see issue #154).
 * @author Laurent Chretienneau
 */
public class MpxAssignmentConverter {

	public void from(net.sf.mpxj.ResourceAssignment mpxAssignment, Assignment assignment, MpxImportState state, Task task, int snapshotId) {
		Resource resource = resolveResource(mpxAssignment, state);
		if (resource == null) {
			throw new IllegalStateException("Unable to resolve resource for assignment " + mpxAssignment.getResourceUniqueID());
		}
		assignment.setTaskAndResource(task, resource);

		// main snapshot only (snapshotId handling for baselines is skipped, see #154)
		assignment.setStart(toLong(mpxAssignment.getStart()));
		assignment.setEnd(toLong(mpxAssignment.getFinish()));
		assignment.setWork(toLong(mpxAssignment.getWork()), null);
		assignment.setPercentComplete(mpxAssignment.getPercentageWorkComplete() == null
				? 0.0 : mpxAssignment.getPercentageWorkComplete().doubleValue() / 100.0);
		assignment.setActualStart(toLong(mpxAssignment.getActualStart()));
		assignment.setActualFinish(toLong(mpxAssignment.getActualFinish()));
		assignment.setActualWork(toLong(mpxAssignment.getActualWork()), null);
		assignment.setRemainingWork(toLong(mpxAssignment.getRemainingWork()), null);
		if (mpxAssignment.getUniqueID() != null)
			assignment.setUniqueId(mpxAssignment.getUniqueID().longValue());
		// work contour: default to flat (0) for import; contour details skipped (#154)
		assignment.setWorkContourType(0);
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
