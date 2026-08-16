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
package com.microproject.exchange;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;

/**
 * 
 */
public class TimeDistributedTypeMapperConstants {
	public static final int ASSIGNMENT_REMAINING_WORK = 1;
	public static final int ASSIGNMENT_ACTUAL_WORK = 2;
	public static final int ASSIGNMENT_ACTUAL_OVERTIME_WORK = 3;
	public static final int ASSIGNMENT_BASELINE_WORK = 4;
	
	/**
	 * Get the msdi enumeration for a time distributed field.  See the PJXML_2003 help file for details on the enumeration
	 * @param numBaseline
	 * @param cost
	 * @param obj
	 * @return msdi type
	 */
	public static int getTimeDistributedType(int numBaseline, boolean cost, Object obj) {
		int value = 0; 
		if (numBaseline == 0) {
			value = 4;
			if (obj instanceof Resource)
				value += 3;
			else if (obj instanceof Task)
				value += 5;
			
		} else {
			value = 10 + 6 * numBaseline;
			if (obj instanceof Task)
				value += 2;
			else if (obj instanceof Resource)
				value += 4;
		}
		if (cost)
			value++; // cost fields always one more than work
		return value;
	}

}
