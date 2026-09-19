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
import com.microproject.pm.time.HasStartAndEnd;

/**
 * Will determine the date when a value is achieved.  The process is rather tricky since there are fixed costs
 * to take into account.  This fuctor is used when calculating reverse queries.
 */
public class DateAtValueFunctor implements CalculationVisitor{
	AssignmentFieldClosureCollection childList;
	double subtotal;
	double value;
	long date;
	
	public static DateAtValueFunctor getInstance(double value, com.microproject.pm.assignment.functor.AssignmentFieldClosureCollection childList) {
		return new DateAtValueFunctor(value, childList);
	}


	/**
	 * Constructor 
	 */
	private DateAtValueFunctor(double value, AssignmentFieldClosureCollection childList) {
		super();
		reset();
		this.value = value;
		this.childList = childList;
	}
	
	
	public void initialize() {
		// no need to reset anything
	}
	
	public void reset() {
		subtotal = 0;
		value = 0;
		date = 0;
	}
	/**
	 * Increment the subtotal by adding up all child functors.  If the value is achieved, calculate the
	 * instant in the range at which it occurs.
	 */
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd) object;

		double sum = childList.getValue();
		subtotal += sum;
		if (date == 0 && subtotal >= value) {
			if (value == 0.0) { // take care of degenerate case
				date = interval.getStart();
				return;
			}
			// if just an instant but the instant has costs that put it over the top, return the instant
			if (interval.getStart() == interval.getEnd()) {
				date = interval.getStart();
				return;
			}
  				
			double fixedSum = childList.getFixedValue(); // get fixed only
			if (subtotal + fixedSum - sum >= value) { // if the fixed cost alone puts it over
				date = interval.getStart();
				return;
			}

			// figure out the date using a prorated amount of variable cost
			sum -= fixedSum; // remove any fixed sum
			double fractionOfDuration = (sum - (subtotal - value)) / sum;
			
			AssignmentFieldFunctor aNonZeroFunctor = childList.getANonZeroFunctor();
			
			long duration = aNonZeroFunctor.getWorkCalendar().compare(interval.getEnd(),interval.getStart(), false);			
			date = aNonZeroFunctor.getWorkCalendar().add(interval.getStart(),(long) (duration * fractionOfDuration),true);
		}
		
	}
	public long getDate() {
		return date;
	}


	public boolean isCumulative() {
		return false;
	}
}
