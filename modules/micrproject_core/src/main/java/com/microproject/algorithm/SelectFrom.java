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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.FalsePredicate;
import org.apache.commons.collections.functors.TruePredicate;

import com.microproject.pm.time.HasStartAndEnd;

/**
 * The part of a query that will apply visitors over generators
 */
public class SelectFrom implements HasStartAndEnd {
	long start;
	long end;
	boolean finished = false;
	private IntervalGenerator generator = null;	
	boolean mustProcessAll = false;
	Consumer<Object> fieldVisitors = null;
	CalculationVisitor[] fieldVisitorArray = null;
	Predicate wherePredicate = TruePredicate.INSTANCE;
	
	public static LinkedList<SelectFrom> selectFromListInstance() {
		return new LinkedList<>();	
	}

	public static LinkedList<SelectFrom> listInstance(SelectFrom a) {
		LinkedList<SelectFrom> list = new LinkedList<>();
		list.add(a);
		return list;
	}

	public static LinkedList<SelectFrom> listInstance(SelectFrom a, SelectFrom b) {
		LinkedList<SelectFrom> list = new LinkedList<>();
		list.add(a);
		list.add(b);		
		return list;
	}

	public static LinkedList<SelectFrom> listInstance(SelectFrom a, SelectFrom b, SelectFrom c) {
		LinkedList<SelectFrom> list = new LinkedList<>();
		list.add(a);
		list.add(b);		
		list.add(c);		
		return list;
	}

	public static LinkedList<SelectFrom> listInstance(SelectFrom a, SelectFrom b, SelectFrom c, SelectFrom d) {
		LinkedList<SelectFrom> list = new LinkedList<>();
		list.add(a);
		list.add(b);		
		list.add(c);
		list.add(d);				
		return list;
	}
	
	protected IntervalGeneratorSet fromGenerators = null;	
	
	public Collection<IntervalGenerator> getFromIntervalGenerators() {
		return fromGenerators == null ? Collections.emptyList() : fromGenerators.getGenerators();
	}
	
	public SelectFrom select(CalculationVisitor[] fieldVisitorArray) {
		this.fieldVisitorArray = fieldVisitorArray;
		this.fieldVisitors = (fieldVisitorArray == null) ? null
				: obj -> { for (CalculationVisitor v : fieldVisitorArray) v.accept(obj); };
		return this;
	}
	
	public SelectFrom select(CalculationVisitor fieldVisitor) {
		if (fieldVisitorArray != null) { // if already something there, add to it
			CalculationVisitor[] newArray =new CalculationVisitor[fieldVisitorArray.length +1];
			System.arraycopy(fieldVisitorArray,0,newArray,0,fieldVisitorArray.length);
			newArray[fieldVisitorArray.length] = fieldVisitor; // add new one to end
			return select(newArray);
		}
		fieldVisitorArray = new CalculationVisitor[] {fieldVisitor}; //make one element array
		this.fieldVisitors = fieldVisitor; // no need to make chained closure since only one element
		return this;
	}
	
	public SelectFrom all() {
		mustProcessAll = true; // must be set after from generators are set!
		return this;
	}
	
	public SelectFrom from(List<IntervalGenerator> fromGeneratorList) {
		if (fromGenerators == null) 
			this.fromGenerators = IntervalGeneratorSet.getInstance(fromGeneratorList);
		else
			this.fromGenerators.getGenerators().addAll(fromGeneratorList);
		return this;
	}
	

	public SelectFrom from(IntervalGenerator fromGenerator) {
		if (fromGenerators == null) 
			this.fromGenerators = IntervalGeneratorSet.getInstance(fromGenerator);
		else
			fromGenerators.getGenerators().add(fromGenerator);
		return this;
	}

	public SelectFrom where(Predicate wherePredicate) {
		this.wherePredicate = wherePredicate;
		return this;
	}
	
