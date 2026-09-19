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

import com.microproject.pm.time.HasStartAndEnd;

/**
 * A functor which applies a value at a specific instant.
 * This functor must be used in conjuction with an InstantIntervalGenerator
 */
public class ValueAtInstant extends AssignmentFieldFunctor {
	long triggerDate;
	double constant;
	public static ValueAtInstant getInstance(long triggerDate, double constant) {
		return new ValueAtInstant(triggerDate, constant);
	}

	/**
	 * 
	 */
	public ValueAtInstant() {
		super();
	}

	/**
	 * @param workCalendar
	 * @param contourBucketIntervalGenerator
	 */
	private ValueAtInstant(long triggerDate, double constant) {
		super(null, null, null);
		this.triggerDate = triggerDate;
		this.constant = constant;
	}

	public void accept(Object object) {
		HasStartAndEnd interval = (HasStartAndEnd)object;
	
		if (interval.getStart() == triggerDate && interval.getEnd() == triggerDate) {		
			value = constant;
			constant = 0.0; // reset so not called twice 
		}
	}

}
