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

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.Factory;

import com.microproject.pm.time.HasStartAndEnd;


/**
 * This class applies an action visitor closure over an interval and select clauses
 */
public class Query implements Factory, HasStartAndEnd {
	long start;
	long end;
	boolean hasGroupBy = false;

	private List<SelectFrom> selectFromClauses = new LinkedList<>();
	private IntervalGenerator groupByGenerator = null;
	private Consumer<Object> actionVisitor = null;
	private List<IntervalGenerator> executedIntervals = new ArrayList<>();

	/**
	 * The constructor is empty.  The query is built by chaining together parts of statement 
	 */
	private Query() {}

	public static Query getInstance() {
		return new Query();
	}
	
	public Query selectFrom(SelectFrom selectFrom) {
		selectFromClauses.add(selectFrom);
		return this;
	}
	
	public Query selectFrom(List<SelectFrom> selectFromClauses) {
		this.selectFromClauses = selectFromClauses;
		return this;
	}
	
	
	public Query groupBy(IntervalGenerator groupByGenerator) {
		if (groupByGenerator == null)
			return this;
		hasGroupBy = true;
		this.groupByGenerator = groupByGenerator;
		return this;
	}
	
	public Query action(Consumer<Object> actionVisitor) {
		this.actionVisitor = actionVisitor;
		return this;
	}

	public IntervalGenerator[] execute() {
		if (groupByGenerator == null) 
			groupByGenerator = RangeIntervalGenerator.continuous();
		executedIntervals.clear();
		create();
		return executedIntervals.toArray(new IntervalGenerator[0]);
	}


	
	
	
	/**
	 * This is the main calculation function.  It will go thru all elements of the group by generator (if any) and
	 * call back the action visitor.
	 * Eventually, it will be capable of returning a generator which itself can be used in a subsequent query
	 */
	public Object create() {
		SelectFrom clause;
		do {

			// set range of this element
			start = groupByGenerator.currentStart();
			end = groupByGenerator.currentEnd();
			executedIntervals.add(RangeIntervalGenerator.getInstance(start, end));
//			System.out.println("query dates " + new java.util.Date(start) + " - " + new java.util.Date(end));		
			Iterator<SelectFrom> i = selectFromClauses.iterator();
			while (i.hasNext()) { // go thru select from clauses until they are used up
				clause = i.next();
				clause.initializeCalculations();
				if (!clause.calculate(start,end)) // if clause is used up, remove it so it won't be treated again
					i.remove();
			}
			
			if (start != 0L && actionVisitor != null)
				actionVisitor.accept(this);
			

			// in case where there is no specified group by, should stop when no more things to treat
			if (!hasGroupBy && selectFromClauses.isEmpty())
				break;
			
		} while (groupByGenerator.evaluate(this));
		return executedIntervals.toArray(new IntervalGenerator[0]);
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
	/**
	 * @return Returns the groupByGenerator.
	 */
	public IntervalGenerator getGroupByGenerator() {
		return groupByGenerator;
	}
	
	public Object currentGroupByObject() {
		return groupByGenerator.current();
	}

	/**
	 * @return Returns the actionVisitor.
	 */
	public Consumer<Object> getActionVisitor() {
		return actionVisitor;
	}

}

