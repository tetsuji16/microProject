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
package com.microproject.pm.assignment;

import java.util.Collection;
import java.util.function.Consumer;


import com.microproject.algorithm.ReverseQuery;
import com.microproject.pm.calendar.WorkCalendar;

/**
 * Interface for classes having time distributed data
 */
public interface HasTimeDistributedData extends TimeDistributedConstants {
	public static final long NO_VALUE_LONG = 0L;
	public static final double NO_VALUE_DOUBLE = 0.0D;
	
	public void buildReverseQuery(ReverseQuery reverseQuery);	
	public void forEachWorkingInterval(Consumer<Object> visitor, boolean mergeWorking, WorkCalendar workCalendar);
	public double cost(long start, long end);
	public double actualCost(long start, long end);
	public double actualFixedCost(long start, long end);
	public double fixedCost(long start, long end);
	public double baselineCost(long start, long end);
	public long work(long start, long end);
	public long baselineWork(long start, long end);
	public long actualWork(long start, long end);
	public long remainingWork(long start, long end);
	public boolean isLabor();
	public Collection childrenToRollup();
	
	public static Object histogramTypes[] = {
		SELECTED
		,THIS_PROJECT
		,AVAILABILITY
	};

	public static Object reverseHistogramTypes[] = {
		THIS_PROJECT
		,SELECTED
		,AVAILABILITY
	};
	public static Object serverHistogramTypes[] = {
		SELECTED
		,THIS_PROJECT
		,OTHER_PROJECTS
		,AVAILABILITY
	};

	public static Object serverReverseHistogramTypes[] = {
		OTHER_PROJECTS
		,THIS_PROJECT
		,SELECTED
		,AVAILABILITY
	};
	
	public static int tracesCount=3;
	public static int serverTracesCount=4;
	
			

	public static Object workTypes[] = {
			WORK,
			ACTUAL_WORK,
			REMAINING_WORK,
			BASELINE_WORK,
			BASELINE1_WORK,
			BASELINE2_WORK,
			BASELINE3_WORK,
			BASELINE4_WORK,
			BASELINE5_WORK,
			BASELINE6_WORK,
			BASELINE7_WORK,
			BASELINE8_WORK,
			BASELINE9_WORK,
			BASELINE10_WORK
	};
	public static Object costTypes[] = {
			COST,
			ACTUAL_COST,
			FIXED_COST,
			ACTUAL_FIXED_COST,
			REMAINING_COST,
			BASELINE_COST,
			ACWP,
			BCWP,
			BCWS,
			BASELINE1_COST,
			BASELINE2_COST,
			BASELINE3_COST,
			BASELINE4_COST,
			BASELINE5_COST,
			BASELINE6_COST,
			BASELINE7_COST,
			BASELINE8_COST,
			BASELINE9_COST,
			BASELINE10_COST,
	};
	public static Object baselineWorkTypes[] = {
			BASELINE_WORK,
			BASELINE1_WORK,
			BASELINE2_WORK,
			BASELINE3_WORK,
			BASELINE4_WORK,
			BASELINE5_WORK,
			BASELINE6_WORK,
			BASELINE7_WORK,
			BASELINE8_WORK,
			BASELINE9_WORK,
			BASELINE10_WORK
	};
}
