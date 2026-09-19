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
package com.microproject.core.time;

import java.util.HashMap;
import java.util.Map;


// 

/**
 * @author Laurent Chretienneau
 * 
 * contants from mpxj with 2 additional ones from ProjectLibre
 */
/** @deprecated Use {@code com.microproject.datatype.TimeUnit} in domain code. Kept for MPXJ compatibility. */
@Deprecated(forRemoval = false)
public enum TimeUnit{
	
	/**
	 * projectlibre specific to indicate the value is not temporal
	 */
	NON_TEMPORAL(-2,""), //-2 in ProjectLibre

	/**
	 * projectlibre specific to indicate no value entered
	 */
	NONE(-1,""), //-1 in ProjectLibre
	
	/**
	 * Constant representing Minutes.
	 */
	MINUTES(0, "m"),

	/**
	 * Constant representing Hours.
	 */
	HOURS(1, "h"),

	/**
	 * Constant representing Days.
	 */
	DAYS(2, "d"),

	/**
	 * Constant representing Weeks.
	 */
	WEEKS(3, "w"),

	/**
	 * Constant representing Months.
	 */
	MONTHS(4, "mo"),

	/**
	 * Constant representing Percent.
	 */
	PERCENT(5, "%"),

	/**
	 * Constant representing Years.
	 */
	YEARS(6, "y"),

	/**
	 * Constant representing Elapsed Minutes.
	 */
	ELAPSED_MINUTES(7, "em"),

	/**
	 * Constant representing Elapsed Hours.
	 */
	ELAPSED_HOURS(8, "eh"),

	/**
	 * Constant representing Elapsed Days.
	 */
	ELAPSED_DAYS(9, "ed"),

	/**
	 * Constant representing Elapsed Weeks.
	 */
	ELAPSED_WEEKS(10, "ew"),

	/**
	 * Constant representing Elapsed Months.
	 */
	ELAPSED_MONTHS(11, "emo"),

	/**
	 * Constant representing Elapsed Years.
	 */
	ELAPSED_YEARS(12, "ey"),

	/**
	 * Constant representing Elapsed Percent.
	 */
	ELAPSED_PERCENT(13, "e%");


	protected int id;
	protected String symbol;
	private static final Map<Integer, TimeUnit> REVERSE_MAP = createReverseMap();
	
	private TimeUnit(int id, String symbol) {
		this.id=id;
		this.symbol=symbol;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public static TimeUnit getInstance(int id){
		//not using values()[id] because type can be negative
		return REVERSE_MAP.get(id);
	}	

	private static Map<Integer, TimeUnit> createReverseMap() {
		Map<Integer, TimeUnit> result = new HashMap<>(values().length);
		for (TimeUnit unit : values()) result.put(unit.getId(), unit);
		return Map.copyOf(result);
	}
	

}
