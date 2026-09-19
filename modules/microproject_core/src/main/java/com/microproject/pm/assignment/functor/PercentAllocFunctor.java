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
 * 
 */
public class PercentAllocFunctor extends AssignmentFieldFunctor {
	private long work = 0;
	double maximumUnits = Double.MAX_VALUE;
	private PercentAllocFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator, boolean threshold) {
		super(assignment,workCalendar, contourBucketIntervalGenerator);
		if (threshold)
			maximumUnits = assignment.getResource().getMaximumUnits();
	}
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
		AbstractContourBucket bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
		if (bucket != null) {		
			long bitOfWork = workCalendar.compare(interval.getEnd(),interval.getStart(), false);
			work += bitOfWork;
			value += bucket.getEffectiveUnits(assignment.getUnits()) * bitOfWork;
		}
	}
	
	public double getValue() {
		if (work == 0)
			return 0.0;
		return Math.min(value / work, maximumUnits);
	}
	
	public void initialize() {
		super.initialize();
		work = 0;
	}
	public static PercentAllocFunctor getInstance(com.microproject.pm.assignment.Assignment assignment, com.microproject.pm.calendar.WorkCalendar workCalendar, com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator contourBucketIntervalGenerator, boolean threshold) {
		return new PercentAllocFunctor(assignment, workCalendar, contourBucketIntervalGenerator,threshold);
	}	
}

