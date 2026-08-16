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

import java.util.ArrayList;
import java.util.List;

import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.functor.AssignmentFieldClosureCollection;
import com.microproject.pm.assignment.functor.DateAtValueFunctor;
import com.microproject.pm.time.HasStartAndEnd;

/**
 * A reverse query is one which calculates a date given a value instead of vice-versa.
 */
public class ReverseQuery {
	private Object type;
	private List<Object> fieldSet = new ArrayList<>();
	private List<SelectFrom> selectFromSet = new ArrayList<>();
	private List<IntervalGenerator> groupBySet = new ArrayList<>();
	private HasTimeDistributedData root;
	private double valueToFind;
	private boolean allowDefaultAssignments = false;

	/**
	 * 
	 */
	private ReverseQuery(Object type,HasTimeDistributedData root, double valueToFind) {
		super();
		this.type = type;
		this.root = root;
		this.valueToFind = valueToFind;
	}
	
	public static long getDateAtValue(Object type,HasTimeDistributedData root, double valueToFind, boolean allowDefaultAssignments) {
		ReverseQuery reverseQuery = getInstance(type, root, valueToFind);
		reverseQuery.allowDefaultAssignments = allowDefaultAssignments;
		return reverseQuery.getDate();
	}


	public static ReverseQuery getInstance(Object type, HasTimeDistributedData root, double valueToFind) {
		return new ReverseQuery(type, root, valueToFind);
	}

	/**
	 * @return Returns the type.
	 */
	public Object getType() {
		return type;
	}

	public void addField(Object field) {
		if (field != null)
			fieldSet.add(field);
	}
	
	public void addSelectFrom(SelectFrom selectFrom) {
		if (selectFrom != null)
			selectFromSet.add(selectFrom);
	}
	
	public void addGroupBy(IntervalGenerator groupBy) {
		if (groupBy != null)
			groupBySet.add(groupBy);
	}


	public long getDate() {
		root.buildReverseQuery(this);
		Query query = Query.getInstance();
		DateAtValueFunctor dateAtValue = DateAtValueFunctor.getInstance(valueToFind, AssignmentFieldClosureCollection.getInstance(fieldSet));	
		
		IntervalGenerator interval = null;
		if (!groupBySet.isEmpty())
			interval = IntervalGeneratorSet.getInstance(groupBySet);
		query.selectFrom(selectFromSet)
			 .groupBy(interval)
			 .action(dateAtValue)
			 .execute();
		return dateAtValue.getDate();
	}


//	public static void iterate(Object type,HasTimeDistributedData root) {
//		ReverseQuery reverseQuery = getInstance(type, root, 0);
//		reverseQuery.iterate();
//	}
//
//
//	private void iterate() {
//		root.buildReverseQuery(this);
//		Query query = Query.getInstance();
//		PrintValueFunctor functor = PrintValueFunctor.getInstance(ClosureCollectionSum.getInstance(AssignmentFieldClosureCollection.getInstance(fieldSet)));
//		query.selectFrom(selectFromSet)
//			 .groupBy(IntervalGeneratorSet.getInstance(groupBySet))
//			 .action(functor)
//			 .execute();
//	}
	
	public final boolean isAllowDefaultAssignments() {
		return allowDefaultAssignments;
	}
	public final void setAllowDefaultAssignments(boolean allowDefaultAssignments) {
		this.allowDefaultAssignments = allowDefaultAssignments;
	}
}
