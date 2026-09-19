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
package com.microproject.grouping.core.summaries;

import com.microproject.algorithm.DoubleValue;
import com.microproject.grouping.core.Node;


public abstract class DivisionSummaryVisitor extends NumberSummaryVisitor implements DoubleValue {
	protected double denominator = 0;
	private boolean nodeBased = true;

	public DivisionSummaryVisitor(boolean nodeBased) {
		this.nodeBased = nodeBased;
	}
	public void accept(Object node) {
	    Object nodeImpl= nodeBased ? ((Node)node).getImpl() : node;
		summary += getNumerator(nodeImpl);
		denominator += getDenominator(nodeImpl);
	}
	public double getValue() {
		if (denominator == 0)
			return 0;
		return summary / denominator;
	}
	public abstract double getNumerator(Object arg0);
	public abstract double getDenominator(Object arg0);

	
	public void reset() {
		super.reset();
		denominator = 0;
	}
	public Object getSummary() {
		return Double.valueOf(getValue());
	}	
	public void addToSummary(Object value) { // not used
	}

}
