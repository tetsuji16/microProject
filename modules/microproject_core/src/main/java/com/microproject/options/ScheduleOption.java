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
package com.microproject.options;

import com.microproject.datatype.TimeUnit;
import com.microproject.pm.scheduling.SchedulingType;

/**
 * Corresponds to Schedule tab on options dialog in MSProject
 */
public class ScheduleOption {
	private static ScheduleOption instance = null;
	public static ScheduleOption getInstance() {
		if (instance == null)
			instance = new ScheduleOption();
		return instance;
	}
	
	int schedulingRule = SchedulingType.FIXED_UNITS;
	boolean effortDriven = true;
	int durationEnteredIn = TimeUnit.DAYS;
	int workUnit = TimeUnit.HOURS;
	int rateEnteredIn = TimeUnit.HOURS;
	boolean newTasksStartToday = false; // default is project start;
	/**
	 * @return Returns the schedulingRule.
	 */
	public int getSchedulingRule() {
		return schedulingRule;
	}

	/**
	 * @param schedulingRule The schedulingRule to set.
	 */
	public void setSchedulingRule(int schedulingRule) {
		this.schedulingRule = schedulingRule;
	}

	/**
	 * @return Returns the effortDriven.
	 */
	public boolean isEffortDriven() {
		return effortDriven;
	}

	/**
	 * @param effortDriven The effortDriven to set.
	 */
	public void setEffortDriven(boolean effortDriven) {
		this.effortDriven = effortDriven;
	}

	/**
	 * @return Returns the durationEnteredIn.
	 */
	public int getDurationEnteredIn() {
		return durationEnteredIn;
	}
	/**
	 * @param durationEnteredIn The durationEnteredIn to set.
	 */
	public void setDurationEnteredIn(int durationEnteredIn) {
		this.durationEnteredIn = durationEnteredIn;
	}

	private boolean honorRequiredDates = true;
	/**
	 * @return Returns the honorRequiredDates.
	 */
	public boolean isHonorRequiredDates() {
		return honorRequiredDates;
	}

	/**
	 * @param honorRequiredDates The honorRequiredDates to set.
	 */
	public void setHonorRequiredDates(boolean honorRequiredDates) {
		this.honorRequiredDates = honorRequiredDates;
	}
	/**
	 * @return Returns the effortDisplay.
	 */
	public int getWorkUnit() {
		return workUnit;
	}
	/**
	 * @param effortDisplay The effortDisplay to set.
	 */
	public void setEffortDisplay(int effortDisplay) {
		this.workUnit = effortDisplay;
	}
	/**
	 * @return Returns the newTasksStartToday.
	 */
	public boolean isNewTasksStartToday() {
		return newTasksStartToday;
	}
	/**
	 * @param newTasksStartToday The newTasksStartToday to set.
	 */
	public void setNewTasksStartToday(boolean newTasksStartToday) {
		this.newTasksStartToday = newTasksStartToday;
	}

	public final int getRateEnteredIn() {
		return rateEnteredIn;
	}

	public final void setRateEnteredIn(int rateEnteredIn) {
		this.rateEnteredIn = rateEnteredIn;
	}
}
