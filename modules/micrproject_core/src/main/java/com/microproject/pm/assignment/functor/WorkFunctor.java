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
package com.microproject.pm.assignment.functor;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * A functor which calculates work (regular, overtime, total)
 */
public class WorkFunctor extends AssignmentFieldOvertimeFunctor {
	long assignmentDuration = 0;
	public static WorkFunctor getInstance(Assignment assignment, WorkCalendar workCalendar,ContourBucketIntervalGenerator contourBucketIntervalGenerator, double overtimeUnits) {
		return new WorkFunctor(assignment, workCalendar, contourBucketIntervalGenerator, overtimeUnits);
	}
	private WorkFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator, double overtimeUnits) {
		super(assignment,workCalendar,contourBucketIntervalGenerator, overtimeUnits);
		if (assignment.getRate().isNonTemporal())
			assignmentDuration = assignment.getDuration();
			
	}
	/**
	 * Calculate regular work, overtime work, and add them to get total work
	 * @param object The SelectFrom from the algorithm
	 */	
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
		AbstractContourBucket bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
		if (bucket != null && bucket.getUnits() != 0) { // neither regular or overtime if contour has 0 units
			double bucketDuration = workCalendar.compare(interval.getEnd(),interval.getStart(), false);
			
			//When we handle overhead, we need to have another interval generator which keeps overhead in sorted order
			// The bucket duration should be multiplied by 1 - overhead.  Code also needs to exist in costFunctor.  maybe others too
			// double overhead = overheadIntervalGenerator.current();
			// bucketDuration *= (1.0 - overhead);
			if (assignmentDuration != 0) {
				bucketDuration /= assignmentDuration; // for unitless
			}

			regularValue += bucket.getEffectiveUnits(assignment.getUnits()) * bucketDuration;
			overtimeValue += overtimeUnits * bucketDuration;
			value = regularValue + overtimeValue;
		}
	}

}

