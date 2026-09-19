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

import com.microproject.algorithm.CalculationVisitor;
import com.microproject.algorithm.DoubleValue;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.pm.calendar.WorkCalendar;

/**
 *
 */
public abstract class AssignmentFieldFunctor implements CalculationVisitor, DoubleValue {
	protected Assignment assignment;
	protected WorkCalendar workCalendar = null;
	protected ContourBucketIntervalGenerator contourBucketIntervalGenerator;
	protected boolean cumulative = false;
	private double multiplier = 1.0D;
	
	public final void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}
	AssignmentFieldFunctor() {}
	AssignmentFieldFunctor(Assignment assignment, WorkCalendar workCalendar, ContourBucketIntervalGenerator contourBucketIntervalGenerator) {
		this.assignment = assignment;
		this.workCalendar = workCalendar;
		this.contourBucketIntervalGenerator = contourBucketIntervalGenerator;
	}
	protected double value = 0.0;
	/**
	 * @return Returns the value.
	 */
	public double getValue() {
		return value * multiplier;
	}

	public void initialize() {
		if (!cumulative)
			reset();
	}
	public void reset() {
		value = 0.0;
	}
	public boolean isCumulative() {
		return cumulative;
	}

	public void setCumulative(boolean cumulative) {
		this.cumulative = cumulative;
	}
	
	public AssignmentFieldFunctor cum() {
		setCumulative(true);
		return this;
	}

	public String toString() {
		return "" + value;
	}
	/**
	 * @return Returns the workCalendar.
	 */
	public WorkCalendar getWorkCalendar() {
		return workCalendar;
	}
	public Assignment getAssignment() {
		return assignment;
	}
	public final ContourBucketIntervalGenerator getContourBucketIntervalGenerator() {
		return contourBucketIntervalGenerator;
	}

}
