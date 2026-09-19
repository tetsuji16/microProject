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
package com.microproject.util;


/**
 *
 */
public class MathUtils {
	/** 
	 * Collapse number down to +1 0 or -1 depending on sign. 
	 * Typically used in compare routines to collapse a difference 
	 * of two longs to an int. 
	 * 
	 * @param diff usually represents the difference of two longs. 
	 * 
	 * @return signum of diff, +1, 0 or -1. 
	 */ 
	public static final int signum( long diff ) 
	{ 
		if ( diff > 0 ) return 1; 
		if ( diff < 0 ) return -1 ; 
		else return 0; 
	} // end signum 

	public static final int signum( double diff ) 
	{ 
		if ( diff > 0 ) return 1; 
		if ( diff < 0 ) return -1 ; 
		else return 0; 
	} // end signum 

	public static final double inRange(double value, double min, double max) {
		if (value < min)
			return min;
		if (value > max)
			return max;
		return value;
	}
	
	private static double roundValue = 1000000.0D;
	public static double roundToDecentPrecision(double value) {
		double z = Math.round(roundValue * value);
		return z / roundValue;
	}
}

