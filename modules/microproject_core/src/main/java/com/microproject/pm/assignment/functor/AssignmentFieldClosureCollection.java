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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 */
public class AssignmentFieldClosureCollection extends AssignmentFieldFunctor {
	Collection closures;
	List<Consumer<Object>> chain = null;
	AssignmentFieldFunctor aNonZeroFunctor = null;
	
	public static AssignmentFieldClosureCollection getInstance(AssignmentFieldFunctor child) {
		return new AssignmentFieldClosureCollection(child);
	}

	public static AssignmentFieldClosureCollection getInstance(Collection closures) {
		return new AssignmentFieldClosureCollection(closures);
	}

	private AssignmentFieldClosureCollection(AssignmentFieldFunctor child) {
		super();
		closures = new LinkedList();
		closures.add(child);
		chain = new ArrayList<Consumer<Object>>();
		chain.add(child);
	}
	
	private AssignmentFieldClosureCollection(Collection closures) {
		this.closures = closures;
		chain = new ArrayList<Consumer<Object>>(closures.size());
		for (Iterator i = closures.iterator(); i.hasNext();)
			chain.add((Consumer<Object>) i.next());
	}

	
	public void accept(Object arg0) {
		for (Consumer<Object> c : chain)
			c.accept(arg0);
	}

	public void initialize() {
		Iterator i = closures.iterator();
		while (i.hasNext()) {
			((AssignmentFieldFunctor)i.next()).initialize();
		}
	}

	public double getFixedValue() {
		value = 0;
		Iterator i = closures.iterator();
		AssignmentFieldFunctor current;
		while (i.hasNext()) {
			current = (AssignmentFieldFunctor)i.next();
		    if (current instanceof CostFunctor) 
		    	value += ((CostFunctor) current).getFixedValue();
		}
		return value;
	}
	
	public double getValue() {
		value = 0;
		Iterator i = closures.iterator();
		AssignmentFieldFunctor current;
		aNonZeroFunctor = null;
		while (i.hasNext()) {
			current = (AssignmentFieldFunctor)i.next();
			double v = current.getValue();
			if (v != 0)
				aNonZeroFunctor = current; // cache a non zero functor for later use
			value += v;
		}
		return value;
	}
	/**
	 * @return Returns the aNonZeroFunctor.
	 */
	public AssignmentFieldFunctor getANonZeroFunctor() {
		return aNonZeroFunctor;
	}

}
