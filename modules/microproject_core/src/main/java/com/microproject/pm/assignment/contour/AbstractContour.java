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

import java.util.ArrayList;
import java.util.LinkedList;

import com.microproject.datatype.DurationFormat;
import com.microproject.pm.time.MutableInterval;

/**
 * Abstract base class for work and cost contours
 * @stereotype strategy 
 */
public abstract class AbstractContour implements Cloneable{
	protected AbstractContourBucket[] contourBuckets = null;	
	protected double maxUnits = 0;
	public abstract int getType();
	public abstract boolean isPersonal();
	public abstract long calcTotalWork(long assignmentDuration);
	public AbstractContour adjustDuration(long newDuration, long actualDuration) {return this;} // only personal contours will treat this
	public AbstractContour adjustUnits(double multiplier, long startingFrom) {return this;} // only personal contours will treat this
	public AbstractContour contourAdjustWork(double multiplier, long actualDuration) {return this;}// only personal contours will treat this
	public AbstractContourBucket[] getContourBuckets() {
		return contourBuckets;
	}
	public abstract String getName();

	public int numBuckets() {
		if (contourBuckets == null)
			return 0;
		return contourBuckets.length;
	}
	
	private long calcSumBucketDuration(long assignmentDuration, boolean excludeNonWorkBuckets) {
		long duration = 0;
		for (int i=0; i < contourBuckets.length; i++) {
			if (contourBuckets[i] == null)
				throw new IllegalStateException("Null contour bucket at index " + i + ": " + toString(assignmentDuration));
			if (!excludeNonWorkBuckets || contourBuckets[i].getUnits() != 0.0) // do not add in durations for time off if excluding nonwork buckets
				duration += contourBuckets[i].getBucketDuration(assignmentDuration);
		}		
		return duration;
	}
	
	public long calcTotalBucketDuration(long assignmentDuration) {
		return calcSumBucketDuration(assignmentDuration,false);
	}
	
	public long calcWorkingBucketDuration(long assignmentDuration) {
		return calcSumBucketDuration(assignmentDuration,true);
	}
	
	protected double calcMaxUnits() {
		double units = 0.0;
		for (int i=0; i < contourBuckets.length; i++) {
			if (contourBuckets[i] != null) // in case called from constructor and array is unitialized
				units = Math.max(units,contourBuckets[i].getUnits());
		}
		return units;
	}
	
	
	/**
	 * Returns an array list containing elements of bucket array 
	 * @return
	 */
	public ArrayList toArrayList() {
		if (contourBuckets == null)
			return null;

		ArrayList list = new ArrayList(contourBuckets.length);
		for (int i=0; i < contourBuckets.length; i++) {
			list.add(contourBuckets[i]);
		}
		return list;
	}
	
	/**
	 * @return Returns the maxUnits.
	 */
	public double getMaxUnits() {
		return maxUnits;
	}

	public AbstractContour(AbstractContourBucket contourBuckets[]) {
		this.contourBuckets = contourBuckets;
		maxUnits = calcMaxUnits();
	}

/**
 * Returns a linked list of buckets that fall between two duration points.  Used when copying planned to actuals.
 * @param start
 * @param end
 * @param assignmentDuration
 * @return
 */
	public LinkedList bucketsBetweenDurations(long start, long end, long assignmentDuration) {
		LinkedList list = new LinkedList();
		AbstractContourBucket bucket = null;
		long currentEnd = 0;
		long currentStart = 0;
		AbstractContourBucket newBucket;
		for (int i=0; i < contourBuckets.length; i++) {
			bucket = contourBuckets[i];
			currentStart = currentEnd;
			currentEnd += bucket.getBucketDuration(assignmentDuration);
			if (currentEnd <= start) // if not at start yet, keep going
				continue;
			if (currentStart >= end) // if past end, stop
				break;
			// Add a new bucket that falls both within this bucket and the between range
			long newBucketDuration = Math.min(end,currentEnd) - Math.max(start,currentStart);
			list.add(PersonalContourBucket.getInstance(newBucketDuration,bucket.getUnits()));
		}
 		return list;
	}

	public String toString(long assignmentDuration) {
		StringBuilder result = new StringBuilder();
		if (contourBuckets == null)
			return null;
		for (int i=0; i < contourBuckets.length; i++) {
			result.append("bucket[" + i + "]");
			if (contourBuckets[i] == null)
				result.append(" NULL!");
			else
				result.append(" duration=" + DurationFormat.format(contourBuckets[i].getBucketDuration(assignmentDuration)) + " units " +  contourBuckets[i].getUnits() +"\n");
		}
		return result.toString();
	}
	
	public String toString() {
		return toString(0);
	}
	/**
	 * @param end
	 * @param extendDuration
	 * @return
	 */
	public abstract AbstractContour extend(long end, long extendDuration);
	/**
	 * @param startOffset
	 * @param extendDuration
	 * @return
	 */
	public abstract AbstractContour extendBefore(long startOffset, long extendDuration);
	
	public abstract MutableInterval getRangeThatIntervalCanBeMoved(long start, long end);	
	public AbstractContour removeFillerAfter(long atDuration) {
		return this;
	}
	
	/**
	 * Remove any starting empty bucket from the contour and return the duration of that bucket
	 * @return
	 */
	public long extractDelay() {
		return 0;
	}
	
	public Object clone() {
		try {
			AbstractContour c=(AbstractContour)super.clone();
			if (contourBuckets!=null){
				c.contourBuckets=new AbstractContourBucket[contourBuckets.length];
				for (int i=0;i<contourBuckets.length;i++){
					c.contourBuckets[i]=(AbstractContourBucket)contourBuckets[i].clone();
				}
			}
			return c;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	public double getLastBucketUnits() {
		return contourBuckets[contourBuckets.length -1].getUnits();
	}


}
