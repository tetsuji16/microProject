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
package com.microproject.options;

/**
 * Advanced options corresponding to the Calculation tab in MS Project options dialog.
 * This class manages settings for time-phased actuals and earned value field calculations.
 * <p>
 * The class follows a singleton pattern via {@link #getInstance()}, but the public constructor
 * allows creating independent instances for testing or isolated configurations.
 * </p>
 */
public class AdvancedOption {

	/** Whether time-phased actuals can set the task start date (default true, matches MSP behavior). */
	private boolean timephasedActualsCanSetTaskStart = true;

	/** Whether earned value fields are cumulative (default true, matches MSP behavior). */
	private boolean earnedValueFieldsCumulative = true;

	private static AdvancedOption instance = null;

	/**
	 * Returns whether earned value fields are calculated cumulatively.
	 *
	 * @return true if earned value fields are cumulative, false otherwise
	 */
	public final boolean isEarnedValueFieldsCumulative() {
		return earnedValueFieldsCumulative;
	}

	/**
	 * Sets whether earned value fields are calculated cumulatively.
	 *
	 * @param earnedValueFieldsCumulative true for cumulative calculation, false otherwise
	 */
	public final void setEarnedValueFieldsCumulative(boolean earnedValueFieldsCumulative) {
		this.earnedValueFieldsCumulative = earnedValueFieldsCumulative;
	}

	/**
	 * Returns whether time-phased actuals can set the task start date.
	 *
	 * @return true if time-phased actuals can set task start, false otherwise
	 */
	public final boolean isTimephasedActualsCanSetTaskStart() {
		return timephasedActualsCanSetTaskStart;
	}

	/**
	 * Sets whether time-phased actuals can set the task start date.
	 *
	 * @param timephasedActualsCanSetTaskStart true to allow setting task start from actuals
	 */
	public final void setTimephasedActualsCanSetTaskStart(boolean timephasedActualsCanSetTaskStart) {
		this.timephasedActualsCanSetTaskStart = timephasedActualsCanSetTaskStart;
	}

	/**
	 * Returns the shared singleton instance. Creates it on first call.
	 *
	 * @return the singleton instance
	 */
	public static AdvancedOption getInstance() {
		if (instance == null)
			instance = new AdvancedOption();
		return instance;
	}

	/**
	 * Creates a new instance with default values. This constructor is public to allow
	 * independent instances for testing or isolated configurations, separate from the
	 * singleton returned by {@link #getInstance()}.
	 */
	public AdvancedOption() {
		super();
	}

}
