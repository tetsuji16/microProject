/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.assignment.contour;

import java.io.Serializable;
import java.util.Comparator;

import com.microproject.algorithm.DoubleValue;
/**
 * Base for serializable work-contour buckets, ordered by ascending allocation units.
 * The {@code units} field and declared serial version UID are part of the persisted contour shape.
 * {@link #staticCompare(Object, Object)} orders a null value before a non-null bucket.
 */
public abstract class AbstractContourBucket
		implements Comparator<AbstractContourBucket>, DoubleValue, Cloneable, Serializable {
	static final long serialVersionUID = -961372350672424649L;//-1664394457689132915L;
	public abstract long getBucketDuration(long assignmentDuration);
	public abstract double weightedSum();
	protected double units = 1.0;

	/**
	 * @return Returns the units.
	 */
	public double getUnits() {
		return units;
	}
	
	/**
	 * Compares two contour buckets by units, placing {@code null} before any bucket.
	 *
	 * @param arg0 first value; may be {@code null}
	 * @param arg1 second value; may be {@code null}
	 * @return a negative value, zero, or a positive value according to the units order
	 */
	public static int staticCompare(Object arg0, Object arg1) {
		if (arg0 == null)
			return -1;
		if (arg1 == null)
			return 1;		
		return Double.compare(((AbstractContourBucket)arg0).units, ((AbstractContourBucket)arg1).units);
	}
	
	@Override
	public int compare(AbstractContourBucket arg0, AbstractContourBucket arg1) {
		return staticCompare(arg0,arg1);
	}
	
	public double getValue() {
		return units;
	}
	/**
	 * Gets the true weight of this bucket by multiplying by assignment units
	 * @param units2
	 * @return
	 */
	public double getEffectiveUnits(double assignmentUnits) {
		return units * assignmentUnits;
	}
	
	public boolean isFiller() {
		return false;
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	
}
