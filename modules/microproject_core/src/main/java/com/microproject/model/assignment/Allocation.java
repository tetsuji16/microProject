/*******************************************************************************
 * MIT License
 *
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
package com.microproject.model.assignment;

import com.microproject.field.FieldContext;

/**
 * Shared allocation contract for reconciling work, units, and remaining duration.
 * Scheduling rules use these operations for both tasks and assignments.
 */
public interface Allocation {
	/** Adjusts the remaining duration and, when requested, its child allocations. */
	void adjustRemainingDuration(long newDuration, boolean doChildren);
	/** Adjusts remaining units, optionally conserving the total units across children. */
	void adjustRemainingUnits(double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits);
	/** Multiplies remaining work by the supplied factor, optionally including children. */
	void adjustRemainingWork(double multiplier, boolean doChildren);
//	long calcWork();
	/** Returns work not yet completed. */
	long getRemainingWork();
	/** Returns the allocation's total units. */
	double getUnits();
	/** Returns units not yet completed. */
	double getRemainingUnits();
	/** Reports whether the units field is read-only in the supplied field context. */
	boolean isReadOnlyUnits(FieldContext fieldContext);
//	long getDurationMillis();
	/** Returns the remaining duration in milliseconds. */
	long getRemainingDuration();
	/** Returns the greatest units value among this allocation's assignments. */
	double getMostLoadedAssignmentUnits();	
}
