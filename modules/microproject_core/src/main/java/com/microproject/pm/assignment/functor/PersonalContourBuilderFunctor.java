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

import java.util.Collection;

import com.microproject.algorithm.CollectionIntervalGenerator;
import com.microproject.algorithm.IntervalValue;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.AbstractContourBucket;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.assignment.contour.PersonalContourBucket;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * This functor adds buckets to a collection.  If an entire bucket is used, it is added as is, otherwise a new one is made.
 */
public class PersonalContourBuilderFunctor extends AssignmentFieldFunctor{
	private Collection collection;
	private long assignmentWork;
	private CollectionIntervalGenerator replacementGenerator;
	private long activeDate = 0;
	AbstractContourBucket previous = null;
	public static PersonalContourBuilderFunctor getInstance(Assignment assignment, WorkCalendar workCalendar, com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator contourBucketIntervalGenerator, CollectionIntervalGenerator replacementGenerator, Collection collection) {
		return new PersonalContourBuilderFunctor(assignment, workCalendar, contourBucketIntervalGenerator, replacementGenerator, collection);
	}

	private PersonalContourBuilderFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator, CollectionIntervalGenerator replacementGenerator, Collection collection) {
		super(assignment, workCalendar, contourBucketIntervalGenerator);
		assignmentWork = assignment.calcWork();
		this.replacementGenerator = replacementGenerator;
		this.collection = collection;
	}

	/**
	 * Add buckets to the collection.  The new interval has priority over the existing contour.  Buckets
	 * are re-used if they are identical.
	 */
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
		
		if (interval.getStart() == 0) // ignore degenerate range 
			return;
		if (interval.getStart() == interval.getEnd())
			return;

		AbstractContourBucket bucket = null;
		long intervalDuration = 0;
		// if beginning a replacement interval
		if (replacementGenerator.isCurrentActive() && replacementGenerator.currentStart() == interval.getStart()) {
			intervalDuration = workCalendar.compare(replacementGenerator.getEnd(),replacementGenerator.getStart(), false); // get duration of new region
			
			// if inserting during a non-working time, need to adjust assignment calendar
			if (intervalDuration == 0) {
				assignment.addCalendarTime(interval.getStart(),interval.getEnd());
			}

			//need to shift start to make room for new ones 
			if (interval.getStart() < assignment.getStart()) {
				assignment.setStart(interval.getStart());
			}
			
			IntervalValue replacementIntervalValue = (IntervalValue)replacementGenerator.current();
			bucket = PersonalContourBucket.getInstance(intervalDuration,replacementIntervalValue.getValue()); // make a new bucket
			activeDate = replacementGenerator.currentEnd(); // ignore everything in the future until active date
	
		} else if (interval.getStart() >= activeDate) { // use contour bucket
			intervalDuration = workCalendar.compare(interval.getEnd(),interval.getStart(), false);
			if (intervalDuration == 0) // don't treat degenerate cased
				return;
			if (contourBucketIntervalGenerator.current() == null) { // if not active, then insert dead time
				bucket =PersonalContourBucket.getInstance(intervalDuration,0); // make a new non-workingbucket
			} else {
				bucket = (AbstractContourBucket) contourBucketIntervalGenerator.current();
				if (intervalDuration != bucket.getBucketDuration(assignmentWork)) // try to use existing bucket 
					bucket = PersonalContourBucket.getInstance(intervalDuration,bucket.getUnits()); // make a new bucket
				
			}
		}
		if (bucket == null) // if no bucket, then do nothing
			return;

		// merge with previous if units are identical
		if (previous != null && previous.getUnits() == bucket.getUnits()) {
			collection.remove(previous);
			bucket = PersonalContourBucket.getInstance(bucket.getBucketDuration(assignmentWork) + previous.getBucketDuration(assignmentWork),previous.getUnits());
		}

		collection.add(bucket);
		previous = bucket; // for merge
	}
}
