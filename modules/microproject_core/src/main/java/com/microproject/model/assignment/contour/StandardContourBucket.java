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

/**
 * A standard contour segment stores a units value over a fraction of an assignment's duration.
 * Its {@code fractionOfDuration} field is part of the serialized contour shape.
 */
public final class StandardContourBucket extends AbstractContourBucket {
	private static final long serialVersionUID = 7171775892861366292L;

	private double fractionOfDuration = 1.0;
	
	/**
	 * @return Returns the fractionOfDuration.
	 */
	public double getFractionOfDuration() {
		return fractionOfDuration;
	}

	/** Returns this segment's share of the supplied assignment duration. */
	public long getBucketDuration(long assignmentDuration) {
		return (long) (assignmentDuration * getFractionOfDuration());
	}
	/**
	 * Returns the contribution of this segment to the contour's weighted total.
	 * @return units multiplied by this segment's duration fraction
	 */
	public double weightedSum() {
		return units * fractionOfDuration;
	}
	

	/**
	 * Construct a contour bucket
	 * @param units - value to apply for this bucket
	 * @param fractionOfDuration - fraction of total duration that this bucket represents
	 */
	StandardContourBucket(double units, double fractionOfDuration) {
		this.units = units;
		this.fractionOfDuration = fractionOfDuration;
	}
	
	public Object clone() {
			return super.clone();
	}


}
