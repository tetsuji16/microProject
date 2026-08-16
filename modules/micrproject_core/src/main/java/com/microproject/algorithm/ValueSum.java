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
package com.microproject.algorithm;

/**
 * A calculation visitor that sums up values
 */
public class ValueSum implements CalculationVisitor, DoubleValue {
	double value = 0.0;
	DoubleValue first;
	DoubleValue second;
	private ValueSum(DoubleValue first, DoubleValue second) {
		super();
		this.first = first;
		this.second = second;

	}
	
	/**
	 * Factory method
	 * @param first
	 * @param second
	 * @return
	 */
	public static ValueSum getInstance(DoubleValue first, DoubleValue second) {
		return new ValueSum(first,second);
	}

	public void initialize() {
		value = 0.0;

	}

	public void accept(Object arg0) {
		value = first.getValue() + second.getValue();

	}

	/**
	 * @return Returns the value.
	 */
	public double getValue() {
		return value;
	}
	
	public String toString() {
		return Double.toString(value);
	}

	public boolean isCumulative() {
		return false;
	}

	public void reset() {
		initialize();
	}

}