	public SelectFrom whereInRange(long start, long end) {
		if (start <= end) { // if non backwards range 
			// If there is already a range, intersect with it
			if (wherePredicate != null && wherePredicate instanceof DateInRangePredicate) {
				DateInRangePredicate range = (DateInRangePredicate)wherePredicate;
				range.limitTo(start,end);
				start = range.getStart();
				end = range.getEnd();
			} else {
				wherePredicate = DateInRangePredicate.getInstance(start,end);
			}
			from(RangeIntervalGenerator.betweenInstance(start,end)); // add a generator assuring the endpoints are treated corrctly
		} else { // take care in cases where range is invalid
			wherePredicate = FalsePredicate.INSTANCE;
		}

		return this;
	}
	
	/**
	 * Initializes all calculation totals for active field visitors.  This will set all non-cumulative ones to 0s
	 * Cumulative ones are not initialized
	 *
	 */
	public void initializeCalculations() {
		if (fieldVisitorArray == null)
			return;
		for (int i =0; i < fieldVisitorArray.length; i++ )
			fieldVisitorArray[i].initialize();
	}
	
	/**
	 * Put fields back to their 0 state. This is used when the clause is used up.  Cumulative fields as well.
	 *
	 */	
	public void resetCalculations() {
		if (fieldVisitorArray == null)
			return;
		for (int i =0; i < fieldVisitorArray.length; i++ )
			fieldVisitorArray[i].reset();
	}
	
	/**
	 * Calculate values in a range of times by calling each visitor on subranges until the range is complete.
	 * @param groupByStart start of calculation range.  currently unused!
	 * @param groupByEnd end of calculation range
	 * @return true if all of the from generators are still active, false if one of them has been used up.
	 */
	public boolean calculate(long groupByStart, long groupByEnd) {
		if (fromGenerators == null || fromGenerators.getGenerators().isEmpty()) {
			finished = true;
			resetCalculations();
			return false;
		}
		if (finished) {// if the last item of a generator was processed in previous call
			resetCalculations(); // since it is no longer active, should always return 0s from now on
			return false;
		}
		while (true) {
			if (generator == null) // will be null on first call, and after the previously active generator has been evaluated
				generator = fromGenerators.earliestEndingGenerator();

			if (generator == null) {
				finished = true;
				break;
			}

			start = Math.max(start,generator.currentStart()); // if current generator was interrupted by ending a range, we need to start at point left off
			end = Math.min(groupByEnd,generator.currentEnd());
			if (end >= start) { // in cases where a clause starts in the middle, such as remaining work, end may be less than start at first
//	System.out.println("SelectFrom start" + new java.util.Date(start) + " end " + new java.util.Date(end) + " " + generator);			
				// evaluate fields
				boolean whereConditionMet = wherePredicate.evaluate(this);
				if (fieldVisitors != null) { 
					for (int i = 0; i < fieldVisitorArray.length; i++) {
						// if we are in the calculation range, or if the functor is cumulative
						if (whereConditionMet || fieldVisitorArray[i].isCumulative()) {
							fieldVisitorArray[i].accept(this);
						}
					}
				}
			}
			start = end; // for next iteration, shift start to current end
			
			if (end == groupByEnd) // at end of groupBy. 
				break;

			if (!generator.evaluate(this)) {
				if (mustProcessAll) { // if all froms must be treated
					fromGenerators.remove(generator);
					finished = fromGenerators.isEmpty(); // any left?
				} else {
					finished = true; // The next time calculate is called, it should return false
				}
				if (finished)
					break;
			}
			generator = null; // The current generator has been finished.  Will need to find earliest next time
		}
		return true;
	}
	
	/**
	 * 
	 */
	private SelectFrom() {
		super();
	}

	/**
	 * Factory method
	 * @return
	 */
	public static SelectFrom getInstance() {
		return new SelectFrom();
	}
	/**
	 * @return Returns the end.
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return Returns the start.
	 */
	public long getStart() {
		return start;
	}

	public String toString() {
		return "Select From where is " + wherePredicate;
	}
	public static final SelectFrom[] NOTHING = new SelectFrom[] {};
	
}
