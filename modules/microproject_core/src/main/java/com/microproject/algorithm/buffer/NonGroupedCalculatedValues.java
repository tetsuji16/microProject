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
package com.microproject.algorithm.buffer;

import java.util.TreeMap;
import java.util.Map;
import java.util.logging.Logger;

import com.microproject.options.CalendarOption;
import com.microproject.pm.calendar.WorkCalendar;


/**
 * Stores an array of values as a bunch of ordered values at dates.  
 */
public class NonGroupedCalculatedValues  implements CalculatedValues  {
	private static final Logger logger = Logger.getLogger(NonGroupedCalculatedValues.class.getName());
	private final TreeMap<Long, Double> values = new TreeMap<>();
	private volatile long[] indexedDates;
	private double[] indexedValues;
	double yScale;
	boolean cumulative;
	long origin;
	private static long MILLIS_PER_DAY = CalendarOption.getInstance().getMillisPerDay();
	public NonGroupedCalculatedValues(double yScale, boolean cumulative, long origin) {
		super();
		this.yScale = yScale;
		this.cumulative = cumulative;
		this.origin = origin;
	}
	
	public NonGroupedCalculatedValues(boolean cumulative, long origin) {
		this(1.0D,cumulative,origin);
	}

	public int size() {
		return values.size();
	}

/**
 * Add or modify existing point
 * @param date
 * @param value
 */
	private void setValue(long date, double value) {
		Long longDate = Long.valueOf(date);
		Double v = values.get(longDate);
		if (v != null) // if already present, add to it
			v = Double.valueOf(v.doubleValue() + value);
		else
			v = Double.valueOf(value);
		values.put(longDate,v);
		indexedDates = null;
		indexedValues = null;
	}

/**
 * Here is how ranges are added
 * @param startDate - date value increases
 * @param endDate - date value decreases
 * @param value - amount of increase/decrease
 */	
	public void set(final int index, final long startDate, final long endDate, final double value, final WorkCalendar assignmentCalendar) {
		if (startDate == 0)
			return;
		
		
		if (!cumulative) {
			long duration = 0;
			double v = value;
			if (assignmentCalendar != null) { // can be null in case of value at date where start and end are the same
				duration = assignmentCalendar.compare(endDate,startDate,false); // need to divide by duration to get value
				if (duration != 0) // avoid divide by zero
					v /= (((double)duration) / CalendarOption.getInstance().getMillisPerDay());
//				else if (origin == 0) // for bars
//					return;
			}
				
			setValue(startDate,v);
			setValue(endDate,-v);
			
		} else {
//System.out.println("start " + new Date(startDate) + " end " + new Date(endDate) + " value" + value );//+ " v/s " + v/s + " cal " + DurationFormat.format(duration));		
			setValue(startDate,0);
			setValue(endDate,value);
		}
	}
	
//	public void finish() {
//		Long[] d = new Long[values.size()];
//		Double[] v = new Double[values.size()];
//		values.keySet().toArray(d);
//		values.values().toArray(v);
//		dates = new Long[d.length*2];
//		vals = new Double[d.length*2];
//		Double previous = new Double(0);
//		double sum = 0;
//		for (int i = 0; i < d.length; i++) {
//			dates[2*i] = d[i];
//			dates[2*i+1] = d[i];
//			vals[2*i] = previous;
//			sum += v[i].doubleValue();
//			
//			vals[2*i+1] = new Double(sum);
//			previous = vals[2*i+1];
//		}
//		
//		//makeCumulative(true); // converts + and - into correct values
//	}
	

	public void makeSeries(boolean cumulative, SeriesCallback callback) {
		double sum = 0;
		int index = 0;
		if (cumulative) {
			for (Map.Entry<Long, Double> entry : values.entrySet()) {
				sum += entry.getValue();
				callback.add(index++, entry.getKey(), sum);
			}
		} else {
			for (Map.Entry<Long, Double> entry : values.entrySet()) {
				callback.add(index++, entry.getKey(), sum);
				sum += entry.getValue();
				callback.add(index++, entry.getKey(), sum);
			}
		}
	}
	public void makeRectilinearSeries(SeriesCallback callback) {
		makeSeries(false,callback);
		
	}

	public void makeContiguousNonZero(IntervalCallback callback, WorkCalendar workCalendar) {
		double sum = 0;
		Map.Entry<Long, Double> previous = null;
		int index = 0;
		for (Map.Entry<Long, Double> entry : values.entrySet()) {
			if (previous != null)
				callback.add(values.size() - 1 - index, previous.getKey(), entry.getKey(), sum);
			sum += entry.getValue();
			previous = entry;
			index++;
		}
	}	
	public void makeCumulative(boolean cumulative) {
		double sum = 0;
		for (Map.Entry<Long, Double> entry : values.entrySet()) {
			double current = entry.getValue();
			if (cumulative) {
				sum += current;
				entry.setValue(sum);
			} else {
				entry.setValue(current - sum);
				sum = current;
			}
		}
		indexedDates = null;
		indexedValues = null;
	}	
	
	
	public Long getDate(int index) {
		ensureIndexedValues();
		return indexedDates[index];
	}
	
	public Double getValue(int index) {
		ensureIndexedValues();
		return indexedValues[index] / yScale;
	}

	private void ensureIndexedValues() {
		if (indexedDates != null)
			return;
		long[] dates = new long[values.size()];
		double[] calculatedValues = new double[values.size()];
		int index = 0;
		for (Map.Entry<Long, Double> entry : values.entrySet()) {
			dates[index] = entry.getKey();
			calculatedValues[index] = entry.getValue();
			index++;
		}
		indexedValues = calculatedValues;
		indexedDates = dates;
	}
	

 
	public void dump() {
		int index = 0;
		for (Map.Entry<Long, Double> entry : values.entrySet())
			logger.fine(index++ + " " + new java.util.Date(entry.getKey()) + " " + entry.getValue());
 	}
	

	
}
