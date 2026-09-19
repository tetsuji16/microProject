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
import com.microproject.pm.time.HasStartAndEnd;

/**
 * Will determine the date when a value is achieved.  The process is rather tricky since there are fixed costs
 * to take into account.  This fuctor is used when calculating reverse queries.
 */
public class ClosureCollectionSum implements CalculationVisitor, DoubleValue {
	AssignmentFieldClosureCollection childList;
	double value;
	double total;
	
	public static ClosureCollectionSum getInstance(AssignmentFieldClosureCollection childList) {
		return new ClosureCollectionSum(childList);
	}


	/**
	 * Constructor 
	 */
	private ClosureCollectionSum( AssignmentFieldClosureCollection childList) {
		super();
		total = 0;
		reset();
		this.childList = childList;
	}
	
	
	public void initialize() {
		reset();
	}
	
	public void reset() {
		value = 0;
	}
	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd) object;
		value = childList.getValue();
		total+= value;
	}


	public boolean isCumulative() {
		return false;
	}
	public double getValue() {
		return childList.getValue();
	}
	public double getTotal() {
		return total;
	}
	
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return ""+getValue() + " " + getTotal();
	}
	
}
