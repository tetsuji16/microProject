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
package com.microproject.pm.task;

import java.io.Serializable;
import java.util.Objects;

/** Persisted display options for Microsoft Project-style Gantt progress lines. */
public record ProgressLineSettings(
		boolean showCurrentLine,
		ReferenceDate referenceDate,
		boolean showRecurringLines,
		int recurringInterval,
		IntervalUnit recurringIntervalUnit) implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final int MAX_RECURRING_INTERVAL = 3650;

	public ProgressLineSettings {
		Objects.requireNonNull(referenceDate, "referenceDate");
		Objects.requireNonNull(recurringIntervalUnit, "recurringIntervalUnit");
		if (recurringInterval < 1 || recurringInterval > MAX_RECURRING_INTERVAL)
			throw new IllegalArgumentException("recurringInterval must be between 1 and " + MAX_RECURRING_INTERVAL);
	}

	public static ProgressLineSettings defaults() {
		return new ProgressLineSettings(false, ReferenceDate.STATUS_DATE, false, 1, IntervalUnit.WEEK);
	}

	public enum ReferenceDate { STATUS_DATE, CURRENT_DATE }
	public enum IntervalUnit { DAY, WEEK, MONTH }
}
