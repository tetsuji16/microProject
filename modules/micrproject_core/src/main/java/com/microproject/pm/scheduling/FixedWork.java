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

/**
 * @stereotype strategy 
 *  * In a... If you revise units... If you revise duration... If you revise work... 
 *  * Fixed-work task Duration is recalculated. Units are recalculated. Duration is recalculated. 
 * 
 */
public class FixedWork implements SchedulingRule {
	public String toString() {
		return Messages.getString("FixedWork.FixedWork"); //$NON-NLS-1$
	}

	public void adjustRemainingUnits(Allocation allocation, double newRemainingUnits, double oldRemainingUnits, boolean doChildren, boolean conserveTotalUnits) {
		FixedUnits.getInstance().adjustRemainingUnits(allocation,newRemainingUnits, oldRemainingUnits, false, false);
	}
	
	public void adjustRemainingDuration(Allocation allocation, long newRemainingDuration, boolean doChildren) {
		if (newRemainingDuration != 0) // avoid degenerate case
			allocation.adjustRemainingUnits(allocation.getRemainingUnits() * ((double) allocation.getRemainingDuration()) / Duration.millis(newRemainingDuration), allocation.getRemainingUnits(), doChildren, false);
		allocation.adjustRemainingDuration(newRemainingDuration, doChildren);
	}

	/* 
	 * If you revise work, Duration is recalculated
	 */
	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, boolean doChildren) {
		FixedUnits.getInstance().adjustRemainingWork(allocation,newRemainingWork, doChildren);
	}

	public void adjustRemainingWork(Allocation allocation, long newRemainingWork, long oldRemainingWork, boolean doChildren) {
		FixedUnits.getInstance().adjustRemainingWork(allocation,newRemainingWork, oldRemainingWork, doChildren);
	}

	
	private FixedWork() {}
	private static FixedWork instance = null;
	
	public static FixedWork getInstance() {
		if (instance == null)
			instance = new FixedWork();
		return instance;
	}

}
