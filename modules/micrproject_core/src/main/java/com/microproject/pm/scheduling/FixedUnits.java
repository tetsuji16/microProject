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
package com.microproject.pm.scheduling;

import com.microproject.datatype.Duration;
import com.microproject.pm.assignment.Allocation;
import com.microproject.pm.assignment.Assignment;
import com.microproject.strings.Messages;

/**
 * @stereotype strategy
 * Work = Units * Duration
 * In a... If you revise units... If you revise duration... If you revise work... 
 * Fixed-units task Duration is recalculated. Work is recalculated. Duration is recalculated. 
 */

public class FixedUnits implements SchedulingRule {
	public String toString() {
		return Messages.getString("FixedUnits.FixedUnits"); //$NON-NLS-1$
	}

	/* 
	 * If you revise units, Duration is recalculated. They are inversely proportional
	 */
	public void adjustRemainingUnits(Allocation allocation, double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits) {
		Assignment result;
		if (newRemainingUnits == 0.0) {// treat degenerate case.  Assigning 0 to units makes the task a milestone, but units becomes 1.0
			allocation.adjustRemainingDuration(0, doChildren);
			allocation.adjustRemainingUnits(1.0, oldRemainingUnits, doChildren, false);
		} else {
			if (oldRemainingUnits == 0.0) {// special case
				allocation.adjustRemainingUnits(1.0, oldRemainingUnits, false, false);
			} else {
				allocation.adjustRemainingWork(oldRemainingUnits / newRemainingUnits, doChildren);
			}
		}
	}
	
	/*
	 * If you revise duration, Work is recalculated
	 */
	public void adjustRemainingDuration(Allocation allocation, long newRemainingDuration, boolean doChildren) {
		allocation.adjustRemainingDuration(newRemainingDuration, doChildren);
	}

	/* 
	 * If you revise work, Duration is recalculated
	 */
	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, boolean doChildren) {
		long oldRemainingWork = allocation.getRemainingWork();
		adjustRemainingWork(allocation,newRemainingWork,oldRemainingWork,doChildren);
	}

	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, long oldRemainingWork, boolean doChildren) {
		long newDuration;
		if (oldRemainingWork == 0) { // degenerate case
			newDuration = (long) (newRemainingWork / allocation.getRemainingUnits());
		} else {
			newRemainingWork = Duration.millis(newRemainingWork);
			newDuration = (long) (allocation.getRemainingDuration() * ((double)newRemainingWork) / oldRemainingWork);
		}
		allocation.adjustRemainingDuration(newDuration, doChildren);
	}	

	
	private FixedUnits() {}
	private static FixedUnits instance = null;
	
	public static FixedUnits getInstance() {
		if (instance == null)
			instance = new FixedUnits();
		return instance;
	}

}
