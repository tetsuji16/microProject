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

import com.microproject.model.snapshot.Snapshottable;

/**
 * Calculation options corresponding to the Calculation tab in MS Project options dialog.
 * This class manages earned value baseline selection, critical path calculation modes,
 * and critical slack threshold.
 * <p>
 * The class follows a singleton pattern via {@link #getInstance()}, but the public constructor
 * allows creating independent instances for testing or isolated configurations.
 * </p>
 * <p>
 * Note: {@link #earnedValueBaselineId} is intentionally typed as {@code Object} to maintain
 * serialization compatibility. The default value is {@link Snapshottable#BASELINE} (Integer.valueOf(0)),
 * but any Object can be set and retrieved unchanged.
 * </p>
 */
public class CalculationOption {
	private static CalculationOption instance = null;

	/**
	 * Returns the shared singleton instance. Creates it on first call.
	 *
	 * @return the singleton instance
	 */
	public static CalculationOption getInstance() {
		if (instance == null)
			instance = new CalculationOption();
		return instance;
	}

	/**
	 * Creates a new instance with default values. This constructor is public to allow
	 * independent instances for testing or isolated configurations, separate from the
	 * singleton returned by {@link #getInstance()}.
	 */
	public CalculationOption() {
		super();
	}

	/** The earned value baseline identifier (default {@link Snapshottable#BASELINE}, type {@code Object} for serialization compatibility). */
	Object earnedValueBaselineId = Snapshottable.BASELINE;

	/** Whether tasks without successors are considered critical (default false). */
	boolean calculateMultipleCriticalPaths = false;

	/** Critical slack threshold in milliseconds (default 0L). */
	long criticalSlackThreshold = 0L;

	/**
	 * Returns the earned value baseline identifier.
	 *
	 * @return the baseline identifier (default {@link Snapshottable#BASELINE}), never null
	 */
	public Object getEarnedValueBaselineId() {
		return earnedValueBaselineId;
	}

	/**
	 * Sets the earned value baseline identifier.
	 * <p>
	 * The parameter type is {@code Object} to maintain serialization compatibility.
	 * Any object can be passed and will be returned unchanged by {@link #getEarnedValueBaselineId()}.
	 *
	 * @param earnedValueBaselineId the baseline identifier to set
	 */
	public void setEarnedValueBaselineId(Object earnedValueBaselineId) {
		this.earnedValueBaselineId = earnedValueBaselineId;
	}

	/**
	 * Returns whether tasks without successors are considered critical.
	 *
	 * @return true if multiple critical paths are calculated, false otherwise
	 */
	public boolean isCalculateMultipleCriticalPaths() {
		return calculateMultipleCriticalPaths;
	}

	/**
	 * Sets whether tasks without successors are considered critical.
	 *
	 * @param calculateMultipleCriticalPaths true to treat tasks without successors as critical
	 */
	public void setCalculateMultipleCriticalPaths(
			boolean calculateMultipleCriticalPaths) {
		this.calculateMultipleCriticalPaths = calculateMultipleCriticalPaths;
	}

	/**
	 * Returns the critical slack threshold in milliseconds.
	 *
	 * @return the critical slack threshold (default 0L)
	 */
	public long getCriticalSlackThreshold() {
		return criticalSlackThreshold;
	}

	/**
	 * Sets the critical slack threshold in milliseconds.
	 *
	 * @param criticalSlackThreshold the threshold in milliseconds
	 */
	public void setCriticalSlackThreshold(long criticalSlackThreshold) {
		this.criticalSlackThreshold = criticalSlackThreshold;
	}
}
