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
package com.microproject.pm.assignment.contour;
import com.microproject.algorithm.IntervalGenerator;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.calendar.WorkCalendar;

/**
 * Generator which goes through a work contour.  Will also treat gap due to a dependency
 */
public class ContourBucketIntervalGenerator implements IntervalGenerator {
	protected final int BEFORE_START = -1; 
	protected final int AFTER_END = -2;
	protected long consumedDuration = 0; // how much used up of planned duration - see DoubleContourBucketIntervalGenerator
	protected int index = BEFORE_START;
	long durationLeftUntilRemainingStartDependency = Long.MAX_VALUE;
	long start = 0;
	long end;
	long assignmentDuration;
	long assignmentActualDuration=0;
	private AbstractContourBucket[] contourBuckets = null;
	WorkCalendar workCalendar;
	long splitAtDuration = Long.MAX_VALUE;
	long splitDuration = 0;
	long remainingSplitBucketDuration = 0;
	AbstractContourBucket specialBucket = null;
	boolean didSplit = false;
	public static ContourBucketIntervalGenerator getInstance(Assignment assignment, Object type) {
		return new ContourBucketIntervalGenerator(assignment,type);		
	}
	
	protected ContourBucketIntervalGenerator(Assignment assignment, Object type) {
		workCalendar = assignment.getEffectiveWorkCalendar();
		contourBuckets = assignment.getContour(type);

		assignmentDuration = assignment.getDurationMillis();
		assignmentActualDuration = assignment.getActualDuration();
		long assignmentStart = assignment.getStart();
		end = assignmentStart; // treat 0th bucket as all time before contour begins
		
		// CalendarDefinition represents reverse-scheduled dates as negative values.
		// Keep the same contour traversal; WorkCalendar.add() reverses the signed
		// timeline, including the split handling below.
		if (assignment.getDependencyStart() > assignmentStart && assignment.getPercentComplete() > 0.0D) { // if a split caused by remaining work being pushed out by a dependency
			durationLeftUntilRemainingStartDependency = workCalendar.compare(assignment.getDependencyStart(),assignmentStart,false);
			if (durationLeftUntilRemainingStartDependency > 0) {
				if (durationLeftUntilRemainingStartDependency > assignmentActualDuration) {
					splitAtDuration = assignmentActualDuration;
					splitDuration = durationLeftUntilRemainingStartDependency - assignmentActualDuration;
				}
			}
		}
	}
	
	public long currentEnd() {
		return end;
	}
	
	public long currentStart() {
		return start;
	}
	
	public Object current() {
		AbstractContourBucket bucket = null;
		if (specialBucket != null)
			bucket = specialBucket;
		else if (index >= 0) // -1 is be
			bucket = contourBuckets[index];
		return bucket;
	}

	public boolean hasNext() {
		return index < contourBuckets.length-1;
	}
	boolean didFirstPart = false;
	public boolean evaluate(Object obj) {
		index++;
		if (index == contourBuckets.length)
			return false;
		start = workCalendar.add(end,0,false); // move to next working time, skipping non calendar time
		specialBucket = null;
		long bucketDuration = contourBuckets[index].getBucketDuration(assignmentDuration);
		consumedDuration += bucketDuration;

		if (consumedDuration >= splitAtDuration) {
			
			remainingSplitBucketDuration = consumedDuration - splitAtDuration; // for latter half of bucket
			
			bucketDuration -= remainingSplitBucketDuration;
			if (bucketDuration > 0) {
				specialBucket = PersonalContourBucket.getInstance(bucketDuration,contourBuckets[index].getUnits());
				index--; // need to repeat the last bucket
			}
			splitAtDuration = Long.MAX_VALUE;// we don't want to treat the start split or this again
			didFirstPart = true;
		} 
		if (specialBucket == null && didFirstPart) {
		
			if (didSplit == false) {
				bucketDuration = splitDuration;
				specialBucket = FillerContourBucket.getInstance(splitDuration);
				index--;
				didSplit = true;
			} else {
				bucketDuration = remainingSplitBucketDuration;
				double units =contourBuckets[index].getUnits();

				if (bucketDuration > 0)	
					specialBucket = PersonalContourBucket.getInstance(remainingSplitBucketDuration,units);
				remainingSplitBucketDuration = 0;
				didFirstPart = false;
			}
		}
		end = workCalendar.add(end,bucketDuration,true);

		return true;
	}

//		start = workCalendar.add(end,0,false); // move to next working time, skipping non calendar time
//		specialBucket = null;
//		long bucketDuration = contourBuckets[index].getBucketDuration(assignmentDuration);
//		consumedDuration += bucketDuration;
//
//		if (consumedDuration > splitAtDuration) {
//			remainingSplitBucketDuration = consumedDuration - splitAtDuration; // for latter half of bucket
//			consumedDuration = splitAtDuration - bucketDuration; // for next pass we want consumed==splitAt
//			
//			bucketDuration -= remainingSplitBucketDuration;
//			
//			specialBucket = PersonalContourBucket.getInstance(bucketDuration,contourBuckets[index].getUnits());
//			index--; // need to repeat the last bucket
//
//		} else if (consumedDuration == splitAtDuration) { // need to do dead time
//			bucketDuration = splitDuration;
//			specialBucket = FillerContourBucket.getInstance(bucketDuration);
//
//			index--;// need to repeat the last bucket
//			splitAtDuration = Long.MAX_VALUE; // we don't want to treat the start split or this again
//		} else if (remainingSplitBucketDuration > 0) {
//			bucketDuration = remainingSplitBucketDuration;
//			double units =contourBuckets[index].getUnits();
//// Turn off this handling.  It seems as if we don't want it after all			
////			if (units == 0) // split occurs during off time - reduce buckets duration to eliminate any off time that occurs during split
////				bucketDuration = Math.max(0,bucketDuration-splitDuration);// need to skip dead time during split duration
//
//			if (bucketDuration > 0)	
//				specialBucket = PersonalContourBucket.getInstance(remainingSplitBucketDuration,units);
//			remainingSplitBucketDuration = 0;
//		}
////		consumedDuration += bucketDuration;
//	
//		end = workCalendar.add(end,bucketDuration,true);
//
//		return true;
//	}

	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return start;
	}

	public boolean isCurrentActive() {
		AbstractContourBucket cur = (AbstractContourBucket) current();
		return (cur != null) && (cur.getUnits() != 0.0);
	}

	public WorkCalendar getWorkCalendar() {
		return workCalendar;
	}

	public boolean canBeShared() {
		return false;
	}

}
