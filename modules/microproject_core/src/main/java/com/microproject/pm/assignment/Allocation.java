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

import com.microproject.field.FieldContext;

/**
 * Interface shared by tasks and assignments to handle adjusting the work, units, duration relationship.  These methods
 * are primarily called by SchedulingRule subclasses
 */
public interface Allocation {
	void adjustRemainingDuration(long newDuration, boolean doChildren);
	void adjustRemainingUnits(double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits);
	void adjustRemainingWork(double multiplier, boolean doChildren);
//	long calcWork();
	long getRemainingWork();
	double getUnits();
	double getRemainingUnits();
	boolean isReadOnlyUnits(FieldContext fieldContext);
//	long getDurationMillis();
	long getRemainingDuration();
	double getMostLoadedAssignmentUnits();	
}
