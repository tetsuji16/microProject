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
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

/**
 * @stereotype strategy 
 *  * In a... If you revise units... If you revise duration... If you revise work... 
 * Fixed-duration task Work is recalculated. Work is recalculated. Units are recalculated. 
 */
public class FixedDuration implements SchedulingRule {
	public String toString() {
		return Messages.getString("FixedDuration.FixedDuration"); //$NON-NLS-1$
	}
	/*
	 * If you revise units, Work is recalculated
	 */
	public void adjustRemainingUnits(Allocation allocation, double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits) {
		allocation.adjustRemainingUnits(newRemainingUnits, oldRemainingUnits, doChildren, conserveTotalUnits);
		
	}
	/* 
	 * If you revise duration, Work is recalculated
	 */
	public void adjustRemainingDuration(Allocation allocation, long newRemainingDuration, boolean doChildren) {
		allocation.adjustRemainingDuration(Duration.millis(newRemainingDuration), doChildren);
	}

	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, boolean doChildren) {
		long oldRemainingWork = allocation.getRemainingWork();
		adjustRemainingWork(allocation,newRemainingWork,oldRemainingWork,doChildren);
	}

	/* 
	 * If you revise work, Units are recalculated
	 */
	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, long oldRemainingWork,boolean doChildren) {
		newRemainingWork = Duration.millis(newRemainingWork);
		double remainingUnits = allocation.getRemainingUnits();
		if (oldRemainingWork == 0) { // degenerate case
			allocation.adjustRemainingDuration((long) (newRemainingWork / remainingUnits), doChildren);
			Alert.warn(Messages.getString("FixedDuration.TheDurationMessage")); //$NON-NLS-1$
		} else {
			allocation.adjustRemainingUnits(remainingUnits * ((double)newRemainingWork) / oldRemainingWork, remainingUnits, doChildren, false);
		}
	}
	
	private FixedDuration() {}
	private static FixedDuration instance = null;
	
	public static FixedDuration getInstance() {
		if (instance == null)
			instance = new FixedDuration();
		return instance;
	}

}
