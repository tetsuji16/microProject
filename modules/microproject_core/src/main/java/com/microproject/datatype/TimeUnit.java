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
package com.microproject.datatype;

/**
 * Code taken from mpxj
 */
public interface TimeUnit {
	
	   public static final int NON_TEMPORAL = -2; // projectlibre specific to indicate the value is not temporal
		
	   public static final int NONE = -1; // projectlibre specific to indicate no value entered
	   
	   /**
	    * Constant representing Minutes
	    */
	   public static final int MINUTES = 0;

	   /**
	    * Constant representing Hours
	    */
	   public static final int HOURS = 1;

	   /**
	    * Constant representing Days
	    */
	   public static final int DAYS = 2;

	   /**
	    * Constant representing Weeks
	    */
	   public static final int WEEKS = 3;

	   /**
	    * Constant representing Months
	    */
	   public static final int MONTHS = 4;

	   /**
	    * Constant representing Years
	    */
	   public static final int YEARS = 5;

	   /**
	    * Constant representing Percent
	    */
	   public static final int PERCENT = 6;

	   /**
	    * Constant representing Elapsed Minutes
	    */
	   public static final int ELAPSED_MINUTES = 7;

	   /**
	    * Constant representing Elapsed Hours
	    */
	   public static final int ELAPSED_HOURS = 8;

	   /**
	    * Constant representing Elapsed Days
	    */
	   public static final int ELAPSED_DAYS = 9;

	   /**
	    * Constant representing Elapsed Weeks
	    */
	   public static final int ELAPSED_WEEKS = 10;

	   /**
	    * Constant representing Elapsed Months
	    */
	   public static final int ELAPSED_MONTHS = 11;

	   /**
	    * Constant representing Elapsed Years
	    */
	   public static final int ELAPSED_YEARS = 12;

	   /**
	    * Constant representing Elapsed Percent
	    */
	   public static final int ELAPSED_PERCENT = 13;

}
