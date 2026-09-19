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

import java.io.Serializable;

import com.microproject.configuration.CalculationPreference;
/**
 * @stereotype mi-detail
 * 
 * An allocation biclet represents the finest grained detail for an assignment.  The amount of work is determined
 * by a value of effort during a duration.
 * I do not store the absolute time value of the start/end because the allocation bucket can be shifted.
 * This class is immutable.
 * 
 * The basic formula Work = Units * Duration applies.  See class SchedulingRule
 * 
 */

public class PersonalContourBucket extends AbstractContourBucket implements Serializable{
	static final long serialVersionUID = 99779271628737L;
    private long duration = 0;

	/**
	 * @return Returns the duration.
	 */
	public long getDuration() {
		return duration;
	}

	public long getBucketDuration(long assignmentDuration) {
		return duration;
	}
	
	protected PersonalContourBucket(long duration, double units) {
		this.duration = duration;
		this.units = units;
	}
	
	public static PersonalContourBucket getInstance(long duration, double units) {
		if (duration < 0)
			throw new IllegalArgumentException("Negative contour bucket duration: " + duration);
		return new PersonalContourBucket(duration,units);
	}
	private PersonalContourBucket(AbstractContourBucket standard, long assignmentDuration) {
		this.duration = standard.getBucketDuration(assignmentDuration);
		this.units = standard.getUnits();
	}
	public double getEffectiveUnits(double assignmentUnits) {
		return units;
	}	
	/**
	 * Copy constructor. Class is immutable
	 * @param from
	 */	
	public PersonalContourBucket(PersonalContourBucket from) {
		this(from.duration, from.units);
	}

	/**
	 * @return Returns the work.
	 */
	public long calcWork() {
		return (long) (units * duration);
	}

	
	public String toString() {
		return "[duration] " + (duration / (1000*60*60)) + "h"
		      + "\n[units] " + units;
	}
	
	public double weightedSum() {
		return units * duration;
	}
	
	/**
	 * Returns a new bucket which has its units multiplied by the multiplier
	 * @param multiplier
	 * @return A new bucket.  Objects of this class are immutable
	 */	
	public PersonalContourBucket adjustUnits(double multiplier) {
		return new PersonalContourBucket(duration,units * multiplier);
	}
	
	/**
	 * Returns a new bucket which has its duration multiplied by the multiplier and its units divided by it
	 * @param multiplier
	 * @return A new bucket.  Objects of this class are immutable
	 */
	 public PersonalContourBucket adjustWork(double multiplier) {
		if (!CalculationPreference.getActive().isNonWorkContourPeriodsStayFixedLength() || units != 0) { // in the case where units are 0, don't touch the bucket
			return new PersonalContourBucket((long) (duration * multiplier), units / multiplier);
		} else {
			return this;
		}
	}
	
	/**
	 * Returns a new bucket which has its duration increased decreased by the offset
	 * @param offset (positive to increase duration, negative to decrease it)
	 * @return A new bucket.  Objects of this class are immutable
	 */
	public PersonalContourBucket adjustDuration(long offset) {
		return new PersonalContourBucket(duration + offset, units);
	}

	public static PersonalContourBucket getInstance(AbstractContourBucket standard, long assignmentDuration) {
		return new PersonalContourBucket(standard, assignmentDuration);
	}
	
	public Object clone() {
			return super.clone();
	}

}
