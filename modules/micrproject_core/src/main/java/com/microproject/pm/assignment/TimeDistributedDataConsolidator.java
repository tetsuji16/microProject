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
import java.util.Iterator;

import com.microproject.pm.costing.EarnedValueValues;

/**
 *
 */
public class TimeDistributedDataConsolidator {


	public static double acwp(long start, long end, Collection<? extends EarnedValueValues> collection) {
		double result = 0.0;
		Iterator<? extends EarnedValueValues> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().acwp(start,end);
		}
		return result;
	}

	public static double bac(long start, long end, Collection<? extends EarnedValueValues> collection){
		double result = 0.0;
		Iterator<? extends EarnedValueValues> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().bac(start,end);
		}
		return result;
	}

	public static double bcwp(long start, long end, Collection<? extends EarnedValueValues> collection){
		double result = 0.0;
		Iterator<? extends EarnedValueValues> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().bcwp(start,end);
		}
		return result;
	}

	public static double bcws(long start, long end, Collection<? extends EarnedValueValues> collection){
		double result = 0.0;
		Iterator<? extends EarnedValueValues> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().bcws(start,end);
		}
		return result;
	}


	public static double cost(long start, long end, Collection<? extends HasTimeDistributedData> collection){
		double result = 0.0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().cost(start,end);
		}
		return result;
	}

	public static double actualCost(long start, long end, Collection<? extends HasTimeDistributedData> collection){
		double result = 0.0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().actualCost(start,end);
		}
		return result;
	}
	
	public static double actualFixedCost(long start, long end, Collection<? extends HasTimeDistributedData> collection){
		double result = 0.0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().actualFixedCost(start,end);
		}
		return result;
	}	
	public static double fixedCost(long start, long end, Collection<? extends HasTimeDistributedData> collection){
		double result = 0.0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().fixedCost(start,end);
		}
		return result;
	}

	public static long work(long start, long end, Collection<? extends HasTimeDistributedData> collection, boolean laborOnly){
		long result = 0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			HasTimeDistributedData data = i.next();
			if (laborOnly && !data.isLabor())
				continue;
			result += data.work(start,end);
		}
		return result;
	}

	public static long actualWork(long start, long end, Collection<? extends HasTimeDistributedData> collection, boolean laborOnly){
		long result = 0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			HasTimeDistributedData data = i.next();
			if (laborOnly && !data.isLabor())
				continue;
			result += data.actualWork(start,end);
		}
		return result;
	}
	

	public static long remainingWork(long start, long end, Collection<? extends HasTimeDistributedData> collection, boolean laborOnly){
		long result = 0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			HasTimeDistributedData data = i.next();
			if (laborOnly && !data.isLabor())
				continue;
			result += data.remainingWork(start,end);
		}
		return result;
	}
		
	public static double baselineCost(long start, long end, Collection<? extends HasTimeDistributedData> collection){
		long result = 0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			result += i.next().baselineCost(start,end);
		}
		return result;
	}
		
	public static long baselineWork(long start, long end, Collection<? extends HasTimeDistributedData> collection, boolean laborOnly){
		long result = 0;
		Iterator<? extends HasTimeDistributedData> i = collection.iterator();
		while (i.hasNext()) {
			HasTimeDistributedData data = i.next();
			if (laborOnly && !data.isLabor())
				continue;
			result += data.baselineWork(start,end);
		}
		return result;
	}
		
	

}
