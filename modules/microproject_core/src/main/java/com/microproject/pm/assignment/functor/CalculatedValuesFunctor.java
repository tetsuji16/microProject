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

import com.microproject.algorithm.DoubleValue;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * 
 */
public class CalculatedValuesFunctor extends AssignmentFieldFunctor {
	AssignmentFieldFunctor child;
	CalculatedValues calculatedValues;
	TimeIteratorGenerator generator;
	boolean mustExecuteChild = false;
	/**
	 * 
	 */
	private CalculatedValuesFunctor(AssignmentFieldFunctor child, CalculatedValues calculatedValues, TimeIteratorGenerator generator) {
		super(child.assignment,child.workCalendar,child.contourBucketIntervalGenerator);
		this.child = child;
		this.calculatedValues = calculatedValues;
		this.generator = generator;
		if (generator == null)
			mustExecuteChild = true;
	}

	public void accept(Object object) {
		if (mustExecuteChild) {
			child.reset();
			child.accept(object);
		}

		HasStartAndEnd interval = (HasStartAndEnd)object;
		
		calculatedValues.set(generator == null ? 0 : generator.getIndex(),
				interval.getStart(),
				interval.getEnd(),
				((DoubleValue)child).getValue(),
				child.getWorkCalendar());
	}

	public static CalculatedValuesFunctor getInstance(AssignmentFieldFunctor child,CalculatedValues calculatedValues, TimeIteratorGenerator generator) {
		return new CalculatedValuesFunctor(child, calculatedValues, generator);
	}

}
