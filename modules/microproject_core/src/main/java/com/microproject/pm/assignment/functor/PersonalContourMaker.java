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

import java.util.ArrayList;
import java.util.Collection;

import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.assignment.contour.PersonalContourBucket;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * This functor adds buckets to a collection.  If an entire bucket is used, it is added as is, otherwise a new one is made.
 */
public class PersonalContourMaker extends AssignmentFieldFunctor{
	private ArrayList list = new ArrayList();
	long assignmentDuration;
	double multiplier = 1.0D;
	
	public static PersonalContourMaker getInstance(Assignment assignment, ContourBucketIntervalGenerator contourBucketIntervalGenerator) {
		return new PersonalContourMaker(assignment, contourBucketIntervalGenerator);
	}

	private PersonalContourMaker(Assignment assignment,ContourBucketIntervalGenerator contourBucketIntervalGenerator) {
		super(assignment, assignment.getEffectiveWorkCalendar(), contourBucketIntervalGenerator);
		assignmentDuration = assignment.getDurationMillis();
		if (!assignment.getWorkContour().isPersonal())
			multiplier = assignment.getUnits();
		
	}

	/**
	 * Add buckets to the collection.  The new interval has priority over the existing contour.  Buckets
	 * are re-used if they are identical.
	 */
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
		AbstractContourBucket bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
		if (bucket == null)
			return;
		if (bucket instanceof PersonalContourBucket)
			list.add(bucket);
		else
			list.add(PersonalContourBucket.getInstance(bucket.getBucketDuration(assignmentDuration),bucket.getUnits() * multiplier));
	}
	
	public Collection getList() {
		return list;
	}
}
