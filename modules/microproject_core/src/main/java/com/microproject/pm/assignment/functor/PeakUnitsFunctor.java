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

/**
 *
 */
public class PeakUnitsFunctor extends AssignmentFieldFunctor {
	public static PeakUnitsFunctor getInstance(com.microproject.pm.assignment.Assignment assignment, com.microproject.pm.calendar.WorkCalendar workCalendar, com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator contourBucketIntervalGenerator) {
		return new PeakUnitsFunctor(assignment, workCalendar, contourBucketIntervalGenerator);
	}
	private PeakUnitsFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator) {
		super(assignment,workCalendar, contourBucketIntervalGenerator);
	}
	public void accept(Object object) {
		AbstractContourBucket bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
		if (bucket != null) {
			value = Math.max(value,bucket.getEffectiveUnits(assignment.getUnits()));
		}
	}
	
	
}

