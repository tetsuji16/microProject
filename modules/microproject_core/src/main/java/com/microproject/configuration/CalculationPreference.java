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
package com.microproject.configuration;

/**
 * These values represent overall behavior of product. These options are not user modifiable.
 */
public class CalculationPreference {
	private boolean assignmentDurationExcludesNonWorkPeriods;
	private boolean nonWorkContourPeriodsStayFixedLength;
	private double earnedValueDivideByZeroValue = 0;

	/**
	 * @return Returns the assignmentDurationExcludesNonWorkPeriods.
	 */
	public boolean isAssignmentDurationExcludesNonWorkPeriods() {
		return assignmentDurationExcludesNonWorkPeriods;
	}

	/**
	 * @return Returns the nonWorkContourPeriodsStayFixedLength.
	 */
	public boolean isNonWorkContourPeriodsStayFixedLength() {
		return nonWorkContourPeriodsStayFixedLength;
	}

	public static final CalculationPreference MS_PROJECT = new CalculationPreference();
	static {
		MS_PROJECT.assignmentDurationExcludesNonWorkPeriods = true;
		MS_PROJECT.nonWorkContourPeriodsStayFixedLength = true;
	}
	
	private static CalculationPreference active = MS_PROJECT;
	/**
	 * @return Returns the active.
	 */
	public static CalculationPreference getActive() {
		return active;
	}

	/**
	 * @return Returns the earnedValueDivideByZeroValue.
	 */
	public double getEarnedValueDivideByZeroValue() {
		return earnedValueDivideByZeroValue;
	}
	/**
	 * @param earnedValueDivideByZeroValue The earnedValueDivideByZeroValue to set.
	 */
	public void setEarnedValueDivideByZeroValue(
			double earnedValueDivideByZeroValue) {
		this.earnedValueDivideByZeroValue = earnedValueDivideByZeroValue;
	}
}
