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
 * Gantt chart display options.
 * This class manages the contiguous completion bar rendering setting.
 * <p>
 * The class follows a singleton pattern via {@link #getInstance()}, but the public constructor
 * allows creating independent instances for testing or isolated configurations.
 * </p>
 */
public class GanttOption {
	private static GanttOption instance = null;

	/**
	 * Returns the shared singleton instance. Creates it on first call.
	 *
	 * @return the singleton instance
	 */
	public static GanttOption getInstance() {
		if (instance == null)
			instance = new GanttOption();
		return instance;
	}

	/** Whether completion bars are drawn contiguously across non-working time (default false). */
	private boolean completionIsContiguous = false;

	/**
	 * Creates a new instance with default values. This constructor is public to allow
	 * independent instances for testing or isolated configurations, separate from the
	 * singleton returned by {@link #getInstance()}.
	 */
	public GanttOption() {
		super();
	}

	/**
	 * Returns whether completion bars are drawn contiguously across non-working time.
	 *
	 * @return true if completion bars are contiguous, false otherwise
	 */
	public final boolean isCompletionIsContiguous() {
		return completionIsContiguous;
	}

	/**
	 * Sets whether completion bars are drawn contiguously across non-working time.
	 *
	 * @param completionIsContiguous true to draw bars contiguously, false to hide on non-working time
	 */
	public final void setCompletionIsContiguous(boolean completionIsContiguous) {
		this.completionIsContiguous = completionIsContiguous;
	}

}
