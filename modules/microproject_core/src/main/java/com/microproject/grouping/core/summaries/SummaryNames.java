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
package com.microproject.grouping.core.summaries;

/**
 *
 */
public interface SummaryNames {
	public static int MAXIMUM = 0; //"Maximum";
	public static int OR = 0; // "OR";
	public static int MINIMUM = 1; //"Minimum";
	public static int AND = 1; //"AND";
	public static int COUNT_ALL = 2; // "Count All";
	public static int SUM = 3; //"Sum";	
	public static int AVERAGE = 4; //"Average";
	public static int AVERAGE_FIRST_SUBLEVEL = 5; //"Average First Sublevel";	
	public static int COUNT_FIRST_SUBLEVEL = 6; //"Count All";	
	public static int COUNT_NONSUMMARIES = 7; //"Count Nonsummaries";
	public static int SAME = 8; //"Groups";
	public static int NONE = -1; //"None";	
	public static int LIST = -2; //"List";
	public static int CUSTOM = -3; //"Custom";	
	public static int THIS = -4; //"This";
}

