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
package com.microproject.model.calendar;

/** Describes the effective workday selected for a calendar date. */
public class DayDescriptor {
	WorkDay workDay = null;
	boolean modified = false;

	/** Returns whether the described day has working time. */
	public boolean isWorking() {
		if (workDay == null)
			return false;
		return workDay.isWorking();
	}

	/** Returns whether this day differs from its inherited or default value. */
	public boolean isModified() {
		return modified;
	}

	/** Returns the day's effective working hours, or {@code null} when absent. */
	public WorkingHours getWorkingHours() {
		if (workDay == null)
			return null;
		return workDay.getWorkingHours();
	}

	/** Returns the workday and override state for diagnostics. */
	public String toString() {
		return workDay + "\nmodified=" + modified;
	}
}
